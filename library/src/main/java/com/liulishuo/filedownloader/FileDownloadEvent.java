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

    public FileDownloadEvent progress() {
        this.status = FileDownloadStatus.progress;

        return this;
    }

    public FileDownloadEvent complete() {
        this.status = FileDownloadStatus.completed;
        return this;
    }

    public FileDownloadEvent error() {
        this.status = FileDownloadStatus.error;
        return this;
    }


    public FileDownloadEvent pause() {
        this.status = FileDownloadStatus.paused;
        return this;
    }

    public FileDownloadEvent pending() {
        this.status = FileDownloadStatus.pending;
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
}
