package com.liulishuo.filedownloader;

import android.text.TextUtils;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Jacksgong on 9/23/15.
 */
public abstract class BaseDownloadTask {

    private int downloadId;

    private final String url;
    private String path;

    private FileDownloadListener listener;
    private FinishListener finishListener;

    private Object tag;
    private Throwable ex;

    private int soFarBytes;
    private int totalBytes;
    private int status = FileDownloadStatus.INVALID_STATUS;


    private boolean isContinue;
    private String etag;

    private int callbackProgressTimes = FileDownloadModel.DEFAULT_CALLBACK_PROGRESS_TIMES;

    private boolean isForceReDownload = false;

    /**
     * 如果{@link #isForceReDownload}为false
     * 并且检查文件是正确的{@link com.liulishuo.filedownloader.services.FileDownloadMgr#checkReuse(String, String)}
     * 则不启动下载直接成功返回，此时该变量为true
     */
    private boolean isReusedOldFile = false;

    private final FileDownloadDriver driver;

    BaseDownloadTask(final String url) {
        this.url = url;
        driver = new FileDownloadDriver(this);
    }

    // --------------------------------------- 以下 对外接口 ----------------------------------------------

    /**
     * @param path Path for save download file
     * @return
     */
    public BaseDownloadTask setPath(final String path) {
        this.path = path;
        FileDownloadLog.d(this, "setPath %s", path);
        return this;
    }

    /**
     * @param listener 对外的监听，在队列中可 以listener为单位进行绑定为一个队列
     * @return
     */
    public BaseDownloadTask setListener(final FileDownloadListener listener) {
        if (this.listener != listener) {
            isAddedEventLst = false;
        }
        this.listener = listener;

        FileDownloadLog.d(this, "setListener %s", listener);
        return this;
    }

    /**
     * @param callbackProgressTimes progress的回调次数，<=0将不会进行progress回调
     * @see {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
     */
    public BaseDownloadTask setCallbackProgressTimes(int callbackProgressTimes) {
        this.callbackProgressTimes = callbackProgressTimes;
        return this;
    }

    /**
     * For cache something you want
     *
     * @param tag
     * @return
     */
    public BaseDownloadTask setTag(final Object tag) {
        this.tag = tag;
        FileDownloadLog.d(this, "setTag %s", tag);
        return this;
    }


    /**
     * Force re download whether already downloaded completed
     * 强制重新下载不会，忽略文件是否是有效存在
     *
     * @param isForceReDownload 是否强制重新下载
     * @return
     */
    public BaseDownloadTask setForceReDownload(final boolean isForceReDownload) {
        this.isForceReDownload = isForceReDownload;
        return this;
    }

    /**
     * 任意的任务结束都会回调到这个回调，warn,error,paused,completed
     *
     * @param finishListener
     * @return
     */
    public BaseDownloadTask setFinishListener(final FinishListener finishListener) {
        this.finishListener = finishListener;
        return this;
    }

    // -------- 结束符 ------

    /**
     * 用于将几个task绑定为一个队列启动的结束符
     *
     * @return downloadId
     * @see FileDownloader#start(FileDownloadListener, boolean)
     */
    public int ready() {

        FileDownloadLog.d(this, "ready 2 download %s", toString());

        FileDownloadList.getImpl().ready(this);

        return getDownloadId();
    }

    /**
     * start download
     * 用于启动一个单独任务
     *
     * @return Download id
     */
    public int start() {
        FileDownloadLog.v(this, "begin call start " +
                        "url[%s], setPath[%s] listener[%s], tag[%s]",
                url, path, listener, tag);

        _adjust();
        _start();

        FileDownloadLog.v(this, "end call start " +
                        "url[%s], setPath[%s], listener[%s], tag[%s]",
                url, path, listener, tag);

        return getDownloadId();
    }

    // -------------- 其他操作 ---------------------

