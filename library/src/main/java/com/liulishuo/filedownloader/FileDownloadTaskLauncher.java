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

import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.util.FileDownloadExecutors;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The global launcher for launching tasks.
 */
class FileDownloadTaskLauncher {

    private static class HolderClass {
        private final static FileDownloadTaskLauncher INSTANCE = new FileDownloadTaskLauncher();

        static {
            // We add the message receiver to the message snapshot flow central, when there is a
            // task request to launch.
            MessageSnapshotFlow.getImpl().setReceiver(new MessageSnapshotGate());
        }
    }

    public static FileDownloadTaskLauncher getImpl() {
        return HolderClass.INSTANCE;
    }

    private final LaunchTaskPool mLaunchTaskPool = new LaunchTaskPool();

    synchronized void launch(final ITaskHunter.IStarter taskStarter) {
        mLaunchTaskPool.asyncExecute(taskStarter);
    }

    synchronized void expireAll() {
        mLaunchTaskPool.expireAll();
    }

    synchronized void expire(final ITaskHunter.IStarter taskStarter) {
        mLaunchTaskPool.expire(taskStarter);
    }

    synchronized void expire(final FileDownloadListener lis) {
        mLaunchTaskPool.expire(lis);
    }


    private static class LaunchTaskPool {

        private ThreadPoolExecutor mPool;

        /**
         * the queue to use for holding tasks before they are
         * executed.  This queue will hold only the {@code Runnable}
         * tasks submitted by the {@code execute} method.
         */
        private LinkedBlockingQueue<Runnable> mWorkQueue;

        public LaunchTaskPool() {
            init();
        }

        public void asyncExecute(final ITaskHunter.IStarter taskStarter) {
            mPool.execute(new LaunchTaskRunnable(taskStarter));
        }

        public void expire(ITaskHunter.IStarter starter) {
            /**
             * @see LaunchTaskRunnable#equals(Object)
             */
            //noinspection SuspiciousMethodCalls
            mWorkQueue.remove(starter);
        }

        public void expire(final FileDownloadListener listener) {
            if (listener == null) {
                FileDownloadLog.w(this, "want to expire by listener, but the listener provided is" +
                        " null");
                return;
            }

            List<Runnable> needPauseList = new ArrayList<>();
            for (Runnable runnable : mWorkQueue) {
                final LaunchTaskRunnable launchTaskRunnable = (LaunchTaskRunnable) runnable;
                if (launchTaskRunnable.isSameListener(listener)) {
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
                mPool.remove(runnable);
            }
        }

        public void expireAll() {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "expire %d tasks",
                        mWorkQueue.size());
            }

            mPool.shutdownNow();
            init();
        }

        private void init() {
            mWorkQueue = new LinkedBlockingQueue<>();
            mPool = FileDownloadExecutors.newDefaultThreadPool(3, mWorkQueue, "LauncherTask");
        }

    }

    private static class LaunchTaskRunnable implements Runnable {
        private final ITaskHunter.IStarter mTaskStarter;
        private boolean mExpired;

        LaunchTaskRunnable(final ITaskHunter.IStarter taskStarter) {
            this.mTaskStarter = taskStarter;
            this.mExpired = false;
        }

        @Override
        public void run() {
            if (mExpired) {
                return;
            }

            mTaskStarter.start();
        }

        public boolean isSameListener(final FileDownloadListener listener) {
            return mTaskStarter != null && mTaskStarter.equalListener(listener);
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) || obj == mTaskStarter;
        }

        public void expire() {
            this.mExpired = true;
        }
    }
}
