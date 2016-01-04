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
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 1/4/16.
 *
 * For file size greater than 1.99G
 */
public abstract class FileDownloadLargeFileListener extends FileDownloadListener {

    public FileDownloadLargeFileListener() {
        this(0);
    }

    public FileDownloadLargeFileListener(int priority) {
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
                        downloaderEvent.getDownloader().getLargeFileSoFarBytes(),
                        downloaderEvent.getDownloader().getLargeFileTotalBytes());
                break;
            case FileDownloadStatus.connected:
                connected(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEtag(),
                        downloaderEvent.getDownloader().isContinue(),
                        downloaderEvent.getDownloader().getLargeFileSoFarBytes(),
                        downloaderEvent.getDownloader().getLargeFileTotalBytes());
                break;
            case FileDownloadStatus.progress:
                progress(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getLargeFileSoFarBytes(),
                        downloaderEvent.getDownloader().getLargeFileTotalBytes());
                break;

            case FileDownloadStatus.blockComplete:
                blockComplete(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.retry:
                retry(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEx(),
                        downloaderEvent.getDownloader().getRetryingTimes(),
                        downloaderEvent.getDownloader().getLargeFileSoFarBytes());
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
                        downloaderEvent.getDownloader().getLargeFileSoFarBytes(),
                        downloaderEvent.getDownloader().getLargeFileTotalBytes());
                break;
            case FileDownloadStatus.warn:
                // already same url & path in pending/running list
                warn(downloaderEvent.getDownloader());
                break;
        }

        return false;
    }


    /**
     * Entry queue, and pending
     *
     * @param task       Current task
     * @param soFarBytes Already downloaded bytes stored in the db
     * @param totalBytes Total bytes stored in the db
     */
    protected abstract void pending(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);

    /**
     * Connected
     *
     * @param task       Current task
     * @param etag       ETag
     * @param isContinue Is resume from breakpoint
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    protected void connected(final BaseDownloadTask task, final String etag, final boolean isContinue, final long soFarBytes, final long totalBytes) {

    }

    /**
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    protected abstract void progress(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);

    /**
     * Block completed in new thread
     *
     * @param task Current task
     */
    protected abstract void blockComplete(final BaseDownloadTask task);

    /**
     * Start Retry
     *
     * @param task          Current task
     * @param ex            why retry
     * @param retryingTimes How many times will retry
     * @param soFarBytes    Number of bytes download so far
     */
    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final long soFarBytes) {

    }

    // final width below methods

    /**
     * Succeed download
     *
     * @param task Current task
     */
    protected abstract void completed(final BaseDownloadTask task);

    /**
     * Download paused
     *
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    protected abstract void paused(final BaseDownloadTask task, final long soFarBytes, final long totalBytes);

    /**
     * Download error
     *
     * @param task Current task
     * @param e    Any throwable on download pipeline
     */
    protected abstract void error(final BaseDownloadTask task, final Throwable e);

    /**
     * There is already an identical task being downloaded
     *
     * @param task Current task
     */
    protected abstract void warn(final BaseDownloadTask task);

}
