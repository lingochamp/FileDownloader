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

import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * The download task.
 */

public class DownloadTask implements BaseDownloadTask, BaseDownloadTask.IRunningTask,
        DownloadTaskHunter.ICaptureTask {

    private final ITaskHunter mHunter;
    private final ITaskHunter.IMessageHandler mMessageHandler;
    private int mId;

    private ArrayList<BaseDownloadTask.FinishListener> mFinishListenerList;

    private final String mUrl;
    private String mPath;
    private String mFilename;
    private boolean mPathAsDirectory;

    private FileDownloadHeader mHeader;

    private FileDownloadListener mListener;

    private SparseArray<Object> mKeyedTags;
    private Object mTag;

    private int mAutoRetryTimes = 0;

    /**
     * If {@code true} will callback directly on the download thread(do not on post the message to
     * the ui thread
     * by {@link android.os.Handler#post(Runnable)}
     */
    private boolean mSyncCallback = false;

    private boolean mIsWifiRequired = false;

    public final static int DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS = 10;
    private int mCallbackProgressTimes = FileDownloadModel.DEFAULT_CALLBACK_PROGRESS_TIMES;
    private int mCallbackProgressMinIntervalMillis = DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS;

    private boolean mIsForceReDownload = false;

    volatile int mAttachKey = 0;
    private boolean mIsInQueueTask = false;

    DownloadTask(final String url) {
        this.mUrl = url;
        mPauseLock = new Object();
        final DownloadTaskHunter hunter = new DownloadTaskHunter(this, mPauseLock);

        mHunter = hunter;
        mMessageHandler = hunter;
    }

    @Override
    public BaseDownloadTask setMinIntervalUpdateSpeed(int minIntervalUpdateSpeedMs) {
        mHunter.setMinIntervalUpdateSpeed(minIntervalUpdateSpeedMs);
        return this;
    }

    @Override
    public BaseDownloadTask setPath(final String path) {
        return setPath(path, false);
    }

    @Override
    public BaseDownloadTask setPath(final String path, final boolean pathAsDirectory) {
        this.mPath = path;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setPath %s", path);
        }

        this.mPathAsDirectory = pathAsDirectory;
        if (pathAsDirectory) {
            /**
             * will be found before the callback of
             * {@link FileDownloadListener#connected(BaseDownloadTask, String, boolean, int, int)}
             */
            this.mFilename = null;
        } else {
            this.mFilename = new File(path).getName();
        }

        return this;
    }

    @Override
    public BaseDownloadTask setListener(final FileDownloadListener listener) {
        this.mListener = listener;

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setListener %s", listener);
        }
        return this;
    }

    @Override
    public BaseDownloadTask setCallbackProgressTimes(int callbackProgressCount) {
        this.mCallbackProgressTimes = callbackProgressCount;
        return this;
    }

    @Override
    public BaseDownloadTask setCallbackProgressMinInterval(int minIntervalMillis) {
        this.mCallbackProgressMinIntervalMillis = minIntervalMillis;
        return this;
    }

    @Override
    public BaseDownloadTask setCallbackProgressIgnored() {
        return setCallbackProgressTimes(-1);
    }

    @Override
    public BaseDownloadTask setTag(final Object tag) {
        this.mTag = tag;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "setTag %s", tag);
        }
        return this;
    }

    @Override
    public BaseDownloadTask setTag(final int key, final Object tag) {
        if (mKeyedTags == null) {
            mKeyedTags = new SparseArray<>(2);
        }
        mKeyedTags.put(key, tag);
        return this;
    }

    @Override
    public BaseDownloadTask setForceReDownload(final boolean isForceReDownload) {
        this.mIsForceReDownload = isForceReDownload;
        return this;
    }

    @Override
    public BaseDownloadTask setFinishListener(final BaseDownloadTask.FinishListener finishListener) {
        addFinishListener(finishListener);
        return this;
    }

    @Override
    public BaseDownloadTask addFinishListener(final BaseDownloadTask.FinishListener finishListener) {
        if (mFinishListenerList == null) {
            mFinishListenerList = new ArrayList<>();
        }

        if (!mFinishListenerList.contains(finishListener)) {
            mFinishListenerList.add(finishListener);
        }
        return this;
    }

    @Override
    public boolean removeFinishListener(final BaseDownloadTask.FinishListener finishListener) {
        return mFinishListenerList != null && mFinishListenerList.remove(finishListener);
    }

    @Override
    public BaseDownloadTask setAutoRetryTimes(int autoRetryTimes) {
        this.mAutoRetryTimes = autoRetryTimes;
        return this;
    }

    @Override
    public BaseDownloadTask addHeader(final String name, final String value) {
        checkAndCreateHeader();
        mHeader.add(name, value);
        return this;
    }

    @Override
    public BaseDownloadTask addHeader(final String line) {
        checkAndCreateHeader();
        mHeader.add(line);
        return this;
    }

    @Override
    public BaseDownloadTask removeAllHeaders(final String name) {
        if (mHeader == null) {
            synchronized (headerCreateLock) {
                // maybe invoking checkAndCreateHear and will to be available.
                if (mHeader == null) {
                    return this;
                }
            }
        }


        mHeader.removeAll(name);
        return this;
    }

    @Override
    public BaseDownloadTask setSyncCallback(final boolean syncCallback) {
        this.mSyncCallback = syncCallback;
        return this;
    }

    @Override
    public BaseDownloadTask setWifiRequired(boolean isWifiRequired) {
        this.mIsWifiRequired = isWifiRequired;
        return this;
    }

    @Override
    public int ready() {
        return asInQueueTask().enqueue();
    }

    @Override
    public InQueueTask asInQueueTask() {
        return new InQueueTaskImpl(this);
    }

    @Override
    public boolean reuse() {
        if (isRunning()) {
            FileDownloadLog.w(this, "This task[%d] is running, if you want start the same task," +
                    " please create a new one by FileDownloader#create", getId());
            return false;
        }

        this.mAttachKey = 0;
        mIsInQueueTask = false;
        mIsMarkedAdded2List = false;
        mHunter.reset();

        return true;
    }

    @Override
    public boolean isUsing() {
        return mHunter.getStatus() != FileDownloadStatus.INVALID_STATUS;
    }


    @Override
    public boolean isRunning() {
        //noinspection SimplifiableIfStatement
        if (FileDownloader.getImpl().getLostConnectedHandler().isInWaitingList(this)) {
            return true;
        }

        return FileDownloadStatus.isIng(getStatus());
    }

    @Override
    public boolean isAttached() {
        return mAttachKey != 0;
    }

    @Override
    public int start() {
        if (mIsInQueueTask) {
            throw new IllegalStateException("If you start the task manually, it means this task " +
                    "doesn't belong to a queue, so you must not invoke BaseDownloadTask#ready() or" +
                    " InQueueTask#enqueue() before you start() this method. For detail: If this" +
                    " task doesn't belong to a queue, what is just an isolated task, you just need" +
                    " to invoke BaseDownloadTask#start() to start this task, that's all. In other" +
                    " words, If this task doesn't belong to a queue, you must not invoke" +
                    " BaseDownloadTask#ready() method or InQueueTask#enqueue() method before" +
                    " invoke BaseDownloadTask#start(), If you do that and if there is the same" +
                    " listener object to start a queue in another thread, this task may be " +
                    "assembled by the queue, in that case, when you invoke BaseDownloadTask#start()" +
                    " manually to start this task or this task is started by the queue, there is" +
                    " an exception buried in there, because this task object is started two times" +
                    " without declare BaseDownloadTask#reuse() : 1. you invoke " +
                    "BaseDownloadTask#start() manually; 2. the queue start this task automatically.");
        }

        return startTaskUnchecked();
    }

    private int startTaskUnchecked() {
        if (isUsing()) {
            if (isRunning()) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("This task is running %d, if you" +
                                " want to start the same task, please create a new one by" +
                                " FileDownloader.create", getId()));
            } else {
                throw new IllegalStateException("This task is dirty to restart, If you want to " +
                        "reuse this task, please invoke #reuse method manually and retry to " +
                        "restart again." + mHunter.toString());
            }
        }

        if (!isAttached()) {
            setAttachKeyDefault();
        }

        mHunter.intoLaunchPool();

        return getId();
    }

    // -------------- Another Operations ---------------------

    private final Object mPauseLock;

    @Override
    public boolean pause() {
        synchronized (mPauseLock) {
            return mHunter.pause();
        }
    }

    @Override
    public boolean cancel() {
        return pause();
    }

    // ------------------- Get -----------------------

    @Override
    public int getId() {
        if (mId != 0) {
            return mId;
        }

        if (!TextUtils.isEmpty(mPath) && !TextUtils.isEmpty(mUrl)) {
            return mId = FileDownloadUtils.generateId(mUrl, mPath, mPathAsDirectory);
        }

        return 0;
    }

    /**
     * @deprecated Used {@link #getId()} instead.
     */
    @Override
    public int getDownloadId() {
        return getId();
    }

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public int getCallbackProgressTimes() {
        return mCallbackProgressTimes;
    }

    @Override
    public int getCallbackProgressMinInterval() {
        return mCallbackProgressMinIntervalMillis;
    }

    @Override
    public String getPath() {
        return mPath;
    }

    @Override
    public boolean isPathAsDirectory() {
        return mPathAsDirectory;
    }

    @Override
    public String getFilename() {
        return mFilename;
    }

    @Override
    public String getTargetFilePath() {
        return FileDownloadUtils.getTargetFilePath(getPath(), isPathAsDirectory(), getFilename());
    }

    @Override
    public FileDownloadListener getListener() {
        return mListener;
    }

    @Override
    public int getSoFarBytes() {
        return getSmallFileSoFarBytes();
    }

    @Override
    public int getSmallFileSoFarBytes() {
        if (mHunter.getSofarBytes() > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) mHunter.getSofarBytes();
    }

    @Override
    public long getLargeFileSoFarBytes() {
        return mHunter.getSofarBytes();
    }

    @Override
    public int getTotalBytes() {
        return getSmallFileTotalBytes();
    }

    @Override
    public int getSmallFileTotalBytes() {
        if (mHunter.getTotalBytes() > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) mHunter.getTotalBytes();
    }

    @Override
    public long getLargeFileTotalBytes() {
        return mHunter.getTotalBytes();
    }

    @Override
    public int getSpeed() {
        return mHunter.getSpeed();
    }

    @Override
    public byte getStatus() {
        return mHunter.getStatus();
    }

    @Override
    public boolean isForceReDownload() {
        return this.mIsForceReDownload;
    }

    @Override
    public Throwable getEx() {
        return getErrorCause();
    }

    @Override
    public Throwable getErrorCause() {
        return mHunter.getErrorCause();
    }

    @Override
    public boolean isReusedOldFile() {
        return mHunter.isReusedOldFile();
    }

    @Override
    public Object getTag() {
        return this.mTag;
    }

    @Override
    public Object getTag(int key) {
        return mKeyedTags == null ? null : mKeyedTags.get(key);
    }


    /**
     * @deprecated Use {@link #isResuming()} instead.
     */
    @Override
    public boolean isContinue() {
        return isResuming();
    }

    @Override
    public boolean isResuming() {
        return mHunter.isResuming();
    }

    @Override
    public String getEtag() {
        return mHunter.getEtag();
    }

    @Override
    public int getAutoRetryTimes() {
        return this.mAutoRetryTimes;
    }

    @Override
    public int getRetryingTimes() {
        return mHunter.getRetryingTimes();
    }

    @Override
    public boolean isSyncCallback() {
        return mSyncCallback;
    }

    @Override
    public boolean isLargeFile() {
        return mHunter.isLargeFile();
    }

    @Override
    public boolean isWifiRequired() {
        return mIsWifiRequired;
    }

    private final Object headerCreateLock = new Object();

    private void checkAndCreateHeader() {
        if (mHeader == null) {
            synchronized (headerCreateLock) {
                if (mHeader == null) {
                    mHeader = new FileDownloadHeader();
                }
            }
        }
    }

    @Override
    public FileDownloadHeader getHeader() {
        return this.mHeader;
    }


    // why this? thread not safe: update,InQueueTask#enqueue, start, pause, start which influence of this
    // in the queue.
    // whether it has been added, whether or not it is removed.
    private volatile boolean mIsMarkedAdded2List = false;

    @Override
    public void markAdded2List() {
        mIsMarkedAdded2List = true;
    }

    @Override
    public void free() {
        mHunter.free();
        if (FileDownloadList.getImpl().isNotContains(this)) {
            mIsMarkedAdded2List = false;
        }
    }

    @Override
    public void startTaskByQueue() {
        startTaskUnchecked();
    }

    @Override
    public void startTaskByRescue() {
        // In this case, we don't need to check, because, we just to rescue this task, it means this
        // task has already called start, but the filedownloader service didn't connected, and now,
        // the service is connected, so we just rescue this task.
        startTaskUnchecked();
    }

    @Override
    public Object getPauseLock() {
        return mPauseLock;
    }

    @Override
    public boolean isContainFinishListener() {
        return mFinishListenerList != null && mFinishListenerList.size() > 0;
    }


    @Override
    public boolean isMarkedAdded2List() {
        return this.mIsMarkedAdded2List;
    }

    @Override
    public BaseDownloadTask.IRunningTask getRunningTask() {
        return this;
    }

    @Override
    public void setFileName(String fileName) {
        mFilename = fileName;
    }

    @Override
    public ArrayList<FinishListener> getFinishListenerList() {
        return mFinishListenerList;
    }

    @Override
    public BaseDownloadTask getOrigin() {
        return this;
    }

    @Override
    public ITaskHunter.IMessageHandler getMessageHandler() {
        return mMessageHandler;
    }

    @Override
    public boolean is(int id) {
        return getId() == id;
    }

    @Override
    public boolean is(FileDownloadListener listener) {
        return getListener() == listener;
    }

    @Override
    public boolean isOver() {
        return FileDownloadStatus.isOver(getStatus());
    }

    @Override
    public int getAttachKey() {
        return mAttachKey;
    }

    @Override
    public void setAttachKeyByQueue(int key) {
        this.mAttachKey = key;
    }

    @Override
    public void setAttachKeyDefault() {
        final int attachKey;
        if (getListener() != null) {
            attachKey = getListener().hashCode();
        } else {
            attachKey = hashCode();
        }
        this.mAttachKey = attachKey;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("%d@%s", getId(), super.toString());
    }

    private final static class InQueueTaskImpl implements InQueueTask {
        private final DownloadTask mTask;

        private InQueueTaskImpl(DownloadTask task) {
            this.mTask = task;
            this.mTask.mIsInQueueTask = true;
        }

        @Override
        public int enqueue() {
            final int id = mTask.getId();

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "add the task[%d] to the queue", id);
            }

            FileDownloadList.getImpl().addUnchecked(mTask);
            return id;
        }
    }
}
