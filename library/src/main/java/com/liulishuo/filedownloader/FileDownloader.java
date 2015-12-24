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

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventPoolImpl;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class FileDownloader {

    /**
     * 不耗时，做一些简单初始化准备工作，不会启动下载进程
     * <p/>
     * 建议在{@link Application#onCreate()}时调用
     */
    public static void init(final Application application) {
        // 下载进程与非下载进程都存一个
        FileDownloadLog.d(FileDownloader.class, "init Downloader");
        FileDownloadHelper.initAppContext(application);
        DownloadEventPool.setImpl(new DownloadEventPoolImpl());
    }

    private final static class HolderClass {
        private final static FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    public BaseDownloadTask create(final String url) {
        return new FileDownloadTask(url);
    }

    /**
     * @param listener start download by same listener
     * @param isSerial 是否需要串行
     */
    public List<Integer> start(final FileDownloadListener listener, final boolean isSerial) {

        if (listener == null) {
            return null;
        }

        final List<Integer> ids = new ArrayList<>();
        final List<BaseDownloadTask> list = FileDownloadList.getImpl().copy(listener);
        for (BaseDownloadTask task : list) {
            ids.add(task.getDownloadId());
        }

        FileDownloadLog.v(this, "start list size[%d] listener[%s] isSerial[%B]", list.size(), listener, isSerial);

        if (isSerial) {
            // serial
            final Handler serialHandler = createSerialHandler(list);
            Message msg = serialHandler.obtainMessage();
            msg.what = WHAT_SERIAL_NEXT;
            msg.arg1 = 0;
            serialHandler.sendMessage(msg);
        } else {
            // parallel
            for (final BaseDownloadTask downloadTask : list) {
                ids.add(downloadTask.start());
            }
        }


        return ids;
    }


    /**
     * @param listener paused download by same listener
     * @see #pause(int)
     */
    public void pause(final FileDownloadListener listener) {
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        for (BaseDownloadTask baseDownloadTask : downloadList) {
            if (baseDownloadTask.getListener() == listener) {
                baseDownloadTask.pause();
            }
        }

    }

    public void pauseAll() {
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        for (BaseDownloadTask baseDownloadTask : downloadList) {
            baseDownloadTask.pause();
        }
    }

    /**
     * @param downloadId pause download by download id
     * @see #pause(FileDownloadListener)
     */
    public void pause(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            FileDownloadLog.w(this, "request pause but not exist %d", downloadId);
            return;
        }
        downloadTask.pause();
    }

    public int getSoFar(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getSofar(downloadId);
        }

        return downloadTask.getSoFarBytes();
    }

    public int getTotal(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getTotal(downloadId);
        }

        return downloadTask.getTotalBytes();
    }

    /**
     * 可以提前绑定服务，提高第一次启动下载的耗时
     */
    public void bindService() {
        if (!FileDownloadServiceUIGuard.getImpl().isConnected()) {
            FileDownloadServiceUIGuard.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
        }
    }

    public void unBindService() {
        if (FileDownloadServiceUIGuard.getImpl().isConnected()) {
            FileDownloadServiceUIGuard.getImpl().unbindByContext(FileDownloadHelper.getAppContext());
        }
    }

    private static Handler createSerialHandler(final List<BaseDownloadTask> serialTasks) {
        Assert.assertTrue("create serial handler list must not empty", serialTasks != null && serialTasks.size() > 0);


        final HandlerThread serialThread = new HandlerThread(String.format("filedownloader serial thread %s",
                serialTasks.get(0).getListener()));
        serialThread.start();

        final SerialHandlerCallback callback = new SerialHandlerCallback();
        final Handler serialHandler = new Handler(serialThread.getLooper(), callback);
        callback.setHandler(serialHandler);
        callback.setList(serialTasks);

        return serialHandler;
    }


    final static int WHAT_SERIAL_NEXT = 1;

    private static class SerialHandlerCallback implements Handler.Callback {
        private Handler handler;
        private List<BaseDownloadTask> list;

        public Handler.Callback setHandler(final Handler handler) {
            this.handler = handler;
            return this;
        }

        public Handler.Callback setList(List<BaseDownloadTask> list) {
            this.list = list;
            return this;
        }

        @Override
        public boolean handleMessage(final Message msg) {
            if (msg.what == WHAT_SERIAL_NEXT) {
                if (msg.arg1 >= list.size()) {
                    // final serial tasks
                    if (this.handler != null && this.handler.getLooper() != null) {
                        this.handler.getLooper().quit();
                        this.handler = null;
                        this.list = null;
                    }

                    FileDownloadLog.d(SerialHandlerCallback.class, "final serial %s %d",
                            this.list == null ? null : this.list.get(0) == null ? null : this.list.get(0).getListener(),
                            msg.arg1);
                    return true;
                }

                final BaseDownloadTask task = this.list.get(msg.arg1);
                if (!FileDownloadList.getImpl().contains(task)) {
                    // pause?
                    FileDownloadLog.d(SerialHandlerCallback.class, "direct go next by not contains %s %d", task, msg.arg1);
                    goNext(msg.arg1 + 1);
                    return true;
                }

                list.get(msg.arg1)
                        .setFinishListener(new BaseDownloadTask.FinishListener() {
                            private int index;

                            public BaseDownloadTask.FinishListener setIndex(int index) {
                                this.index = index;
                                return this;
                            }

                            @Override
                            public void over() {
                                goNext(this.index);
                            }
                        }.setIndex(msg.arg1 + 1))
                        .start();

            }
            return true;
        }

        private void goNext(final int nextIndex) {
            if (this.handler == null || this.list == null) {
                FileDownloadLog.w(this, "need go next %d, but params is not ready %s %s", nextIndex, this.handler, this.list);
                return;
            }

            Message nextMsg = this.handler.obtainMessage();
            nextMsg.what = WHAT_SERIAL_NEXT;
            nextMsg.arg1 = nextIndex;
            FileDownloadLog.d(SerialHandlerCallback.class, "start next %s %s",
                    this.list == null ? null : this.list.get(0) == null ? null :
                            this.list.get(0).getListener(), nextMsg.arg1);
            this.handler.sendMessage(nextMsg);
        }
    }

}
