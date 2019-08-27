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
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.liulishuo.filedownloader.download.CustomComponentHolder;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.List;

/**
 * The basic entrance for FileDownloader.
 *
 * @see com.liulishuo.filedownloader.services.FileDownloadService The service for FileDownloader.
 * @see FileDownloadProperties
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloader {

    /**
     * You can invoke this method anytime before you using the FileDownloader.
     * <p>
     * If you want to register your own customize components please using
     * {@link #setupOnApplicationOnCreate(Application)} on the {@link Application#onCreate()}
     * instead.
     *
     * @param context the context of Application or Activity etc..
     */
    public static void setup(Context context) {
        FileDownloadHelper.holdContext(context.getApplicationContext());
    }

    /**
     * Using this method to setup the FileDownloader only you want to register your own customize
     * components for Filedownloader, otherwise just using {@link #setup(Context)} instead.
     * <p/>
     * Please invoke this method on the {@link Application#onCreate()} because of the customize
     * components must be assigned before FileDownloader is running.
     * <p/>
     * Such as:
     * <p/>
     * class MyApplication extends Application {
     *     ...
     *     public void onCreate() {
     *          ...
     *          FileDownloader.setupOnApplicationOnCreate(this)
     *              .idGenerator(new MyIdGenerator())
     *              .database(new MyDatabase())
     *              ...
     *              .commit();
     *          ...
     *     }
     *     ...
     * }
     * @param application the application.
     * @return the customize components maker.
     */
    public static DownloadMgrInitialParams.InitCustomMaker setupOnApplicationOnCreate(
            Application application) {
        final Context context = application.getApplicationContext();
        FileDownloadHelper.holdContext(context);

        DownloadMgrInitialParams.InitCustomMaker customMaker =
                new DownloadMgrInitialParams.InitCustomMaker();
        CustomComponentHolder.getImpl().setInitCustomMaker(customMaker);

        return customMaker;
    }

    /**
     * @deprecated please use {@link #setup(Context)} instead.
     */
    public static void init(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("the provided context must not be null!");
        }

        setup(context);
    }


    /**
     * @deprecated please using {@link #setupOnApplicationOnCreate(Application)} instead.
     */
    public static void init(final Context context,
                            final DownloadMgrInitialParams.InitCustomMaker maker) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(FileDownloader.class, "init Downloader with params: %s %s",
                    context, maker);
        }

        if (context == null) {
            throw new IllegalArgumentException("the provided context must not be null!");
        }

        FileDownloadHelper.holdContext(context.getApplicationContext());

        CustomComponentHolder.getImpl().setInitCustomMaker(maker);
    }

    private static final class HolderClass {
        private static final FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    /**
     * For avoiding missing screen frames.
     * <p/>
     * This mechanism is used for avoid methods in {@link FileDownloadListener} is invoked too
     * frequent in result the system missing screen frames in the main thread.
     * <p>
     * We wrap the message package which size is {@link FileDownloadMessageStation#SUB_PACKAGE_SIZE}
     * and post the package to the main thread with the interval:
     * {@link FileDownloadMessageStation#INTERVAL} milliseconds.
     * <p/>
     * The default interval is 10ms, if {@code intervalMillisecond} equal to or less than 0, each
     * callback in {@link FileDownloadListener} will be posted to the main thread immediately.
     *
     * @param intervalMillisecond The time interval between posting two message packages.
     * @see #enableAvoidDropFrame()
     * @see #disableAvoidDropFrame()
     * @see #setGlobalHandleSubPackageSize(int)
     */
    public static void setGlobalPost2UIInterval(final int intervalMillisecond) {
        FileDownloadMessageStation.INTERVAL = intervalMillisecond;
    }

    /**
     * For avoiding missing screen frames.
     * <p/>
     * This mechanism is used for avoid methods in {@link FileDownloadListener} is invoked too
     * frequent in result the system missing screen frames in the main thread.
     * <p>
     * We wrap the message package which size is {@link FileDownloadMessageStation#SUB_PACKAGE_SIZE}
     * and post the package to the main thread with the interval:
     * {@link FileDownloadMessageStation#INTERVAL} milliseconds.
     * <p>
     * The default count of message for a message package is 5.
     *
     * @param packageSize The count of message for a message package.
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void setGlobalHandleSubPackageSize(final int packageSize) {
        if (packageSize <= 0) {
            throw new IllegalArgumentException("sub package size must more than 0");
        }
        FileDownloadMessageStation.SUB_PACKAGE_SIZE = packageSize;
    }

    /**
     * Avoid missing screen frames, this leads to all callbacks in {@link FileDownloadListener} do
     * not be invoked at once when it has already achieved to ensure callbacks don't be too frequent
     *
     * @see #isEnabledAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void enableAvoidDropFrame() {
        setGlobalPost2UIInterval(FileDownloadMessageStation.DEFAULT_INTERVAL);
    }

    /**
     * Disable avoiding missing screen frames, let all callbacks in {@link FileDownloadListener}
     * can be invoked at once when it achieve.
     *
     * @see #isEnabledAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static void disableAvoidDropFrame() {
        setGlobalPost2UIInterval(-1);
    }

    /**
     * @return {@code true} if enabled the function of avoiding missing screen frames.
     * @see #enableAvoidDropFrame()
     * @see #disableAvoidDropFrame()
     * @see #setGlobalPost2UIInterval(int)
     */
    public static boolean isEnabledAvoidDropFrame() {
        return FileDownloadMessageStation.isIntervalValid();
    }

    /**
     * Create a download task.
     */
    public BaseDownloadTask create(final String url) {
        return new DownloadTask(url);
    }

    /**
     * Start the download queue by the same listener.
     *
     * @param listener Used to assemble tasks which is bound by the same {@code listener}
     * @param isSerial Whether start tasks one by one rather than parallel.
     * @return {@code true} if start tasks successfully.
     */
    public boolean start(final FileDownloadListener listener, final boolean isSerial) {

        if (listener == null) {
            FileDownloadLog.w(this, "Tasks with the listener can't start, because the listener "
                    + "provided is null: [null, %B]", isSerial);
            return false;
        }


        return isSerial
                ? getQueuesHandler().startQueueSerial(listener)
                : getQueuesHandler().startQueueParallel(listener);
    }


    /**
     * Pause the download queue by the same {@code listener}.
     *
     * @param listener the listener.
     * @see #pause(int)
     */
    public void pause(final FileDownloadListener listener) {
        FileDownloadTaskLauncher.getImpl().expire(listener);
        final List<BaseDownloadTask.IRunningTask> taskList =
                FileDownloadList.getImpl().copy(listener);
        for (BaseDownloadTask.IRunningTask task : taskList) {
            task.getOrigin().pause();
        }
    }

    /**
     * Pause all tasks running in FileDownloader.
     */
    public void pauseAll() {
        FileDownloadTaskLauncher.getImpl().expireAll();
        final BaseDownloadTask.IRunningTask[] downloadList = FileDownloadList.getImpl().copy();
        for (BaseDownloadTask.IRunningTask task : downloadList) {
            task.getOrigin().pause();
        }
        // double check, for case: File Download progress alive but ui progress has died and relived
        // so FileDownloadList not always contain all running task exactly.
        if (FileDownloadServiceProxy.getImpl().isConnected()) {
            FileDownloadServiceProxy.getImpl().pauseAllTasks();
        } else {
            PauseAllMarker.createMarker();
        }
    }

    /**
     * Pause downloading tasks with the {@code id}.
     *
     * @param id the {@code id} .
     * @return The size of tasks has been paused.
     * @see #pause(FileDownloadListener)
     */
    public int pause(final int id) {
        List<BaseDownloadTask.IRunningTask> taskList = FileDownloadList.getImpl()
                .getDownloadingList(id);
        if (null == taskList || taskList.isEmpty()) {
            FileDownloadLog.w(this, "request pause but not exist %d", id);
            return 0;
        }

        for (BaseDownloadTask.IRunningTask task : taskList) {
            task.getOrigin().pause();
        }

        return taskList.size();
    }

    /**
     * Clear the data with the provided {@code id}.
     * Normally used to deleting the data in filedownloader database, when it is paused or in
     * downloading status. If you want to re-download it clearly.
     * <p/>
     * <strong>Note:</strong> YOU NO NEED to clear the data when it is already completed downloading
     * because the data would be deleted when it completed downloading automatically by
     * FileDownloader.
     * <p>
     * If there are tasks with the {@code id} in downloading, will be paused first;
     * If delete the data with the {@code id} in the filedownloader database successfully, will try
     * to delete its intermediate downloading file and downloaded file.
     *
     * @param id             the download {@code id}.
     * @param targetFilePath the target path.
     * @return {@code true} if the data with the {@code id} in filedownloader database was deleted,
     * and tasks with the {@code id} was paused; {@code false} otherwise.
     */
    public boolean clear(final int id, final String targetFilePath) {
        pause(id);

        if (FileDownloadServiceProxy.getImpl().clearTaskData(id)) {
            // delete the task data in the filedownloader database successfully or no data with the
            // id in filedownloader database.
            final File intermediateFile = new File(FileDownloadUtils.getTempPath(targetFilePath));
            if (intermediateFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                intermediateFile.delete();
            }

            final File targetFile = new File(targetFilePath);
            if (targetFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
            }

            return true;
        }

        return false;
    }

    /**
     * Clear all data in the filedownloader database.
     * <p>
     * <strong>Note:</strong> Normally, YOU NO NEED to clearAllTaskData manually, because the
     * FileDownloader will maintain those data to ensure only if the data available for resuming
     * can be kept automatically.
     *
     * @see #clear(int, String)
     */
    public void clearAllTaskData() {
        pauseAll();

        FileDownloadServiceProxy.getImpl().clearAllTaskData();
    }

    /**
     * Get downloaded bytes so far by the downloadId.
     */
    public long getSoFar(final int downloadId) {
        BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(downloadId);
        if (task == null) {
            return FileDownloadServiceProxy.getImpl().getSofar(downloadId);
        }

        return task.getOrigin().getLargeFileSoFarBytes();
    }

    /**
     * Get the total bytes of the target file for the task with the {code id}.
     */
    public long getTotal(final int id) {
        BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            return FileDownloadServiceProxy.getImpl().getTotal(id);
        }

        return task.getOrigin().getLargeFileTotalBytes();
    }

    /**
     * @param id The downloadId.
     * @return The downloading status without cover the completed status (if completed you will
     * receive
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
     * @param id   The downloadId.
     * @param path The target file path.
     * @return the downloading status.
     * @see FileDownloadStatus
     * @see #getStatus(String, String)
     * @see #getStatusIgnoreCompleted(int)
     */
    public byte getStatus(final int id, final String path) {
        byte status;
        BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            status = FileDownloadServiceProxy.getImpl().getStatus(id);
        } else {
            status = task.getOrigin().getStatus();
        }

        if (path != null && status == FileDownloadStatus.INVALID_STATUS) {
            if (FileDownloadUtils.isFilenameConverted(FileDownloadHelper.getAppContext())
                    && new File(path).exists()) {
                status = FileDownloadStatus.completed;
            }
        }

        return status;
    }

    /**
     * Find the running task by {@code url} and default path, and replace its listener with
     * the new one {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
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
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
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
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
     * @see #replaceListener(String, FileDownloadListener)
     * @see #replaceListener(String, String, FileDownloadListener)
     */
    public int replaceListener(int id, FileDownloadListener listener) {
        final BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            return 0;
        }

        task.getOrigin().setListener(listener);
        return task.getOrigin().getId();
    }

    /**
     * Start and bind the FileDownloader service.
     * <p>
     * <strong>Tips:</strong> The FileDownloader service will start and bind automatically when any
     * task is request to start.
     *
     * @see #bindService(Runnable)
     * @see #isServiceConnected()
     * @see #addServiceConnectListener(FileDownloadConnectListener)
     */
    public void bindService() {
        if (!isServiceConnected()) {
            FileDownloadServiceProxy.getImpl()
                    .bindStartByContext(FileDownloadHelper.getAppContext());
        }
    }

    /**
     * Start and bind the FileDownloader service and run {@code runnable} as soon as the binding is
     * successful.
     * <p>
     * <strong>Tips:</strong> The FileDownloader service will start and bind automatically when any
     * task is request to start.
     *
     * @param runnable the command will be executed as soon as the FileDownloader Service is
     *                 successfully bound.
     * @see #isServiceConnected()
     * @see #bindService()
     * @see #addServiceConnectListener(FileDownloadConnectListener)
     */
    public void bindService(final Runnable runnable) {
        if (isServiceConnected()) {
            runnable.run();
        } else {
            FileDownloadServiceProxy.getImpl().
                    bindStartByContext(FileDownloadHelper.getAppContext(), runnable);
        }
    }

    /**
     * Unbind and stop the downloader service.
     */
    public void unBindService() {
        if (isServiceConnected()) {
            FileDownloadServiceProxy.getImpl().unbindByContext(FileDownloadHelper.getAppContext());
        }
    }

    /**
     * Unbind and stop the downloader service when there is no task running in the FileDownloader.
     *
     * @return {@code true} if unbind and stop the downloader service successfully, {@code false}
     * there are some tasks running in the FileDownloader.
     */
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
     * @return {@code true} if the downloader service has been started and connected.
     */
    public boolean isServiceConnected() {
        return FileDownloadServiceProxy.getImpl().isConnected();
    }

    /**
     * Add the listener for listening when the status of connection with the downloader service is
     * changed.
     *
     * @param listener The downloader service connection listener.
     * @see #removeServiceConnectListener(FileDownloadConnectListener)
     */
    public void addServiceConnectListener(final FileDownloadConnectListener listener) {
        FileDownloadEventPool.getImpl().addListener(DownloadServiceConnectChangedEvent.ID,
                listener);
    }

    /**
     * Remove the listener for listening when the status of connection with the downloader service
     * is changed.
     *
     * @param listener The downloader service connection listener.
     * @see #addServiceConnectListener(FileDownloadConnectListener)
     */
    public void removeServiceConnectListener(final FileDownloadConnectListener listener) {
        FileDownloadEventPool.getImpl().removeListener(DownloadServiceConnectChangedEvent.ID,
                listener);
    }

    /**
     * Start the {@code notification} with the {@code id}. This will let the downloader service
     * change to a foreground service.
     * <p>
     * In foreground status, will save the FileDownloader alive, even user kill the application
     * from recent apps.
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
     *                     {@link android.app.NotificationManager#notify(int, Notification)
     *                     NotificationManager.notify(int, Notification)}; must not be 0.
     * @param notification The notification to be displayed.
     * @see #stopForeground(boolean)
     */
    public void startForeground(int id, Notification notification) {
        FileDownloadServiceProxy.getImpl().startForeground(id, notification);
    }

    /**
     * Remove the downloader service from the foreground state, allowing it to be killed if
     * more memory is needed.
     *
     * @param removeNotification {@code true} if the notification previously provided
     *                           to {@link #startForeground} will be removed. {@code false} it will
     *                           be remained until a later call removes it (or the service is
     *                           destroyed).
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
     * @return Whether is successful to set the task completed. If the {@code path} not exist will
     * be false; If the length of the file in {@code path} is not equal to {@code totalBytes} will
     * be false; If the task with {@code url} and {@code path} is downloading will be false.
     * Otherwise will be true.
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
     * @deprecated If you invoked this method, please remove the code directly feel free, it doesn't
     * need any longer. In new mechanism(filedownloader 0.3.3 or higher), FileDownloader doesn't
     * store completed tasks in Database anymore, because all downloading files have temp a file
     * name.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean setTaskCompleted(String url, String path, long totalBytes) {
        FileDownloadLog.w(this, "If you invoked this method, please remove it directly feel free, "
                + "it doesn't need any longer");
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
     * @deprecated If you invoked this method, please remove the code directly feel free, it doesn't
     * need any longer. In new mechanism(filedownloader 0.3.3 or higher), FileDownloader doesn't
     * store completed tasks in Database anymore, because all downloading files have temp a file
     * name.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean setTaskCompleted(
            @SuppressWarnings("deprecation") List<FileDownloadTaskAtom> taskAtomList) {
        FileDownloadLog.w(this, "If you invoked this method, please remove it directly feel free, "
                + "it doesn't need any longer");
        return true;
    }

    /**
     * Set the maximum count of the network thread, what is the number of simultaneous downloads in
     * FileDownloader.
     *
     * @param count the number of simultaneous downloads, scope: [1, 12].
     * @return whether is successful to set the max network thread count.
     * If there are any actively executing tasks in FileDownloader, you will receive a warn
     * priority log int the logcat and this operation would be failed.
     */
    public boolean setMaxNetworkThreadCount(final int count) {
        if (!FileDownloadList.getImpl().isEmpty()) {
            FileDownloadLog.w(this, "Can't change the max network thread count, because there "
                    + "are actively executing tasks in FileDownloader, please try again after all"
                    + " actively executing tasks are completed or invoking FileDownloader#pauseAll"
                    + " directly.");
            return false;
        }

        return FileDownloadServiceProxy.getImpl().setMaxNetworkThreadCount(count);
    }

    /**
     * If the FileDownloader service is not started and connected, FileDownloader will try to start
     * it and try to bind with it. The current thread will also be blocked until the FileDownloader
     * service is started and a connection is established, and then the request you
     * invoke in {@link FileDownloadLine} will be executed.
     * <p>
     * If the FileDownloader service has been started and connected, the request you invoke in
     * {@link FileDownloadLine} will be executed immediately.
     * <p>
     * <strong>Note:</strong> FileDownloader can not block the main thread, because the system is
     * also call-backs the {@link ServiceConnection#onServiceConnected(ComponentName, IBinder)}
     * method in the main thread.
     * <p>
     * <strong>Tips:</strong> The FileDownloader service will start and bind automatically when any
     * task is request to start.
     *
     * @see FileDownloadLine
     * @see #bindService(Runnable)
     */
    public FileDownloadLine insureServiceBind() {
        return new FileDownloadLine();
    }

    /**
     * If the FileDownloader service is not started and connected will return {@code false}
     * immediately, and meanwhile FileDownloader will try to start FileDownloader service and try to
     * bind with it, and after it is bound successfully the request you invoke in
     * {@link FileDownloadLineAsync} will be executed automatically.
     * <p>
     * If the FileDownloader service has been started and connected, the request you invoke in
     * {@link FileDownloadLineAsync} will be executed immediately.
     *
     * @see FileDownloadLineAsync
     * @see #bindService(Runnable)
     */
    public FileDownloadLineAsync insureServiceBindAsync() {
        return new FileDownloadLineAsync();
    }

    private static final Object INIT_QUEUES_HANDLER_LOCK = new Object();
    private IQueuesHandler mQueuesHandler;

    IQueuesHandler getQueuesHandler() {
        if (mQueuesHandler == null) {
            synchronized (INIT_QUEUES_HANDLER_LOCK) {
                if (mQueuesHandler == null) {
                    mQueuesHandler = new QueuesHandler();
                }
            }
        }
        return mQueuesHandler;
    }

    private static final Object INIT_LOST_CONNECTED_HANDLER_LOCK = new Object();
    private ILostServiceConnectedHandler mLostConnectedHandler;

    ILostServiceConnectedHandler getLostConnectedHandler() {
        if (mLostConnectedHandler == null) {
            synchronized (INIT_LOST_CONNECTED_HANDLER_LOCK) {
                if (mLostConnectedHandler == null) {
                    mLostConnectedHandler = new LostServiceConnectedHandler();
                    addServiceConnectListener((FileDownloadConnectListener) mLostConnectedHandler);
                }
            }
        }

        return mLostConnectedHandler;
    }


}
