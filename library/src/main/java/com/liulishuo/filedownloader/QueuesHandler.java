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

import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Jacksgong on 20/08/2016.
 * <p>
 * The default queue handler.
 */

class QueuesHandler implements IQueuesHandler {

    private final Object mPauseLock;
    private final SparseArray<Handler> mRunningSerialMap;

    public QueuesHandler(Object pauseLock) {
        this.mPauseLock = pauseLock;
        this.mRunningSerialMap = new SparseArray<>();
    }

    @Override
    public boolean startQueueParallel(FileDownloadListener listener) {
        final int attachKey = listener.hashCode();

        final List<BaseDownloadTask> list = FileDownloadList.getImpl().
                assembleTasksToStart(attachKey, listener);

        if (onAssembledTasksToStart(attachKey, list, listener, false)) {
            return false;
        }

        for (BaseDownloadTask task : list) {
            task.start();
        }

        return true;
    }

    @Override
    public boolean startQueueSerial(FileDownloadListener listener) {
        final SerialHandlerCallback callback = new SerialHandlerCallback();
        final int attachKey = callback.hashCode();

        final List<BaseDownloadTask> list = FileDownloadList.getImpl().
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
    public void unFreezeAllSerialQueues() {
        for (int i = 0; i < mRunningSerialMap.size(); i++) {
            final int key = mRunningSerialMap.keyAt(i);
            Handler handler = mRunningSerialMap.get(key);
            unFreezeSerialHandler(handler);
        }

    }

    @Override
    public boolean contain(int attachKey) {
        return mRunningSerialMap.get(attachKey) != null;
    }

    private boolean onAssembledTasksToStart(int attachKey, final List<BaseDownloadTask> list,
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
                            "because can't find any task with the provided listener: [%s, %B]",
                    listener, isSerial);

            return true;
        }

        return false;

    }


    final static int WHAT_SERIAL_NEXT = 1;
    final static int WHAT_FREEZE = 2;
    final static int WHAT_UNFREEZE = 3;


    private class SerialHandlerCallback implements Handler.Callback {
        private Handler handler;
        private List<BaseDownloadTask> list;
        private int runningIndex = 0;
        private SerialFinishListener serialFinishListener;

        SerialHandlerCallback() {
            serialFinishListener =
                    new SerialFinishListener(new WeakReference<>(this));
        }

        public void setHandler(final Handler handler) {
            this.handler = handler;
        }

        public void setList(List<BaseDownloadTask> list) {
            this.list = list;
        }

        @Override
        public boolean handleMessage(final Message msg) {
            if (msg.what == WHAT_SERIAL_NEXT) {
                if (msg.arg1 >= list.size()) {
                    synchronized (mRunningSerialMap) {
                        mRunningSerialMap.remove(list.get(0).attachKey);
                    }
                    // final serial tasks
                    if (this.handler != null && this.handler.getLooper() != null) {
                        this.handler.getLooper().quit();
                        this.handler = null;
                        this.list = null;
                        this.serialFinishListener = null;
                    }

                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(SerialHandlerCallback.class, "final serial %s %d",
                                this.list == null ? null : this.list.get(0) == null ? null : this.list.get(0).getListener(),
                                msg.arg1);
                    }
                    return true;
                }

                runningIndex = msg.arg1;
                final BaseDownloadTask stackTopTask = this.list.get(runningIndex);
                synchronized (mPauseLock) {
                    if (!FileDownloadList.getImpl().contains(stackTopTask)) {
                        // pause?
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(SerialHandlerCallback.class,
                                    "direct go next by not contains %s %d", stackTopTask, msg.arg1);
                        }
                        goNext(msg.arg1 + 1);
                        return true;
                    }
                }


                stackTopTask
                        .addFinishListener(serialFinishListener.setNextIndex(runningIndex + 1))
                        .start();

            } else if (msg.what == WHAT_FREEZE) {
                freeze();
            } else if (msg.what == WHAT_UNFREEZE) {
                unfreeze();
            }
            return true;
        }

        public void freeze() {
            list.get(runningIndex).removeFinishListener(serialFinishListener);
            handler.removeCallbacksAndMessages(null);
        }

        public void unfreeze() {
            goNext(runningIndex);
        }

        private void goNext(final int nextIndex) {
            if (this.handler == null || this.list == null) {
                FileDownloadLog.w(this, "need go next %d, but params is not ready %s %s",
                        nextIndex, this.handler, this.list);
                return;
            }

            Message nextMsg = this.handler.obtainMessage();
            nextMsg.what = WHAT_SERIAL_NEXT;
            nextMsg.arg1 = nextIndex;
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(SerialHandlerCallback.class, "start next %s %s",
                        this.list == null ? null : this.list.get(0) == null ? null :
                                this.list.get(0).getListener(), nextMsg.arg1);
            }
            this.handler.sendMessage(nextMsg);
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
