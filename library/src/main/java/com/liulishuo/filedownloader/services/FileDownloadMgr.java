package com.liulishuo.filedownloader.services;


import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;

/**
 * Copyright (c) 2015 LingoChamp Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadMgr implements DownloadEventSampleListener.IEventListener {
    private IFileDownloadDBHelper mHelper;

    // TODO 对OkHttpClient，看如何可以有效利用OkHttpClient进行相关优化，进行有关封装
    private OkHttpClient client;

    private DownloadEventSampleListener mListener;
    private FileDownloadThreadPool mThreadPool = new FileDownloadThreadPool();

    private FileDownloadNotificationMgr mNotificationMgr;

    public FileDownloadMgr() {
        mHelper = new FileDownloadDBHelper();
        mListener = new DownloadEventSampleListener(this);
        mNotificationMgr = new FileDownloadNotificationMgr();

        // init client
        client = new OkHttpClient();
        // TODO 设置超时

        DownloadEventPool.getImpl().addListener(DownloadTransferEvent.ID, mListener);
    }


    public synchronized int start(String url, String path, FileDownloadNotificationModel notificaitonData, int callbackProgressTimes) {
        final int id = FileDownloadUtils.generateId(url, path);

        if (checkResume(id)) {
            FileDownloadLog.d(this, "resume %d", id);
            return id;
        }

        final FileDownloadModel model = new FileDownloadModel();
        model.setUrl(url);
        model.setPath(path);
        model.setCallbackProgressTimes(callbackProgressTimes);

        model.setNeedNotification(notificaitonData.isNeed());
        model.setTitle(notificaitonData.getTitle());
        model.setDesc(notificaitonData.getDesc());

        model.setId(id);
        model.setSoFar(0);
        model.setTotal(0);
        model.setStatus(FileDownloadStatus.pending);
        model.setIsCancel(false);

        mHelper.update(model);

        mThreadPool.execute(new FileDownloadRunnable(client, model, mHelper));

        if (notificaitonData.isNeed()) {
            if (mNotificationMgr.get(id) == null) {
                mNotificationMgr.update(model);
            }
            mNotificationMgr.showNoProgress(id, FileDownloadStatus.pending);
        }

        return id;
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
     * @return Already succeed & exists
     */
    public FileDownloadTransferModel checkReuse(String url, String path) {
        final int downloadId = FileDownloadUtils.generateId(url, path);
        final FileDownloadModel model = mHelper.find(downloadId);
        FileDownloadTransferModel transferModel = null;
        // 这个方法判断应该在checkDownloading之后，如果在下载中，那么这些判断都将产生错误。 存在小概率事件，有可能，此方法判断过程中，刚好下载完成, 这里需要对同一DownloadId的Runnable与该方法同步
        do {
            if (model == null) {
                // 数据不存在
                FileDownloadLog.w(this, "model not exist %s", url);
                break;
            }

            if (model.getStatus() != FileDownloadStatus.completed) {
                // 数据状态没完成
                FileDownloadLog.w(this, "status not completed %s %s", model.getStatus(), url);
                break;
            }

            final File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                // 文件不存在
                FileDownloadLog.w(this, "file not exists %s", url);
                break;
            }

            if (model.getSoFar() != model.getTotal()) {
                // 脏数据
                FileDownloadLog.w(this, "sofar[%d] not equal total[%d] %s", model.getSoFar(), model.getTotal(), url);
                break;
            }

            if (file.length() != model.getTotal()) {
                // 无效文件
                FileDownloadLog.w(this, "file length[%d] not equal total[%d] %s", file.length(), model.getTotal(), url);
                break;
            }

            transferModel = new FileDownloadTransferModel(model);
        } while (false);


        FileDownloadLog.d(this, "check reuse %d enable(%B)", downloadId, transferModel != null);
        return transferModel;
    }

    private boolean checkResume(final int downloadId) {
        return resume(downloadId);
    }

    public boolean resume(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return false;
        }

        if (model.getStatus() != FileDownloadStatus.paused) {
            return false;
        }

        model.setIsCancel(false);

        FileDownloadLog.d(this, "start resume %d %d %d", id, model.getSoFar(), model.getTotal());
        mThreadPool.execute(new FileDownloadRunnable(client, model, mHelper));

        return true;
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
     * @param id
     * @return
     * @deprecated 不需要 直接用{@link #pause(int)}
     */
    public boolean remove(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return false;
        }

        FileDownloadLog.d(this, "remove %d", id);
        model.setIsCancel(true);
        return true;
    }

    public int getSofar(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return 0;
        }

        return model.getSoFar();
    }

    public int getTotal(final int id) {
        final FileDownloadModel model = mHelper.find(id);
        if (model == null) {
            return 0;
        }

        return model.getTotal();
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        if (event instanceof DownloadTransferEvent) {
            switch (((DownloadTransferEvent) event).getTransfer().getStatus()) {
                case FileDownloadStatus.error:
                case FileDownloadStatus.completed:
                    mNotificationMgr.showNoProgress(((DownloadTransferEvent) event).getTransfer().getDownloadId(),
                            ((DownloadTransferEvent) event).getTransfer().getStatus());
                    mNotificationMgr.cancel(((DownloadTransferEvent) event).getTransfer().getDownloadId());
                    break;
                case FileDownloadStatus.progress:
                    mNotificationMgr.showProgress(((DownloadTransferEvent) event).getTransfer().getDownloadId(),
                            ((DownloadTransferEvent) event).getTransfer().getSoFarBytes(),
                            ((DownloadTransferEvent) event).getTransfer().getTotalBytes());
                    break;
                case FileDownloadStatus.pending:
                case FileDownloadStatus.paused:
                    mNotificationMgr.showNoProgress(((DownloadTransferEvent) event).getTransfer().getDownloadId(), ((DownloadTransferEvent) event).getTransfer().getStatus());
                    break;
            }
        }
        return false;
    }
}

