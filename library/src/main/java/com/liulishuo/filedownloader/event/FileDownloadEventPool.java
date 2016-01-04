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

package com.liulishuo.filedownloader.event;

import com.liulishuo.filedownloader.DownloadTaskEvent;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;

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

    public synchronized void send2Service(final DownloadTaskEvent event) {
        wait2SendThreadCount++;
        sendPool.execute(new Runnable() {
            @Override
            public void run() {
                if (isShutDownThread(event)) {
                    FileDownloadLog.v(FileDownloadEventPool.class, "pass event because: shutdown already. %s", event.getTaskListener());
                    return;
                }
                wait2SendThreadCount--;
                publish(event);
            }
        });
    }

    public void receiveByService(final DownloadTransferEvent event) {
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

    public synchronized void shutdownSendPool() {
        wait2SendThreadCount = 0;
        sendPool.shutdownNow();
        sendPool = Executors.newFixedThreadPool(3);
    }

    private List<ShutDownItem> needShutdownList = new ArrayList<>();

    public synchronized void shutdownSendPool(final FileDownloadListener lis) {
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
