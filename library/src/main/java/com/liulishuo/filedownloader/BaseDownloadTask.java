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

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;

import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.message.MessageSnapshotTaker;
import com.liulishuo.filedownloader.message.MessageSnapshotThreadPool;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jacksgong on 9/23/15.
 * <p/>
 * A atom download task.
 *
 * @see FileDownloader
 * @see FileDownloadTask
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public abstract class BaseDownloadTask {

    private int downloadId;

    private final String url;
    private String path;

    private FileDownloadHeader header;

    private FileDownloadListener listener;

    private SparseArray<Object> keyedTags;
    private Object tag;
    private Throwable ex;

    private long soFarBytes;
    private long totalBytes;
    private byte status = FileDownloadStatus.INVALID_STATUS;
    private int autoRetryTimes = 0;
    // Number of times to try again
    private int retryingTimes = 0;
    // The min interval millisecond for updating the download speed.
    private int minIntervalUpdateSpeed = 5;

    private boolean resuming;
    private String etag;

    private boolean isLargeFile;

    /**
     * 如果是true 会直接在下载线程回调，而不会调用{@link android.os.Handler#post(Runnable)} 抛到UI线程。
     * <p/>
     * if true will callback directly on the download thread(do not on post the message to the ui thread
     * by {@link android.os.Handler#post(Runnable)}
     */
    private boolean syncCallback = false;

    public final static int DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS = 10;
    private int callbackProgressTimes = FileDownloadModel.DEFAULT_CALLBACK_PROGRESS_TIMES;
    private int callbackProgressMinIntervalMillis = DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS;

    private boolean isForceReDownload = false;

    private boolean isReusedOldFile = false;

    volatile boolean using = false;

    private final IFileDownloadMessenger messenger;

    private long lastCalcSpeedSofarTime;
    private long lastCalcSpeedSofar;

    private long startDownloadSofar;
    private long startDownloadTime;

    // KB/s
    private int speed;

    BaseDownloadTask(final String url) {
        this.url = url;
        messenger = new FileDownloadMessenger(this);
    }

    // --------------------------------------- FOLLOWING FUNCTION FOR OUTSIDE ----------------------------------------------

    /**
     * @param minIntervalUpdateSpeedMs The min interval millisecond for updating the download speed
     *                                 in downloading process(Status equal to progress).
     *                                 Default 5 ms. If less than or equal to 0, will not calculate
     *                                 the download speed in process.
     * @see #calcSpeed(long)
     */
    public BaseDownloadTask setMinIntervalUpdateSpeed(int minIntervalUpdateSpeedMs) {
        this.minIntervalUpdateSpeed = minIntervalUpdateSpeedMs;
        return this;
    }

    /**
     * @param path Absolute path for save the download file
     */
    public BaseDownloadTask setPath(final String path) {
        this.path = path;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setPath %s", path);
        }
        return this;
    }

    /**
     * @param listener For callback download status(pending,connected,progress,
     *                 blockComplete,retry,error,paused,completed,warn)
     */
    public BaseDownloadTask setListener(final FileDownloadListener listener) {
        this.listener = listener;

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setListener %s", listener);
        }
        return this;
    }

    /**
     * Set the maximal callback count of
     * {@link FileDownloadListener#progress(BaseDownloadTask, int, int)} during the entire process
     * of downloading.
     * <p>
     * Note: this function will not work if the URL is refer to 'chucked' resource.
     *
     * @param callbackProgressCount The maximal callback count of
     *                              {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     *                              during the entire process of downloading.
     *                              Default value is 100, If the value less than or equal to 0, you
     *                              will not receive any callback of
     *                              {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     *                              .
     * @see #setCallbackProgressMinInterval(int)
     */
    public BaseDownloadTask setCallbackProgressTimes(int callbackProgressCount) {
        this.callbackProgressTimes = callbackProgressCount;
        return this;
    }

    /**
     * Set the minimum time interval between each callback of
     * {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}.
     *
     * @param minIntervalMillis The minimum time bytes interval between each callback of
     *                          {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     *                          Unit: millisecond.
     *                          Default value is {@link #DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS}.
     *                          Scope: [5, {@link Integer#MAX_VALUE}
     * @see #setCallbackProgressTimes(int)
     */
    public BaseDownloadTask setCallbackProgressMinInterval(int minIntervalMillis) {
        this.callbackProgressMinIntervalMillis = minIntervalMillis;
        return this;
    }

    /**
     * Ignore all callbacks of {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     * during the entire process of downloading.
     */
    public BaseDownloadTask setCallbackProgressIgnored() {
        return setCallbackProgressTimes(-1);
    }

    /**
     * Sets the tag associated with this task, not be used by internal.
     */
    public BaseDownloadTask setTag(final Object tag) {
        this.tag = tag;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setTag %s", tag);
        }
        return this;
    }

    /**
     * Set a tag associated with this task, not be used by internal.
     *
     * @param key The key of identifying the tag.
     *            If the key already exists, the old data will be replaced.
     * @param tag An Object to tag the task with
     */
    public BaseDownloadTask setTag(final int key, final Object tag) {
        if (keyedTags == null) {
            keyedTags = new SparseArray<>(2);
        }
        keyedTags.put(key, tag);
        return this;
    }


    /**
     * Force re download whether already downloaded completed
     *
     * @param isForceReDownload If set to true, will not check whether the file is downloaded
     *                          by past, default false
     */
    public BaseDownloadTask setForceReDownload(final boolean isForceReDownload) {
        this.isForceReDownload = isForceReDownload;
        return this;
    }

    /**
     * @deprecated Replace with {@link #addFinishListener(FinishListener)}
     */
    public BaseDownloadTask setFinishListener(final FinishListener finishListener) {
        addFinishListener(finishListener);
        return this;
    }

    private ArrayList<FinishListener> finishListenerList;

    /**
     * This listener's method {@link FinishListener#over()} will be invoked in Internal-Flow-Thread
     * directly, which is controlled by {@link MessageSnapshotThreadPool}.
     *
     * @param finishListener Just consider whether the task is over.
     * @see FileDownloadStatus#isOver(int)
     */
    public BaseDownloadTask addFinishListener(final FinishListener finishListener) {
        if (finishListenerList == null) {
            finishListenerList = new ArrayList<>();
        }

        if (!finishListenerList.contains(finishListener)) {
            finishListenerList.add(finishListener);
        }
        return this;
    }

    public boolean removeFinishListener(final FinishListener finishListener) {
        return finishListenerList != null && finishListenerList.remove(finishListener);
    }

    /**
     * Set the number of times to automatically retry when encounter any error
     *
     * @param autoRetryTimes default 0
     */
    public BaseDownloadTask setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        return this;
    }

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see okhttp3.Headers.Builder#add(String, String)
     */
    public BaseDownloadTask addHeader(final String name, final String value) {
        checkAndCreateHeader();
        header.add(name, value);
        return this;
    }

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see okhttp3.Headers.Builder#add(String, String)
     */
    public BaseDownloadTask addHeader(final String line) {
        checkAndCreateHeader();
        header.add(line);
        return this;
    }

    /**
     * @see okhttp3.Headers.Builder#removeAll(String)
     */
    public BaseDownloadTask removeAllHeaders(final String name) {
        if (header == null) {
            synchronized (headerCreateLock) {
                // maybe invoking checkAndCreateHear and will to be available.
                if (header == null) {
                    return this;
                }
            }
        }


        header.removeAll(name);
        return this;
    }

    /**
     * @param syncCallback if true will invoke callbacks of {@link FileDownloadListener} directly
     *                     on the download thread(do not post the message to the ui thread
     *                     by {@link android.os.Handler#post(Runnable)}
     */
    public BaseDownloadTask setSyncCallback(final boolean syncCallback) {
        this.syncCallback = syncCallback;
        return this;
    }

    // -------- Following function for ending ------

    /**
     * Ready task( For queue task )
     * <p/>
     * 用于将几个task绑定为一个队列启动的结束符
     *
     * @return downloadId
     * @see FileDownloader#start(FileDownloadListener, boolean)
     */
    public int ready() {

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "ready 2 download %s", toString());
        }

        FileDownloadList.getImpl().ready(this);

        return getId();
    }

    /**
     * Reuse this task withhold request params: path、url、header、isForceReDownloader、etc.
     *
     * @return Successful reuse or not.
     */
    public boolean reuse() {
        if (isRunning()) {
            FileDownloadLog.w(this, "This task is running %d, if you want start the same task," +
                    " please create a new one by FileDownloader.create", getId());
            return false;
        }

        this.using = false;
        this.etag = null;
        this.resuming = false;
        this.retryingTimes = 0;
        this.isReusedOldFile = false;
        this.ex = null;
        resetSpeed();
        clearMarkAdded2List();


        setStatus(FileDownloadStatus.INVALID_STATUS);
        this.soFarBytes = 0;
        this.totalBytes = 0;
        messenger.reAppointment(this);

        return true;
    }

    /**
     * @return Whether this task object has already started and used in FileDownload Engine. If true,
     * it isn't allow to {@link #start()} again for this task object.
     * @see #isRunning()
     * @see #start()
     * @see #reuse()
     */
    public boolean isUsing() {
        return this.using;
    }

    /**
     * @return Whether this task object is running in FileDownload Engine. If true, it isn't allow
     * to {@link #start()} again for this task object, and even not allow to {@link #reuse()}.
     * @see #isUsing()
     * @see #start()
     */
    public boolean isRunning() {
        //noinspection SimplifiableIfStatement
        if (!isUsing()) {
            return false;
        }

        return FileDownloadStatus.isIng(getStatus()) || FileDownloadList.getImpl().contains(this);
    }

    private int startUnchecked() {
        if (FileDownloadMonitor.isValid()) {
            FileDownloadMonitor.getMonitor().onRequestStart(this);
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.v(this, "call start " +
                            "url[%s], setPath[%s] listener[%s], tag[%s]",
                    url, path, listener, tag);
        }

        boolean ready = true;

        try {
            _adjust();
            _checkFile(path);
        } catch (Throwable e) {
            ready = false;

            FileDownloadList.getImpl().add(this);
            FileDownloadList.getImpl().remove(this, catchException(e));
        }

        if (ready) {
            FileDownloadTaskLauncher.getImpl().launch(this);
        }


        return getId();
    }

    /**
     * start the task.
     *
     * @return Download id
     */
    public int start() {

        if (isUsing()) {
            if (isRunning()) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("This task is running %d, if you" +
                                " want to start the same task, please create a new one by" +
                                " FileDownloader.create", getId()));
            } else {
                throw new IllegalStateException("This task is dirty to restart, If you want to " +
                        "reuse this task, please invoke #reuse method manually and retry to " +
                        "restart again.");
            }
        }

        this.using = true;

        return startUnchecked();
    }

    // -------------- Another Operations ---------------------

    /**
     * Why pause? not stop? because invoke this method(pause) will clear all data about this task
     * in memory, and stop the total processing about this task. but when you start the paused task,
     * it would be continue downloading from the breakpoint as default.
     *
     * @return If true, successful pause this task by status of pause, otherwise this task has
     * already in over status before invoke this method(Maybe occur high concurrent situation).
     * @see FileDownloader#pause(int)
     * @see FileDownloader#pause(FileDownloadListener)
     * @see FileDownloader#pauseAll()
     */
    public boolean pause() {
        if (FileDownloadStatus.isOver(getStatus())) {
            if (FileDownloadLog.NEED_LOG) {
                /**
                 * The over-status call-backed and set the over-status to this task between here
                 * area and remove from the {@link FileDownloadList}.
                 *
                 * High concurrent cause.
                 */
                FileDownloadLog.d(this, "High concurrent cause, Already is over, can't pause " +
                        "again, %d %d", getStatus(), getId());
            }
            return false;
        }
        setStatus(FileDownloadStatus.paused);

        _pauseExecute();

        calcAverageSpeed(this.soFarBytes);
        // For make sure already added event listener for receive paused event
        FileDownloadList.getImpl().add(this);
        FileDownloadList.getImpl().remove(this, MessageSnapshotTaker.catchPause(this));

        return true;
    }

    // ------------------- get -----------------------

    /**
     * The identify download id is generated by Url & Path
     * {@link FileDownloadUtils#generateId(String, String)}.
     *
     * @return The identify id for this task.
     * @see FileDownloader#pause(int)
     * @see FileDownloader#getStatus(String, String)
     * @see FileDownloader#getTotal(int)
     * @see FileDownloader#getSoFar(int)
     */
    public int getId() {
        if (downloadId != 0) {
            return downloadId;
        }

        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(url)) {
            return downloadId = FileDownloadUtils.generateId(url, path);
        }

        return 0;
    }


    /**
     * @return The identify id for this task.
     * @deprecated Used {@link #getId()} instead.
     */
    public int getDownloadId() {
        return getId();
    }

    /**
     * Get download url
     *
     * @return download url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return The maximal callback count of
     * {@link FileDownloadListener#progress(BaseDownloadTask, int, int)} during the entire process
     * of downloading.
     */
    public int getCallbackProgressTimes() {
        return callbackProgressTimes;
    }

    /**
     * @return The minimum time interval between each callback of
     * {@link FileDownloadListener#progress(BaseDownloadTask, int, int)} .
     */
    public int getCallbackProgressMinInterval() {
        return callbackProgressMinIntervalMillis;
    }

    /**
     * @return absolute path for save the download file
     */
    public String getPath() {
        return path;
    }

    /**
     * @return Current FileDownloadListener
     */
    public FileDownloadListener getListener() {
        return listener;
    }

    /**
     * @return Number of bytes download so far
     * @deprecated replace with {@link #getSmallFileSoFarBytes()}}}}
     */
    public int getSoFarBytes() {
        return getSmallFileSoFarBytes();
    }

    /**
     * @return The downloaded so far bytes which size is less than or equal to 1.99G
     */
    public int getSmallFileSoFarBytes() {
        if (soFarBytes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) soFarBytes;
    }

    public long getLargeFileSoFarBytes() {
        return soFarBytes;
    }

    /**
     * @return Total bytes, available
     * after {@link FileDownloadListener#connected(BaseDownloadTask, String, boolean, int, int)}/ already have in db
     * @deprecated replace with {@link #getSmallFileTotalBytes()}}
     */
    public int getTotalBytes() {
        return getSmallFileTotalBytes();
    }

    /**
     * @return The total bytes which size is less than or equal to 1.99G
     */
    public int getSmallFileTotalBytes() {
        if (totalBytes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) totalBytes;
    }

    public long getLargeFileTotalBytes() {
        return totalBytes;
    }

    /**
     * If in downloading process(status equal {@link FileDownloadStatus#progress}) : Calculating
     * when the interval from the last calculation more than {@link #minIntervalUpdateSpeed} before
     * each {@link FileDownloadListener#progress(BaseDownloadTask, int, int)} call-back method.
     * <p/>
     * If finished({@link FileDownloadStatus#isOver(int)}): Would be average speed. The scope is
     * (connected, over).
     *
     * @return KB/s
     * @see #setMinIntervalUpdateSpeed(int)
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     * @return Current status
     * @see FileDownloadStatus
     */
    public byte getStatus() {
        return status;
    }

    /**
     * @return Force re-download,do not care about whether already downloaded or not
     */
    public boolean isForceReDownload() {
        return this.isForceReDownload;
    }

    /**
     * @return Throwable
     */
    public Throwable getEx() {
        return ex;
    }


    /**
     * @return Whether reused the downloaded file by past.
     * @see #isReusedOldFile
     */
    public boolean isReusedOldFile() {
        return isReusedOldFile;
    }

    /**
     * @return The task's tag
     */
    public Object getTag() {
        return this.tag;
    }

    /**
     * Returns the tag associated with this task and the specified key.
     *
     * @param key The key identifying the tag
     * @return the object stored in this take as a tag, or {@code null} if not
     * set
     * @see #setTag(int, Object)
     * @see #getTag()
     */
    public Object getTag(int key) {
        return keyedTags == null ? null : keyedTags.get(key);
    }


    /**
     * @deprecated Use {@link #isResuming()} instead.
     */
    public boolean isContinue() {
        return this.resuming;
    }

    /**
     * @return Is resume by breakpoint, available
     * after {@link FileDownloadListener#connected(BaseDownloadTask, String, boolean, int, int)}
     */
    public boolean isResuming() {
        return this.resuming;
    }

    /**
     * @return ETag, available
     * after {@link FileDownloadListener#connected(BaseDownloadTask, String, boolean, int, int)}
     */
    public String getEtag() {
        return this.etag;
    }

    /**
     * @return The number of times to automatically retry
     */
    public int getAutoRetryTimes() {
        return this.autoRetryTimes;
    }

    /**
     * @return The current number of trey. available
     * after {@link FileDownloadListener#retry(BaseDownloadTask, Throwable, int, int)}
     */
    public int getRetryingTimes() {
        return this.retryingTimes;
    }

    /**
     * @return whether sync callback directly on the download thread, do not post to the ui thread.
     */
    public boolean isSyncCallback() {
        return syncCallback;
    }

    /**
     * @return Whether the length of downloading file is more than or equal to 2G.
     * @see #getLargeFileSoFarBytes()
     * @see #getLargeFileTotalBytes()
     */
    public boolean isLargeFile() {
        return isLargeFile;
    }

    // --------------------------------------- ABOVE FUNCTIONS FOR OUTSIDE ----------------------------------------------

    // --------------------------------------- FOLLOWING FUNCTIONS FOR INTERNAL --------------------------------------------------

    private void _checkFile(final String path) {
        File file = new File(path);
        if (file.exists()) {
            return;
        }

        if (!file.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }
    }

    protected boolean _checkCanStart() {
        return true;
    }

    protected boolean _checkCanReuse() {
        return false;
    }

    // Assign default value if need
    private void _adjust() {
        if (path == null) {
            path = FileDownloadUtils.getDefaultSaveFilePath(url);
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "save path is null to %s", path);
            }
        }
    }

    void _start() {

        try {
            // Whether service was already started.
            if (!_checkCanStart()) {
                this.using = false;
                // Not ready
                return;
            }

            FileDownloadList.getImpl().add(this);
            if (_checkCanReuse()) {
                // Will be removed when the complete message is received in #update
                return;
            }

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "start downloaded by ui process %s", getUrl());
            }

            _startExecute();

        } catch (Throwable e) {
            e.printStackTrace();

            FileDownloadList.getImpl().remove(this, catchException(e));
        }

    }

    /**
     * Execute start
     */
    protected abstract void _startExecute();

    // Execute pause
    protected abstract boolean _pauseExecute();

    protected abstract int _getStatusFromServer(final int downloadId);

    private Runnable cacheRunnable;

    private Runnable _getOverCallback() {
        if (cacheRunnable != null) {
            return cacheRunnable;
        }

        return cacheRunnable = new Runnable() {
            @Override
            public void run() {
                clear();
            }
        };
    }

    private void _setRetryingTimes(final int times) {
        this.retryingTimes = times;
    }


    private final Object headerCreateLock = new Object();

    private void checkAndCreateHeader() {
        if (header == null) {
            synchronized (headerCreateLock) {
                if (header == null) {
                    header = new FileDownloadHeader();
                }
            }
        }
    }

    // Status, will changed before enqueue/dequeue/notify
    private void setStatus(byte status) {
        if (status > FileDownloadStatus.MAX_INT ||
                status < FileDownloadStatus.MIN_INT) {
            throw new RuntimeException(
                    FileDownloadUtils.formatString("status undefined, %d", status));
        }
        this.status = status;
    }


    private void resetSpeed() {
        this.speed = 0;
        this.lastCalcSpeedSofarTime = 0;
    }

    private void markStartDownload() {
        this.startDownloadTime = SystemClock.uptimeMillis();
        this.startDownloadSofar = this.soFarBytes;
    }

    private void calcAverageSpeed(final long overSofar) {
        if (startDownloadTime <= 0 || startDownloadSofar <= 0) {
            return;
        }

        long downloadSize = overSofar - startDownloadSofar;
        this.lastCalcSpeedSofarTime = 0;
        long interval = SystemClock.uptimeMillis() - startDownloadTime;
        if (interval < 0) {
            speed = (int) downloadSize;
        } else {
            speed = (int) (downloadSize / interval);
        }
    }

    private void calcSpeed(long sofar) {
        if (minIntervalUpdateSpeed <= 0) {
            return;
        }

        boolean isUpdateData = false;
        do {
            if (lastCalcSpeedSofarTime == 0) {
                isUpdateData = true;
                break;
            }

            long interval = SystemClock.uptimeMillis() - lastCalcSpeedSofarTime;
            if (interval >= minIntervalUpdateSpeed || (speed == 0 && interval > 0)) {
                speed = (int) ((sofar - lastCalcSpeedSofar) / interval);
                speed = Math.max(0, speed);
                isUpdateData = true;
                break;
            }
        } while (false);

        if (isUpdateData) {
            lastCalcSpeedSofar = sofar;
            lastCalcSpeedSofarTime = SystemClock.uptimeMillis();
        }
    }

    // --------------------------------------- ABOVE FUNCTIONS FOR INTERNAL --------------------------------------------------

    // --------------------------------------- FOLLOWING FUNCTIONS FOR INTERNAL COOPERATION --------------------------------------------------

    // Clear References
    void clear() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "clear %s", this);
        }
    }

    /**
     * @return Make sure one event to one task
     */
    String generateEventId() {
        return toString();
    }

    FileDownloadHeader getHeader() {
        return this.header;
    }

    MessageSnapshot catchException(Throwable ex) {
        setStatus(FileDownloadStatus.error);
        this.ex = ex;
        return MessageSnapshotTaker.catchException(this);
    }

    // Messenger
    IFileDownloadMessenger getMessenger() {
        return this.messenger;
    }

    // ------------------
    // Begin task execute
    void begin() {
        if (FileDownloadMonitor.isValid()) {
            FileDownloadMonitor.getMonitor().onTaskBegin(this);
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.v(this, "filedownloader:lifecycle:start %s by %d ", toString(), getStatus());
        }
    }

    // Being processed
    void ing() {
        if (FileDownloadMonitor.isValid() && getStatus() == FileDownloadStatus.started) {
            FileDownloadMonitor.getMonitor().onTaskStarted(this);
        }
    }

    // End task
    void over() {
        if (FileDownloadMonitor.isValid()) {
            FileDownloadMonitor.getMonitor().onTaskOver(this);
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.v(this, "filedownloader:lifecycle:over %s by %d ", toString(), getStatus());
        }

        if (finishListenerList != null) {
            @SuppressWarnings("unchecked") final ArrayList<FinishListener> listenersCopy =
                    (ArrayList<FinishListener>) finishListenerList.clone();
            final int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).over(this);
            }
        }
    }

    boolean updateKeepFlow(final MessageSnapshot snapshot) {
        final int currentStatus = getStatus();
        final int nextStatus = snapshot.getStatus();

        if (FileDownloadStatus.paused == currentStatus && FileDownloadStatus.isIng(nextStatus)) {
            if (FileDownloadLog.NEED_LOG) {
                /**
                 * Occur such situation, must be the running-status waiting for turning up in flow
                 * thread pool(or binder thread) when there is someone invoked the {@link #pause()} .
                 *
                 * High concurrent cause.
                 */
                FileDownloadLog.d(this, "High concurrent cause, callback pending, but has already" +
                        " be paused %d", getId());
            }
            return true;
        }

        if (!FileDownloadStatus.isKeepFlow(currentStatus, nextStatus)) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "can't update status change by keep flow, %d, but the" +
                        " current status is %d, %d", status, getStatus(), getId());
            }

            return false;
        }

        update(snapshot);
        return true;
    }

    boolean updateKeepAhead(final MessageSnapshot snapshot) {
        if (!FileDownloadStatus.isKeepAhead(getStatus(), snapshot.getStatus())) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "can't update status change by keep ahead, %d, but the" +
                        " current status is %d, %d", status, getStatus(), getId());
            }
            return false;
        }

        update(snapshot);
        return true;
    }

    private void update(final MessageSnapshot snapshot) {
        setStatus(snapshot.getStatus());
        this.isLargeFile = snapshot.isLargeFile();

        switch (snapshot.getStatus()) {
            case FileDownloadStatus.pending:
                this.soFarBytes = snapshot.getLargeSofarBytes();
                this.totalBytes = snapshot.getLargeTotalBytes();

                // notify
                getMessenger().notifyPending(snapshot);
                break;
            case FileDownloadStatus.started:
                // notify
                getMessenger().notifyStarted(snapshot);
                break;
            case FileDownloadStatus.connected:
                this.totalBytes = snapshot.getLargeTotalBytes();
                this.resuming = snapshot.isResuming();
                this.etag = snapshot.getEtag();

                markStartDownload();

                // notify
                getMessenger().notifyConnected(snapshot);
                break;
            case FileDownloadStatus.progress:
                this.soFarBytes = snapshot.getLargeSofarBytes();
                calcSpeed(snapshot.getLargeSofarBytes());

                // notify
                getMessenger().notifyProgress(snapshot);
                break;
//            case FileDownloadStatus.blockComplete:
            /**
             * Handled by {@link FileDownloadList#removeByCompleted(BaseDownloadTask)}
             */
//                break;
            case FileDownloadStatus.retry:
                this.soFarBytes = snapshot.getLargeSofarBytes();
                this.ex = snapshot.getThrowable();
                _setRetryingTimes(snapshot.getRetryingTimes());

                resetSpeed();
                // notify
                getMessenger().notifyRetry(snapshot);
                break;
            case FileDownloadStatus.error:
                this.ex = snapshot.getThrowable();
                this.soFarBytes = snapshot.getLargeSofarBytes();

                calcAverageSpeed(this.soFarBytes);
                // to FileDownloadList
                FileDownloadList.getImpl().remove(this, snapshot);

                break;
            case FileDownloadStatus.paused:
                /**
                 * Handled by {@link #pause()}
                 */
                break;
            case FileDownloadStatus.completed:
                this.isReusedOldFile = snapshot.isReusedDownloadedFile();
                // only carry total data back
                this.soFarBytes = snapshot.getLargeTotalBytes();
                this.totalBytes = snapshot.getLargeTotalBytes();

                calcAverageSpeed(this.soFarBytes);
                // to FileDownloadList
                FileDownloadList.getImpl().remove(this, snapshot);

                break;
            case FileDownloadStatus.warn:
                resetSpeed();
                final int count = FileDownloadList.getImpl().count(getId());
                if (count <= 1) {
                    // 1. this progress kill by sys and relive,
                    // for add at least one listener
                    // or 2. pre downloading task has already completed/error/paused
                    // request status
                    final int currentStatus = _getStatusFromServer(downloadId);
                    FileDownloadLog.w(this, "warn, but no listener to receive progress, " +
                            "switch to pending %d %d", getId(), currentStatus);

                    //noinspection StatementWithEmptyBody
                    if (FileDownloadStatus.isIng(currentStatus)) {
                        // ing, has callbacks
                        // keep and wait callback

                        setStatus(FileDownloadStatus.pending);
                        this.totalBytes = snapshot.getLargeTotalBytes();
                        this.soFarBytes = snapshot.getLargeSofarBytes();

                        markStartDownload();

                        ((MessageSnapshot.IWarnMessageSnapshot) snapshot).turnToPending();
                        getMessenger().notifyPending(snapshot);
                        break;
                    } else {
                        // already over and no callback
                    }

                }

                // to FileDownloadList
                FileDownloadList.getImpl().remove(this, snapshot);
                break;
        }
    }

    // why this? thread not safe: update,ready, _start, pause, start which influence of this
    // in the queue.
    // whether it has been added, whether or not it is removed.
    private volatile boolean isMarkedAdded2List = false;

    void markAdded2List() {
        isMarkedAdded2List = true;
    }

    void clearMarkAdded2List() {
        isMarkedAdded2List = false;
    }

    boolean isMarkedAdded2List() {
        return this.isMarkedAdded2List;
    }

    // --------------------------------------- ABOVE FUNCTIONS FOR INTERNAL COOPERATION --------------------------------------------------

    // -------------------------------------------------

    /**
     * @return for OkHttpTag/ queue tag
     * <p/>
     * As in same queue has same chainKey
     */
    protected int getChainKey() {
        // TODO 极低概率不唯一
        return getListener().hashCode();
    }


    // ---------------------------------------------
    @SuppressWarnings("UnusedParameters")
    public interface FinishListener {
        /**
         * Will be invoked when the {@code task} is over({@link FileDownloadStatus#isOver(int)}).
         * This method will be invoked in Non-UI-Thread and this thread is controlled by
         * {@link MessageSnapshotThreadPool}.
         *
         * @param task is over, the status would be one of below:
         *             {@link FileDownloadStatus#completed}、{@link FileDownloadStatus#warn}、
         *             {@link FileDownloadStatus#error}、{@link FileDownloadStatus#paused}.
         * @see FileDownloadStatus#isOver(int)
         */
        void over(final BaseDownloadTask task);
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("%d@%s", getId(), super.toString());
    }

}
