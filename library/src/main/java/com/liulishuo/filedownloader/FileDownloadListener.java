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


import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.event.IDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;

/**
 * Created by Jacksgong on 9/7/15.
 * <p/>
 * normal chain {@link #pending} -> {@link #connected} -> {@link #progress}  -> {@link #blockComplete} -> {@link #completed}
 * may final width {@link #paused}/{@link #completed}/{@link #error}/{@link #warn}
 * if reuse just {@link #blockComplete} ->{@link #completed}
 *
 * @see FileDownloadLargeFileListener
 */
public abstract class FileDownloadListener extends IDownloadListener {

    public FileDownloadListener() {
    }

    /**
     * @param priority not handle priority any more
     * @see #FileDownloadListener()
     * @deprecated not handle priority any more
     */
    public FileDownloadListener(int priority) {
        FileDownloadLog.w(this, "not handle priority any more");
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
                        downloaderEvent.getDownloader().getSmallFileSoFarBytes(),
                        downloaderEvent.getDownloader().getSmallFileTotalBytes());
                break;
            case FileDownloadStatus.started:
                started(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.connected:
                connected(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEtag(),
                        downloaderEvent.getDownloader().isResuming(),
                        downloaderEvent.getDownloader().getSmallFileSoFarBytes(),
                        downloaderEvent.getDownloader().getSmallFileTotalBytes());
                break;
            case FileDownloadStatus.progress:
                progress(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getSmallFileSoFarBytes(),
                        downloaderEvent.getDownloader().getSmallFileTotalBytes());
                break;

            case FileDownloadStatus.blockComplete:
                blockComplete(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.retry:
                retry(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEx(),
                        downloaderEvent.getDownloader().getRetryingTimes(),
                        downloaderEvent.getDownloader().getSmallFileSoFarBytes());
                break;

            case FileDownloadStatus.completed:
                completed(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.error:
                error(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEx());
                break;
            case FileDownloadStatus.paused:
                paused(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getSmallFileSoFarBytes(),
                        downloaderEvent.getDownloader().getSmallFileTotalBytes());
                break;
            case FileDownloadStatus.warn:
                // already same url & path in pending/running list
                warn(downloaderEvent.getDownloader());
                break;
        }

        return false;
    }


    /**
     * Enqueue, and pending
     *
     * @param task       Current task
     * @param soFarBytes Already downloaded bytes stored in the db
     * @param totalBytes Total bytes stored in the db
     * @see IFileDownloadMessage#notifyPending()
     */
    protected abstract void pending(final BaseDownloadTask task, final int soFarBytes,
                                    final int totalBytes);

    /**
     * Finish pending, and start the download runnable.
     *
     * @param task Current task.
     * @see IFileDownloadMessage#notifyStarted()
     */
    protected void started(final BaseDownloadTask task) {
    }

    /**
     * Connected
     *
     * @param task       Current task
     * @param etag       ETag
     * @param isContinue Is resume from breakpoint
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @see IFileDownloadMessage#notifyConnected()
     */
    protected void connected(final BaseDownloadTask task, final String etag,
                             final boolean isContinue, final int soFarBytes, final int totalBytes) {

    }

    /**
     * Fetching datum and Writing to local disk.
     *
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @see IFileDownloadMessage#notifyProgress()
     */
    protected abstract void progress(final BaseDownloadTask task, final int soFarBytes,
                                     final int totalBytes);

    /**
     * Block completed in new thread
     *
     * @param task Current task
     * @see IFileDownloadMessage#notifyBlockComplete()
     */
    protected abstract void blockComplete(final BaseDownloadTask task);

    /**
     * Occur a exception and has chance{@link BaseDownloadTask#setAutoRetryTimes(int)} to retry and
     * start Retry
     *
     * @param task          Current task
     * @param ex            why retry
     * @param retryingTimes How many times will retry
     * @param soFarBytes    Number of bytes download so far
     * @see IFileDownloadMessage#notifyRetry()
     */
    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes,
                         final int soFarBytes) {

    }

    // final width below methods

    /**
     * Succeed download
     *
     * @param task Current task
     * @see IFileDownloadMessage#notifyCompleted()
     */
    protected abstract void completed(final BaseDownloadTask task);

    /**
     * Download paused
     *
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @see IFileDownloadMessage#notifyPaused()
     */
    protected abstract void paused(final BaseDownloadTask task, final int soFarBytes,
                                   final int totalBytes);

    /**
     * Download error
     *
     * @param task Current task
     * @param e    Any throwable on download pipeline
     * @see IFileDownloadMessage#notifyError()
     * @see com.liulishuo.filedownloader.exception.FileDownloadHttpException
     * @see com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException
     * @see com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException
     */
    protected abstract void error(final BaseDownloadTask task, final Throwable e);

    /**
     * There is already an identical task being downloaded
     *
     * @param task Current task
     * @see IFileDownloadMessage#notifyWarn()
     */
    protected abstract void warn(final BaseDownloadTask task);

}
