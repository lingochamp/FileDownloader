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
import com.liulishuo.filedownloader.model.FileDownloadStatus;
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
    private static final ArrayList<BaseDownloadTask> NEED_RESTART_LIST = new ArrayList<>();

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
        handleNoNeedRestart();
    }

    @Override
    public void over() {
        super.over();

        handleNoNeedRestart();
    }

    @Override
    protected boolean _startExecute() {
        final boolean succeed = FileDownloadServiceUIGuard.getImpl().
                startDownloader(
                        getUrl(),
                        getPath(),
                        getCallbackProgressTimes(),
                        getAutoRetryTimes(),
                        isForceReDownload());

        if (succeed) {
            handleNoNeedRestart();
        }

        return succeed;
    }

    @Override
    protected boolean _checkCanStart() {
        if (!FileDownloadServiceUIGuard.getImpl().isConnected()) {
            synchronized (NEED_RESTART_LIST) {
                if (!FileDownloadServiceUIGuard.getImpl().isConnected()) {
                    // 没有连上 服务
                    FileDownloadLog.d(this, "no connect service !! %s", getDownloadId());
                    FileDownloadServiceUIGuard.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
                    NEED_RESTART_LIST.add(this);
                    return false;
                }
            }
        }

        handleNoNeedRestart();

        return true;
    }

    @Override
    public boolean pause() {
        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }

        return super.pause();
    }

    private void handleNoNeedRestart() {

        // connected
        if (NEED_RESTART_LIST.size() > 0) {
            synchronized (NEED_RESTART_LIST) {
                NEED_RESTART_LIST.remove(this);
            }
        } else {
            // safe
            // 1. only relate with  FileDownloadList, which will be invoked before this, so if empty
            // will be safe to pass
            // 2. this method will be invoked one by one in the task with task lifecycle
        }

    }

    @Override
    protected boolean _pauseExecute() {
        return FileDownloadServiceUIGuard.getImpl().pauseDownloader(getDownloadId());
    }

    @Override
    protected int _getStatusFromServer(final int downloadId) {
        return FileDownloadServiceUIGuard.getImpl().getStatus(downloadId);
    }

    private static class FileDownloadInternalLis implements DownloadEventSampleListener.IEventListener {

        @Override
        public boolean callback(IDownloadEvent event) {
            if (event instanceof DownloadTransferEvent) {

                // For fewer copies,do not carry all data in transfer model.
                final FileDownloadTransferModel transfer = ((DownloadTransferEvent) event).getTransfer();
                final List<BaseDownloadTask> taskList = FileDownloadList.getImpl().getList(transfer.getDownloadId());


                if (taskList.size() > 0) {

                    FileDownloadLog.d(FileDownloadTask.class, "~~~callback %s old[%s] new[%s] %d", transfer.getDownloadId(), taskList.get(0).getStatus(), transfer.getStatus(), taskList.size());

                    if (transfer.getStatus() == FileDownloadStatus.warn) {
                        // just update one task, another will be maintained to receive other status
                        final BaseDownloadTask task = taskList.get(0);
                        task.update(transfer);
                    } else {
                        // guarantee: 1. BaseDownloadTask#update pass no change status.
                        //  2. FileDownloadList#remove only notify in case of remove succeed.
                        for (BaseDownloadTask task : taskList) {
                            task.update(transfer);
                        }
                    }



                } else {
                    FileDownloadLog.d(FileDownloadTask.class, "callback event transfer %d, but is contains false", transfer.getStatus());
                }
                return true;
            }

            if (event instanceof DownloadServiceConnectChangedEvent) {
                FileDownloadLog.d(FileDownloadTask.class, "callback connect service %s", ((DownloadServiceConnectChangedEvent) event).getStatus());
                if (((DownloadServiceConnectChangedEvent) event).getStatus() == DownloadServiceConnectChangedEvent.ConnectStatus.connected) {
                    List<BaseDownloadTask> needRestartList;
                    synchronized (NEED_RESTART_LIST) {
                        needRestartList = (List<BaseDownloadTask>) NEED_RESTART_LIST.clone();
                        NEED_RESTART_LIST.clear();
                    }

                    for (BaseDownloadTask o : needRestartList) {
                        o.start();
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

            return false;
        }

    }


}
