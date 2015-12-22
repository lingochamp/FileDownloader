package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadTask extends BaseDownloadTask {

    private static DownloadEventSampleListener DOWNLOAD_INTERNAL_LIS;
    private static List<BaseDownloadTask> NEED_RESTART_LIST = new ArrayList<>();

    public FileDownloadTask(String url) {
        super(url);
        if (DOWNLOAD_INTERNAL_LIS == null) {
            DOWNLOAD_INTERNAL_LIS = new DownloadEventSampleListener(new FileDownloadInternalLis());
            DownloadEventPool.getImpl().addListener(DownloadServiceConnectChangedEvent.ID, DOWNLOAD_INTERNAL_LIS);
            DownloadEventPool.getImpl().addListener(DownloadTransferEvent.ID, DOWNLOAD_INTERNAL_LIS);
        }
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

        setSoFarBytes(model.getSoFarBytes());
        setTotalBytes(model.getTotalBytes());
        return true;

    }

    @Override
    protected int _startExecute() {
        final int result = FileDownloadServiceUIGuard.getImpl().
                startDownloader(
                        getUrl(),
                        getPath(),
                        new FileDownloadNotificationModel(isNeedNotification(), getNotificationTitle(), getNotificationDesc()),
                        getCallbackProgressTimes());

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

    private class FileDownloadInternalLis implements DownloadEventSampleListener.IEventListener {

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

                    if (needRestartList == null) {
                        // 不可能!
                        FileDownloadLog.e(FileDownloadTask.class, "need restart list == null!");
                    } else {
                        for (Object o : needRestartList) {
                            ((FileDownloadTask) o).start();
                        }
                    }
                } else {
                    // 断开了连接
                    // TODO 做多重特定引擎支持的时候，这里需要特殊处理
                    FileDownloadList.getImpl().divert(NEED_RESTART_LIST);

                    synchronized (NEED_RESTART_LIST) {
                        for (BaseDownloadTask fileDownloadInternal : NEED_RESTART_LIST) {
                            // TODO 缺少通知用户的操作
                            fileDownloadInternal.clear();
                        }
                    }
                }

                return false;

            }

            if (event instanceof DownloadTransferEvent) {

                /**
                 * 注意!! 为了优化有部分数据在某些情况下是没有带回来的
                 */
                final FileDownloadTransferModel transfer = ((DownloadTransferEvent) event).getTransfer();
                final BaseDownloadTask task = FileDownloadList.getImpl().get(transfer.getDownloadId());


                // UI线程第二手转包到目标listener
                if (task != null) {
                    FileDownloadLog.d(FileDownloadTask.class, "~~~callback %s old[%s] new[%s]", task.getDownloadId(), task.getStatus(), transfer.getStatus());
                    task.updateData(transfer);
                } else {
                    FileDownloadLog.d(FileDownloadTask.class, "callback event transfer %d, but is contains false", transfer.getStatus());
                }
                return true;
            }

            return false;
        }

    }


}
