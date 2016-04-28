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

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import junit.framework.Assert;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jacksgong on 12/21/15.
 * <p/>
 * The messenger for sending messages to  {@link FileDownloadListener}.
 *
 * @see IFileDownloadMessenger
 */
class FileDownloadMessenger implements IFileDownloadMessenger {

    private boolean largeMessage;
    private BaseDownloadTask task;

    private Queue<FileDownloadMessage> parcelQueue;

    FileDownloadMessenger(final BaseDownloadTask task) {
        init(task);
    }

    private void init(final BaseDownloadTask task) {
        this.task = task;
        parcelQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void notifyBegin() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify begin %s", task);
        }

        task.begin();

        largeMessage = task.getListener() instanceof FileDownloadLargeFileListener;
    }

    @Override
    public void notifyPending() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify pending %s", task);
        }

        task.ing();

        process(FileDownloadStatus.pending);
    }

    @Override
    public void notifyStarted() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify started %s", task);
        }

        task.ing();

        process(FileDownloadStatus.started);
    }

    @Override
    public void notifyConnected() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify connected %s", task);
        }

        task.ing();

        process(FileDownloadStatus.connected);
    }

    @Override
    public void notifyProgress() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify progress %s %d %d",
                    task, task.getLargeFileSoFarBytes(), task.getLargeFileTotalBytes());
        }
        if (task.getCallbackProgressTimes() <= 0) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "notify progress but client not request notify %s", task);
            }
            return;
        }

        task.ing();

        process(FileDownloadStatus.progress);

    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify block completed %s %s", task, Thread.currentThread().getName());
        }

        task.ing();

        process(FileDownloadStatus.blockComplete);
    }

    @Override
    public void notifyRetry() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify retry %s %d %d %s", task,
                    task.getAutoRetryTimes(), task.getRetryingTimes(), task.getEx());
        }

        task.ing();

        process(FileDownloadStatus.retry);
    }

    // Over state, from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify warn %s", task);
        }

        task.over();

        process(FileDownloadStatus.warn);
    }

    @Override
    public void notifyError() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify error %s %s", task, task.getEx());
        }

        task.over();

        process(FileDownloadStatus.error);
    }

    @Override
    public void notifyPaused() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify paused %s", task);
        }

        task.over();

        process(FileDownloadStatus.paused);
    }

    @Override
    public void notifyCompleted() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify completed %s", task);
        }

        task.over();

        process(FileDownloadStatus.completed);
    }

    private final Object blockCompletedLock = new Object();

    private void process(int status) {
        final boolean send;

        if (status == FileDownloadStatus.blockComplete || status == FileDownloadStatus.completed) {
            synchronized (blockCompletedLock) {
                send = offer(status);
            }
        } else {
            send = offer(status);
        }

        if (send) {
            FileDownloadMessageStation.getImpl().requestEnqueue(this);
        }

    }

    private boolean offer(int status) {
        Assert.assertTrue(
                FileDownloadUtils.formatString("request process message %d, but has already over %d",
                        status, parcelQueue.size()), task != null);


        final boolean send;
        final FileDownloadMessage message;

        final boolean needWaitingForComplete = !parcelQueue.isEmpty();

        if (largeMessage) {
            message = FileDownloadMessage.writeLargeFileMessage(task, status);
        } else {
            message = FileDownloadMessage.writeMessage(task, status);
        }

        if (needWaitingForComplete &&
                (status == FileDownloadStatus.blockComplete || status == FileDownloadStatus.completed)) {
            send = false;
            // waiting...
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "waiting %d", status);
            }
        } else {
            send = true;
        }

        parcelQueue.offer(message);

        return send;
    }

    @Override
    public void handoverMessage() {
        final boolean next;

        synchronized (blockCompletedLock) {
            final FileDownloadMessage message;
            message = parcelQueue.poll();

            Assert.assertTrue(
                    FileDownloadUtils.formatString(
                            "can't handover the message, no master to receive this " +
                                    "message(status[%d]) size[%d]",
                            message.getSnapshot().getStatus(), parcelQueue.size()),
                    task != null);

            task.getListener().callback(message);

            next = messageArrived(message.getSnapshot().getStatus());
        }

        if (next) {
            FileDownloadMessageStation.getImpl().requestEnqueue(this);
        }
    }

    public boolean messageArrived(int status) {
        if (FileDownloadStatus.isOver(status)) {
            if (!parcelQueue.isEmpty()) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the messenger[%s] has already " +
                                        "accomplished all his job, but there still are some messages in" +
                                        " parcel queue[%d]",
                                this, parcelQueue.size()));
            }
            task.clear();
            task = null;
            return false;
        }

        if (!parcelQueue.isEmpty()) {
            final FileDownloadMessage nextMessage = parcelQueue.peek();

            final int nextStatus = nextMessage.getSnapshot().getStatus();
            if (status == FileDownloadStatus.blockComplete ||
                    nextStatus == FileDownloadStatus.blockComplete) {
                // completed
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "request completed status %d, %d", status, nextStatus);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handoverDirectly() {
        return task.isSyncCallback();
    }

    @Override
    public boolean hasReceiver() {
        return task.getListener() != null;
    }

    @Override
    public void reAppointment(BaseDownloadTask task) {
        if (this.task != null) {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("the messenger is working, can't " +
                            "re-appointment for %s", task));
        }

        init(task);
    }

    @Override
    public boolean isBlockingCompleted() {
        return parcelQueue.peek().getSnapshot().getStatus() == FileDownloadStatus.blockComplete;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("%d:%s", task.getDownloadId(), super.toString());
    }
}
