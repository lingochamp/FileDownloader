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

import com.liulishuo.filedownloader.message.BlockCompleteMessage;
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

    private BaseDownloadTask.IRunningTask mTask;
    private BaseDownloadTask.LifeCycleCallback mLifeCycleCallback;

    private Queue<FileDownloadMessage> parcelQueue;

    FileDownloadMessenger(final BaseDownloadTask.IRunningTask task,
                          final BaseDownloadTask.LifeCycleCallback callback) {
        init(task, callback);
    }

    private void init(final BaseDownloadTask.IRunningTask task, BaseDownloadTask.LifeCycleCallback callback) {
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
        final BaseDownloadTask originTask = mTask.getOrigin();
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify progress %s %d %d",
                    originTask, originTask.getLargeFileSoFarBytes(), originTask.getLargeFileTotalBytes());
        }
        if (originTask.getCallbackProgressTimes() <= 0) {
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
            final BaseDownloadTask originTask = mTask.getOrigin();
            FileDownloadLog.d(this, "notify retry %s %d %d %s", mTask,
                    originTask.getAutoRetryTimes(), originTask.getRetryingTimes(), originTask.getErrorCause());
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
            FileDownloadLog.d(this, "notify error %s %s", mTask, mTask.getOrigin().getErrorCause());
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

    private void process(MessageSnapshot snapshot) {
        offer(snapshot);

        FileDownloadMessageStation.getImpl().requestEnqueue(this);

    }

    private void offer(MessageSnapshot snapshot) {
        final byte status = snapshot.getStatus();
        Assert.assertTrue(
                FileDownloadUtils.formatString("request process message %d, but has already over %d",
                        status, parcelQueue.size()), mTask != null);

        parcelQueue.offer(new FileDownloadMessage(mTask.getOrigin(), snapshot));
    }

    @Override
    public void handoverMessage() {
        final FileDownloadMessage message = parcelQueue.poll();
        final int currentStatus = message.getSnapshot().getStatus();

        Assert.assertTrue(
                FileDownloadUtils.formatString(
                        "can't handover the message, no master to receive this " +
                                "message(status[%d]) size[%d]",
                        currentStatus, parcelQueue.size()),
                mTask != null);
        final BaseDownloadTask originTask = mTask.getOrigin();

        final FileDownloadListener listener = originTask.getListener();
        final ITaskHunter.IMessageHandler messageHandler = mTask.getMessageHandler();

        // If this task is in the over state, try to retire this messenger.
        if (FileDownloadStatus.isOver(currentStatus)) {
            if (!parcelQueue.isEmpty()) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the messenger[%s] has already " +
                                        "accomplished all his job, but there still are some messages in" +
                                        " parcel queue[%d]",
                                this, parcelQueue.size()));
            }
            mTask = null;
        }

        if (currentStatus == FileDownloadStatus.blockComplete) {
            try {
                if (listener != null) {
                    listener.callback(message);
                }
                notifyCompleted(((BlockCompleteMessage) message.getSnapshot()).
                        transmitToCompleted());
            } catch (Throwable throwable) {
                notifyError(messageHandler.prepareErrorMessage(throwable));
            }
        } else if (listener != null) {
            listener.callback(message);
        }
    }

    @Override
    public boolean handoverDirectly() {
        return mTask.getOrigin().isSyncCallback();
    }

    @Override
    public boolean hasReceiver() {
        return mTask.getOrigin().getListener() != null || FileDownloadMonitor.getMonitor() != null;
    }

    @Override
    public void reAppointment(BaseDownloadTask.IRunningTask task,
                              BaseDownloadTask.LifeCycleCallback callback) {
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
        return FileDownloadUtils.formatString("%d:%s", mTask.getOrigin().getId(), super.toString());
    }
}
