package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.util.FileDownloadLog;

/**
 * Created by Jacksgong on 12/21/15.
 */
class FileDownloadDriver implements IFileDownloadMessage {

    private BaseDownloadTask download;

    FileDownloadDriver(final BaseDownloadTask download) {
        this.download = download;
    }

    // 启动 from FileDownloadList, to addEventListener ---------------
    @Override
    public void notifyStarted() {
        FileDownloadLog.d(this, "notify started %s", download);

        download.begin();
    }

    // 中间层  from DownloadInternal, to user ---------------------------
    @Override
    public void notifyPending() {
        FileDownloadLog.d(this, "notify pending %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
                .pending());

        download.ing();
    }

    @Override
    public void notifyConnected() {
        FileDownloadLog.d(this, "notify connected %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
        .connected());

        download.ing();
    }

    @Override
    public void notifyProgress() {
        FileDownloadLog.d(this, "notify progress %s %d %d", download, download.getSoFarBytes(), download.getTotalBytes());
        if (download.getCallbackProgressTimes() <= 0) {
            // 只有可能存在一次，是在首次获得总大小的时候
            FileDownloadLog.d(this, "notify progress but client not request notify %s", download);
            return;
        }

        DownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent().
                progress());

        download.ing();
    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete() {
        FileDownloadLog.d(this, "notify block completed %s %s", download, Thread.currentThread().getName());

        DownloadEventPool.getImpl().publish(download.getIngEvent().blockComplete());
        download.ing();
    }

    // 结束层 from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn() {
        FileDownloadLog.d(this, "notify warn %s", download);
        DownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent().
                warn());

        download.over();
    }

    @Override
    public void notifyError() {
        FileDownloadLog.e(this, download.getEx(), "notify error %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent().
                error());

        download.over();
    }

    @Override
    public void notifyPaused() {
        FileDownloadLog.d(this, "notify paused %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent().
                pause());

        download.over();
    }

    @Override
    public void notifyCompleted() {
        FileDownloadLog.d(this, "notify completed %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent().
                complete());

        download.over();
    }
}
