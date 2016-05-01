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

import android.os.Handler;

import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 9/24/15.
 * <p/>
 * An atom task wraps the real action with {@link FileDownloadServiceUIGuard}.
 */
class FileDownloadTask extends BaseDownloadTask {

    private static final DownloadEventSampleListener DOWNLOAD_INTERNAL_LIS;
    private static final ArrayList<BaseDownloadTask> NEED_RESTART_LIST = new ArrayList<>();

    static {
        DOWNLOAD_INTERNAL_LIS = new DownloadEventSampleListener(new FileDownloadInternalLis());
        FileDownloadEventPool.getImpl().addListener(DownloadServiceConnectChangedEvent.ID, DOWNLOAD_INTERNAL_LIS);
        MessageSnapshotFlow.getImpl().setReceiver(new InternalMessageReceiver());
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
    protected void _startExecute() {
        final boolean succeed = FileDownloadServiceProxy.getImpl().
                startDownloader(
                        getUrl(),
                        getPath(),
                        getCallbackProgressTimes(),
                        getAutoRetryTimes(),
                        getHeader());

        if (!succeed) {
            //noinspection StatementWithEmptyBody
            if (_checkCanStart()) {
                final MessageSnapshot snapshot = catchException(
                        new RuntimeException("Occur Unknow Error, when request to start" +
                                " maybe some problem in binder, maybe the process was killed in unexpected."));
                if (!FileDownloadList.getImpl().contains(this)) {
                    synchronized (NEED_RESTART_LIST) {
                        if (NEED_RESTART_LIST.contains(this)) {
                            NEED_RESTART_LIST.remove(this);
                        }
                    }
                    FileDownloadList.getImpl().add(this);
                }

                FileDownloadList.getImpl().remove(this, snapshot);

            } else {
                // the process was killed when request stating. will be restarted by NEED_RESTART_LIST.
            }
        } else {
            handleNoNeedRestart();
        }

    }

    @Override
    protected boolean _checkCanReuse() {
        if (isForceReDownload()) {
            return false;
        }

        final MessageSnapshot snapshot = FileDownloadServiceProxy.getImpl().checkReuse(getId());
        if (snapshot != null) {
            MessageSnapshotFlow.getImpl().inflow(snapshot);
            return true;
        }


        return super._checkCanReuse();
    }

    @Override
    protected boolean _checkCanStart() {
        if (!FileDownloadServiceProxy.getImpl().isConnected()) {
            synchronized (NEED_RESTART_LIST) {
                if (!FileDownloadServiceProxy.getImpl().isConnected()) {
                    // 没有连上 服务
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(this, "no connect service !! %s", getId());
                    }
                    FileDownloadServiceProxy.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
                    if (!NEED_RESTART_LIST.contains(this)) {
                        NEED_RESTART_LIST.add(this);
                    }
                    return false;
                }
            }
        }

        handleNoNeedRestart();

        return true;
    }

    @Override
    public boolean pause() {
        handleNoNeedRestart();

        return super.pause();
    }

    private void handleNoNeedRestart() {

        // connected
        //noinspection StatementWithEmptyBody
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
        return FileDownloadServiceProxy.getImpl().pauseDownloader(getId());
    }

    @Override
    protected int _getStatusFromServer(final int downloadId) {
        return FileDownloadServiceProxy.getImpl().getStatus(downloadId);
    }

    private static class InternalMessageReceiver implements MessageSnapshotFlow.MessageReceiver {

        @Override
        public void receive(MessageSnapshot snapshot) {

            // For fewer copies,do not carry all data in transfer model.
            final List<BaseDownloadTask> taskList = FileDownloadList.getImpl().getList(snapshot.getId());


            if (taskList.size() > 0) {

                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadTask.class, "~~~callback %s old[%s] new[%s] %d",
                            snapshot.getId(), taskList.get(0).getStatus(), snapshot.getStatus(), taskList.size());
                }

