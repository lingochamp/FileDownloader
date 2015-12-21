package com.liulishuo.filedownloader;


import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.event.IDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 9/7/15.
 * <p/>
 * normal chain {@link #pending} -> {@link #progress}  -> {@link #blockComplete} -> {@link #complete}
 * may final width {@link #pause}/{@link #complete}/{@link #error}/{@link #warn}
 * if reuse just {@link #blockComplete} ->{@link #complete}
 */
public abstract class FileDownloadListener extends IDownloadListener {

    public FileDownloadListener() {
        this(0);
    }

    public FileDownloadListener(int priority) {
        super(priority);
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        if (!(event instanceof FileDownloadEvent)) {
            return false;
        }

        final FileDownloadEvent downloaderEvent = ((FileDownloadEvent) event);


        switch (downloaderEvent.getStatus()) {
            case FileDownloadStatus.pending:
                pending(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getDownloadedSofar(),
                        downloaderEvent.getDownloader().getTotalSizeBytes());
                break;
            case FileDownloadStatus.progress:
                progress(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getDownloadedSofar(),
                        downloaderEvent.getDownloader().getTotalSizeBytes());
                break;
            case FileDownloadStatus.paused:
                pause(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getDownloadedSofar(),
                        downloaderEvent.getDownloader().getTotalSizeBytes());
                break;

            case FileDownloadStatus.preCompleteOnNewThread:
                blockComplete(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.completed:
                complete(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.error:
                error(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEx());
                break;
            case FileDownloadStatus.warn:
                // already same url & path in pending/running list
                warn(downloaderEvent.getDownloader());
                break;
        }

        return false;
    }


    protected abstract void pending(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);

    protected abstract void progress(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);


    /**
     * block complete in new thread
     *
     * @param atom
     */
    protected abstract void blockComplete(final BaseDownloadTask task);

    // final width below methods

    /**
     * succeed download
     *
     * @param atom
     */
    protected abstract void complete(final BaseDownloadTask task);

    protected abstract void pause(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);

    /**
     * @param atom
     * @param e
     */
    protected abstract void error(final BaseDownloadTask task, final Throwable e);

    /**
     * @param atom same download already start & pending/downloading
     */
    protected abstract void warn(final BaseDownloadTask task);

}
