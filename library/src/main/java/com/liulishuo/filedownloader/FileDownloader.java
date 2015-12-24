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
     * <p/>
     * 建议在{@link Application#onCreate()}时调用
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
     * @param listener start download by same listener
     * @param isSerial 是否需要串行
     */
    public List<Integer> start(final FileDownloadListener listener, final boolean isSerial) {
        ExecutorService threadPool = null;
        if (isSerial) {
            threadPool = Executors.newFixedThreadPool(1);
        }

        final List<BaseDownloadTask> list = FileDownloadList.getImpl().copy(listener);
        FileDownloadLog.v(this, "start list size[%d] listener[%s] isSerial[%B]", list.size(), listener, isSerial);

        int index = 0;
        final List<Integer> ids = new ArrayList<>();
        for (final BaseDownloadTask downloadTask : list) {
            if (downloadTask.getListener() == listener) {

                if (threadPool != null) {
                    // 串行处理
                    threadPool.execute(new Runnable() {

                        Runnable setIndex(int index){
                            this.index = index;
                            return this;
                        }

                        int index = 0;
                        final Object lockThread = new Object();
                        boolean isFinal = false;

                        @Override
                        public void run() {
                            if (!FileDownloadList.getImpl().contains(downloadTask)) {
                                FileDownloadLog.d(FileDownloader.class, "serial go on %d %s but, list not contain", index, downloadTask);
                                // paused?
                                return;
                            }

                            downloadTask.setFinishListener(new BaseDownloadTask.FinishListener() {
                                @Override
                                public void over() {
                                    isFinal = true;
                                    synchronized (lockThread) {
                                        lockThread.notifyAll();
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
                            }

                            FileDownloadLog.v(FileDownloader.class, "end task(index:%d) and go next %s", index, downloadTask);

                        }
                    }.setIndex(index++));

                } else {
                    ids.add(downloadTask.start());
                }
            }
        }

        return ids;
    }


    /**
     * @param listener paused download by same listener
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

    public int getSoFar(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getSofar(downloadId);
        }

        return downloadTask.getSoFarBytes();
    }

    public int getTotal(final int downloadId) {
        BaseDownloadTask downloadTask = FileDownloadList.getImpl().get(downloadId);
        if (downloadTask == null) {
            return FileDownloadServiceUIGuard.getImpl().getTotal(downloadId);
        }

        return downloadTask.getTotalBytes();
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
