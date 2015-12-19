package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.FileDownloadTransferEvent;
import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.event.FileEventSampleListener;
import com.liulishuo.filedownloader.event.FileServiceConnectChangedEvent;
import com.liulishuo.filedownloader.event.IFileEvent;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadInternal extends BaseFileDownloadInternal {

    private static FileEventSampleListener DOWNLOAD_INTERNAL_LIS;
    private static List<FileDownloadInternal> NEED_RESTART_LIST = new ArrayList<>();

    public FileDownloadInternal(String url, List<BaseFileDownloadInternal> downloadList) {
        super(url, downloadList);
        if (DOWNLOAD_INTERNAL_LIS == null) {
            DOWNLOAD_INTERNAL_LIS = new FileEventSampleListener(new FileDownloadInternalLis());
            FileEventPool.getImpl().addListener(FileServiceConnectChangedEvent.ID, DOWNLOAD_INTERNAL_LIS);
            FileEventPool.getImpl().addListener(FileDownloadTransferEvent.ID, DOWNLOAD_INTERNAL_LIS);
        }
    }

    @Override
    protected boolean removeExecute() {
        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
        }
        return FileDownloadServiceUIGuard.getImpl().removeDownloader(getDownloadId());
    }

    @Override
    protected void endDownloaded(Throwable e) {
        super.endDownloaded(e);
        synchronized (NEED_RESTART_LIST) {
            NEED_RESTART_LIST.remove(this);
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
    protected void addEventListener() {
        super.addEventListener();
    }

    @Override
    protected void removeEventListener() {
        super.removeEventListener();
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

    @Override
    protected boolean resumeExecute() {
        return FileDownloadServiceUIGuard.getImpl().resumeDownloader(getDownloadId());
    }

    private class FileDownloadInternalLis implements FileEventSampleListener.IEventListener {

        @Override
        public boolean callback(IFileEvent event) {
            if (event instanceof FileServiceConnectChangedEvent) {
                FileDownloadLog.d(FileDownloadInternal.class, "callback connect service %s", ((FileServiceConnectChangedEvent) event).getStatus());
                if (((FileServiceConnectChangedEvent) event).getStatus() == FileServiceConnectChangedEvent.ConnectStatus.connected) {
                    Object[] needRestartList;
                    synchronized (NEED_RESTART_LIST) {
                        needRestartList = NEED_RESTART_LIST.toArray();
                        NEED_RESTART_LIST.clear();
                    }

                    if (needRestartList == null) {
                        // 不可能!
                        FileDownloadLog.e(FileDownloadInternal.class, "need restart list == null!");
                    } else {
                        for (Object o : needRestartList) {
                            ((FileDownloadInternal) o).start();
                        }
                    }
                } else {
                    // 断开了连接
                    // TODO 做多重特定引擎支持的时候，这里需要特殊处理
                    final FileDownloadInternal[] needRestartList = new FileDownloadInternal[getDownloadList().size()];
                    getDownloadList().toArray(needRestartList);
                    getDownloadList().clear();
                    synchronized (NEED_RESTART_LIST) {
                        NEED_RESTART_LIST.addAll(Arrays.asList(needRestartList));
                    }

                    for (FileDownloadInternal fileDownloadInternal : needRestartList) {
                        fileDownloadInternal.clear();
                    }
                }

                return false;

            }

            if (event instanceof FileDownloadTransferEvent) {

                BaseFileDownloadInternal downloadInternal = null;
                final FileDownloadTransferModel transfer = ((FileDownloadTransferEvent) event).getTransfer();
                for (BaseFileDownloadInternal baseFileDownloaderInternal : FileDownloader.getImpl().getDownloadList()) {
                    // TODO 这里只处理第一个的通知
                    if (baseFileDownloaderInternal.getDownloadId() == transfer.getDownloadId()) {
                        downloadInternal = baseFileDownloaderInternal;
                        break;
                    }
                }


                // UI线程第二手转包到目标listener
                if (downloadInternal != null) {
                    FileDownloadLog.d(FileDownloadInternal.class, "~~~callback %s old[%s] new[%s]", downloadInternal.getDownloadId(), downloadInternal.getStatus(), transfer.getStatus());
                    switch (transfer.getStatus()) {
                        case FileDownloadStatus.progress:
                            if (downloadInternal.getStatus() == FileDownloadStatus.progress && transfer.getSofarBytes() == downloadInternal.getDownloadedSofar() && transfer.getTotalBytes() == downloadInternal.getTotalSizeBytes()) {

                                FileDownloadLog.w(FileDownloadInternal.class, "unused values! by process callback");
                                break;
                            }
                            downloadInternal.notifyProgress(transfer.getSofarBytes(), transfer.getTotalBytes());
                            break;
                        case FileDownloadStatus.completed:
                            if (downloadInternal.getStatus() == FileDownloadStatus.completed) {
                                FileDownloadLog.w(FileDownloadInternal.class, "already completed , callback by process whith same transfer");
                                break;
                            }
                            downloadInternal.setDownloadedSofar(transfer.getTotalBytes());
                            downloadInternal.notifyCompleted();
                            break;
                        case FileDownloadStatus.error:
                            if (downloadInternal.getStatus() == FileDownloadStatus.error) {
                                FileDownloadLog.w(FileDownloadInternal.class, "already err , callback by other status same transfer");
                                break;
                            }
                            downloadInternal.notifyErrored(transfer.getThrowable());
                            break;
                        case FileDownloadStatus.paused:
                            if (downloadInternal.getStatus() == FileDownloadStatus.paused) {
                                FileDownloadLog.w(FileDownloadInternal.class, "already paused , callback by other status same transfer");
                                break;
                            }
                            downloadInternal.notifyPaused(transfer.getSofarBytes(), transfer.getTotalBytes());
                            break;
                        case FileDownloadStatus.pending:
                            if (downloadInternal.getStatus() == FileDownloadStatus.paused && transfer.getSofarBytes() == downloadInternal.getDownloadedSofar() && transfer.getTotalBytes() == downloadInternal.getTotalSizeBytes()) {
                                FileDownloadLog.w(FileDownloadInternal.class, "already pending , callback by other status same transfer");
                                break;
                            }
                            downloadInternal.notifyPending(transfer.getSofarBytes(), transfer.getTotalBytes());
                            break;
                    }
                } else {
                    FileDownloadLog.d(this, "callback event transfer %s, but is contains false", transfer.getStatus());
                }

            }
            return false;
        }
    }

}
