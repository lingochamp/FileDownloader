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

/**
 * The listener for listening the downloading status changing.
 * <p>
 * This listener will be used when the file size of the task is greater than 1.99G.
 */
@SuppressWarnings({"WeakerAccess", "UnusedParameters"})
public abstract class FileDownloadLargeFileListener extends FileDownloadListener {

    public FileDownloadLargeFileListener() {
    }

    /**
     * @see #FileDownloadLargeFileListener()
     * @deprecated not handle priority any more
     */
    public FileDownloadLargeFileListener(int priority) {
        //noinspection deprecation
        super(priority);
    }

    /**
     * Entry queue, and pending
     *
     * @param task       Current task
     * @param soFarBytes Already downloaded bytes stored in the db
     * @param totalBytes Total bytes stored in the db
     */
    protected abstract void pending(final BaseDownloadTask task, final long soFarBytes,
                                    final long totalBytes);

    /**
     * @param task       The task
     * @param soFarBytes Already downloaded bytes stored in the db
     * @param totalBytes Total bytes stored in the db
     * @deprecated replaced with {@link #pending(BaseDownloadTask, long, long)}
     */
    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    }

    /**
     * Connected
     *
     * @param task       Current task
     * @param etag       ETag
     * @param isContinue Is resume from breakpoint
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    @SuppressWarnings("EmptyMethod")
    protected void connected(final BaseDownloadTask task, final String etag,
                             final boolean isContinue,
                             final long soFarBytes, final long totalBytes) {
    }

    /**
     * @param task       The task
     * @param etag       ETag
     * @param isContinue Is resume from breakpoint
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @deprecated replaced with {@link #connected(BaseDownloadTask, String, boolean, long, long)}
     */
    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes,
                             int totalBytes) {
    }

    /**
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    protected abstract void progress(final BaseDownloadTask task, final long soFarBytes,
                                     final long totalBytes);

    /**
     * @param task       The task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @deprecated replaced with {@link #progress(BaseDownloadTask, long, long)}
     */
    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    }

    /**
     * Start Retry
     *
     * @param task          Current task
     * @param ex            why retry
     * @param retryingTimes How many times will retry
     * @param soFarBytes    Number of bytes download so far
     */
    @SuppressWarnings("EmptyMethod")
    protected void retry(final BaseDownloadTask task, final Throwable ex,
                         final int retryingTimes, final long soFarBytes) {
    }

    /**
     * @param task          The task
     * @param ex            Why retry
     * @param retryingTimes How many times will retry
     * @param soFarBytes    Number of bytes download so far
     * @deprecated replaced with {@link #retry(BaseDownloadTask, Throwable, int, long)}
     */
    @SuppressWarnings("EmptyMethod")
    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
    }

    /**
     * Download paused
     *
     * @param task       Current task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     */
    protected abstract void paused(final BaseDownloadTask task, final long soFarBytes,
                                   final long totalBytes);

    /**
     * @param task       The task
     * @param soFarBytes Number of bytes download so far
     * @param totalBytes Total size of the download in bytes
     * @deprecated replaced with {@link #paused(BaseDownloadTask, long, long)}
     */
    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    }
}
