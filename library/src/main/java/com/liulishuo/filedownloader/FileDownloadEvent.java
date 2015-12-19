package com.liulishuo.filedownloader;


import com.liulishuo.filedownloader.event.IFileEvent;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 9/7/15.
 */
class FileDownloadEvent extends IFileEvent {

    public FileDownloadEvent(final BaseFileDownloadInternal downloader) {
        super(downloader.generateEventId());
        this.downloader = downloader;
    }

    private BaseFileDownloadInternal downloader;

    private int status;
    private int downloadedSofar;
    private int totalSizeBytes;

    /**
     * @param downloadedSofar
     * @param totalSizeBytes
     * @return
     */
    public FileDownloadEvent progress(final int downloadedSofar, final int totalSizeBytes) {
        this.status = FileDownloadStatus.progress;

        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        return this;
    }

    public FileDownloadEvent complete() {
        this.status = FileDownloadStatus.completed;
        return this;
    }

    private Throwable e;

    public FileDownloadEvent error(final Throwable e) {
        this.status = FileDownloadStatus.error;
        this.e = e;
        return this;
    }


    public FileDownloadEvent pause(final int downloadedSofar, final int totalSizeBytes) {
        this.status = FileDownloadStatus.paused;

        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        return this;
    }

    public FileDownloadEvent pending(final int downloadedSofar, final int totalSizeBytes) {
        this.status = FileDownloadStatus.pending;

        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        return this;
    }

    public FileDownloadEvent preCompleteOnNewThread() {
        this.status = FileDownloadStatus.preCompleteOnNewThread;
        return this;
    }

    public FileDownloadEvent warn(){
        this.status = FileDownloadStatus.warn;
        return this;
    }
    public FileDownloadEvent callback(Runnable runnable) {
        this.callback = runnable;
        return this;
    }

    public BaseFileDownloadInternal getDownloader() {
        return this.downloader;
    }

    public int getStatus() {
        return this.status;
    }

    public Throwable getThrowable() {
        return this.e;
    }

    public int getDownloadedSofar() {
        return downloadedSofar;
    }

    public int getTotalSizeBytes() {
        return totalSizeBytes;
    }

}
