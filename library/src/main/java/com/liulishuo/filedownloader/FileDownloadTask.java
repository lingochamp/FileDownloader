package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
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
    public void clearBasic() {
        super.clearBasic();
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
    protected boolean checkDownloading(final String url, final String path) {
        return FileDownloadServiceUIGuard.getImpl().checkIsDownloading(url, path);
    }

    @Override
    protected boolean checkCanReuse() {


        if (isForceReDownload()) {
            return false;
        }

        final FileDownloadTransferModel model = FileDownloadServiceUIGuard.getImpl().checkReuse(getUrl(), getSavePath());
        if (model == null) {
            return super.checkCanReuse();
        }

        setDownloadId(model.getDownloadId());
        setDownloadedSofar(model.getSofarBytes());
        setTotalSizeBytes(model.getTotalBytes());
        setStatus(model.getStatus());
        return true;

    }

    @Override
    protected int startExecute() {
        final int result = FileDownloadServiceUIGuard.getImpl().
                startDownloader(
                        getUrl(),
                        getSavePath(),
                        new FileDownloadNotificationModel(isNeedNotification(), getNotificationTitle(), getNotificationDesc()),
                        getProgressCallbackTimes());

        if (result != 0) {
            synchronized (NEED_RESTART_LIST) {
                NEED_RESTART_LIST.remove(this);
            }
        }

        return result;
    }

    @Override
    protected boolean checkCanStart() {
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
    protected boolean pauseExecute() {
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

                final FileDownloadTransferModel transfer = ((DownloadTransferEvent) event).getTransfer();
                final BaseDownloadTask downloadInternal = FileDownloadList.getImpl().get(transfer.getDownloadId());


                // UI线程第二手转包到目标listener
                if (downloadInternal != null) {
                    FileDownloadLog.d(FileDownloadTask.class, "~~~callback %s old[%s] new[%s]", downloadInternal.getDownloadId(), downloadInternal.getStatus(), transfer.getStatus());
                    switch (transfer.getStatus()) {
                        case FileDownloadStatus.progress:
                            if (downloadInternal.getStatus() == FileDownloadStatus.progress && transfer.getSofarBytes() == downloadInternal.getDownloadedSofar() && transfer.getTotalBytes() == downloadInternal.getTotalSizeBytes()) {

                                FileDownloadLog.w(FileDownloadTask.class, "unused values! by process callback");
                                break;
                            }

                            copyStatus(transfer, downloadInternal);
                            downloadInternal.getDriver().notifyProgress();

                            break;
                        case FileDownloadStatus.completed:
                            if (downloadInternal.getStatus() == FileDownloadStatus.completed) {
                                FileDownloadLog.w(FileDownloadTask.class, "already completed , callback by process whith same transfer");
                                break;
                            }

                            copyStatus(transfer, downloadInternal);
                            FileDownloadList.getImpl().removeByCompleted(downloadInternal);

                            break;
                        case FileDownloadStatus.error:
                            if (downloadInternal.getStatus() == FileDownloadStatus.error) {
                                FileDownloadLog.w(FileDownloadTask.class, "already err , callback by other status same transfer");
                                break;
                            }

                            copyStatus(transfer, downloadInternal);
                            downloadInternal.setEx(transfer.getThrowable());

                            FileDownloadList.getImpl().removeByError(downloadInternal);

                            break;
                        case FileDownloadStatus.paused:
                            // 由调BaseFileDownloadInternal#pause直接根据回调结果处理
//                            if (downloadInternal.getStatus() == FileDownloadStatus.paused) {
//                                FileDownloadLog.w(FileDownloadInternal.class, "already paused , callback by other status same transfer");
//                                break;
//                            }
//                            downloadInternal.setDownloadedSofar(transfer.getSofarBytes());
//                            downloadInternal.notifyPaused();
                            break;
                        case FileDownloadStatus.pending:
                            if (downloadInternal.getStatus() == FileDownloadStatus.paused && transfer.getSofarBytes() == downloadInternal.getDownloadedSofar() && transfer.getTotalBytes() == downloadInternal.getTotalSizeBytes()) {
                                FileDownloadLog.w(FileDownloadTask.class, "already pending , callback by other status same transfer");
                                break;
                            }


                            copyStatus(transfer, downloadInternal);
                            downloadInternal.getDriver().notifyPending();
                            break;
                    }
                } else {
                    FileDownloadLog.d(this, "callback event transfer %s, but is contains false", transfer.getStatus());
                }

            }
            return false;
        }

        private void copyStatus(final FileDownloadTransferModel transfer, final BaseDownloadTask downloadInternal) {
            downloadInternal.setStatus(transfer.getStatus());
            downloadInternal.setDownloadedSofar(transfer.getSofarBytes());
            downloadInternal.setTotalSizeBytes(transfer.getTotalBytes());
        }
    }

}
