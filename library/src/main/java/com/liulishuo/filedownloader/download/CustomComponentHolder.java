/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.download;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.database.FileDownloadDatabase;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;
import com.liulishuo.filedownloader.services.ForegroundServiceConfig;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.util.FileDownloadExecutors;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The holder for supported custom components.
 */
public class CustomComponentHolder {
    private DownloadMgrInitialParams initialParams;

    private FileDownloadHelper.ConnectionCountAdapter connectionCountAdapter;
    private FileDownloadHelper.ConnectionCreator connectionCreator;
    private FileDownloadHelper.OutputStreamCreator outputStreamCreator;
    private FileDownloadDatabase database;
    private FileDownloadHelper.IdGenerator idGenerator;
    private ForegroundServiceConfig foregroundServiceConfig;

    private static final class LazyLoader {
        private static final CustomComponentHolder INSTANCE = new CustomComponentHolder();
    }

    public static CustomComponentHolder getImpl() {
        return LazyLoader.INSTANCE;
    }

    public void setInitCustomMaker(DownloadMgrInitialParams.InitCustomMaker initCustomMaker) {
        synchronized (this) {
            initialParams = new DownloadMgrInitialParams(initCustomMaker);
            connectionCreator = null;
            outputStreamCreator = null;
            database = null;
            idGenerator = null;
        }
    }

    public FileDownloadConnection createConnection(String url) throws IOException {
        return getConnectionCreator().create(url);
    }

    public FileDownloadOutputStream createOutputStream(File file) throws IOException {
        return getOutputStreamCreator().create(file);
    }

    public FileDownloadHelper.IdGenerator getIdGeneratorInstance() {
        if (idGenerator != null) return idGenerator;

        synchronized (this) {
            if (idGenerator == null) {
                idGenerator = getDownloadMgrInitialParams().createIdGenerator();
            }
        }

        return idGenerator;
    }

    public FileDownloadDatabase getDatabaseInstance() {
        if (database != null) return database;

        synchronized (this) {
            if (database == null) {
                database = getDownloadMgrInitialParams().createDatabase();
                maintainDatabase(database.maintainer());
            }
        }

        return database;
    }

    public ForegroundServiceConfig getForegroundConfigInstance() {
        if (foregroundServiceConfig != null) return foregroundServiceConfig;

        synchronized (this) {
            if (foregroundServiceConfig == null) {
                foregroundServiceConfig = getDownloadMgrInitialParams()
                        .createForegroundServiceConfig();
            }
        }

        return foregroundServiceConfig;
    }

    public int getMaxNetworkThreadCount() {
        return getDownloadMgrInitialParams().getMaxNetworkThreadCount();
    }

    public boolean isSupportSeek() {
        return getOutputStreamCreator().supportSeek();
    }

    public int determineConnectionCount(int downloadId, String url, String path, long totalLength) {
        return getConnectionCountAdapter()
                .determineConnectionCount(downloadId, url, path, totalLength);
    }

    private FileDownloadHelper.ConnectionCountAdapter getConnectionCountAdapter() {
        if (connectionCountAdapter != null) return connectionCountAdapter;

        synchronized (this) {
            if (connectionCountAdapter == null) {
                connectionCountAdapter = getDownloadMgrInitialParams()
                        .createConnectionCountAdapter();
            }
        }

        return connectionCountAdapter;
    }

    private FileDownloadHelper.ConnectionCreator getConnectionCreator() {
        if (connectionCreator != null) return connectionCreator;

        synchronized (this) {
            if (connectionCreator == null) {
                connectionCreator = getDownloadMgrInitialParams().createConnectionCreator();
            }
        }

        return connectionCreator;
    }

    private FileDownloadHelper.OutputStreamCreator getOutputStreamCreator() {
        if (outputStreamCreator != null) return outputStreamCreator;

        synchronized (this) {
            if (outputStreamCreator == null) {
                outputStreamCreator = getDownloadMgrInitialParams().createOutputStreamCreator();
            }
        }

        return outputStreamCreator;
    }

    private DownloadMgrInitialParams getDownloadMgrInitialParams() {
        if (initialParams != null) return initialParams;

        synchronized (this) {
            if (initialParams == null) initialParams = new DownloadMgrInitialParams();
        }

        return initialParams;
    }