    /**
     * 停止任务, 对于线程而言会直接关闭，清理所有相关数据，不会hold住任何东西
     *
     * 如果重新启动，默认会断点续传，所以为pause
     *
     * @return
     */
    public boolean pause() {
        setStatus(FileDownloadStatus.paused);

        final boolean result = _pauseExecute();

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
     * @return 获得有效的对应当前download task的id
     * id生成与url和path相关
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
     * @return 下载连接
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return progress 回调最多次数
     */
    public int getCallbackProgressTimes() {
        return callbackProgressTimes;
    }

    /**
     * @return 下载文件存储路径
     */
    public String getPath() {
        return path;
    }

    /**
     * @return 监听器，可作为绑定为一个队列的target
     */
    public FileDownloadListener getListener() {
        return listener;
    }

    /**
     * @return 下载的进度
     */
    public int getSoFarBytes() {
        return soFarBytes;
    }

    /**
     * @return 下载文件的总大小
     * 在启动第一次progress回调的时候获得，
     * 这个是在连接上以后从头部获得
     */
    public int getTotalBytes() {
        return totalBytes;
    }

    /**
     * @return 当前状态
     * @see FileDownloadStatus
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return 是否强制下载
     */
    public boolean isForceReDownload() {
        return this.isForceReDownload;
    }

    /**
     * @return 错误内容
     */
    public Throwable getEx() {
        return ex;
    }


    /**
     * @return
     * @see #isReusedOldFile
     */
    public boolean isReusedOldFile() {
        return isReusedOldFile;
    }

    /**
     * @return 用户层级需要cache在task中的数据
     */
    public Object getTag() {
        return this.tag;
    }

    /**
     * @return 是否是成功断点续传，在{@link FileDownloadStatus#connected}上以后获得该值
     */
    public boolean isContinue() {
        return this.isContinue;
    }

    /**
     * @return ETag
     */
    public String getEtag() {
        return this.etag;
    }

    // --------------------------------------- 以上 对外接口 ----------------------------------------------

    // --------------------------------------- 以下 内部机制 --------------------------------------------------

    private boolean isAddedEventLst = false;

    private void _addEventListener() {
        if (this.listener != null && !isAddedEventLst) {
            FileDownloadLog.d(this, "[_addEventListener] %s", generateEventId());
            DownloadEventPool.getImpl().addListener(generateEventId(), this.listener);
            isAddedEventLst = true;
        }
    }

    private void _removeEventListener() {
        if (this.listener != null) {
            FileDownloadLog.d(this, "[_removeEventListener] %s", generateEventId());
            DownloadEventPool.getImpl().removeListener(generateEventId(), this.listener);
            isAddedEventLst = false;
        }
    }

    private void _checkFile(final String path) {
        File file = new File(path);
        if (file.exists()) {
            return;
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    protected boolean _checkCanStart() {
        return true;
    }

    /**
     * @return
     * @warn need give values
     */
    protected boolean _checkCanReuse() {
        return false;
    }

    /**
     * @return 是否正在下载
     */
    protected boolean _checkDownloading(final String url, final String path) {
        return false;
    }

    // 矫正一些没有初始化的数据
    private void _adjust() {
        if (path == null) {
            path = FileDownloadUtils.getDefaultSaveFilePath(url);
            FileDownloadLog.e(this, "save path is null to %s", path);
        }
    }

    private void _start() {

        try {
            _addEventListener();

            _checkFile(path);

            // 服务是否启动
            if (!_checkCanStart()) {
                // 没有准备好
                return;
            }

            FileDownloadList.getImpl().add(this);

            // 是否正在下载
            if (_checkDownloading(getUrl(), getPath())) {
                // 正在下载
                // 这里就直接结束了
                FileDownloadLog.d(this, "Current is downloading %d", getDownloadId());

                setStatus(FileDownloadStatus.warn);
                FileDownloadList.getImpl().removeByWarn(this);

                return;
            }

            if (_checkCanReuse()) {
                FileDownloadLog.d(this, "reuse downloaded file %s", getUrl());
                this.isReusedOldFile = true;

                setStatus(FileDownloadStatus.completed);
                FileDownloadList.getImpl().removeByCompleted(this);

            } else {
                FileDownloadLog.d(this, "start downloaded by ui process %s", getUrl());
                this.isReusedOldFile = false;

                if (_startExecute() == 0) {
                    setEx(new RuntimeException("not run download, not got download id"));
                    FileDownloadList.getImpl().removeByError(this);
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();

            setEx(e);
            FileDownloadList.getImpl().removeByError(this);
        }

    }

    // 处理启动
    protected abstract int _startExecute();

    // 处理暂停
    protected abstract boolean _pauseExecute();

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

    // --------------------------------------- 以上 内部机制 --------------------------------------------------

    // --------------------------------------- 以下 内部协作接口 --------------------------------------------------

    FileDownloadEvent getOverEvent() {
        // 结束以后做好清理
        return new FileDownloadEvent(this).callback(_getOverCallback());
    }

    FileDownloadEvent getIngEvent() {
        return new FileDownloadEvent(this);
    }
    // ----------

    // 清理资源
    void clear() {
        _removeEventListener();
        FileDownloadLog.d(this, "clear");
    }

    /**
     * @return 确保以当前Downloader为单位唯一
     */
    String generateEventId() {
        return toString();
    }

    // 错误内容
    void setEx(Throwable ex) {
        this.ex = ex;
    }

    // 下载进度变化
    void setSoFarBytes(int soFarBytes) {
        this.soFarBytes = soFarBytes;
    }

    // 总大小变化
    void setTotalBytes(int totalBytes) {
        this.totalBytes = totalBytes;
    }

    // 状态变化，在入队/出队/通知 之前改变
    void setStatus(int status) {
        if (status > FileDownloadStatus.MAX_INT ||
                status < FileDownloadStatus.MIN_INT) {
            throw new RuntimeException(String.format("status undefined, %d", status));
        }
        this.status = status;
    }

    // 驱动器
    FileDownloadDriver getDriver() {
        return this.driver;
    }

    // ------------------
    // 开始进入队列
    void begin() {
        _addEventListener();
    }

    // 进行中任意回调
    void ing() {
    }

    // 结束
    void over() {
        if (finishListener != null) {
            finishListener.over();
        }
    }

    /**
     * @param transfer 为了优化有部分数据在某些情况下是没有带回来的
     */
    void update(final FileDownloadTransferModel transfer) {
        switch (transfer.getStatus()) {
            case FileDownloadStatus.pending:
                if (getStatus() == FileDownloadStatus.pending) {
                    FileDownloadLog.w(this, "already pending %d", getDownloadId());
                    break;
                }
                this.setStatus(transfer.getStatus());
                this.setSoFarBytes(transfer.getSoFarBytes());
                this.setTotalBytes(transfer.getTotalBytes());

                // 抛通知
                getDriver().notifyPending();
                break;
            case FileDownloadStatus.connected:
                if (getStatus() == FileDownloadStatus.connected) {
                    FileDownloadLog.w(this, "already connected %d", transfer.getDownloadId());
                    break;
                }

                setStatus(transfer.getStatus());
                setTotalBytes(transfer.getTotalBytes());
                setSoFarBytes(transfer.getSoFarBytes());
                this.isContinue = transfer.isContinue();
                this.etag = transfer.getEtag();

                // 抛通知
                getDriver().notifyConnected();
                break;
            case FileDownloadStatus.progress:
                if (getStatus() == FileDownloadStatus.progress && transfer.getSoFarBytes() == getSoFarBytes()) {
                    FileDownloadLog.w(this, "unused values! by process callback");
                    break;
                }

                setStatus(transfer.getStatus());
                setSoFarBytes(transfer.getSoFarBytes());

                // 抛通知
                getDriver().notifyProgress();
                break;
            case FileDownloadStatus.blockComplete:
                // 该事件是在complete消息处理(FileDownloadList中根据complete之前回调这个消息)
                break;
            case FileDownloadStatus.error:
                if (getStatus() == FileDownloadStatus.error) {
                    FileDownloadLog.w(this, "already err , callback by other status same transfer");
                    break;
                }

                setStatus(transfer.getStatus());
                setEx(transfer.getThrowable());
                setSoFarBytes(transfer.getSoFarBytes());

                FileDownloadList.getImpl().removeByError(this);

                break;
            case FileDownloadStatus.paused:
                // 由#pause直接根据回调结果处理
                break;
            case FileDownloadStatus.completed:
                if (getStatus() == FileDownloadStatus.completed) {
                    FileDownloadLog.w(this, "already completed , callback by process with same transfer");
                    break;
                }

                setStatus(transfer.getStatus());
                setSoFarBytes(getTotalBytes());

                // 抛给list处理
                FileDownloadList.getImpl().removeByCompleted(this);

                break;
            case FileDownloadStatus.warn:
                // 由#start直接根据回调处理
                break;
        }
    }
    // --------------------------------------- 以上 内部协作接口 --------------------------------------------------

    // -------------------------------------------------

    /**
     * @return for OkHttpTag/ queue tag
     *
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
