package com.liulishuo.filedownloader;

import android.app.Application;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventPoolImpl;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class FileDownloader {

    /**
     * 不耗时，做一些简单初始化准备工作，不会启动下载进程
     *
     * @param application
     */
    public static void init(final Application application) {
        // 下载进程与非下载进程都存一个
        FileDownloadLog.d(FileDownloader.class, "init Downloader");
        FileDownloadHelper.initAppContext(application);
        DownloadEventPool.setImpl(new DownloadEventPoolImpl());
    }

    private final static class HolderClass {
        private final static FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    public BaseDownloadTask create(final String url) {
        return new FileDownloadTask(url);
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

        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        final List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < downloadList.length; i++) {
            final BaseDownloadTask downloadInternal = downloadList[i];
            if (downloadInternal.getListener() == listener) {

                if (threadPool != null) {
                    // 串行处理
                    threadPool.execute(new Runnable() {

                        final Object lockThread = new Object();
                        boolean isFinal = false;

                        @Override
                        public void run() {
                            if (!FileDownloadList.getImpl().contains(downloadInternal)) {
                                // paused?
                                return;
                            }

                            downloadInternal.setFinishListener(new BaseDownloadTask.FinishListener() {
                                @Override
                                public void over() {
                                    isFinal = true;
                                    synchronized (lockThread) {
                                        lockThread.notify();
                                    }
                                }
                            }).start();

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
     * paused download by same listener
     *
     * @param listener
     * @see #pause(int)
     */
    public void pause(final FileDownloadListener listener) {
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        for (BaseDownloadTask baseDownloadTask : downloadList) {
            if (baseDownloadTask.getListener() == listener) {
                baseDownloadTask.pause();
            }
        }

    }

    public void pauseAll() {
        final BaseDownloadTask[] downloadList = FileDownloadList.getImpl().copy();
        for (BaseDownloadTask baseDownloadTask : downloadList) {
            baseDownloadTask.pause();
        }
    }

    /**
     * paused download by download id
     *
     * @param downloadId
     * @see #pause(FileDownloadListener)
     */
    public void pause(final int downloadId) {
        BaseDownloadTask downloaderInternal = FileDownloadList.getImpl().get(downloadId);
        if (downloaderInternal == null) {
            FileDownloadLog.w(this, "request pause but not exist %d", downloadId);
            return;
        }
        downloaderInternal.pause();
    }

    public int getSofar(final int downloadId) {
        BaseDownloadTask downloaderInternal = FileDownloadList.getImpl().get(downloadId);
        if (downloaderInternal == null) {
            return FileDownloadServiceUIGuard.getImpl().getSofar(downloadId);
        }

        return downloaderInternal.getSoFarBytes();
    }

    public int getTotal(final int downloadId) {
        BaseDownloadTask downloaderInternal = FileDownloadList.getImpl().get(downloadId);
        if (downloaderInternal == null) {
            return FileDownloadServiceUIGuard.getImpl().getTotal(downloadId);
        }

        return downloaderInternal.getTotalBytes();
    }

    /**
     * 可以提前绑定服务，提高第一次启动下载的耗时
     */
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


}
