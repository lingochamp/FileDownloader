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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseArray;

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The default queue handler.
 */

class QueuesHandler implements IQueuesHandler {

    private final SparseArray<Handler> mRunningSerialMap;

    public QueuesHandler() {
        this.mRunningSerialMap = new SparseArray<>();
    }

    @Override
    public boolean startQueueParallel(FileDownloadListener listener) {
        final int attachKey = listener.hashCode();

        final List<BaseDownloadTask.IRunningTask> list = FileDownloadList.getImpl().
                assembleTasksToStart(attachKey, listener);

        if (onAssembledTasksToStart(attachKey, list, listener, false)) {
            return false;
        }

        for (BaseDownloadTask.IRunningTask task : list) {
            task.startTaskByQueue();
        }

        return true;
    }

    @Override
    public boolean startQueueSerial(FileDownloadListener listener) {
        final SerialHandlerCallback callback = new SerialHandlerCallback();
        final int attachKey = callback.hashCode();

        final List<BaseDownloadTask.IRunningTask> list = FileDownloadList.getImpl().
                assembleTasksToStart(attachKey, listener);

        if (onAssembledTasksToStart(attachKey, list, listener, true)) {
            return false;
        }

        final HandlerThread serialThread = new HandlerThread(
                FileDownloadUtils.formatString("filedownloader serial thread %s-%d",
                        listener, attachKey));
        serialThread.start();

        final Handler serialHandler = new Handler(serialThread.getLooper(), callback);
        callback.setHandler(serialHandler);
        callback.setList(list);

        callback.goNext(0);

        synchronized (mRunningSerialMap) {
            mRunningSerialMap.put(attachKey, serialHandler);
        }

        return true;
    }

    @Override
    public void freezeAllSerialQueues() {
        for (int i = 0; i < mRunningSerialMap.size(); i++) {
            final int key = mRunningSerialMap.keyAt(i);
            Handler handler = mRunningSerialMap.get(key);
            freezeSerialHandler(handler);
        }
    }

    @Override
    public void unFreezeSerialQueues(List<Integer> attachKeyList) {
        for (Integer attachKey : attachKeyList) {
            final Handler handler = mRunningSerialMap.get(attachKey);
            unFreezeSerialHandler(handler);
        }
    }

    @Override
    public int serialQueueSize() {
        return mRunningSerialMap.size();
    }

    @Override
    public boolean contain(int attachKey) {
        return mRunningSerialMap.get(attachKey) != null;
    }

    private boolean onAssembledTasksToStart(int attachKey, final List<BaseDownloadTask.IRunningTask> list,
                                            final FileDownloadListener listener, boolean isSerial) {
        if (FileDownloadMonitor.isValid()) {
            FileDownloadMonitor.getMonitor().onRequestStart(list.size(), true, listener);
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.v(FileDownloader.class, "start list attachKey[%d] size[%d] " +
                    "listener[%s] isSerial[%B]", attachKey, list.size(), listener, isSerial);
        }

        if (list == null || list.isEmpty()) {
            FileDownloadLog.w(FileDownloader.class, "Tasks with the listener can't start, " +
                            "because can't find any task with the provided listener, maybe tasks " +
                            "instance has been started in the past, so they are all are inUsing, if " +
                            "in this case, you can use [BaseDownloadTask#reuse] to reuse theme " +
                            "first then start again: [%s, %B]",
                    listener, isSerial);

            return true;
        }

        return false;

    }


    final static int WHAT_SERIAL_NEXT = 1;
    final static int WHAT_FREEZE = 2;
    final static int WHAT_UNFREEZE = 3;


    private class SerialHandlerCallback implements Handler.Callback {
        private Handler mHandler;
        private List<BaseDownloadTask.IRunningTask> mList;
        private int mRunningIndex = 0;
        private SerialFinishListener mSerialFinishListener;

        SerialHandlerCallback() {
            mSerialFinishListener =
                    new SerialFinishListener(new WeakReference<>(this));
        }

