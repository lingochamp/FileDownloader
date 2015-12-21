package com.liulishuo.filedownloader;

import android.app.Activity;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.IOException;

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

    private Object tag;
    private Throwable ex;

    private int downloadedSofar;
    private int totalSizeBytes;
    private int status = FileDownloadStatus.pending;

    private int progressCallbackTimes = FileDownloadModel.DEFAULT_NOTIFY_NUMS;

    private boolean isForceReDownload = false;

    /**
     * 如果{@link #isForceReDownload}为false
     * 并且检查文件是正确的{@link com.liulishuo.filedownloader.services.FileDownloadMgr#checkReuse(String, String)}
     * 则不启动下载直接成功返回，此时该变量为true
     */
    private boolean isReusedOldFile = false;

    private FileDownloadDriver driver;

    public BaseFileDownloadInternal(final String url) {
        this.url = url;
        driver = new FileDownloadDriver(this);
    }

    private boolean isAddedEventLst = false;

    protected void addEventListener() {
        if (this.listener != null && !isAddedEventLst) {
            FileDownloadLog.d(this, "[addEventListener] %s", generateEventId());
            DownloadEventPool.getImpl().addListener(generateEventId(), this.listener);
            isAddedEventLst = true;
        }
    }

    protected void removeEventListener() {
        if (this.listener != null) {
            FileDownloadLog.d(this, "[removeEventListener] %s", generateEventId());
            DownloadEventPool.getImpl().removeListener(generateEventId(), this.listener);
            isAddedEventLst = false;
        }
    }

    /**
     * @return 确保以当前Downloader为单位唯一
     */
    public String generateEventId() {
        return toString();
    }

    /**
     * @param progressCallbackTimes progress的回调次数，<=0将不会进行progress回调
     * @see {@link FileDownloadListener#progress(BaseFileDownloadInternal, long, long)}
     */
    protected void setProgressCallbackTimes(int progressCallbackTimes) {
        this.progressCallbackTimes = progressCallbackTimes;
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

    protected void setDownloadedSofar(int downloadedSofar) {
        this.downloadedSofar = downloadedSofar;
    }

    protected void setTotalSizeBytes(int totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    protected void setIsForceRedownload(boolean isForceRedownload) {
        this.isForceReDownload = isForceRedownload;
    }

    protected void setStatus(int status) {
        if (status > FileDownloadStatus.MAX_INT ||
                status < FileDownloadStatus.MIN_INT) {
            throw new RuntimeException(String.format("status undefined, %d", status));
        }
        this.status = status;
    }

    private FinishListener mFinishListener;

    public BaseFileDownloadInternal setFinishListener(final FinishListener finishListener) {
        this.mFinishListener = finishListener;
        return this;
    }

    // --------------------------------------------
    public FileDownloadDriver getDriver() {
        return this.driver;
    }

    public void begin() {

    }

    public void ing() {

    }

    public void over() {

        if (mFinishListener != null) {
            mFinishListener.over();
        }
    }


    public int ready() {

        FileDownloadLog.d(this, "ready 2 download %s", toString());

        FileDownloadList.getImpl().ready(this);

        return getDownloadId();
    }

    // --------------------------------------------

    /**
     * start download
     *
     * @return Download id
     */
    public int start() {
        FileDownloadLog.d(this, "begin call start url[%s], savePath[%s], listener[%s], isNeedNotification[%B], notificationTitle[%s], notificationDesc[%s]," +
                " tag[%s]", url, savePath, listener, isNeedNotification, notificationTitle, notificationDesc, tag);

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

            FileDownloadList.getImpl().add(this);

            // 是否正在下载
            if (checkDownloading(getUrl(), getSavePath())) {
                // 正在下载
                // 这里就直接结束了
                FileDownloadLog.d(this, "Current is downloading %d", getDownloadId());

                setStatus(FileDownloadStatus.warn);
                FileDownloadList.getImpl().removeByWarn(this);

                return 0;
            }

            if (checkCanReuse()) {
                FileDownloadLog.d(this, "reuse downloaded file %s", getUrl());
                this.isReusedOldFile = true;


                setStatus(FileDownloadStatus.completed);
                FileDownloadList.getImpl().removeByCompleted(this);

            } else {
                FileDownloadLog.d(this, "start downloaded by ui process %s", getUrl());
                this.isReusedOldFile = false;

                downloadId = startExecute();
                if (downloadId == 0) {
                    setEx(new RuntimeException("not run download, not got download id"));
                    FileDownloadList.getImpl().removeByError(this);
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();

            setEx(e);
            FileDownloadList.getImpl().removeByError(this);
        }

        FileDownloadLog.d(this, "end call start url[%s], savePath[%s], listener[%s], isNeedNotification[%B], notificationTitle[%s], notificationDesc[%s]," +
                        "tag[%s]", url, savePath, listener, isNeedNotification, notificationTitle, notificationDesc,
                tag);

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
        setStatus(FileDownloadStatus.paused);

        final boolean result = pauseExecute();


        if (result) {
            FileDownloadList.getImpl().removeByPaused(this);
        } else {
            FileDownloadLog.w(this, "pause false %s", toString());
            // 一直依赖不在下载进程队列中
            // 只有可能是 串行 还没有执行到 or 并行还没来得及加入进的
            FileDownloadList.getImpl().removeByPaused(this);

        }
        return result;
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
    public BaseFileDownloadInternal forceReDownload() {
        this.isForceReDownload = true;
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

    public BaseFileDownloadInternal progressCallbackTimes(final int nums) {
        setProgressCallbackTimes(nums);
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
        return this.tag;
    }

    /**
     * For cache something you want
     *
     * @param tag
     * @return
     */
    public BaseFileDownloadInternal setTag(final Object tag) {
        this.tag = tag;
        FileDownloadLog.d(this, "setTag %s", tag);
        return this;
    }


    // -------------------------------------------------

    public int getDownloadId() {
        // TODO 这里和savePah有关，但是savePath如果为空在start以后会重新生成因此有坑
        return downloadId == 0 ? FileDownloadUtils.generateId(url, savePath) : downloadId;
    }

    /**
     * @return for OkHttpTag/ queue tag
     * <p/>
     * As in same queue has same chainKey
     */
    protected int getChainKey() {
        // TODO 极低概率不唯一
        return getListener().hashCode();
    }

    public String getUrl() {
        return url;
    }

    public int getProgressCallbackTimes() {
        return progressCallbackTimes;
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

    public int getDownloadedSofar() {
        return downloadedSofar;
    }

    public int getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public int getStatus() {
        return status;
    }

    public boolean isForceReDownload() {
        return this.isForceReDownload;
    }

    public Throwable getEx() {
        return ex;
    }

    public void setEx(Throwable ex) {
        this.ex = ex;
    }

    /**
     * @return
     * @see #isReusedOldFile
     */
    public boolean isReusedOldFile() {
        return isReusedOldFile;
    }

    // ---------------------------------------------
    public interface FinishListener {
        void over();
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


    private Runnable cacheRunnable;

    public Runnable getOverCallback() {
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

    @Override
    public String toString() {
        return String.format("%d@%s", getDownloadId(), super.toString());
    }

}
