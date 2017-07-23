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

    private Queue<MessageSnapshot> parcelQueue;

    private boolean mIsDiscard = false;

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
        if (mTask == null) {
            if (FileDownloadLog.NEED_LOG) {
                // the most possible of occurring this case is the thread for flowing the paused
                // message is different with others.
                FileDownloadLog.d(this, "occur this case, it would be the host task of this " +
                                "messenger has been over(paused/warn/completed/error) on the " +
                                "other thread before receiving the snapshot(id[%d], status[%d])",
                        snapshot.getId(), snapshot.getStatus());
            }
            return;
        }

        if (mIsDiscard || mTask.getOrigin().getListener() == null) {
            if ((FileDownloadMonitor.isValid() || mTask.isContainFinishListener()) &&
                    snapshot.getStatus() == FileDownloadStatus.blockComplete) {
                // there is a FileDownloadMonitor, so we have to ensure the 'BaseDownloadTask#over'
                // can be invoked.
                mLifeCycleCallback.onOver();
            }

            inspectAndHandleOverStatus(snapshot.getStatus());
        } else {
            parcelQueue.offer(snapshot);

            FileDownloadMessageStation.getImpl().requestEnqueue(this);
        }
    }

    private void inspectAndHandleOverStatus(int status) {
        // If this task is in the over state, try to retire this messenger.
        if (FileDownloadStatus.isOver(status)) {
            if (!parcelQueue.isEmpty()) {
                final MessageSnapshot queueTopTask = parcelQueue.peek();
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the messenger[%s](with id[%d]) has already " +
                                        "accomplished all his job, but there still are some messages in" +
                                        " parcel queue[%d] queue-top-status[%d]",
                                this, queueTopTask.getId(), parcelQueue.size(), queueTopTask.getStatus()));
            }
            mTask = null;
        }
    }

    @Override
    public void handoverMessage() {
        if (mIsDiscard) {
            return;
        }

        final MessageSnapshot message = parcelQueue.poll();
        final int currentStatus = message.getStatus();
        final BaseDownloadTask.IRunningTask task = mTask;

        Assert.assertTrue(
                FileDownloadUtils.formatString(
                        "can't handover the message, no master to receive this " +
                                "message(status[%d]) size[%d]",
                        currentStatus, parcelQueue.size()),
                task != null);
        final BaseDownloadTask originTask = task.getOrigin();

        final FileDownloadListener listener = originTask.getListener();
        final ITaskHunter.IMessageHandler messageHandler = task.getMessageHandler();

        inspectAndHandleOverStatus(currentStatus);

        if (listener == null || listener.isInvalid()) {
            return;
        }

        if (currentStatus == FileDownloadStatus.blockComplete) {
            try {
                listener.blockComplete(originTask);
                notifyCompleted(((BlockCompleteMessage) message).transmitToCompleted());
            } catch (Throwable throwable) {
                notifyError(messageHandler.prepareErrorMessage(throwable));
            }
        } else {
            FileDownloadLargeFileListener largeFileListener = null;
            if (listener instanceof FileDownloadLargeFileListener) {
                largeFileListener = (FileDownloadLargeFileListener) listener;
            }

            switch (currentStatus) {
                case FileDownloadStatus.pending:
                    if (largeFileListener != null) {
                        largeFileListener.pending(originTask,
                                message.getLargeSofarBytes(),
                                message.getLargeTotalBytes());
                    } else {
                        listener.pending(originTask,
                                message.getSmallSofarBytes(),
                                message.getSmallTotalBytes());
                    }

                    break;
                case FileDownloadStatus.started:
                    listener.started(originTask);
                    break;
                case FileDownloadStatus.connected:
                    if (largeFileListener != null) {
                        largeFileListener.connected(originTask,
                                message.getEtag(),
                                message.isResuming(),
                                originTask.getLargeFileSoFarBytes(),
                                message.getLargeTotalBytes());

                    } else {
                        listener.connected(originTask,
                                message.getEtag(),
                                message.isResuming(),
                                originTask.getSmallFileSoFarBytes(),
                                message.getSmallTotalBytes());
                    }

                    break;
                case FileDownloadStatus.progress:
                    if (largeFileListener != null) {
                        largeFileListener.progress(originTask,
                                message.getLargeSofarBytes(),
                                originTask.getLargeFileTotalBytes());

                    } else {
                        listener.progress(originTask,
                                message.getSmallSofarBytes(),
                                originTask.getSmallFileTotalBytes());
                    }
                    break;
                case FileDownloadStatus.retry:
                    if (largeFileListener != null) {
                        largeFileListener.retry(originTask,
                                message.getThrowable(),
                                message.getRetryingTimes(),
                                message.getLargeSofarBytes());
                    } else {
                        listener.retry(originTask,
                                message.getThrowable(),
                                message.getRetryingTimes(),
                                message.getSmallSofarBytes());
                    }

                    break;
                case FileDownloadStatus.completed:
                    listener.completed(originTask);
                    break;
                case FileDownloadStatus.error:
                    listener.error(originTask,
                            message.getThrowable());
                    break;
                case FileDownloadStatus.paused:
                    if (largeFileListener != null) {
                        largeFileListener.paused(originTask,
                                message.getLargeSofarBytes(),
                                message.getLargeTotalBytes());
                    } else {
                        listener.paused(originTask,
                                message.getSmallSofarBytes(),
                                message.getSmallTotalBytes());
                    }
                    break;
                case FileDownloadStatus.warn:
                    // already same url & path in pending/running list
                    listener.warn(originTask);
                    break;
            }
        }
    }

    @Override
    public boolean handoverDirectly() {
        return mTask.getOrigin().isSyncCallback();
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
        return parcelQueue.peek().getStatus() == FileDownloadStatus.blockComplete;
    }

    @Override
    public void discard() {
        mIsDiscard = true;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("%d:%s", mTask == null ? -1 : mTask.getOrigin().getId(), super.toString());
    }
}