                final String updateSync = FileDownloadUtils.formatString("%s%s", taskList.get(0).getUrl(),
                        taskList.get(0).getPath());

                synchronized (updateSync.intern()) {
                    boolean consumed = false;
                    for (BaseDownloadTask task : taskList) {
                        if (task.updateKeepFlow(snapshot)) {
                            consumed = true;
                            break;
                        }
                    }

                    if (!consumed && taskList.size() == 1) {
                        // Cover the most case for restarting from the low memory status.
                        consumed = taskList.get(0).updateKeepAhead(snapshot);
                    }

                    if (!consumed) {
                        String log = "The flow callback did not consumed, id:" + snapshot.getId() + " status:"
                                + snapshot.getStatus() + " task-count:" + taskList.size();
                        for (BaseDownloadTask task : taskList) {
                            log += " | " + task.getStatus();
                        }
                        FileDownloadLog.w(FileDownloadTask.class, log);
                    }

                }

            } else {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadTask.class, "callback event transfer %d," +
                            " but is contains false", snapshot.getStatus());
                }
            }
        }
    }

    private static class FileDownloadInternalLis implements DownloadEventSampleListener.IEventListener {

        @Override
        public boolean callback(IDownloadEvent event) {
            if (event instanceof DownloadServiceConnectChangedEvent) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadTask.class, "callback connect service %s",
                            ((DownloadServiceConnectChangedEvent) event).getStatus());
                }
                if (((DownloadServiceConnectChangedEvent) event).getStatus() ==
                        DownloadServiceConnectChangedEvent.ConnectStatus.connected) {
                    List<BaseDownloadTask> needRestartList;

                    synchronized (NEED_RESTART_LIST) {
                        //noinspection unchecked
                        needRestartList = (List<BaseDownloadTask>) NEED_RESTART_LIST.clone();
                        NEED_RESTART_LIST.clear();

                        for (BaseDownloadTask o : needRestartList) {
                            if (FileDownloader.RUNNING_SERIAL_MAP.containsKey(o.getListener())) {
                                o.ready();
                                continue;
                            }
                            //noinspection StatementWithEmptyBody
                            if (!o.using) {
                                o.start();
                            } else {
                                /** already handled
                                 * by {@link FileDownloadEventPool#launchTask(DownloadTaskEvent)}
                                 * **/
                            }
                        }

                        for (Handler handler : FileDownloader.RUNNING_SERIAL_MAP.values()) {
                            FileDownloader.unFreezeSerialHandler(handler);
                        }
                    }

                } else if (((DownloadServiceConnectChangedEvent) event).getStatus() ==
                        DownloadServiceConnectChangedEvent.ConnectStatus.lost) {
                    // lost the connection to the service
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(FileDownloadTask.class, "lost the connection to the " +
                                        "file download service, and current active task size is %d",
                                FileDownloadList.getImpl().size());
                    }

                    // TODO Multi-engine support, need to deal with similar situation
                    if (FileDownloadList.getImpl().size() > 0) {
                        synchronized (NEED_RESTART_LIST) {
                            FileDownloadList.getImpl().divert(NEED_RESTART_LIST);
                            for (BaseDownloadTask baseDownloadTask : NEED_RESTART_LIST) {
                                baseDownloadTask.using = false;
                                baseDownloadTask.clearMarkAdded2List();
                            }

                            for (Handler handler : FileDownloader.RUNNING_SERIAL_MAP.values()) {
                                FileDownloader.freezeSerialHandler(handler);
                            }
                        }


                    }

                } else {
                    // do nothing for unbind manually
                    // TODO maybe need handle something on file downloader service
                    if (FileDownloadList.getImpl().size() > 0) {
                        FileDownloadLog.w(FileDownloadTask.class, "file download service has be unbound" +
                                        " but the size of active tasks are not empty %d ",
                                FileDownloadList.getImpl().size());
                    }
                }

                return false;

            }

            return false;
        }

    }


}
