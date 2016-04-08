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

import android.text.TextUtils;
import android.util.SparseArray;

import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.event.IDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
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


    private boolean resuming;
    private String etag;

    /**
     * 如果是true 会直接在下载线程回调，而不会调用{@link android.os.Handler#post(Runnable)} 抛到UI线程。
     * <p/>
     * if true will callback directly on the download thread(do not on post the message to the ui thread
     * by {@link android.os.Handler#post(Runnable)}
     */
    private boolean syncCallback = false;
    private int callbackProgressTimes = FileDownloadModel.DEFAULT_CALLBACK_PROGRESS_TIMES;

    private boolean isForceReDownload = false;

    /**
     * 如果{@link #isForceReDownload}为false
     * 并且检查文件是正确的{@link com.liulishuo.filedownloader.services.FileDownloadMgr#checkReuse(int, FileDownloadModel)}
     * 则不启动下载直接成功返回，此时该变量为true
     */
    private boolean isReusedOldFile = false;

    private final FileDownloadDriver driver;

    BaseDownloadTask(final String url) {
        this.url = url;
        driver = new FileDownloadDriver(this);
    }

    // --------------------------------------- FOLLOWING FUNCTION FOR INIT -----------------------------------------------

    static {
        FileDownloadEventPool.getImpl().addListener(DownloadTaskEvent.ID, new IDownloadListener() {
            @Override
            public boolean callback(IDownloadEvent event) {
                final DownloadTaskEvent taskEvent = (DownloadTaskEvent) event;
                switch (taskEvent.getOperate()) {
                    case DownloadTaskEvent.Operate.REQUEST_START:
                        taskEvent.consume()._start();
                        break;
                    default:
                        FileDownloadLog.e(DownloadTaskEvent.ID, "exception: do not recognize" +
                                " operate %s", taskEvent.getOperate());
                }
                return true;
            }
        });
    }
    // --------------------------------------- FOLLOWING FUNCTION FOR OUTSIDE ----------------------------------------------

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
     * Set maximal callback times on
     * callback {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     *
     * @param callbackProgressTimes Maximal callback progress status times,
     *                              Default 100, <=0 will not have any progress callback
     */
    public BaseDownloadTask setCallbackProgressTimes(int callbackProgressTimes) {
        this.callbackProgressTimes = callbackProgressTimes;
        return this;
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
     * any status follow end，warn,error,paused,completed
     *
     * @deprecated Replace with {@link #addFinishListener(FinishListener)}
     */
    public BaseDownloadTask setFinishListener(final FinishListener finishListener) {
        addFinishListener(finishListener);
        return this;
    }

    private ArrayList<FinishListener> finishListenerList;

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

        return getDownloadId();
    }

    /**
     * start download
     * <p/>
     * 用于启动一个单独任务
     *
     * @return Download id
     */
    public int start() {
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

            setStatus(FileDownloadStatus.error);
            setEx(e);
            FileDownloadList.getImpl().add(this);
            FileDownloadList.getImpl().removeByError(this);
        }

        if (ready) {
            FileDownloadEventPool.getImpl().send2Service(new DownloadTaskEvent(this)
                    .requestStart());
        }


        return getDownloadId();
    }

    // -------------- Another Operations ---------------------

    /**
     * Pause task
     * <p/>
     * 停止任务, 对于线程而言会直接关闭，清理所有相关数据，不会hold住任何东西
     * <p/>
     * 如果重新启动，默认会断点续传，所以为pause
     */
    public boolean pause() {
        setStatus(FileDownloadStatus.paused);

        final boolean result = _pauseExecute();

        // For make sure already added event listener for receive paused event
        FileDownloadList.getImpl().add(this);
        if (result) {
            FileDownloadList.getImpl().removeByPaused(this);
        } else {
            FileDownloadLog.w(this, "paused false %s", toString());
            // 一直依赖不在下载进程队列中
            // 只有可能是 串行 还没有执行到 or 并行还没来得及加入进的
            FileDownloadList.getImpl().removeByPaused(this);

        }
        return result;
    }

    // ------------------- get -----------------------

    /**
     * Get download id (generate by url & path)
     * id生成与url和path相关
     *
     * @return 获得有效的对应当前download task的id
     */
    public int getDownloadId() {
        // TODO 这里和savePah有关，但是savePath如果为空在start以后会重新生成因此有坑
        if (downloadId != 0) {
            return downloadId;
        }

        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(url)) {
            return downloadId = FileDownloadUtils.generateId(url, path);
        }

        return 0;
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
     * @return maximal callback times on
     * callback {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     */
    public int getCallbackProgressTimes() {
        return callbackProgressTimes;
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
     * @return Is reused downloaded old file, 是否是使用了已经存在的有效文件，而非启动下载
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

    // --------------------------------------- ABOVE FUNCTIONS FOR OUTSIDE ----------------------------------------------

    // --------------------------------------- FOLLOWING FUNCTIONS FOR INTERNAL --------------------------------------------------

    private void _checkFile(final String path) {
        File file = new File(path);
        if (file.exists()) {
            return;
        }

        if (!file.getParentFile().exists()) {
            //TODO file check really
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

    private void _start() {

        try {

            // Whether service was already started.
            if (!_checkCanStart()) {
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

            if (!_startExecute()) {
                setEx(new RuntimeException("not run download, not got download id"));
                FileDownloadList.getImpl().removeByError(this);
            }

        } catch (Throwable e) {
            e.printStackTrace();

            setEx(e);
            FileDownloadList.getImpl().removeByError(this);
        }

    }

    /**
     * Execute start
     *
     * @return succeed
     */
    protected abstract boolean _startExecute();

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
    // --------------------------------------- ABOVE FUNCTIONS FOR INTERNAL --------------------------------------------------

    // --------------------------------------- FOLLOWING FUNCTIONS FOR INTERNAL COOPERATION --------------------------------------------------

    FileDownloadEvent getOverEvent() {
        // Clean references after the end
        return new FileDownloadEvent(this).callback(_getOverCallback());
    }

    FileDownloadEvent getIngEvent() {
        return new FileDownloadEvent(this);
    }
    // ----------

    // Clear References
    void clear() {
        if (finishListenerList != null) {
            finishListenerList.clear();
        }
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

    // Error cause
    void setEx(Throwable ex) {
        this.ex = ex;
    }

    // The number of download so far
    void setSoFarBytes(long soFarBytes) {
        this.soFarBytes = soFarBytes;
    }

    // Total bytes
    void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    // Status, will changed before enqueue/dequeue/notify
    void setStatus(byte status) {
        if (status > FileDownloadStatus.MAX_INT ||
                status < FileDownloadStatus.MIN_INT) {
            throw new RuntimeException(String.format("status undefined, %d", status));
        }
        this.status = status;
    }

    // Driver
    FileDownloadDriver getDriver() {
        return this.driver;
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
            final ArrayList<FinishListener> listenersCopy =
                    (ArrayList<FinishListener>) finishListenerList.clone();
            final int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).over();
            }
        }
    }

    private void printNotMatchReasonLog(int status) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "can't update status change, %d, but the current" +
                    " status is %d, %d", status, getStatus(), getDownloadId());
        }
    }

    /**
     * @param transfer In order to optimize some of the data in some cases is not back
     */
    boolean update(final FileDownloadTransferModel transfer) {
        boolean match = false;
        switch (transfer.getStatus()) {
            case FileDownloadStatus.pending:
                if (getStatus() != FileDownloadStatus.INVALID_STATUS) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }
                match = true;
                this.setStatus(transfer.getStatus());
                this.setSoFarBytes(transfer.getSoFarBytes());
                this.setTotalBytes(transfer.getTotalBytes());

                // notify
                getDriver().notifyPending();
                break;
            case FileDownloadStatus.started:
                if (getStatus() != FileDownloadStatus.pending) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }
                match = true;
                this.setStatus(transfer.getStatus());

                // notify
                getDriver().notifyStarted();
                break;
            case FileDownloadStatus.connected:
                if (getStatus() != FileDownloadStatus.retry &&
                        getStatus() != FileDownloadStatus.started) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }

                match = true;
                setStatus(transfer.getStatus());
                setTotalBytes(transfer.getTotalBytes());
                setSoFarBytes(transfer.getSoFarBytes());
                this.resuming = transfer.isResuming();
                this.etag = transfer.getEtag();

                // notify
                getDriver().notifyConnected();
                break;
            case FileDownloadStatus.progress:
                if (getStatus() != FileDownloadStatus.progress &&
                        getStatus() != FileDownloadStatus.connected) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }

                match = true;
                setStatus(transfer.getStatus());
                setSoFarBytes(transfer.getSoFarBytes());

                // notify
                getDriver().notifyProgress();
                break;
            case FileDownloadStatus.blockComplete:
                /**
                 * Handled by {@link FileDownloadList#removeByCompleted(BaseDownloadTask)}
                 */
                break;
            case FileDownloadStatus.retry:
                if (getStatus() != FileDownloadStatus.progress &&
                        getStatus() != FileDownloadStatus.pending &&
                        getStatus() != FileDownloadStatus.connected) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }

                match = true;
                setStatus(transfer.getStatus());
                setSoFarBytes(transfer.getSoFarBytes());
                setEx(transfer.getThrowable());
                _setRetryingTimes(transfer.getRetryingTimes());

                // notify
                getDriver().notifyRetry();
                break;
            case FileDownloadStatus.error:
                if (getStatus() == FileDownloadStatus.error) {
                    FileDownloadLog.w(this, "%d already err(%s) , callback by other status same transfer",
                            getDownloadId(), getEx());
                    break;
                }

                match = true;
                setStatus(transfer.getStatus());
                setEx(transfer.getThrowable());
                setSoFarBytes(transfer.getSoFarBytes());

                // to FileDownloadList
                FileDownloadList.getImpl().removeByError(this);

                break;
            case FileDownloadStatus.paused:
                /**
                 * Handled by {@link #pause()}
                 */
                break;
            case FileDownloadStatus.completed:
                if (getStatus() != FileDownloadStatus.INVALID_STATUS &&
                        getStatus() != FileDownloadStatus.connected &&
                        getStatus() != FileDownloadStatus.progress) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }

                match = true;
                this.isReusedOldFile = transfer.isReusedOldFile();
                setStatus(transfer.getStatus());
                // only carry total data back
                setSoFarBytes(transfer.getTotalBytes());
                setTotalBytes(transfer.getTotalBytes());

                // to FileDownloadList
                FileDownloadList.getImpl().removeByCompleted(this);

                break;
            case FileDownloadStatus.warn:
                if (getStatus() != FileDownloadStatus.INVALID_STATUS) {
                    printNotMatchReasonLog(transfer.getStatus());
                    break;
                }

                match = true;
                final int count = FileDownloadList.getImpl().count(getDownloadId());
                if (count <= 1) {
                    // 1. this progress kill by sys and relive,
                    // for add at least one listener
                    // or 2. pre downloading task has already completed/error/paused
                    // request status
                    final int currentStatus = _getStatusFromServer(downloadId);
                    FileDownloadLog.w(this, "warn, but no listener to receive progress, " +
                            "switch to pending %d %d", getDownloadId(), currentStatus);

                    if (FileDownloadStatus.isIng(currentStatus)) {
                        // ing, has callbacks
                        // keep and wait callback

                        setStatus(FileDownloadStatus.pending);
                        getDriver().notifyPending();
                        break;
                    } else {
                        // already over and no callback
                    }

                }

                setStatus(transfer.getStatus());

                // to FileDownloadList
                FileDownloadList.getImpl().removeByWarn(this);
                break;
        }

        return match;
    }

    // why this? thread not safe: update,ready, _start, pause, start which influence of this
    // in the queue.
    // whether it has been added, whether or not it is removed.
    private volatile boolean isMarkedAdded2List = false;

    void markAdded2List() {
        isMarkedAdded2List = true;
    }

    void clearMarkAdded2List(){
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
    public interface FinishListener {
        void over();
    }

    @Override
    public String toString() {
        return String.format("%d@%s", getDownloadId(), super.toString());
    }

}
