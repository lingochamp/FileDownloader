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

import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jacksgong on 4/26/16.
 * <p/>
 * The global launcher for launching a task.
 */
class FileDownloadTaskLauncher {

    private static class HolderClass {
        private final static FileDownloadTaskLauncher INSTANCE = new FileDownloadTaskLauncher();
    }

    public static FileDownloadTaskLauncher getImpl() {
        return HolderClass.INSTANCE;
    }

    private final LaunchTaskPool launchTaskPool = new LaunchTaskPool();

    synchronized void launch(final BaseDownloadTask task) {
        launchTaskPool.asyncExecute(task);
    }

    synchronized void expireAll() {
        launchTaskPool.expireAll();
    }

    synchronized void expire(final FileDownloadListener lis) {
        launchTaskPool.expire(lis);
    }


    private static class LaunchTaskPool {

        private ThreadPoolExecutor pool;

        /**
         * the queue to use for holding tasks before they are
         * executed.  This queue will hold only the {@code Runnable}
         * tasks submitted by the {@code execute} method.
         */
        private BlockingQueue<Runnable> workQueue;

        public LaunchTaskPool() {
            init();
        }

        public void asyncExecute(final BaseDownloadTask task) {
            pool.execute(new LaunchTaskRunnable(task));
        }

        public void expire(final FileDownloadListener listener) {
            if (listener == null) {
                FileDownloadLog.w(this, "want to expire by listener, but the listener provided is" +
                        " null");
                return;
            }

            List<Runnable> needPauseList = new ArrayList<>();
            for (Runnable runnable : workQueue) {
                final LaunchTaskRunnable launchTaskRunnable = (LaunchTaskRunnable) runnable;
                if (launchTaskRunnable.equal(listener)) {
                    launchTaskRunnable.expire();
                    needPauseList.add(runnable);
                }
            }

            if (needPauseList.isEmpty()) {
                return;
            }

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "expire %d tasks with listener[%s]", needPauseList.size(),
                        listener);
            }

            for (Runnable runnable : needPauseList) {
                pool.remove(runnable);
            }
        }

        public void expireAll() {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "expire %d tasks", workQueue.size());
            }
            pool.shutdownNow();
            init();
        }

        private void init() {
            workQueue = new LinkedBlockingQueue<>();
            pool = new ThreadPoolExecutor(3, 3,
                    10L, TimeUnit.MILLISECONDS,
                    workQueue);
        }

    }

    private static class LaunchTaskRunnable implements Runnable {
        private final BaseDownloadTask task;
        private boolean expired;

        LaunchTaskRunnable(final BaseDownloadTask task) {
            this.task = task;
            this.expired = false;
        }

        @Override
        public void run() {
            if (expired) {
                return;
            }
            task._start();
        }

        public boolean equal(final FileDownloadListener listener) {
            return task != null && task.getListener() == listener;
        }

        public void expire() {
            this.expired = true;
        }
    }

}
