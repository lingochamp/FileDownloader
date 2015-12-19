package com.liulishuo.filedownloader;


import com.liulishuo.filedownloader.event.IFileEvent;
import com.liulishuo.filedownloader.event.IFileListener;

/**
 * Created by Jacksgong on 9/7/15.
 * <p/>
 * normal chain {@link #pending} -> {@link #progress}  -> {@link #preCompleteOnNewThread} -> {@link #complete}
 * may final width {@link #pause}/{@link #complete}/{@link #error}/{@link #warn}
 * if reuse just {@link #preCompleteOnNewThread} ->{@link #complete}
 */
public abstract class FileDownloadListener extends IFileListener {

    public FileDownloadListener() {
        this(0);
    }

    public FileDownloadListener(int priority) {
        super(priority);
    }

    @Override
    public boolean callback(IFileEvent event) {
        if (!(event instanceof FileDownloadEvent)) {
            return false;
        }

        final FileDownloadEvent downloaderEvent = ((FileDownloadEvent) event);

        switch (downloaderEvent.getStatus()) {
            case progress:
                progress(downloaderEvent.getDownloader(), downloaderEvent.getDownloadedSofar(), downloaderEvent.getTotalSizeBytes());
                break;
            case paused:
                pause(downloaderEvent.getDownloader(), downloaderEvent.getDownloadedSofar(), downloaderEvent.getTotalSizeBytes());
                break;
            case pending:
                pending(downloaderEvent.getDownloader(), downloaderEvent.getDownloadedSofar(), downloaderEvent.getTotalSizeBytes());
                break;
            case preCompleteOnNewThread:
                preCompleteOnNewThread(downloaderEvent.getDownloader());
                break;
            case completed:
                complete(downloaderEvent.getDownloader());
                break;
            case error:
                error(downloaderEvent.getDownloader(), downloaderEvent.getThrowable());
                break;
            case warn:
                // already same url & path in pending/running list
                warn(downloaderEvent.getDownloader());
                break;
        }

        return false;
    }


    /**
     * @param downloader
     */
    protected abstract void progress(final BaseFileDownloadInternal downloader, final long downloadedSofar, final long totalSizeBytes);


    protected abstract void pending(final BaseFileDownloadInternal downloader, final long downloadedSofar, final long totalSizeBytes);

    /**
     * block complete in new thread
     *
     * @param downloader
     */
    protected abstract void preCompleteOnNewThread(final BaseFileDownloadInternal downloader);

    // final width below methods

    /**
     * succeed download
     *
     * @param downloader
     */
    protected abstract void complete(final BaseFileDownloadInternal downloader);

    protected abstract void pause(final BaseFileDownloadInternal downloader, final long downloadedSofar, final long totalSizeBytes);

    /**
     * @param downloader
     * @param e
     */
    protected abstract void error(final BaseFileDownloadInternal downloader, final Throwable e);

    /**
     * @param downloader same download already start & pending/downloading
     */
    protected void warn(final BaseFileDownloadInternal downloader) {

    }

}
