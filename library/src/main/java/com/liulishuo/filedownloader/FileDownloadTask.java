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

package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.FileDownloadEventPool;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadTask extends BaseDownloadTask {

    private static final DownloadEventSampleListener DOWNLOAD_INTERNAL_LIS;
    private static final List<BaseDownloadTask> NEED_RESTART_LIST = new ArrayList<>();

    static {
        DOWNLOAD_INTERNAL_LIS = new DownloadEventSampleListener(new FileDownloadInternalLis());
        FileDownloadEventPool.getImpl().addListener(DownloadServiceConnectChangedEvent.ID, DOWNLOAD_INTERNAL_LIS);
        FileDownloadEventPool.getImpl().addListener(DownloadTransferEvent.ID, DOWNLOAD_INTERNAL_LIS);
    }

    FileDownloadTask(String url) {
        super(url);
    }


    @Override
    public void clear() {
        super.clear();
        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }
    }

    @Override
    public void over() {
        super.over();

        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }
    }

    @Override
    protected boolean _checkDownloading(final String url, final String path) {
        return FileDownloadServiceUIGuard.getImpl().checkIsDownloading(url, path);
    }

    @Override
    protected boolean _checkCanReuse() {


        if (isForceReDownload()) {
            return false;
        }

        final FileDownloadTransferModel model = FileDownloadServiceUIGuard.getImpl().checkReuse(getUrl(), getPath());
        if (model == null) {
            return super._checkCanReuse();
        }

        setSoFarBytes(model.getTotalBytes());
        setTotalBytes(model.getTotalBytes());
        return true;

    }

    @Override
    protected int _startExecute() {
        final int result = FileDownloadServiceUIGuard.getImpl().
                startDownloader(
                        getUrl(),
                        getPath(),
                        getCallbackProgressTimes(),
                        getAutoRetryTimes());

        if (result != 0) {
            synchronized (NEED_RESTART_LIST) {
                NEED_RESTART_LIST.remove(this);
            }
        }

        return result;
    }

    @Override
    protected boolean _checkCanStart() {
        synchronized (NEED_RESTART_LIST) {
            if (!FileDownloadServiceUIGuard.getImpl().isConnected()) {
                // 没有连上 服务
                FileDownloadLog.d(this, "no connect service !! %s", getDownloadId());
                FileDownloadServiceUIGuard.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
                NEED_RESTART_LIST.add(this);
                return false;
            }
        }

        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }
        return true;
    }

    @Override
    public boolean pause() {
        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }

        return super.pause();
    }

    @Override
    protected boolean _pauseExecute() {
        return FileDownloadServiceUIGuard.getImpl().pauseDownloader(getDownloadId());
    }

    private static class FileDownloadInternalLis implements DownloadEventSampleListener.IEventListener {

        @Override
        public boolean callback(IDownloadEvent event) {
            if (event instanceof DownloadServiceConnectChangedEvent) {
                FileDownloadLog.d(FileDownloadTask.class, "callback connect service %s", ((DownloadServiceConnectChangedEvent) event).getStatus());
                if (((DownloadServiceConnectChangedEvent) event).getStatus() == DownloadServiceConnectChangedEvent.ConnectStatus.connected) {
                    Object[] needRestartList;
                    synchronized (NEED_RESTART_LIST) {
                        needRestartList = NEED_RESTART_LIST.toArray();
                        NEED_RESTART_LIST.clear();
                    }

                    for (Object o : needRestartList) {
                        ((FileDownloadTask) o).start();
                    }

                } else {
                    // Disconnected from service
                    // TODO Multi-engine support, need to deal with similar situation
                    FileDownloadList.getImpl().divert(NEED_RESTART_LIST);

                    synchronized (NEED_RESTART_LIST) {
                        for (BaseDownloadTask fileDownloadInternal : NEED_RESTART_LIST) {
                            fileDownloadInternal.clear();
                        }
                    }
                }

                return false;

            }

            if (event instanceof DownloadTransferEvent) {

                // For fewer copies,do not carry all data in transfer model.
                final FileDownloadTransferModel transfer = ((DownloadTransferEvent) event).getTransfer();
                final BaseDownloadTask task = FileDownloadList.getImpl().get(transfer.getDownloadId());


                if (task != null) {
                    FileDownloadLog.d(FileDownloadTask.class, "~~~callback %s old[%s] new[%s]", task.getDownloadId(), task.getStatus(), transfer.getStatus());
                    task.update(transfer);
                } else {
                    FileDownloadLog.d(FileDownloadTask.class, "callback event transfer %d, but is contains false", transfer.getStatus());
                }
                return true;
            }

            return false;
        }

    }


}
