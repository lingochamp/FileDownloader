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


import android.text.TextUtils;

import com.liulishuo.filedownloader.IThreadPoolMonitor;
import com.liulishuo.filedownloader.download.CustomComponentHolder;
import com.liulishuo.filedownloader.download.DownloadLaunchRunnable;
import com.liulishuo.filedownloader.download.DownloadRunnable;
import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.List;

/**
 * The downloading manager in FileDownloadService, which is used to control all download-inflow.
 * <p/>
 * Handling real {@link #start(String, String, boolean, int, int, int, boolean, FileDownloadHeader,
 * boolean)}.
 *
 * @see FileDownloadThreadPool
 * @see DownloadLaunchRunnable
 * @see DownloadRunnable
 */
class FileDownloadManager implements IThreadPoolMonitor {
    private final FileDownloadDatabase mDatabase;
    private final FileDownloadThreadPool mThreadPool;

    public FileDownloadManager() {
        final CustomComponentHolder holder = CustomComponentHolder.getImpl();
        this.mDatabase = holder.getDatabaseInstance();
        this.mThreadPool = new FileDownloadThreadPool(holder.getMaxNetworkThreadCount());
    }

    // synchronize for safe: check downloading, check resume, update data, execute runnable
    public synchronized void start(final String url, final String path, final boolean pathAsDirectory,
                                   final int callbackProgressTimes,
                                   final int callbackProgressMinIntervalMillis,
                                   final int autoRetryTimes, final boolean forceReDownload,
                                   final FileDownloadHeader header, final boolean isWifiRequired) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "request start the task with url(%s) path(%s) isDirectory(%B)",
                    url, path, pathAsDirectory);
        }

        final int id = FileDownloadUtils.generateId(url, path, pathAsDirectory);
        FileDownloadModel model = mDatabase.find(id);

        List<ConnectionModel> dirConnectionModelList = null;

        if (!pathAsDirectory && model == null) {
            // try dir data.
            final int dirCaseId = FileDownloadUtils.generateId(url, FileDownloadUtils.getParent(path),
                    true);
            model = mDatabase.find(dirCaseId);
            if (model != null && path.equals(model.getTargetFilePath())) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "task[%d] find model by dirCaseId[%d]", id, dirCaseId);
                }

                dirConnectionModelList = mDatabase.findConnectionModel(dirCaseId);
            }
        }

        if (FileDownloadHelper.inspectAndInflowDownloading(id, model, this, true)) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "has already started download %d", id);
            }
            return;
        }

        final String targetFilePath = model != null ? model.getTargetFilePath() :
                FileDownloadUtils.getTargetFilePath(path, pathAsDirectory, null);
        if (FileDownloadHelper.inspectAndInflowDownloaded(id, targetFilePath, forceReDownload,
                true)) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "has already completed downloading %d", id);
            }
            return;
        }

        final long sofar = model != null ? model.getSoFar() : 0;
        final String tempFilePath = model != null ? model.getTempFilePath() :
                FileDownloadUtils.getTempPath(targetFilePath);
        if (FileDownloadHelper.inspectAndInflowConflictPath(id, sofar, tempFilePath, targetFilePath,
                this)) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "there is an another task with the same target-file-path %d %s",
                        id, targetFilePath);
                // because of the file is dirty for this task.
                if (model != null) {
                    mDatabase.remove(id);
                    mDatabase.removeConnections(id);
                }
            }
            return;
        }

        // real start
        // - create model
        boolean needUpdate2DB;
        if (model != null &&
                (model.getStatus() == FileDownloadStatus.paused ||
                        model.getStatus() == FileDownloadStatus.error) // FileDownloadRunnable invoke
            // #isBreakpointAvailable to determine whether it is really invalid.
                ) {
            if (model.getId() != id) {
                // in try dir case.
                mDatabase.remove(model.getId());
                mDatabase.removeConnections(model.getId());

                model.setId(id);
                model.setPath(path, pathAsDirectory);
                if (dirConnectionModelList != null) {
                    for (ConnectionModel connectionModel : dirConnectionModelList) {
                        connectionModel.setId(id);
                        mDatabase.insertConnectionModel(connectionModel);
                    }
                }

                needUpdate2DB = true;
            } else {
                if (!TextUtils.equals(url, model.getUrl())) {
                    // for cover the case of reusing the downloaded processing with the different url( using with idGenerator ).
                    model.setUrl(url);
                    needUpdate2DB = true;
                } else {
                    needUpdate2DB = false;
                }
            }
        } else {
            if (model == null) {
                model = new FileDownloadModel();
            }
            model.setUrl(url);
            model.setPath(path, pathAsDirectory);

            model.setId(id);
            model.setSoFar(0);
            model.setTotal(0);
            model.setStatus(FileDownloadStatus.pending);
            model.setConnectionCount(1);
            needUpdate2DB = true;
        }

        // - update model to db
        if (needUpdate2DB) {
            mDatabase.update(model);
        }

        final DownloadLaunchRunnable.Builder builder = new DownloadLaunchRunnable.Builder();

        final DownloadLaunchRunnable runnable =
                builder.setModel(model)
                        .setHeader(header)
                        .setThreadPoolMonitor(this)
                        .setMinIntervalMillis(callbackProgressMinIntervalMillis)
                        .setCallbackProgressMaxCount(callbackProgressTimes)
                        .setForceReDownload(forceReDownload)
                        .setWifiRequired(isWifiRequired)
                        .setMaxRetryTimes(autoRetryTimes)
                        .build();

        // - execute
        mThreadPool.execute(runnable);

    }

    public boolean isDownloading(String url, String path) {
        return isDownloading(FileDownloadUtils.generateId(url, path));
    }

    public boolean isDownloading(int id) {
        return isDownloading(mDatabase.find(id));
    }

    public boolean pause(final int id) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "request pause the task %d", id);
        }

        final FileDownloadModel model = mDatabase.find(id);
        if (model == null) {
            return false;
        }

        mThreadPool.cancel(id);
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
        final FileDownloadModel model = mDatabase.find(id);
        if (model == null) {
            return 0;
        }

        final int connectionCount = model.getConnectionCount();
        if (connectionCount <= 1) {
            return model.getSoFar();
        } else {
            final List<ConnectionModel> modelList = mDatabase.findConnectionModel(id);
            if (modelList == null || modelList.size() != connectionCount) {
                return 0;
            } else {
                return ConnectionModel.getTotalOffset(modelList);
            }
        }
    }

    public long getTotal(final int id) {
        final FileDownloadModel model = mDatabase.find(id);
        if (model == null) {
            return 0;
        }

        return model.getTotal();
    }

    public byte getStatus(final int id) {
        final FileDownloadModel model = mDatabase.find(id);
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

    @Override
    public boolean isDownloading(FileDownloadModel model) {
        if (model == null) {
            return false;
        }

        final boolean isInPool = mThreadPool.isInThreadPool(model.getId());
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
                            model.getId(), model.getStatus());
                    // handle as not in downloading, going to re-downloading.
                    isDownloading = false;

                }
            }
        } while (false);

        return isDownloading;
    }

    @Override
    public int findRunningTaskIdBySameTempPath(String tempFilePath, int excludeId) {
        return mThreadPool.findRunningTaskIdBySameTempPath(tempFilePath, excludeId);
    }

    public boolean clearTaskData(int id) {
        if (id == 0) {
            FileDownloadLog.w(this, "The task[%d] id is invalid, can't clear it.", id);
            return false;
        }

        if (isDownloading(id)) {
            FileDownloadLog.w(this, "The task[%d] is downloading, can't clear it.", id);
            return false;
        }

        mDatabase.remove(id);
        mDatabase.removeConnections(id);
        return true;
    }

    public void clearAllTaskData() {
        mDatabase.clear();
    }
}

