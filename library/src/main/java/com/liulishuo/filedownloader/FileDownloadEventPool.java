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

import com.liulishuo.filedownloader.event.DownloadEventPoolImpl;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event Pool for process which not :filedownloader process
 * <p/>
 * Created by Jacksgong on 12/26/15.
 */
public class FileDownloadEventPool extends DownloadEventPoolImpl {

    private ExecutorService sendPool = Executors.newFixedThreadPool(3);
    private final ExecutorService receivePool = Executors.newFixedThreadPool(3);

    private static class HolderClass {
        private final static FileDownloadEventPool INSTANCE = new FileDownloadEventPool();
    }

    private FileDownloadEventPool() {
        super();
    }

    public static FileDownloadEventPool getImpl() {
        return HolderClass.INSTANCE;
    }

    private volatile int wait2SendThreadCount = 0;

    synchronized void send2Service(final DownloadTaskEvent event) {
        wait2SendThreadCount++;
        sendPool.execute(new Runnable() {
            @Override
            public void run() {
                if (isShutDownThread(event)) {
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.v(FileDownloadEventPool.class, "pass event because:" +
                                " shutdown already. %s", event.getTaskListener());
                    }
                    return;
                }
                wait2SendThreadCount--;
                publish(event);
            }
        });
    }

    void receiveByService(final DownloadTransferEvent event) {
        if (event.getTransfer() != null
                && event.getTransfer().getStatus() == FileDownloadStatus.completed) {
            // for block complete callback FileDownloadList#remove
            asyncPublishInNewThread(event);
        } else {
            receivePool.execute(new Runnable() {
                @Override
                public void run() {
                    publish(event);
                }
            });
        }
    }

    @Override
    public boolean publish(IDownloadEvent event) {
        if (event instanceof FileDownloadEvent) {
            ((FileDownloadEvent) event).getDownloader().getListener().callback(event);
            return true;
        }
        return super.publish(event);
    }

    void send2UIThread(final FileDownloadEvent event) {
        send2UIThread(event, false);
    }

    void send2UIThread(final FileDownloadEvent event, boolean immediately) {
        if (!isIntervalValid()) {
            if (send2UIPollThread != null && send2UIPollThread.isAlive()) {
                // handle  The INTERVAL is disabled when FileDownloader is active.
                synchronized (send2UIThreadLock) {
                    if (send2UIPollThread != null && send2UIPollThread.isAlive()) {
                        this.send2UIPollThread.interrupt();
                        this.send2UIThreadRunnable.requestKillSelf();
                        this.send2UIThreadRunnable = null;
                        this.send2UIPollThread = null;
                        asyncPublishInMain(event);
                        return;
                    }
                }

            }
        }

        if (!isIntervalValid() || immediately) {
            asyncPublishInMain(event);
            return;
        }

        if (send2UIThreadRunnable == null) {
            synchronized (send2UIThreadLock) {
                if (send2UIThreadRunnable == null) {
                    send2UIThreadRunnable = new WaitingRunnable(new WeakReference<>(this));
                }

                enqueue(event);
                return;
            }
        }

        enqueue(event);

    }

    public static boolean isIntervalValid() {
        return INTERVAL > 0;
    }


    private void enqueue(final FileDownloadEvent event) {
        send2UIThreadRunnable.offer(event);

        if (send2UIPollThread == null || !send2UIPollThread.isAlive()) {
            // already dead or completed
            synchronized (send2UIThreadLock) {
                if (send2UIPollThread == null || !send2UIPollThread.isAlive()) {
                    send2UIPollThread = new Thread(send2UIThreadRunnable);
                    send2UIPollThread.start();
                }
            }
        }
    }

    // ----------------- WAIT AND FREE INTERNAL TECH，SPLIT AND SUB-PACKAGE TECH ----------------
    private Thread send2UIPollThread;
    private WaitingRunnable send2UIThreadRunnable;
    private final Object send2UIThreadLock = new Object();

    public final static int DEFAULT_INTERVAL = 10;
    public final static int DEFAULT_SUB_PACKAGE_SIZE = 5;

    /**
     * For avoid dropped ui refresh frame.
     * 避免掉帧
     * <p/>
     * Every {@link FileDownloadEventPool#INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadEventPool#SUB_PACKAGE_SIZE} events on the ui thread at most.
     * <p/>
     * 每{@link FileDownloadEventPool#INTERVAL}毫秒抛最多1个Message到ui线程，并且每次抛到ui线程后，
     * 在ui线程最多处理处理{@link FileDownloadEventPool#SUB_PACKAGE_SIZE} 个回调。
     */
    static int INTERVAL = DEFAULT_INTERVAL;// 10ms, 0.01s, ui refresh 16ms per frame.
    static int SUB_PACKAGE_SIZE = DEFAULT_SUB_PACKAGE_SIZE; // the size of one package for callback on the ui thread.


    private static final class WaitingRunnable implements Runnable {

        private boolean isDead = false;
        private WeakReference<FileDownloadEventPool> wpool;
        private long lastTriggerMills;

        WaitingRunnable(final WeakReference<FileDownloadEventPool> wPool) {
            this.wpool = wPool;
        }

        private ArrayList<FileDownloadEvent> waitQueue =
                new ArrayList<>();

        private boolean waiting = false;
        private final Object queueLock = new Object();
        private volatile boolean isDigestedNotify = true;

        public void offer(FileDownloadEvent event) {
            if (isDead) {
                throw new IllegalArgumentException("already dead, can't digest events");
            }
            synchronized (queueLock) {
                waitQueue.add(event);
            }

            requestNotify();
        }

        public void requestKillSelf() {
            this.isDead = true;
            requestNotify();
        }

        private void requestNotify() {
            boolean notifyValid;
            do {
                // isn't digested the last notify.
                // no need.
                if (!isDigestedNotify) {
                    notifyValid = false;
                    break;
                }

                // already digested but not waiting,
                // no need.
                if (!waiting) {
                    notifyValid = false;
                    break;
                }

                // waiting............................................................

                // already dead, need clear all event,
                // will notify.
                if (isDead) {
                    notifyValid = true;
                    break;
                }

                //already digested and is waiting, and already in the end.
                // will notify.
                if (FileDownloadList.getImpl().size() <= 0) {
                    //particular case
                    notifyValid = true;
                    break;
                }

                // FileDownload is active(will request by others)......................

                // already digested and FileDownload is active and is waiting, but wait
                // interval isn't enough. no need.
                if (System.currentTimeMillis() - lastTriggerMills <= INTERVAL) {
                    notifyValid = false;
                    break;
                }

                // already digested and FileDownload is active and is waiting, and wait
                // interval is enough. will notify
                notifyValid = true;
            } while (false);

            if (notifyValid) {
                synchronized (this) {
                    isDigestedNotify = false;
                    notifyAll();
                }
            }
        }

        @Override
        public void run() {

            synchronized (this) {
                while (FileDownloadList.getImpl().size() > 0 || waitQueue.size() > 0) {
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.v(WaitingRunnable.class, "loop: for size %d %d",
                                FileDownloadList.getImpl().size(), waitQueue.size());
                    }

                    if (isDead && waitQueue.size() <= 0) {
                        break;
                    }

                    if (wpool == null || wpool.get() == null) {
                        FileDownloadLog.e(WaitingRunnable.class, "trigger to callback 2 ui, but " +
                                "event pool is nil %d", waitQueue.size());
                        break;
                    }
                    wpool.get().post2UI(new Runnable() {
                        @Override
                        public void run() {
                            loopMessage();
                        }
                    });

                    if (isDead && waitQueue.size() <= 0) {
                        break;
                    }
                    // take a break
                    try {
                        waiting = true;
                        wait();
                    } catch (InterruptedException e) {
                        if (waitQueue.size() <= 0) {
                            break;
                        }
                    }

                    waiting = false;
                }

            }
        }

        private boolean loopMessage() {
            ArrayList<FileDownloadEvent> toDealQueue = new ArrayList<>();
            boolean isNeedDealSubPackage = false;
            synchronized (queueLock) {
                isDigestedNotify = true;
                if (waitQueue.size() <= 0) {
                    FileDownloadLog.w(WaitingRunnable.class, "callback trigger to send 2 ui thread but" +
                            "wait queue is empty");
                    return false;
                }

                final ArrayList<FileDownloadEvent> copyQueue =
                        (ArrayList<FileDownloadEvent>) waitQueue.clone();
                waitQueue.clear();

                if (copyQueue.size() > SUB_PACKAGE_SIZE) {
                    isNeedDealSubPackage = true;
                    for (int i = 0; i < copyQueue.size(); i++) {
                        if (i > SUB_PACKAGE_SIZE) {
                            waitQueue.add(copyQueue.get(i));
                        } else {
                            toDealQueue.add(copyQueue.get(i));
                        }
                    }
                } else {
                    toDealQueue = copyQueue;
                }
            }

            for (int i = 0; i < toDealQueue.size(); i++) {
                if (wpool == null || wpool.get() == null) {
                    FileDownloadLog.e(WaitingRunnable.class, "trigger to send 2 ui thread and " +
                            "wait queue is available but event pool is nil");
                    return false;
                }

                wpool.get().publish(toDealQueue.get(i));
            }

            lastTriggerMills = System.currentTimeMillis();
            if (isNeedDealSubPackage) {
                requestNotify();
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------
    synchronized void shutdownSendPool() {
        wait2SendThreadCount = 0;
        sendPool.shutdownNow();
        sendPool = Executors.newFixedThreadPool(3);
    }

    private List<ShutDownItem> needShutdownList = new ArrayList<>();

    synchronized void shutdownSendPool(final FileDownloadListener lis) {
        if (wait2SendThreadCount > 0) {
            if (!needShutdownList.contains(lis)) {
                needShutdownList.add(new ShutDownItem(lis, wait2SendThreadCount));
            }
        }
    }

    private boolean isShutDownThread(final DownloadTaskEvent event) {
        boolean result = false;
        ShutDownItem item = null;
        for (ShutDownItem shutDownItem : needShutdownList) {
            if (shutDownItem.checkAndConsume(event)) {
                item = shutDownItem;
                result = true;
                break;
            }
        }

        if (result && item != null && item.isExpired()) {
            needShutdownList.remove(item);
        }

        return result;
    }

    private static class ShutDownItem {
        private FileDownloadListener listener;
        private int snapshotWaitCount;
        private int needCheckCount;

        public ShutDownItem(final FileDownloadListener listener, final int wait2SendThreadCount) {
            this.listener = listener;
            this.needCheckCount = this.snapshotWaitCount = wait2SendThreadCount;
        }

        public boolean isExpired() {
            return this.needCheckCount <= 0;
        }

        public boolean checkAndConsume(final DownloadTaskEvent event) {
            if (event == null || event.getTaskListener() == null) {
                return false;
            }

            if (listener == event.getTaskListener()) {
                needCheckCount--;
                return true;
            }

            return false;
        }
    }
}
