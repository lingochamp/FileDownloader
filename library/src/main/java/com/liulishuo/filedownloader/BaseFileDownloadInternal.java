package com.liulishuo.filedownloader;

import android.app.Activity;

import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 9/23/15.
 */
public abstract class BaseFileDownloadInternal {

    private int downloadId;
    private String url;

    private String savePath;

    private FileDownloadListener listener;

    private boolean isNeedNotification;
    private String notificationTitle;
    private String notificationDesc;

    private String activityName;
    private Object mTag;

    private int downloadedSofar;
    private int totalSizeBytes;
    private int status;

    private int progressNotifyNums;

    private List<BaseFileDownloadInternal> downloadList;

    private boolean isForceRedownload = false;

    public BaseFileDownloadInternal(final String url, final List<BaseFileDownloadInternal> downloadList) {
        this.url = url;
        this.downloadList = downloadList;
    }

    private boolean isAddedEventLst = false;

    protected void addEventListener() {
        if (this.listener != null && !isAddedEventLst) {
            FileDownloadLog.d(this, "[addEventListener] %s", listener);
            FileEventPool.getImpl().addListener(generateEventId(), this.listener);
            isAddedEventLst = true;
        }
    }

    protected void removeEventListener() {
        if (this.listener != null) {
            FileDownloadLog.d(this, "[removeEventListener] %s", listener);
            FileEventPool.getImpl().removeListener(generateEventId(), this.listener);
            isAddedEventLst = false;
        }
    }

    public String generateEventId() {
        return Integer.toString(FileDownloadUtils.generateId(url, savePath));
    }

    protected void setProgressNotifyNums(int progressNotifyNums) {
        this.progressNotifyNums = progressNotifyNums;
    }

    protected void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    protected void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    protected void setUrl(String url) {
        this.url = url;
    }

    protected void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    protected void setListener(FileDownloadListener listener) {
        if (this.listener != listener) {
            isAddedEventLst = false;
        }
        this.listener = listener;
    }

    protected void setIsNeedNotification(boolean isNeedNotification) {
        this.isNeedNotification = isNeedNotification;
    }

    protected void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    protected void setNotificationDesc(String notificationDesc) {
        this.notificationDesc = notificationDesc;
    }

    protected void setmTag(Object mTag) {
        this.mTag = mTag;
    }

    protected void setDownloadedSofar(int downloadedSofar) {
        this.downloadedSofar = downloadedSofar;
    }

    protected void setTotalSizeBytes(int totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    protected void setIsForceRedownload(boolean isForceRedownload) {
        this.isForceRedownload = isForceRedownload;
    }

    protected void setStatus(int status) {
        if (status > FileDownloadStatus.MAX_INT ||
                status < FileDownloadStatus.MIN_INT) {
            throw new RuntimeException(String.format("status undefined, %d", status));
        }
        this.status = status;
    }

    protected List<BaseFileDownloadInternal> getDownloadList() {
        return this.downloadList;
    }

    private FinishListener mFinishListener;

    public BaseFileDownloadInternal setFinishListener(final FinishListener finishListener) {
        this.mFinishListener = finishListener;
        return this;
    }
    // --------------------------------------------

    protected void notifyPaused(final int downloadedSofar, final int totalSizeBytes) {
        FileDownloadLog.d(this, "notify paused %s %d %d", downloadId, downloadedSofar, totalSizeBytes);

        this.status = FileDownloadStatus.paused;
        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        FileEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(this).pause(downloadedSofar, totalSizeBytes));
        removeEventListener();

        if (mFinishListener != null) {
            mFinishListener.finalPause();
        }
    }

