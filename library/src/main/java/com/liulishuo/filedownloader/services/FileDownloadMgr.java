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

import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadMgr {
    private final IFileDownloadDBHelper mHelper;

    // TODO 对OkHttpClient，看如何可以有效利用OkHttpClient进行相关优化，进行有关封装
    private final OkHttpClient client;

    private final FileDownloadThreadPool mThreadPool = new FileDownloadThreadPool();

    public FileDownloadMgr() {
        mHelper = new FileDownloadDBHelper();

        // init client
        client = new OkHttpClient();
        // TODO 设置超时
    }


    // synchronize for safe: check downloading, check resume, update data, execute runnable
    public synchronized void start(final String url, final String path, final int callbackProgressTimes,
                                   final int autoRetryTimes, final FileDownloadHeader header) {
        final int id = FileDownloadUtils.generateId(url, path);

        // check is already in download pool
        if (checkDownloading(url, path)) {
            FileDownloadLog.d(this, "has already started download %d", id);
            // warn
            final FileDownloadTransferModel warnModel = new FileDownloadTransferModel();
            warnModel.setDownloadId(id);
            warnModel.setStatus(FileDownloadStatus.warn);

            FileDownloadProcessEventPool.getImpl()
                    .publish(new DownloadTransferEvent(warnModel));
            return;
        }

        // real start

        // - create model
        FileDownloadModel model = mHelper.find(id);
        boolean needUpdate2DB;
        if (model != null &&
                (model.getStatus() == FileDownloadStatus.paused ||
                        model.getStatus() == FileDownloadStatus.error) // FileDownloadRunnable invoke
            // #checkBreakpointAvailable  to determine whether it is really invalid.
                ) {
            // TODO pending data is no use, if not resume by break point
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
        model.setIsCancel(false);

        // - update model to db
        if (needUpdate2DB) {
            mHelper.update(model);
        }

        // - execute
        mThreadPool.execute(new FileDownloadRunnable(client, model, mHelper, autoRetryTimes, header));

    }

    public boolean checkDownloading(String url, String path) {
        final int downloadId = FileDownloadUtils.generateId(url, path);
        final FileDownloadModel model = mHelper.find(downloadId);
        final boolean isInPool = mThreadPool.isInThreadPool(downloadId);

        boolean isDownloading;
        do {
            if (model == null ||
                    (model.getStatus() != FileDownloadStatus.pending && model.getStatus() != FileDownloadStatus.progress)
                    ) {

                if (isInPool) {
                    // status 不是pending/processing & 线程池有，只有可能是线程同步问题，status已经设置为complete/error/pause但是线程还没有执行完
                    // TODO 这里需要特殊处理，小概率事件，需要对同一DownloadId的Runnable与该方法同步
                    isDownloading = true;
                } else {
                    // status 不是pending/processing & 线程池没有，直接返回不在下载中
                    isDownloading = false;

                }
            } else {
                //model != null && status 为pending/progress其中一个
                if (isInPool) {
                    // status 是pending/processing & 线程池有，直接返回正在下载中
                    isDownloading = true;
                } else {
                    // status 是pending/processing & 线程池没有，只有可能异常状态，打e级log，直接放回不在下载中
                    FileDownloadLog.e(this, "status is[%s] & thread is not has %d", model.getStatus(), downloadId);
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
        boolean result = false;

        do {
            if (model == null) {
                FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d model == null", downloadId);
                break;
            }

            if (model.getStatus() != FileDownloadStatus.paused
                    && model.getStatus() != FileDownloadStatus.retry
                    && model.getStatus() != FileDownloadStatus.pending // may pending in case of enqueue
                    ) {
                FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d status[%d] isn't paused",
                        downloadId, model.getStatus());
                break;
            }

            if (TextUtils.isEmpty(model.getETag())) {
                FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d etag is empty", downloadId);
                break;
            }


            File file = new File(model.getPath());
            final boolean isExists = file.exists();
            final boolean isDirectory = file.isDirectory();

            if (!isExists || isDirectory) {
                FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d file not suit, exists[%B], directory[%B]",
                        downloadId, isExists, isDirectory);
                break;
            }

            final long fileLength = file.length();

            if (fileLength < model.getSoFar()
                    || (model.getTotal() != -1  // not chunk transfer encoding data
                    && fileLength >= model.getTotal())) {
                // 脏数据
                FileDownloadLog.d(FileDownloadMgr.class, "can't continue %d dirty data fileLength[%d] sofar[%d] total[%d]",
                        downloadId, fileLength, model.getSoFar(), model.getTotal());
                break;

            }

            result = true;
        } while (false);


        return result;
    }

    public FileDownloadTransferModel checkReuse(final int downloadId) {
        FileDownloadTransferModel transferModel = null;

        final FileDownloadModel model = mHelper.find(downloadId);
        final boolean canReuse = checkReuse(downloadId, model);
        if (canReuse) {
            transferModel = new FileDownloadTransferModel(model);
            transferModel.setUseOldFile(true);
        }

        return transferModel;
    }

    /**
     * @return Already succeed & exists
     */
    public static boolean checkReuse(final int downloadId, final FileDownloadModel model) {
        boolean result = false;
        // 这个方法判断应该在checkDownloading之后，如果在下载中，那么这些判断都将产生错误。
        // 存在小概率事件，有可能，此方法判断过程中，刚好下载完成, 这里需要对同一DownloadId的Runnable与该方法同步
        do {
            if (model == null) {
                // 数据不存在
                FileDownloadLog.w(FileDownloadMgr.class, "can't reuse %d model not exist", downloadId);
                break;
            }

            if (model.getStatus() != FileDownloadStatus.completed) {
                // 数据状态没完成
                FileDownloadLog.w(FileDownloadMgr.class, "can't reuse %d status not completed %s",
                        downloadId, model.getStatus());
                break;
            }

            final File file = new File(model.getPath());
            if (!file.exists() || !file.isFile()) {
                // 文件不存在
                FileDownloadLog.w(FileDownloadMgr.class, "can't reuse %d file not exists", downloadId);
                break;
            }

            if (model.getSoFar() != model.getTotal()) {
                // 脏数据
                FileDownloadLog.w(FileDownloadMgr.class, "can't reuse %d soFar[%d] not equal total[%d] %d",
                        downloadId, model.getSoFar(), model.getTotal());
                break;
            }

            if (file.length() != model.getTotal()) {
                // 无效文件
                FileDownloadLog.w(FileDownloadMgr.class, "can't reuse %d file length[%d] not equal total[%d]",
                        downloadId, file.length(), model.getTotal());
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

        FileDownloadLog.d(this, "paused %d", id);
        model.setIsCancel(true);
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

        FileDownloadLog.d(this, "pause all tasks %d", list.size());

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

    public int getStatus(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return FileDownloadStatus.INVALID_STATUS;
        }

        return model.getStatus();
    }

    public boolean isIdle() {
        return mThreadPool.exactSize() <= 0;
    }
}