        public void setHandler(final Handler handler) {
            this.mHandler = handler;
        }

        public void setList(List<BaseDownloadTask.IRunningTask> list) {
            this.mList = list;
        }

        @Override
        public boolean handleMessage(final Message msg) {
            if (msg.what == WHAT_SERIAL_NEXT) {
                if (msg.arg1 >= mList.size()) {
                    synchronized (mRunningSerialMap) {
                        mRunningSerialMap.remove(mList.get(0).getAttachKey());
                    }
                    // final serial tasks
                    if (this.mHandler != null && this.mHandler.getLooper() != null) {
                        this.mHandler.getLooper().quit();
                        this.mHandler = null;
                        this.mList = null;
                        this.mSerialFinishListener = null;
                    }

                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(SerialHandlerCallback.class, "final serial %s %d",
                                this.mList == null ? null : this.mList.get(0) == null ?
                                        null : this.mList.get(0).getOrigin().getListener(),
                                msg.arg1);
                    }
                    return true;
                }

                mRunningIndex = msg.arg1;
                final BaseDownloadTask.IRunningTask stackTopTask = this.mList.get(mRunningIndex);
                synchronized (stackTopTask.getPauseLock()) {
                    if (stackTopTask.getOrigin().getStatus() != FileDownloadStatus.INVALID_STATUS ||
                            FileDownloadList.getImpl().isNotContains(stackTopTask)) {
                        // pause?
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(SerialHandlerCallback.class,
                                    "direct go next by not contains %s %d", stackTopTask, msg.arg1);
                        }
                        goNext(msg.arg1 + 1);
                        return true;
                    }

                    stackTopTask.getOrigin()
                            .addFinishListener(mSerialFinishListener.setNextIndex(mRunningIndex + 1));
                    stackTopTask.startTaskByQueue();
                }

            } else if (msg.what == WHAT_FREEZE) {
                freeze();
            } else if (msg.what == WHAT_UNFREEZE) {
                unfreeze();
            }
            return true;
        }

        public void freeze() {
            mList.get(mRunningIndex).getOrigin().removeFinishListener(mSerialFinishListener);
            mHandler.removeCallbacksAndMessages(null);
        }

        public void unfreeze() {
            goNext(mRunningIndex);
        }

        private void goNext(final int nextIndex) {
            if (this.mHandler == null || this.mList == null) {
                FileDownloadLog.w(this, "need go next %d, but params is not ready %s %s",
                        nextIndex, this.mHandler, this.mList);
                return;
            }

            Message nextMsg = this.mHandler.obtainMessage();
            nextMsg.what = WHAT_SERIAL_NEXT;
            nextMsg.arg1 = nextIndex;
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(SerialHandlerCallback.class, "start next %s %s",
                        this.mList == null ? null : this.mList.get(0) == null ? null :
                                this.mList.get(0).getOrigin().getListener(), nextMsg.arg1);
            }
            this.mHandler.sendMessage(nextMsg);
        }
    }

    private static class SerialFinishListener implements BaseDownloadTask.FinishListener {
        private final WeakReference<SerialHandlerCallback> wSerialHandlerCallback;

        private SerialFinishListener(WeakReference<SerialHandlerCallback> wSerialHandlerCallback) {
            this.wSerialHandlerCallback = wSerialHandlerCallback;
        }

        private int nextIndex;

        public BaseDownloadTask.FinishListener setNextIndex(int index) {
            this.nextIndex = index;
            return this;
        }

        @Override
        public void over(final BaseDownloadTask task) {
            if (wSerialHandlerCallback != null && wSerialHandlerCallback.get() != null) {
                wSerialHandlerCallback.get().goNext(this.nextIndex);
            }
        }
    }

    private void freezeSerialHandler(Handler handler) {
        handler.sendEmptyMessage(WHAT_FREEZE);
    }

    private void unFreezeSerialHandler(Handler handler) {
        handler.sendEmptyMessage(WHAT_UNFREEZE);
    }
}
