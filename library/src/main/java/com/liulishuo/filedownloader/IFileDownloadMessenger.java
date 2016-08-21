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

import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.services.FileDownloadRunnable;

import java.io.FileDescriptor;

/**
 * @see com.liulishuo.filedownloader.model.FileDownloadStatus
 */
interface IFileDownloadMessenger {

    /**
     * The task is just received to handle.
     * <p/>
     * FileDownloader accept the task.
     *
     * @return Whether allow it to begin.
     */
    boolean notifyBegin();

    /**
     * The task is pending.
     * <p/>
     * enqueue, and pending, waiting.
     *
     * @see com.liulishuo.filedownloader.services.FileDownloadThreadPool
     */
    void notifyPending(MessageSnapshot snapshot);

    /**
     * The download runnable of the task has started running.
     * <p/>
     * Finish pending, and start download runnable.
     *
     * @see FileDownloadRunnable#onStarted()
     */
    void notifyStarted(MessageSnapshot snapshot);

    /**
     * The task is running.
     * <p/>
     * Already connected to the server, and received the Http-response.
     *
     * @see FileDownloadRunnable#onConnected(boolean, long, String, String)
     */
    void notifyConnected(MessageSnapshot snapshot);

    /**
     * The task is running.
     * <p/>
     * Fetching datum, and write to local disk.
     *
     * @see FileDownloadRunnable#onProgress(long, long, FileDescriptor)
     */
    void notifyProgress(MessageSnapshot snapshot);

    /**
     * The task is running.
     * <p/>
     * Already completed download, and block the current thread to do something, such as unzip,etc.
     *
     * @see FileDownloadRunnable#onComplete(long)
     */
    void notifyBlockComplete(MessageSnapshot snapshot);

    /**
     * The task over.
     * <p/>
     * Occur a exception when downloading, but has retry
     * chance {@link BaseDownloadTask#setAutoRetryTimes(int)}, so retry(re-connect,re-download).
     */
    void notifyRetry(MessageSnapshot snapshot);

    /**
     * The task over.
     * <p/>
     * There has already had some same Tasks(Same-URL & Same-SavePath) in Pending-Queue or is
     * running.
     *
     * @see com.liulishuo.filedownloader.services.FileDownloadMgr#start(String, String, boolean, int, int, int, boolean, FileDownloadHeader)
     * @see com.liulishuo.filedownloader.services.FileDownloadMgr#isDownloading(String, String)
     */
    void notifyWarn(MessageSnapshot snapshot);

    /**
     * The task is over.
     * <p/>
     * Occur a exception, but don't has any chance to retry.
     *
     * @see FileDownloadRunnable#onError(Throwable)
     * @see com.liulishuo.filedownloader.exception.FileDownloadHttpException
     * @see com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException
     * @see com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException
     */
    void notifyError(MessageSnapshot snapshot);

    /**
     * The task is over.
     * <p/>
     * Pause manually by {@link BaseDownloadTask#pause()}.
     *
     * @see BaseDownloadTask#pause()
     */
    void notifyPaused(MessageSnapshot snapshot);

    /**
     * The task is over.
     * <p/>
     * Achieve complete ceremony.
     *
     * @see FileDownloadRunnable#onComplete(long)
     */
    void notifyCompleted(MessageSnapshot snapshot);

    /**
     * Handover a message to {@link FileDownloadListener}.
     */
    void handoverMessage();

    /**
     * @return {@code true} if handover a message to {@link FileDownloadListener} directly(do not
     * need to post the callback to the main thread).
     * @see BaseDownloadTask#isSyncCallback()
     */
    boolean handoverDirectly();

    /**
     * @return {@code true} if has receiver(or listener) to receiver messages.
     * @see BaseDownloadTask#getListener()
     */
    boolean hasReceiver();

    /**
     * @param task Re-appointment for this task, when this messenger has already accomplished the
     *             old one.
     */
    void reAppointment(BaseDownloadTask task, BaseDownloadTask.LifeCycleCallback callback);

    /**
     * The 'block completed'(status) message will be handover in the non-UI thread and block the
     * 'completed'(status) message.
     *
     * @return {@code true} if the status of the current message is
     * {@link com.liulishuo.filedownloader.model.FileDownloadStatus#blockComplete}.
     */
    boolean isBlockingCompleted();
}
