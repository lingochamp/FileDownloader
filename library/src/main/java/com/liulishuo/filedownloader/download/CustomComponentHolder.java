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
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;
import com.liulishuo.filedownloader.services.FileDownloadDatabase;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

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

    private final static class LazyLoader {
        private final static CustomComponentHolder INSTANCE = new CustomComponentHolder();
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

    public FileDownloadOutputStream createOutputStream(File file) throws FileNotFoundException {
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

    public int getMaxNetworkThreadCount() {
        return getDownloadMgrInitialParams().getMaxNetworkThreadCount();
    }

    public boolean isSupportSeek() {
        return getOutputStreamCreator().supportSeek();
    }

    public int determineConnectionCount(int downloadId, String url, String path, long totalLength) {
        return getConnectionCountAdapter().determineConnectionCount(downloadId, url, path, totalLength);
    }

    private FileDownloadHelper.ConnectionCountAdapter getConnectionCountAdapter() {
        if (connectionCountAdapter != null) return connectionCountAdapter;

        synchronized (this) {
            if (connectionCountAdapter == null)
                connectionCountAdapter = getDownloadMgrInitialParams().createConnectionCountAdapter();
        }

        return connectionCountAdapter;
    }

    private FileDownloadHelper.ConnectionCreator getConnectionCreator() {
        if (connectionCreator != null) return connectionCreator;

        synchronized (this) {
            if (connectionCreator == null)
                connectionCreator = getDownloadMgrInitialParams().createConnectionCreator();
        }

        return connectionCreator;
    }

    private FileDownloadHelper.OutputStreamCreator getOutputStreamCreator() {
        if (outputStreamCreator != null) return outputStreamCreator;

        synchronized (this) {
            if (outputStreamCreator == null)
                outputStreamCreator = getDownloadMgrInitialParams().createOutputStreamCreator();
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

    private static void maintainDatabase(FileDownloadDatabase.Maintainer maintainer) {
        final Iterator<FileDownloadModel> iterator = maintainer.iterator();
        long refreshDataCount = 0;
        long removedDataCount = 0;
        long resetIdCount = 0;
        final FileDownloadHelper.IdGenerator idGenerator = getImpl().getIdGeneratorInstance();

        final long startTimestamp = System.currentTimeMillis();
        try {
            while (iterator.hasNext()) {
                boolean isInvalid = false;
                final FileDownloadModel model = iterator.next();
                do {
                    if (model.getStatus() == FileDownloadStatus.progress ||
                            model.getStatus() == FileDownloadStatus.connected ||
                            model.getStatus() == FileDownloadStatus.error ||
                            (model.getStatus() == FileDownloadStatus.pending && model.getSoFar() > 0)
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
                    // consider check in new thread, but SQLite lock | file lock aways effect, so sync
                    if (model.getStatus() == FileDownloadStatus.paused &&
                            FileDownloadUtils.isBreakpointAvailable(model.getId(), model,
                                    model.getPath(), null)) {
                        // can be reused in the old mechanism(no-temp-file).

                        final File tempFile = new File(model.getTempFilePath());

                        if (!tempFile.exists() && targetFile.exists()) {
                            final boolean successRename = targetFile.renameTo(tempFile);
                            if (FileDownloadLog.NEED_LOG) {
                                FileDownloadLog.d(FileDownloadDatabase.class,
                                        "resume from the old no-temp-file architecture [%B], [%s]->[%s]",
                                        successRename, targetFile.getPath(), tempFile.getPath());

                            }
                        }
                    }

                    /**
                     * Remove {@code model} from DB if it can't used for judging whether the
                     * old-downloaded file is valid for reused & it can't used for resuming from
                     * BREAKPOINT, In other words, {@code model} is no use anymore for FileDownloader.
                     */
                    if (model.getStatus() == FileDownloadStatus.pending && model.getSoFar() <= 0) {
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
                    iterator.remove();
                    maintainer.onRemovedInvalidData(model);
                    removedDataCount++;
                } else {
                    final int oldId = model.getId();
                    final int newId = idGenerator.transOldId(oldId, model.getUrl(), model.getPath(), model.isPathAsDirectory());
                    if (newId != oldId) {
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(FileDownloadDatabase.class, "the id is changed on restoring from db: old[%d] -> new[%d]", oldId, newId);
                        }
                        model.setId(newId);
                        maintainer.changeFileDownloadModelId(oldId, model);
                        resetIdCount++;
                    }

                    maintainer.onRefreshedValidData(model);
                    refreshDataCount++;
                }
            }

        } finally {
            FileDownloadUtils.markConverted(FileDownloadHelper.getAppContext());
            maintainer.onFinishMaintain();
            // 566 data consumes about 140ms
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(FileDownloadDatabase.class,
                        "refreshed data count: %d , delete data count: %d, reset id count: %d. consume %d",
                        refreshDataCount, removedDataCount, resetIdCount, System.currentTimeMillis() - startTimestamp);
            }
        }
    }
}
