package com.liulishuo.filedownloader;

import android.app.Application;
import android.util.Log;

import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.event.FileEventPoolImpl;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class FileDownloader {
    private final static String TAG = "FileDownloader";

    // TODO 这里优化不用Copy on Write
    private CopyOnWriteArrayList<BaseFileDownloadInternal> downloadList = new CopyOnWriteArrayList<>();

    public List<BaseFileDownloadInternal> getDownloadList() {
        return this.downloadList;
    }

    /**
     * 不耗时，做一些简单初始化准备工作，不会启动下载进程
     *
     * @param application
     */
    public static void init(final Application application) {
        // 下载进程与UI进程都存一个
        Log.d(TAG, "init Downloader");
        FileDownloadHelper.initAppContext(application);
        FileEventPool.setImpl(new FileEventPoolImpl());
    }

    private final static class HolderClass {
        private final static FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    public BaseFileDownloadInternal create(final String url) {
        return new FileDownloadInternal(url, downloadList);
    }

    /**
     * start download by same listener
     *
     * @param listener
     * @param isSerial 是否需要串行
     * @return
     */
    public List<Integer> start(final FileDownloadListener listener, final boolean isSerial) {
        ExecutorService threadPool = null;
        if (isSerial) {
            threadPool = Executors.newFixedThreadPool(1);
        }

        final Object[] downloadList = getDownloadList().toArray();
        final List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < downloadList.length; i++) {
            final BaseFileDownloadInternal downloadInternal = (BaseFileDownloadInternal) downloadList[i];
            if (downloadInternal.getListener() == listener) {

                if (threadPool != null) {
                    // 串行处理
                    threadPool.execute(new Runnable() {

                        final Object lockThread = new Object();
                        boolean isFinal = false;

                        @Override
                        public void run() {
                            if (!getDownloadList().contains(downloadInternal)) {
                                // paused?
                                return;
                            }

                            downloadInternal.setFinishListener(new BaseFileDownloadInternal.FinishListener() {
                                @Override
                                public void finalError(Throwable e) {
                                    isFinal = true;
                                    // 目前如果是失败一个，就把所有的给停了
                                    // TODO 这个看下有没有让用户主动触发的方法
                                    pause(listener);
                                    synchronized (lockThread) {
                                        lockThread.notify();
                                    }
                                }

                                @Override
                                public void finalComplete() {
                                    isFinal = true;
                                    synchronized (lockThread) {
                                        lockThread.notify();
                                    }
                                }

                                @Override
                                public void finalPause() {
                                    isFinal = true;
                                    synchronized (lockThread) {
                                        lockThread.notify();
                                    }
                                }

                                @Override
                                public void finalWarn() {
                                    isFinal = true;
                                    synchronized (lockThread) {
                                        lockThread.notify();
                                    }
                                }
                            })
                                    .start();

                            if (!isFinal) {
                                synchronized (lockThread) {
                                    try {
                                        lockThread.wait();
                                    } catch (InterruptedException e) {
                                        // TODO 下载失败的方式抛出
                                        e.printStackTrace();
                                    }
                                }

                                if (!isFinal) {
                                    // TODO 以错误的方式输出，这里不应该还没有结束
                                }
                            }

                        }
                    });

                } else {
                    ids.add(downloadInternal.start());
                }
            }
        }

        return ids;
    }


    /**
     * pause download by same listener
     *
     * @param listener
     * @see #pause(int)
     */
    public void pause(final FileDownloadListener listener) {
        final List<BaseFileDownloadInternal> downloadList = getDownloadList();
        final Object[] os = downloadList.toArray();
        for (Object o : os) {
            final BaseFileDownloadInternal downloadInternal = (BaseFileDownloadInternal) o;
            if (downloadInternal.getListener() == listener) {
                if (!downloadInternal.pause()) {
                    // 还没有开始下载，可能在pending?
                    downloadList.remove(downloadInternal);
                    downloadInternal.clear();
                }
            }
        }
    }

    public void pauseAll() {
        final List<BaseFileDownloadInternal> downloadList = getDownloadList();
        final Object[] os = downloadList.toArray();
        for (Object o : os) {
            final BaseFileDownloadInternal downloadInternal = (BaseFileDownloadInternal) o;
            if (!downloadInternal.pause()) {
                // 还没有开始下载，可能在pending?
                //TODO 所有的remove 都有没有告知外界?是否从架构层解决该问题
                downloadList.remove(downloadInternal);
                downloadInternal.clear();
            }
        }
    }

    /**
     * clear download by same listener
     * <p/>
     * Need {@link #pause(FileDownloadListener)} first?
     *
     * @param listener
     * @see #clear(int)
     */
    public void clear(final FileDownloadListener listener) {
        final List<BaseFileDownloadInternal> downloadList = getDownloadList();
        for (int i = 0; i < downloadList.size(); i++) {
            final BaseFileDownloadInternal downloadInternal = downloadList.get(i);
            if (downloadInternal.getListener() == listener) {
                downloadInternal.clear();
            }
        }
    }

    /**
     * pause download by download id
     *
     * @param downloadId
     * @see #pause(FileDownloadListener)
     */
    public void pause(final int downloadId) {
        BaseFileDownloadInternal downloaderInternal = getDownloaderInternal(downloadId);
        if (downloaderInternal == null) {
            return;
        }
        downloaderInternal.pause();
    }

    /**
     * clear download by download id
     * <p/>
     * Need {@link #pause(int)} first?
     *
     * @param downloadId
     * @see #clear(FileDownloadListener)
     */
    public void clear(final int downloadId) {
        BaseFileDownloadInternal downloaderInternal = getDownloaderInternal(downloadId);
        if (downloaderInternal == null) {
            return;
        }

        downloaderInternal.clear();
        downloadList.remove(downloaderInternal);
    }

    public void resume(final int downloadId) {
        BaseFileDownloadInternal downloaderInternal = getDownloaderInternal(downloadId);
        if (downloaderInternal == null) {
            return;
        }

        downloaderInternal.resume();
    }

    public int getSofar(final int downloadId) {
        BaseFileDownloadInternal downloaderInternal = getDownloaderInternal(downloadId);
        if (downloaderInternal == null) {
            return FileDownloadServiceUIGuard.getImpl().getSofar(downloadId);
        }

        return downloaderInternal.getDownloadedSofar();
    }

    public int getTotal(final int downloadId) {
        BaseFileDownloadInternal downloaderInternal = getDownloaderInternal(downloadId);
        if (downloaderInternal == null) {
            return FileDownloadServiceUIGuard.getImpl().getTotal(downloadId);
        }

        return downloaderInternal.getTotalSizeBytes();
    }

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

    private BaseFileDownloadInternal getDownloaderInternal(final int downloadId) {
        for (BaseFileDownloadInternal downloaderInternal : getDownloadList()) {
            if (downloaderInternal.getDownloadId() == downloadId) {
                return downloaderInternal;
            }
        }

        return null;
    }

    /**
     * cancelSystemDownloader & remove file
     *
     * @param downloadId will invalid
     * @return the number of downloads actually cancelled
     */
//    public int cancelSystemDownloader(final long downloadId) {
//        return EngzoSystemDownloaderInternal.DM.remove(downloadId);
//    }

}
