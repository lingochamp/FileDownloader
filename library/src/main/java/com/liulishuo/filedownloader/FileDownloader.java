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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import junit.framework.Assert;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 12/17/15.
 * <p/>
 * The basic entrance for FileDownloader.
 *
 * @see com.liulishuo.filedownloader.services.FileDownloadService The service for FileDownloader.
 * @see FileDownloadProperties
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloader {

    /**
     * Just cache Application's Context
     *
     * @see #init(Context, FileDownloadHelper.OkHttpClientCustomMaker, int)
     */
    public static void init(final Context context) {
        init(context, null);
    }


    /**
     * @see #init(Context, FileDownloadHelper.OkHttpClientCustomMaker, int)
     */
    public static void init(final Context context,
                            /** Nullable **/FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker) {
        init(context, okHttpClientCustomMaker, 0);

    }

    /**
     * Cache {@code context} in Main-Process and FileDownloader-Process; And will init the
     * OkHttpClient in FileDownloader-Process, if the {@code okHttpClientCustomMaker} is provided.
     * <p/>
     * Must be invoked at{@link Application#onCreate()} in the Main process and the filedownloader
     * process.
     *
     * @param context                 This context will be hold in FileDownloader, so recommend
     *                                use {@link Application#getApplicationContext()}.
     * @param okHttpClientCustomMaker Nullable, For Customize {@link OkHttpClient},
     *                                Only be used on the ':filedownloader' progress.
     * @param maxNetworkThreadCount   The max network thread count, what is the number of
     *                                simultaneous downloads in FileDownloader.
     *                                If this value is 0, the value will be ignored and use
     *                                {@link FileDownloadProperties#DOWNLOAD_MAX_NETWORK_THREAD_COUNT}
     *                                which is defined in filedownloader.properties instead.
     * @see #init(Application)
     * @see com.liulishuo.filedownloader.util.FileDownloadHelper.OkHttpClientCustomMaker
     * @see #setMaxNetworkThreadCount(int)
     */
    public static void init(final Context context,
                            /** Nullable **/final FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker,
                            /** [1,12] **/final int maxNetworkThreadCount) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(FileDownloader.class, "init Downloader");
        }
        FileDownloadHelper.holdContext(context);

        if (FileDownloadUtils.isDownloaderProcess(context)) {
            FileDownloadHelper.initializeDownloadMgrParams(okHttpClientCustomMaker,
                    maxNetworkThreadCount);

            try {
                FileDownloadUtils.setMinProgressStep(FileDownloadProperties.getImpl().DOWNLOAD_MIN_PROGRESS_STEP);
                FileDownloadUtils.setMinProgressTime(FileDownloadProperties.getImpl().DOWNLOAD_MIN_PROGRESS_TIME);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
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
     * Every {@link FileDownloadMessageStation#INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadMessageStation#SUB_PACKAGE_SIZE} events(callbacks) on the ui thread.
     * <p/>
     * 每{@link FileDownloadMessageStation#INTERVAL}毫秒抛最多1个Message到ui线程，并且每次抛到ui线程后，
     * 在ui线程最多处理处理{@link FileDownloadMessageStation#SUB_PACKAGE_SIZE} 个回调。
     * <p/>
     * 默认值是10ms，当该值小于0时，每个回调都会立刻抛回ui线程，可能会对UI的Looper照成较大压力，也可能引发掉帧。
     *
     * @param intervalMillisecond interval for ui {@link Handler#post(Runnable)}
     *                            default is {@link FileDownloadMessageStation#DEFAULT_INTERVAL}
     *                            if the value is less than 0, each callback will always
     *                            {@link Handler#post(Runnable)} to ui thread immediately, may will
     *                            cause drop frames, may will produce great pressure on the UI Thread Looper
     * @see #enableAvoidDropFrame()
     * @see #disableAvoidDropFrame()
     * @see #setGlobalHandleSubPackageSize(int)
     */
    public static void setGlobalPost2UIInterval(final int intervalMillisecond) {
        FileDownloadMessageStation.INTERVAL = intervalMillisecond;
    }

    /**
     * For Avoid Missing Screen Frames.
     * 避免掉帧
     * <p/>
     * Every {@link FileDownloadMessageStation#INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadMessageStation#SUB_PACKAGE_SIZE} events(callbacks) on the ui thread.
     * <p/>
     * 每{@link FileDownloadMessageStation#INTERVAL}毫秒抛最多1个Message到ui线程，并且每次抛到ui线程后，
     * 在ui线程最多处理处理{@link FileDownloadMessageStation#SUB_PACKAGE_SIZE} 个回调。
     *
     * @param packageSize per sub-package size for handle event on 1 ui {@link Handler#post(Runnable)}
     *                    default is {@link FileDownloadMessageStation#DEFAULT_SUB_PACKAGE_SIZE}
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void setGlobalHandleSubPackageSize(final int packageSize) {
        if (packageSize <= 0) {
            throw new IllegalArgumentException("sub package size must more than 0");
        }
        FileDownloadMessageStation.SUB_PACKAGE_SIZE = packageSize;
    }

    /**
     * Avoid missing screen frames, but this leads to all callbacks in {@link FileDownloadListener}
     * do not  be invoked at once when it has already achieved.
     *
     * @see #isEnabledAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void enableAvoidDropFrame() {
        setGlobalPost2UIInterval(FileDownloadMessageStation.DEFAULT_INTERVAL);
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
        return FileDownloadMessageStation.isIntervalValid();
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
            //TODO PROVIDING LOG.
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
            synchronized (RUNNING_SERIAL_MAP) {
                RUNNING_SERIAL_MAP.put(listener, serialHandler);
            }
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
        FileDownloadTaskLauncher.getImpl().expire(listener);
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
     * Pause all tasks
     */
    public void pauseAll() {
        FileDownloadTaskLauncher.getImpl().expireAll();
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        synchronized (pauseLock) {
            for (BaseDownloadTask baseDownloadTask : downloadList) {
                baseDownloadTask.pause();
            }
        }
        // double check, for case: File Download progress alive but ui progress has died and relived,
        // so FileDownloadList not always contain all running task exactly.
        if (FileDownloadServiceProxy.getImpl().isConnected()) {
            FileDownloadServiceProxy.getImpl().pauseAllTasks();
        } else {
            if (pauseAllRunnable == null) {
                pauseAllRunnable = new Runnable() {
                    @Override
                    public void run() {
                        FileDownloadServiceProxy.getImpl().pauseAllTasks();
                    }
                };
            }
            FileDownloadServiceProxy.getImpl().bindStartByContext(FileDownloadHelper.getAppContext(), pauseAllRunnable);
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
            return FileDownloadServiceProxy.getImpl().getSofar(downloadId);
        }

        return downloadTask.getLargeFileSoFarBytes();
    }

    /**
     * Get file total bytes by the downloadId
     */
    public long getTotal(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceProxy.getImpl().getTotal(downloadId);
        }

        return downloadTask.getLargeFileTotalBytes();
    }

    /**
     * @param id The downloadId.
     * @return The downloading status without cover the completed status (if completed you will receive
     * {@link FileDownloadStatus#INVALID_STATUS} ).
     * @see #getStatus(String, String)
     * @see #getStatus(int, String)
     */
    public byte getStatusIgnoreCompleted(final int id) {
        return getStatus(id, null);
    }

    /**
     * @param url  The downloading URL.
     * @param path The downloading file's path.
     * @return The downloading status.
     * @see #getStatus(int, String)
     * @see #getStatusIgnoreCompleted(int)
     */
    public byte getStatus(final String url, final String path) {
        return getStatus(FileDownloadUtils.generateId(url, path), path);
    }

    /**
     * @param downloadId The downloadId.
     * @param path       Use to judge whether has already completed downloading.
     * @return the downloading status.
     * @see FileDownloadStatus
     * @see #getStatus(String, String)
     * @see #getStatusIgnoreCompleted(int)
     */
    public byte getStatus(final int downloadId, final String path) {
        byte status;
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            status = FileDownloadServiceProxy.getImpl().getStatus(downloadId);
        } else {
            status = downloadTask.getStatus();
        }

        if (path != null && status == FileDownloadStatus.INVALID_STATUS) {
            if (FileDownloadUtils.isFilenameConverted(FileDownloadHelper.getAppContext()) &&
                    new File(path).exists()) {
                status = FileDownloadStatus.completed;
            }
        }

        return status;
    }

    /**
     * Find the running task by {@code url} and default path, and replace its listener with
     * the new one {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0.
     * @see #replaceListener(int, FileDownloadListener)
     * @see #replaceListener(String, String, FileDownloadListener)
     */
    public int replaceListener(String url, FileDownloadListener listener) {
        return replaceListener(url, FileDownloadUtils.getDefaultSaveFilePath(url), listener);
    }

    /**
     * Find the running task by {@code url} and {@code path}, and replace its listener with
     * the new one {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0.
     * @see #replaceListener(String, FileDownloadListener)
     * @see #replaceListener(int, FileDownloadListener)
     */
    public int replaceListener(String url, String path, FileDownloadListener listener) {
        return replaceListener(FileDownloadUtils.generateId(url, path), listener);
    }

    /**
     * Find the running task by {@code id}, and replace its listener width the new one
     * {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0.
     * @see #replaceListener(String, FileDownloadListener)
     * @see #replaceListener(String, String, FileDownloadListener)
     */
    public int replaceListener(int id, FileDownloadListener listener) {
        final BaseDownloadTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            return 0;
        }

        task.setListener(listener);
        return task.getId();
    }

    /**
     * Bind & start ':filedownloader' process manually(Do not need, will bind & start automatically by Download Engine if real need)
     */
    public void bindService() {
        if (!isServiceConnected()) {
            FileDownloadServiceProxy.getImpl().bindStartByContext(FileDownloadHelper.getAppContext());
        }
    }

    /**
     * Unbind & stop ':filedownloader' process manually(Do not need, will unbind & stop automatically by System if leave unused period)
     */
    public void unBindService() {
        if (isServiceConnected()) {
            FileDownloadServiceProxy.getImpl().unbindByContext(FileDownloadHelper.getAppContext());
        }
    }

    public boolean unBindServiceIfIdle() {
        // check idle
        if (!isServiceConnected()) {
            return false;
        }

        if (FileDownloadList.getImpl().isEmpty()
                && FileDownloadServiceProxy.getImpl().isIdle()) {
            unBindService();
            return true;
        }

        return false;
    }

    /**
     * @return has connected File Download service
     */
    public boolean isServiceConnected() {
        return FileDownloadServiceProxy.getImpl().isConnected();
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

    /**
     * In foreground status, will save the FileDownloader alive, even user kill the application from
     * recent apps.
     * <p/>
     * Make FileDownloader service run in the foreground, supplying the ongoing
     * notification to be shown to the user while in this state.
     * By default FileDownloader services are background, meaning that if the system needs to
     * kill them to reclaim more memory (such as to display a large page in a
     * web browser), they can be killed without too much harm.  You can set this
     * flag if killing your service would be disruptive to the user, such as
     * if your service is performing background downloading, so the user
     * would notice if their app stopped downloading.
     *
     * @param id           The identifier for this notification as per
     *                     {@link NotificationManager#notify(int, Notification)
     *                     NotificationManager.notify(int, Notification)}; must not be 0.
     * @param notification The Notification to be displayed.
     * @see #stopForeground(boolean)
     */
    public void startForeground(int id, Notification notification) {
        FileDownloadServiceProxy.getImpl().startForeground(id, notification);
    }

    /**
     * Remove FileDownload service from foreground state, allowing it to be killed if
     * more memory is needed.
     *
     * @param removeNotification If true, the notification previously provided
     *                           to {@link #startForeground} will be removed.  Otherwise it will remain
     *                           until a later call removes it (or the service is destroyed).
     * @see #startForeground(int, Notification)
     */
    public void stopForeground(boolean removeNotification) {
        FileDownloadServiceProxy.getImpl().stopForeground(removeNotification);
    }

    /**
     * @param url        The url of the completed task.
     * @param path       The absolute path of the completed task's save file.
     * @param totalBytes The content-length of the completed task, the length of the file in the
     *                   {@code path} must be equal to this value.
     * @return Whether is successful to set the task completed. If the {@code path} not exist will be
     * false; If the length of the file in {@code path} is not equal to {@code totalBytes} will be
     * false; If the task with {@code url} and {@code path} is downloading will be false. Otherwise
     * will be true.
     * @see FileDownloadUtils#isFilenameConverted(Context)
     * <p>
     * <p/>
     * Recommend used to telling the FileDownloader Engine that the task with the {@code url}  and
     * the {@code path} has already completed downloading, in case of your task has already
     * downloaded by other ways(not by FileDownloader Engine), and after success to set the task
     * completed, FileDownloader will check the task with {@code url} and the {@code path} whether
     * completed by {@code totalBytes}.
     * <p/>
     * Otherwise, If FileDownloader Engine isn't know your task's status, whatever your task with
     * the {@code url} and the {@code path} has already downloaded in other way, FileDownloader
     * Engine will ignore the exist file and redownload it, because FileDownloader Engine don't know
     * the exist file whether it is valid.
     * @see #setTaskCompleted(List)
     * @deprecated If you invoked this method, please remove it directly feel free, it doesn't need
     * any longer. In new mechanism(filedownloader 0.3.3 or higher), FileDownloader doesn't store
     * completed tasks in Database anymore, because all downloading files have temp a file name.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean setTaskCompleted(String url, String path, long totalBytes) {
        FileDownloadLog.w(this, "If you invoked this method, please remove it directly feel free, " +
                "it doesn't need any longer");
        return true;
    }

    /**
     * Recommend used to telling the FileDownloader Engine that a bulk of tasks have already
     * downloaded by other ways(not by the FileDownloader Engine).
     * <p/>
     * The FileDownloader Engine need to know the status of completed, because if you want to
     * download any tasks, FileDownloader Engine judges whether the task need downloads or not
     * according its status which existed in DB.
     *
     * @param taskAtomList The bulk of tasks.
     * @return Whether is successful to update all tasks' status to the Filedownloader Engine. If
     * one task atom among them is not match the Rules in
     * FileDownloadMgr#obtainCompletedTaskShelfModel(String, String, long)
     * will receive false, and non of them would be updated to DB.
     * @see #setTaskCompleted(String, String, long)
     * @deprecated If you invoked this method, please remove it directly feel free, it doesn't need
     * any longer. In new mechanism(filedownloader 0.3.3 or higher), FileDownloader doesn't store
     * completed tasks in Database anymore, because all downloading files have temp a file name.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean setTaskCompleted(@SuppressWarnings("deprecation") List<FileDownloadTaskAtom> taskAtomList) {
        FileDownloadLog.w(this, "If you invoked this method, please remove it directly feel free, " +
                "it doesn't need any longer");
        return true;
    }

    /**
     * Set the max network thread count, what is the number of simultaneous downloads in
     * FileDownloader.
     *
     * @param count the number of simultaneous downloads, scope: [1, 12].
     * @return whether is successful to set the max network thread count.
     * If there are any actively executing tasks in FileDownloader, you will receive a warn
     * priority log int the logcat and this operation would be failed.
     * @see #init(Context, FileDownloadHelper.OkHttpClientCustomMaker, int)
     */
    public boolean setMaxNetworkThreadCount(final int count) {
        if (!FileDownloadList.getImpl().isEmpty()) {
            FileDownloadLog.w(this, "Can't change the max network thread count, because there " +
                    "are actively executing tasks in FileDownloader, please try again after all" +
                    " actively executing tasks are completed or invoking FileDownloader#pauseAll" +
                    " directly.");
            return false;
        }

        return FileDownloadServiceProxy.getImpl().setMaxNetworkThreadCount(count);
    }

    private static Handler createSerialHandler(final List<BaseDownloadTask> serialTasks) {
        Assert.assertTrue("create serial handler list must not empty", serialTasks != null && serialTasks.size() > 0);


        final HandlerThread serialThread = new HandlerThread(
                FileDownloadUtils.formatString("filedownloader serial thread %s",
                        serialTasks.get(0).getListener()));
        serialThread.start();

        final SerialHandlerCallback callback = new SerialHandlerCallback();
        final Handler serialHandler = new Handler(serialThread.getLooper(), callback);
        callback.setHandler(serialHandler);
        callback.setList(serialTasks);

        return serialHandler;
    }


    final static HashMap<FileDownloadListener, Handler> RUNNING_SERIAL_MAP = new HashMap<>();

    final static int WHAT_SERIAL_NEXT = 1;
    final static int WHAT_FREEZE = 2;
    final static int WHAT_UNFREEZE = 3;

    private static class SerialHandlerCallback implements Handler.Callback {
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
                    synchronized (RUNNING_SERIAL_MAP) {
                        RUNNING_SERIAL_MAP.remove(list.get(0).getListener());
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
                synchronized (pauseLock) {
                    if (!FileDownloadList.getImpl().contains(stackTopTask)) {
                        // pause?
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(SerialHandlerCallback.class, "direct go next by not contains %s %d", stackTopTask, msg.arg1);
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

    static void freezeSerialHandler(Handler handler) {
        handler.sendEmptyMessage(WHAT_FREEZE);
    }

    static void unFreezeSerialHandler(Handler handler) {
        handler.sendEmptyMessage(WHAT_UNFREEZE);
    }
}
