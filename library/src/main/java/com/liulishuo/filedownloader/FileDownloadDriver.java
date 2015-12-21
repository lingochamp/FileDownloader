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

    // 启动 from FileDownloadList, to addListener ---------------
    @Override
    public void notifyStarted() {
        FileDownloadLog.d(this, "notify started %s", download);
        download.addEventListener();

        download.begin();
    }

    // 中间层  from DownloadInternal, to user ---------------------------
    @Override
    public void notifyPending() {
        FileDownloadLog.d(this, "notify pending %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download)
                .pending());

        download.ing();
    }

    @Override
    public void notifyProgress() {
        FileDownloadLog.d(this, "notify progress %s %d %d", download, download.getDownloadedSofar(), download.getTotalSizeBytes());

        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download).
                progress());

        download.ing();
    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete() {
        FileDownloadLog.d(this, "notify block complete %s %s", download, Thread.currentThread().getName());

        DownloadEventPool.getImpl().publish(new FileDownloadEvent(download).preCompleteOnNewThread());
        download.ing();
    }

    // 结束层 from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn() {
        FileDownloadLog.d(this, "notify warn %s", download);
        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download).
                warn().
                callback(download.getOverCallback()));

        download.over();
    }

    @Override
    public void notifyError() {
        FileDownloadLog.e(this, download.getEx(), "notify error %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download).
                error().
                callback(download.getOverCallback()));

        download.over();
    }

    @Override
    public void notifyPaused() {
        FileDownloadLog.d(this, "notify paused %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download).
                pause().
                callback(download.getOverCallback()));

        download.over();
    }

    @Override
    public void notifyCompleted() {
        FileDownloadLog.d(this, "notify completed %s", download);

        DownloadEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(download).
                complete().
                callback(download.getOverCallback()));

        download.over();
    }
}