    private static void maintainDatabase(final FileDownloadDatabase.Maintainer maintainer) {
        final Iterator<FileDownloadModel> iterator = maintainer.iterator();
        final AtomicInteger removedDataCount = new AtomicInteger(0);
        final AtomicInteger resetIdCount = new AtomicInteger(0);
        final AtomicInteger refreshDataCount = new AtomicInteger(0);
        final FileDownloadHelper.IdGenerator idGenerator = getImpl().getIdGeneratorInstance();

        final long startTimestamp = System.currentTimeMillis();
        final List<Future> futures = new ArrayList<>();
        final ThreadPoolExecutor maintainThreadPool = FileDownloadExecutors.newDefaultThreadPool(3,
                FileDownloadUtils.getThreadPoolName("MaintainDatabase"));
        try {
            while (iterator.hasNext()) {
                final FileDownloadModel model = iterator.next();
                final Future modelFuture = maintainThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        boolean isInvalid = false;
                        do {
                            if (model.getStatus() == FileDownloadStatus.progress
                                    || model.getStatus() == FileDownloadStatus.connected
                                    || model.getStatus() == FileDownloadStatus.error
                                    || (model.getStatus() == FileDownloadStatus.pending && model
                                    .getSoFar() > 0)
                            ) {
                                // Ensure can be covered by RESUME FROM BREAKPOINT.
                                model.setStatus(FileDownloadStatus.paused);
                            }
                            final String targetFilePath = model.getTargetFilePath();
                            if (targetFilePath == null) {
                                // no target file path, can't used to resume from breakpoint.
                                isInvalid = true;
                                break;
                            }

                            final File targetFile = new File(targetFilePath);
                            // consider check in new thread, but SQLite lock | file lock aways
                            // effect, so sync
                            if (model.getStatus() == FileDownloadStatus.paused
                                    && FileDownloadUtils.isBreakpointAvailable(model.getId(), model,
                                    model.getPath(), null)) {
                                // can be reused in the old mechanism(no-temp-file).

                                final File tempFile = new File(model.getTempFilePath());

                                if (!tempFile.exists() && targetFile.exists()) {
                                    final boolean successRename = targetFile.renameTo(tempFile);
                                    if (FileDownloadLog.NEED_LOG) {
                                        FileDownloadLog.d(FileDownloadDatabase.class,
                                                "resume from the old no-temp-file architecture "
                                                        + "[%B], [%s]->[%s]",
                                                successRename, targetFile.getPath(),
                                                tempFile.getPath());
                                    }
                                }
                            }

                            /**
                             * Remove {@code model} from DB if it can't used for judging whether the
                             * old-downloaded file is valid for reused & it can't used for resuming
                             * from BREAKPOINT, In other words, {@code model} is no use anymore for
                             * FileDownloader.
                             */
                            if (model.getStatus() == FileDownloadStatus.pending
                                    && model.getSoFar() <= 0) {
                                // This model is redundant.
                                isInvalid = true;
                                break;
                            }

                            if (!FileDownloadUtils.isBreakpointAvailable(model.getId(), model)) {
                                // It can't used to resuming from breakpoint.
                                isInvalid = true;
                                break;
                            }

                            if (targetFile.exists()) {
                                // It has already completed downloading.
                                isInvalid = true;
                                break;
                            }

                        } while (false);


                        if (isInvalid) {
                            maintainer.onRemovedInvalidData(model);
                            removedDataCount.addAndGet(1);
                        } else {
                            final int oldId = model.getId();
                            final int newId = idGenerator.transOldId(oldId, model.getUrl(),
                                    model.getPath(), model.isPathAsDirectory());
                            if (newId != oldId) {
                                if (FileDownloadLog.NEED_LOG) {
                                    FileDownloadLog.d(FileDownloadDatabase.class,
                                            "the id is changed on restoring from db:"
                                                    + " old[%d] -> new[%d]",
                                            oldId, newId);
                                }
                                model.setId(newId);
                                maintainer.changeFileDownloadModelId(oldId, model);
                                resetIdCount.addAndGet(1);
                            }

                            maintainer.onRefreshedValidData(model);
                            refreshDataCount.addAndGet(1);
                        }
                    }
                });
                futures.add(modelFuture);
            }

        } finally {
            final Future markConvertedFuture = maintainThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    FileDownloadUtils.markConverted(FileDownloadHelper.getAppContext());
                }
            });
            futures.add(markConvertedFuture);
            final Future finishMaintainFuture = maintainThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    maintainer.onFinishMaintain();
                }
            });
            futures.add(finishMaintainFuture);
            for (Future future : futures) {
                try {
                    if (!future.isDone()) future.get();
                } catch (Exception e) {
                    if (FileDownloadLog.NEED_LOG) FileDownloadLog
                            .e(FileDownloadDatabase.class, e, e.getMessage());
                }
            }
            futures.clear();
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(FileDownloadDatabase.class,
                        "refreshed data count: %d , delete data count: %d, reset id count:"
                                + " %d. consume %d",
                        refreshDataCount.get(), removedDataCount.get(), resetIdCount.get(),
                        System.currentTimeMillis() - startTimestamp);
            }
        }
    }
}