    protected void notifyPending(final int downloadedSofar, final int totalSizeBytes) {
        FileDownloadLog.d(this, "notify pending %s %d %d", downloadId, downloadedSofar, totalSizeBytes);

        this.status = FileDownloadStatus.pending;
        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        FileEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(this).pending(downloadedSofar, totalSizeBytes));
    }

    protected void notifyStarted() {
        FileDownloadLog.d(this, "notify start %s", downloadId);
        addEventListener();

        if (!downloadList.contains(this)) {
            downloadList.add(this);
        }

    }

    /**
     * 全下载器UI进程同步，检测是否存在相同的任务
     */
    private void checkSameTask() {
        synchronized (downloadList) {
            List<BaseFileDownloadInternal> removeList = new ArrayList<>();
            final Object[] objects = getDownloadList().toArray();
            for (Object object : objects) {
                final BaseFileDownloadInternal downloadInternal = (BaseFileDownloadInternal) object;
                if (downloadInternal.getDownloadId() == this.getDownloadId() &&
                        downloadInternal != this) {
                    removeList.add(downloadInternal);
                }
            }

            for (final BaseFileDownloadInternal baseFileDownloadInternal : removeList) {
                baseFileDownloadInternal.notifyWarn(new Runnable() {
                    @Override
                    public void run() {
                        baseFileDownloadInternal.removeEventListener();
                    }
                });
            }

            // 把后面有重复的清除掉
            downloadList.removeAll(removeList);
        }
    }

    protected void notifyWarn(final Runnable runnable) {
        FileDownloadLog.d(this, "notify warn same url & path %s", getDownloadId());
        FileEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(this).warn().callback(runnable));

        if (mFinishListener != null) {
            mFinishListener.finalWarn();
        }
    }

    public int ready() {

        // for test

        addEventListener();
        if (!downloadList.contains(this)) {
            downloadList.add(this);
        }

        FileDownloadLog.d(this, "ready 2 download %s %d %s", getDownloadId(), downloadList.size(), this.getUrl());
        return getDownloadId();
    }


    protected void notifyProgress(final int downloadedSofar, final int totalSizeBytes) {
        FileDownloadLog.d(this, "notify progress %s %d %d", downloadId, downloadedSofar, totalSizeBytes);

        this.status = FileDownloadStatus.progress;
        this.downloadedSofar = downloadedSofar;
        this.totalSizeBytes = totalSizeBytes;

        FileEventPool.getImpl().asyncPublishInMain(new FileDownloadEvent(this).progress(downloadedSofar, totalSizeBytes));
    }

    protected void notifyCompleted() {
        FileDownloadLog.d(this, "notify completed %s", downloadId);

        this.status = FileDownloadStatus.completed;
        this.downloadedSofar = this.totalSizeBytes;

        endDownloaded(null);
    }

    protected void notifyErrored(final Throwable e) {
        FileDownloadLog.e(this, e, "notify err %s %s", downloadId, e.getMessage());

        this.status = FileDownloadStatus.error;

        endDownloaded(e);
    }

    private Thread preCompleteThread;

    protected void endDownloaded(final Throwable e) {

        final FileDownloadEvent event = new FileDownloadEvent(BaseFileDownloadInternal.this);

        if (!downloadList.contains(this)) {
            if (e != null) {
                // fail
                FileEventPool.getImpl().asyncPublishInMain(event.error(e).
                        callback(getCloaseListenerCallback()));

                downloadList.remove(BaseFileDownloadInternal.this);
                clearBasic();

                // 结束整个过程
                if (mFinishListener != null) {
                    mFinishListener.finalError(e);
                }
            } else {
                clear();
            }

            return;
        }

        preCompleteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Throwable err = e;
                boolean isDownloadSucced = (err == null);

                boolean isPreCompletedSucced = true;

                try {
                    if (isDownloadSucced) {
                        FileEventPool.getImpl().publish(event.preCompleteOnNewThread());
                    }

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    isPreCompletedSucced = false;
                    err = new Throwable(String.format("pre completed err %s", ex.getMessage()), ex);
                }

                downloadList.remove(BaseFileDownloadInternal.this);
                clearBasic();

                // 结束整个过程
                if (isDownloadSucced && isPreCompletedSucced) {
                    // succeed
                    FileEventPool.getImpl().asyncPublishInMain(event.complete().
                            callback(getCloaseListenerCallback()));
                    if (mFinishListener != null) {
                        mFinishListener.finalComplete();
                    }
                } else {
                    // fail
                    FileEventPool.getImpl().asyncPublishInMain(event.error(err).
                            callback(getCloaseListenerCallback()));
                    if (mFinishListener != null) {
                        mFinishListener.finalError(err);
                    }
                }


            }
        });

        preCompleteThread.start();

    }


    public void onBindActivityResumed(final Activity activity) {
        if (downloadList.contains(this)) {
            addEventListener();
        }
    }

    public void onBindActivityPaused(final Activity activity) {
        removeEventListener();
    }
    // --------------------------------------------

    /**
     * start download
     *
     * @return Download id
     */
    public int start() {
        FileDownloadLog.d(this, "begin call start url[%s], savePath[%s], listener[%s], isNeedNotification[%B], notificationTitle[%s], notificationDesc[%s]," +
                " tag[%s]", url, savePath, listener, isNeedNotification, notificationTitle, notificationDesc, mTag);

        if (savePath == null) {
            savePath = FileDownloadUtils.getDefaultSaveFilePath(url);
            FileDownloadLog.e(this, "save path is null to %s", savePath);
        }


        addEventListener();

        if (isNeedNotification) {
            // TODO 替换app name
            notificationTitle = notificationTitle == null ? "app name" : notificationTitle;
        }


        this.downloadId = 0;

        try {
            checkFile(savePath);

            // 服务是否启动
            if (!checkCanStart()) {
                // 没有准备好
                return 0;
            }

            // 是否正在下载
            if (checkDownloading(getUrl(), getSavePath())) {
                // 正在下载
                // 这里就直接结束了
                FileDownloadLog.d(this, "Current is downloading %d", getDownloadId());
                downloadList.remove(this);
                notifyWarn(new Runnable() {
                    @Override
                    public void run() {
                        clear();
                    }
                });

                return 0;
            }

            if (checkCanReuse()) {
                FileDownloadLog.d(this, "reuse downloaded file %s", getUrl());
                notifyStarted();
                notifyCompleted();
            } else {
                FileDownloadLog.d(this, "start downloaded by ui process %s", getUrl());
                notifyStarted();
                downloadId = startExecute();
                if (downloadId == 0) {
                    notifyErrored(new RuntimeException("not run download, not got download id"));
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            notifyErrored(e);
        }

        FileDownloadLog.d(this, "end call start url[%s], savePath[%s], listener[%s], isNeedNotification[%B], notificationTitle[%s], notificationDesc[%s]," +
                        "activityName[%s], tag[%s]", url, savePath, listener, isNeedNotification, notificationTitle, notificationDesc, activityName,
                mTag);

        return downloadId;

    }

    protected boolean checkCanStart() {
        return true;
    }

    /**
     * @return
     * @warn need give values
     */
    protected boolean checkCanReuse() {
        return false;
    }

    /**
     * @return 是否正在下载
     */
    protected boolean checkDownloading(final String url, final String path) {
        return false;
    }

    /**
     * @return
     */
    protected abstract int startExecute();

    protected abstract boolean pauseExecute();

    public boolean pause() {
        if (preCompleteThread != null) {
            preCompleteThread.interrupt();
        }
        return pauseExecute();
    }

    protected abstract boolean resumeExecute();

    public void resume() {
        addEventListener();
        final boolean succeed = resumeExecute();
        if (!succeed) {
            removeEventListener();
        }
    }

    protected abstract boolean removeExecute();

    public void remove() {
        removeExecute();
        downloadId = 0;
    }


    public void clear() {
        removeEventListener();
        FileDownloadLog.d(this, "clear");
        clearBasic();
    }

    public void clearBasic() {
        FileDownloadLog.d(this, "clear");
    }

    /**
     * Force re download whether already downloaded complete
     *
     * @return
     */
    public BaseFileDownloadInternal forceRedownload() {
        this.isForceRedownload = true;
        return this;
    }

    /**
     * listener lifecycle bind activity's lifecycle
     *
     * @param activity
     * @return
     * @deprecated
     */
    public BaseFileDownloadInternal bindActivity(final Activity activity) {
        //TODO 有待考究
//        this.activityName = activity.toString();
        return this;
    }

    /**
     * @param path Path for save download file
     * @return
     */
    public BaseFileDownloadInternal savePath(final String path) {
        this.savePath = path;
        FileDownloadLog.d(this, "savePath %s", path);
        return this;
    }

    public BaseFileDownloadInternal addListener(final FileDownloadListener listener) {
        setListener(listener);
        FileDownloadLog.d(this, "addListener %s", listener);
        return this;
    }

    public BaseFileDownloadInternal progressNotifyNums(final int nums) {
        setProgressNotifyNums(nums);
        return this;
    }

    /**
     * Need notify & Default Notification Title is AppName
     *
     * @return
     */
    public BaseFileDownloadInternal needNotification() {
        return needNotification(null, null);
    }

    public BaseFileDownloadInternal needNotification(final String title) {
        return needNotification(title, null);
    }

    /**
     * Need notify
     *
     * @param title
     * @param desc
     * @return
     */
    public BaseFileDownloadInternal needNotification(final String title, final String desc) {
        this.isNeedNotification = true;
        this.notificationTitle = title;
        this.notificationDesc = desc;
        FileDownloadLog.d(this, "needNotification %s, %s", title, desc);
        return this;
    }

    public Object getTag() {
        return this.mTag;
    }

    /**
     * For cache something you want
     *
     * @param tag
     * @return
     */
    public BaseFileDownloadInternal setTag(final Object tag) {
        this.mTag = tag;
        FileDownloadLog.d(this, "setTag %s", tag);
        return this;
    }


    // -------------------------------------------------

    public int getDownloadId() {
        return downloadId == 0 ? FileDownloadUtils.generateId(url, savePath) : downloadId;
    }

    public String getUrl() {
        return url;
    }

    public int getProgressNotifyNums() {
        return progressNotifyNums;
    }

    public String getSavePath() {
        return savePath;
    }

    public FileDownloadListener getListener() {
        return listener;
    }

    public boolean isNeedNotification() {
        return isNeedNotification;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public String getNotificationDesc() {
        return notificationDesc;
    }

    public Object getmTag() {
        return mTag;
    }

    public int getDownloadedSofar() {
        return downloadedSofar;
    }

    public int getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public int getStatus() {
        return status;
    }

    public String getActivityName() {
        return activityName;
    }

    public boolean isForceRedownload() {
        return this.isForceRedownload;
    }

    // ---------------------------------------------
    public interface FinishListener {
        void finalError(final Throwable e);

        void finalComplete();

        void finalPause();

        void finalWarn();
    }

    protected boolean checkFile(final String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return true;
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        return false;
    }


    private Runnable getCloaseListenerCallback() {
        return new Runnable() {
            @Override
            public void run() {
                removeEventListener();
                setListener(null);
            }
        };
    }

    @Override
    public String toString() {
        return String.format("%d %s", getDownloadId(), super.toString());
    }
}
