/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.services;


import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.message.MessageSnapshotTaker;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 9/24/15.
 * <p/>
 * The Download Manager in FileDownloadService, which is used to control all download-inflow.
 * <p/>
 * Handling real {@link #start(String, String, int, int, int, FileDownloadHeader)};
 *
 * @see FileDownloadThreadPool
 * @see FileDownloadRunnable
 */
class FileDownloadMgr {
    private final IFileDownloadDBHelper mHelper;

    private OkHttpClient client = null;

    private final FileDownloadThreadPool mThreadPool;

    public FileDownloadMgr() {

        final DownloadMgrInitialParams params = FileDownloadHelper.getDownloadMgrInitialParams();
        mHelper = new FileDownloadDBHelper();

        final OkHttpClient client;
        final int maxNetworkThreadCount;
        if (params != null) {
            client = params.makeCustomOkHttpClient();
            maxNetworkThreadCount = params.getMaxNetworkThreadCount();
        } else {
            client = null;
            maxNetworkThreadCount = 0;
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "init the download manager with initialParams: " +
                            "okhttpClient[is customize: %B], maxNetworkThreadCount[%d]",
                    client != null, maxNetworkThreadCount);
        }

        // init client
        if (this.client != client) {
            this.client = client;
        } else {
            // in this case, the client must be null, see #41
            this.client = new OkHttpClient();
        }

        mThreadPool = new FileDownloadThreadPool(maxNetworkThreadCount);
    }


    // synchronize for safe: check downloading, check resume, update data, execute runnable
    public synchronized void start(final String url, final String path,
                                   final int callbackProgressTimes,
                                   final int callbackProgressMinIntervalMillis,
                                   final int autoRetryTimes, final FileDownloadHeader header) {
        final int id = FileDownloadUtils.generateId(url, path);
        FileDownloadModel model = mHelper.find(id);

        // check has already in download pool
        if (checkDownloading(id)) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "has already started download %d", id);
            }
            // warn
            MessageSnapshotFlow.getImpl().
                    inflow(MessageSnapshotTaker.take(FileDownloadStatus.warn, model));
            return;
        }

        final File oldFile = new File(path);
        if (oldFile.exists()) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "has already completed downloading %d", id);
            }

            // completed
            MessageSnapshotFlow.getImpl().
                    inflow(MessageSnapshotTaker.catchCanReusedOldFile(id, oldFile));
            return;
        }

        // real start

        // - create model
        boolean needUpdate2DB;
        if (model != null &&
                (model.getStatus() == FileDownloadStatus.paused ||
                        model.getStatus() == FileDownloadStatus.error) // FileDownloadRunnable invoke
            // #checkBreakpointAvailable  to determine whether it is really invalid.
                ) {
            needUpdate2DB = false;
        } else {
            if (model == null) {
                model = new FileDownloadModel();
            }
            model.setUrl(url);
            model.setPath(path);

            model.setId(id);
            model.setSoFar(0);
            model.setTotal(0);
            model.setStatus(FileDownloadStatus.pending);
            needUpdate2DB = true;
        }

        model.setCallbackProgressTimes(callbackProgressTimes);

        // - update model to db
        if (needUpdate2DB) {
            mHelper.update(model);
        }

        // - execute
        mThreadPool.execute(new FileDownloadRunnable(client, model, mHelper, autoRetryTimes, header,
                callbackProgressMinIntervalMillis));

    }

    private boolean needStart(int downloadId) {
        final FileDownloadModel model = mHelper.find(downloadId);
        if (model == null) {
            return true;
        }

        boolean needStart = false;
        do {

            if (checkDownloading(downloadId)) {
                break;
            }

            /**
             * The task doesn't need to ensure whether it has already downloaded in here,
             * because it has already handled in  {@link FileDownloadTask#_checkCanReuse()}.
             */
            needStart = true;

        } while (false);

        return needStart;
    }

    public boolean checkDownloading(String url, String path) {
        return checkDownloading(FileDownloadUtils.generateId(url, path));
    }

    @SuppressWarnings("WeakerAccess")
    public boolean checkDownloading(int downloadId) {
        final FileDownloadModel model = mHelper.find(downloadId);
        if (model == null) {
            return false;
        }

        final boolean isInPool = mThreadPool.isInThreadPool(downloadId);
        boolean isDownloading;

        do {
            if (FileDownloadStatus.isOver(model.getStatus())) {

                //noinspection RedundantIfStatement
                if (isInPool) {
                    // already finished, but still in the pool.
                    // handle as downloading.
                    isDownloading = true;
                } else {
                    // already finished, and not in the pool.
                    // make sense.
                    isDownloading = false;

                }
            } else {
                if (isInPool) {
                    // not finish, in the pool.
                    // make sense.
                    isDownloading = true;
                } else {
                    // not finish, but not in the pool.
                    // beyond expectation.
                    FileDownloadLog.e(this, "%d status is[%s](not finish) & but not in the pool",
                            downloadId, model.getStatus());
                    // handle as not in downloading, going to re-downloading.
                    isDownloading = false;

                }
            }
        } while (false);

        return isDownloading;
    }

    /**
     * @return can resume by break point
     */
    public static boolean checkBreakpointAvailable(final int downloadId, final FileDownloadModel model) {
        return checkBreakpointAvailable(downloadId, model, model.getTempPath());
    }

    public static boolean checkBreakpointAvailable(final int downloadId, final FileDownloadModel model,
                                                   final String path) {
        boolean result = false;

        do {
            if (model == null) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d model == null", downloadId);
                }
                break;
            }

            File file = new File(path);
            final boolean isExists = file.exists();
            final boolean isDirectory = file.isDirectory();

            if (!isExists || isDirectory) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d file not suit, exists[%B], directory[%B]",
                            downloadId, isExists, isDirectory);
                }
                break;
            }

            final long fileLength = file.length();

            if (model.getSoFar() == 0) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d the downloaded-record is zero.",
                            downloadId);
                }
                break;
            }

            if (fileLength < model.getSoFar() ||
                    (model.getTotal() != -1  // not chunk transfer encoding data
                            &&
                            (fileLength > model.getTotal() || model.getSoFar() >= model.getTotal()))
                    ) {
                // dirty data.
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d dirty data" +
                                    " fileLength[%d] sofar[%d] total[%d]",
                            downloadId, fileLength, model.getSoFar(), model.getTotal());
                }
                break;
            }

            result = true;
        } while (false);


        return result;
    }

    public boolean pause(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return false;
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "paused %d", id);
        }

        mThreadPool.cancel(id);
        /**
         * 耦合 by {@link FileDownloadRunnable#run()} 中的 {@link com.squareup.okhttp.Request.Builder#tag(Object)}
         * 目前在okHttp里还是每个单独任务
         */
        // 之所以注释掉，不想这里回调error，okHttp中会根据okHttp所在被cancel的情况抛error
//        client.cancel(id);
        return true;
    }

    /**
     * Pause all running task
     */
    public void pauseAll() {
        List<Integer> list = mThreadPool.getAllExactRunningDownloadIds();

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "pause all tasks %d", list.size());
        }

        for (Integer id : list) {
            pause(id);
        }
    }

    public long getSoFar(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return 0;
        }

        return model.getSoFar();
    }

    public long getTotal(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return 0;
        }

        return model.getTotal();
    }

    public byte getStatus(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return FileDownloadStatus.INVALID_STATUS;
        }

        return model.getStatus();
    }

    public boolean isIdle() {
        return mThreadPool.exactSize() <= 0;
    }

    public synchronized boolean setMaxNetworkThreadCount(int count) {
        return mThreadPool.setMaxNetworkThreadCount(count);
    }

}

