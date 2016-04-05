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
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import junit.framework.Assert;

import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 12/17/15.
 * <p/>
 * The basic entrance for FileDownloader.
 */
public class FileDownloader {

    /**
     * Just cache Application's Context
     */
    public static void init(final Context context) {
        init(context, null);
    }

    /**
     * Cache {@code context} in Main-Process and FileDownloader-Process; And will init the
     * OkHttpClient in FileDownloader-Process, if the {@code okHttpClientCustomMaker} is provided.
     * <p/>
     * Must be invoked at{@link Application#onCreate()}.
     *
     * @param context                 This context will be hold in FileDownloader, so recommend
     *                                use {@link Application#getApplicationContext()}.
     * @param okHttpClientCustomMaker Nullable, For Customize {@link OkHttpClient},
     *                                Only be used on the ':filedownloader' progress.
     * @see #init(Application)
     * @see com.liulishuo.filedownloader.util.FileDownloadHelper.OkHttpClientCustomMaker
     */
    public static void init(final Context context,
                            FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(FileDownloader.class, "init Downloader");
        }
        FileDownloadHelper.holdContext(context);

        if (okHttpClientCustomMaker != null && FileDownloadUtils.isDownloaderProcess(context)) {
            FileDownloadHelper.setOkHttpClient(okHttpClientCustomMaker.customMake());
        }
    }

    /**
     * @deprecated Consider use {@link #init(Context)} instead.
     */
    public static void init(final Application application) {
        init(application.getApplicationContext());
    }

    /**
     * @deprecated Consider use {@link #init(Context, FileDownloadHelper.OkHttpClientCustomMaker)}
     * instead.
     */
    public static void init(final Application application,
                            FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker) {
        init(application.getApplicationContext(), okHttpClientCustomMaker);
    }

    private final static class HolderClass {
        private final static FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    /**
     * For Avoid Missing Screen Frames.
     * 避免掉帧
     * <p/>
     * Every {@link FileDownloadEventPool#INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadEventPool#SUB_PACKAGE_SIZE} events(callbacks) on the ui thread.
     * <p/>
     * 每{@link FileDownloadEventPool#INTERVAL}毫秒抛最多1个Message到ui线程，并且每次抛到ui线程后，
     * 在ui线程最多处理处理{@link FileDownloadEventPool#SUB_PACKAGE_SIZE} 个回调。
     * <p/>
     * 默认值是10ms，当该值小于0时，每个回调都会立刻抛回ui线程，可能会对UI的Looper照成较大压力，也可能引发掉帧。
     *
     * @param intervalMillisecond interval for ui {@link Handler#post(Runnable)}
     *                            default is {@link FileDownloadEventPool#DEFAULT_INTERVAL}
     *                            if the value is less than 0, each callback will always
     *                            {@link Handler#post(Runnable)} to ui thread immediately, may will
     *                            cause drop frames, may will produce great pressure on the UI Thread Looper
     * @see #enableAvoidDropFrame()
     * @see #disableAvoidDropFrame()
     * @see #setGlobalHandleSubPackageSize(int)
     */
    public static void setGlobalPost2UIInterval(final int intervalMillisecond) {
        FileDownloadEventPool.INTERVAL = intervalMillisecond;
    }

    /**
     * For Avoid Missing Screen Frames.
     * 避免掉帧
     * <p/>
     * Every {@link FileDownloadEventPool#INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadEventPool#SUB_PACKAGE_SIZE} events(callbacks) on the ui thread.
     * <p/>
     * 每{@link FileDownloadEventPool#INTERVAL}毫秒抛最多1个Message到ui线程，并且每次抛到ui线程后，
     * 在ui线程最多处理处理{@link FileDownloadEventPool#SUB_PACKAGE_SIZE} 个回调。
     *
     * @param packageSize per sub-package size for handle event on 1 ui {@link Handler#post(Runnable)}
     *                    default is {@link FileDownloadEventPool#DEFAULT_SUB_PACKAGE_SIZE}
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void setGlobalHandleSubPackageSize(final int packageSize) {
        if (packageSize <= 0) {
            throw new IllegalArgumentException("sub package size must more than 0");
        }
        FileDownloadEventPool.SUB_PACKAGE_SIZE = packageSize;
    }

    /**
     * Avoid missing screen frames, but this leads to all callbacks in {@link FileDownloadListener}
     * do not  be invoked at once when it has already achieved.
     *
     * @see #isEnabledAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void enableAvoidDropFrame() {
        setGlobalPost2UIInterval(FileDownloadEventPool.DEFAULT_INTERVAL);
    }

    /**
     * Disable avoid missing screen frames, let all callbacks in {@link FileDownloadListener} will be invoked
     * at once when it achieve.
     *
     * @see #isEnabledAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void disableAvoidDropFrame() {
        setGlobalPost2UIInterval(-1);
    }

    /**
     * @return has already enabled Avoid Missing Screen Frames
     * @see #enableAvoidDropFrame()
     * @see #disableAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static boolean isEnabledAvoidDropFrame() {
        return FileDownloadEventPool.isIntervalValid();
    }

    /**
     * Create a download task
     */
    public BaseDownloadTask create(final String url) {
        return new FileDownloadTask(url);
    }

    /**
     * Start the download queue by the same listener
     *
     * @param listener start download by same listener
     * @param isSerial is execute them linearly
     */
    public void start(final FileDownloadListener listener, final boolean isSerial) {

        if (listener == null) {
            return;
        }

        final List<BaseDownloadTask> list = FileDownloadList.getImpl().copy(listener);

        if (FileDownloadMonitor.isValid()) {
            FileDownloadMonitor.getMonitor().onRequestStart(list.size(), isSerial, listener);
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.v(this, "start list size[%d] listener[%s] isSerial[%B]", list.size(), listener, isSerial);
        }

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
                downloadTask.start();
            }
        }
    }


    /**
     * Pause the download queue by the same listener
     *
     * @param listener paused download by same listener
     * @see #pause(int)
     */
    public void pause(final FileDownloadListener listener) {
        FileDownloadEventPool.getImpl().shutdownSendPool(listener);
        final List<BaseDownloadTask> downloadList = FileDownloadList.getImpl().copy(listener);
        synchronized (pauseLock) {
            for (BaseDownloadTask baseDownloadTask : downloadList) {
                baseDownloadTask.pause();
            }
        }


    }

    private Runnable pauseAllRunnable;
    private final static Object pauseLock = new Object();

    /**
     * Pause all task
     */
    public void pauseAll() {
        FileDownloadEventPool.getImpl().shutdownSendPool();
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        synchronized (pauseLock) {
            for (BaseDownloadTask baseDownloadTask : downloadList) {
                baseDownloadTask.pause();
            }
        }
        // double check, for case: File Download progress alive but ui progress has died and relived,
        // so FileDownloadList not always contain all running task exactly.
        if (FileDownloadServiceUIGuard.getImpl().isConnected()) {
            FileDownloadServiceUIGuard.getImpl().pauseAllTasks();
        } else {
            if (pauseAllRunnable == null) {
                pauseAllRunnable = new Runnable() {
                    @Override
                    public void run() {
                        FileDownloadServiceUIGuard.getImpl().pauseAllTasks();
                    }
                };
            }
            FileDownloadServiceUIGuard.getImpl().bindStartByContext(FileDownloadHelper.getAppContext(), pauseAllRunnable);
        }

    }

    /**
     * Pause the download task by the downloadId
     *
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

    /**
     * Get downloaded so far bytes by the downloadId
     */
    public long getSoFar(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getSofar(downloadId);
        }

        return downloadTask.getLargeFileSoFarBytes();
    }

    /**
     * Get file total bytes by the downloadId
     */
    public long getTotal(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getTotal(downloadId);
        }

        return downloadTask.getLargeFileTotalBytes();
    }

    /**
     * @param downloadId Download Id
     * @return download status,
     * if service has not connected,
     * will be {@link com.liulishuo.filedownloader.model.FileDownloadStatus#INVALID_STATUS}
     * if already has over(error,paused,completed,warn),
     * will come from File Download Service
     * if there are no data in File Download Service ,
     * will be {@link com.liulishuo.filedownloader.model.FileDownloadStatus#INVALID_STATUS}
     */
    public int getStatus(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getStatus(downloadId);
        }

        return downloadTask.getStatus();
    }

    /**
     * Bind & start ':filedownloader' process manually(Do not need, will bind & start automatically by Download Engine if real need)
     */
    public void bindService() {
        if (!isServiceConnected()) {
            FileDownloadServiceUIGuard.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
        }
    }

    /**
     * Unbind & stop ':filedownloader' process manually(Do not need, will unbind & stop automatically by System if leave unused period)
     */
    public void unBindService() {
        if (isServiceConnected()) {
            FileDownloadServiceUIGuard.getImpl().unbindByContext(FileDownloadHelper.getAppContext());
        }
    }

    public boolean unBindServiceIfIdle() {
        // check idle
        if (!isServiceConnected()) {
            return false;
        }

        if (FileDownloadList.getImpl().isEmpty()
                && FileDownloadServiceUIGuard.getImpl().isIdle()) {
            unBindService();
            return true;
        }

        return false;
    }

    /**
     * @return has connected File Download service
     */
    public boolean isServiceConnected() {
        return FileDownloadServiceUIGuard.getImpl().isConnected();
    }

    /**
     * @param listener add listener for listening File Download connect/disconnect moment
     * @see #removeServiceConnectListener(FileDownloadConnectListener)
     */
    public void addServiceConnectListener(final FileDownloadConnectListener listener) {
        FileDownloadEventPool.getImpl().addListener(DownloadServiceConnectChangedEvent.ID
                , listener);
    }

    /**
     * @param listener remove listener which has been
     *                 added by {@link #addServiceConnectListener(FileDownloadConnectListener)}
     * @see #addServiceConnectListener(FileDownloadConnectListener)
     */
    public void removeServiceConnectListener(final FileDownloadConnectListener listener) {
        FileDownloadEventPool.getImpl().removeListener(DownloadServiceConnectChangedEvent.ID
                , listener);
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

                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(SerialHandlerCallback.class, "final serial %s %d",
                                this.list == null ? null : this.list.get(0) == null ? null : this.list.get(0).getListener(),
                                msg.arg1);
                    }
                    return true;
                }

                final BaseDownloadTask task = this.list.get(msg.arg1);
                synchronized (pauseLock) {
                    if (!FileDownloadList.getImpl().contains(task)) {
                        // pause?
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(SerialHandlerCallback.class, "direct go next by not contains %s %d", task, msg.arg1);
                        }
                        goNext(msg.arg1 + 1);
                        return true;
                    }
                }


                list.get(msg.arg1)
                        .addFinishListener(new BaseDownloadTask.FinishListener() {
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

}
