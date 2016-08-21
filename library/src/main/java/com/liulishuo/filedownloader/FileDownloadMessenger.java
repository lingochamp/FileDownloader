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

import com.liulishuo.filedownloader.message.FileDownloadMessage;
import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import junit.framework.Assert;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The messenger for sending messages to  {@link FileDownloadListener}.
 *
 * @see IFileDownloadMessenger
 */
class FileDownloadMessenger implements IFileDownloadMessenger {

    private BaseDownloadTask mTask;
    private BaseDownloadTask.LifeCycleCallback mLifeCycleCallback;

    private Queue<FileDownloadMessage> parcelQueue;

    FileDownloadMessenger(final BaseDownloadTask task,
                          final BaseDownloadTask.LifeCycleCallback callback) {
        init(task, callback);
    }

    private void init(final BaseDownloadTask task, BaseDownloadTask.LifeCycleCallback callback) {
        this.mTask = task;
        this.mLifeCycleCallback = callback;
        parcelQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public boolean notifyBegin() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify begin %s", mTask);
        }

        if (mTask == null) {
            FileDownloadLog.w(this, "can't begin the task, the holder fo the messenger is nil, %d",
                    parcelQueue.size());
            return false;
        }

        mLifeCycleCallback.onBegin();

        return true;
    }

    @Override
    public void notifyPending(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify pending %s", mTask);
        }

        mLifeCycleCallback.onIng();

        process(snapshot);
    }

    @Override
    public void notifyStarted(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify started %s", mTask);
        }

        mLifeCycleCallback.onIng();

        process(snapshot);
    }

    @Override
    public void notifyConnected(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify connected %s", mTask);
        }

        mLifeCycleCallback.onIng();

        process(snapshot);
    }

    @Override
    public void notifyProgress(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify progress %s %d %d",
                    mTask, mTask.getLargeFileSoFarBytes(), mTask.getLargeFileTotalBytes());
        }
        if (mTask.getCallbackProgressTimes() <= 0) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "notify progress but client not request notify %s", mTask);
            }
            return;
        }

        mLifeCycleCallback.onIng();

        process(snapshot);

    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify block completed %s %s", mTask, Thread.currentThread().getName());
        }

        mLifeCycleCallback.onIng();

        process(snapshot);
    }

    @Override
    public void notifyRetry(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify retry %s %d %d %s", mTask,
                    mTask.getAutoRetryTimes(), mTask.getRetryingTimes(), mTask.getErrorCause());
        }

        mLifeCycleCallback.onIng();

        process(snapshot);
    }

    // Over state, from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify warn %s", mTask);
        }

        mLifeCycleCallback.onOver();

        process(snapshot);
    }

    @Override
    public void notifyError(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify error %s %s", mTask, mTask.getErrorCause());
        }

        mLifeCycleCallback.onOver();

        process(snapshot);
    }

    @Override
    public void notifyPaused(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify paused %s", mTask);
        }

        mLifeCycleCallback.onOver();

        process(snapshot);
    }

    @Override
    public void notifyCompleted(MessageSnapshot snapshot) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify completed %s", mTask);
        }

        mLifeCycleCallback.onOver();

        process(snapshot);
    }

    private final Object blockCompletedLock = new Object();

    private void process(MessageSnapshot snapshot) {
        final boolean send;
        final byte status = snapshot.getStatus();

        if (status == FileDownloadStatus.blockComplete || status == FileDownloadStatus.completed) {
            synchronized (blockCompletedLock) {
                send = offer(snapshot);
            }
        } else {
            send = offer(snapshot);
        }

        if (send) {
            FileDownloadMessageStation.getImpl().requestEnqueue(this);
        }

    }

    private boolean offer(MessageSnapshot snapshot) {
        final byte status = snapshot.getStatus();
        Assert.assertTrue(
                FileDownloadUtils.formatString("request process message %d, but has already over %d",
                        status, parcelQueue.size()), mTask != null);


        final boolean send;
        final FileDownloadMessage message = new FileDownloadMessage(mTask, snapshot);

        final boolean needWaitingForComplete = !parcelQueue.isEmpty();

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
        final boolean continueEnqueue;

        synchronized (blockCompletedLock) {
            final FileDownloadMessage message;
            message = parcelQueue.poll();
            final int currentStatus = message.getSnapshot().getStatus();

            Assert.assertTrue(
                    FileDownloadUtils.formatString(
                            "can't handover the message, no master to receive this " +
                                    "message(status[%d]) size[%d]",
                            currentStatus, parcelQueue.size()),
                    mTask != null);

            final FileDownloadListener listener = mTask.getListener();
            continueEnqueue = isContinueEnqueue(currentStatus);

            if (listener == null) {
                FileDownloadLog.w(this, "The task[%d] can't receive the message(status: [%d])," +
                                " its download listener might be removed when it is running in" +
                                " FileDownloader",
                        mTask.getId(), currentStatus);
            } else {
                listener.callback(message);
            }

        }

        if (continueEnqueue) {
            FileDownloadMessageStation.getImpl().requestEnqueue(this);
        }
    }

    private boolean isContinueEnqueue(int status) {
        if (FileDownloadStatus.isOver(status)) {
            if (!parcelQueue.isEmpty()) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the messenger[%s] has already " +
                                        "accomplished all his job, but there still are some messages in" +
                                        " parcel queue[%d]",
                                this, parcelQueue.size()));
            }
            mTask = null;
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
        return mTask.isSyncCallback();
    }

    @Override
    public boolean hasReceiver() {
        return mTask.getListener() != null;
    }

    @Override
    public void reAppointment(BaseDownloadTask task, BaseDownloadTask.LifeCycleCallback callback) {
        if (this.mTask != null) {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("the messenger is working, can't " +
                            "re-appointment for %s", task));
        }

        init(task, callback);
    }

    @Override
    public boolean isBlockingCompleted() {
        return parcelQueue.peek().getSnapshot().getStatus() == FileDownloadStatus.blockComplete;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("%d:%s", mTask.getId(), super.toString());
    }
}
