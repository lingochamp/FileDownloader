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

package com.liulishuo.filedownloader.services;

import android.util.SparseArray;

import com.liulishuo.filedownloader.download.DownloadLaunchRunnable;
import com.liulishuo.filedownloader.util.FileDownloadExecutors;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The thread pool for driving the downloading runnable, which real access the network.
 */
class FileDownloadThreadPool {

    private SparseArray<DownloadLaunchRunnable> runnablePool = new SparseArray<>();

    private ThreadPoolExecutor mThreadPool;

    private final String THREAD_PREFIX = "Network";
    private int mMaxThreadCount;

    FileDownloadThreadPool(final int maxNetworkThreadCount) {
        mThreadPool = FileDownloadExecutors.newDefaultThreadPool(maxNetworkThreadCount, THREAD_PREFIX);
        mMaxThreadCount = maxNetworkThreadCount;
    }

    public synchronized boolean setMaxNetworkThreadCount(int count) {
        if (exactSize() > 0) {
            FileDownloadLog.w(this, "Can't change the max network thread count, because the " +
                    " network thread pool isn't in IDLE, please try again after all running" +
                    " tasks are completed or invoking FileDownloader#pauseAll directly.");
            return false;
        }

        final int validCount = FileDownloadProperties.getValidNetworkThreadCount(count);

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "change the max network thread count, from %d to %d",
                    mMaxThreadCount, validCount);
        }

        final List<Runnable> taskQueue = mThreadPool.shutdownNow();
        mThreadPool = FileDownloadExecutors.newDefaultThreadPool(validCount, THREAD_PREFIX);

        if (taskQueue.size() > 0) {
            FileDownloadLog.w(this, "recreate the network thread pool and discard %d tasks",
                    taskQueue.size());
        }

        mMaxThreadCount = validCount;
        return true;
    }

    public void execute(DownloadLaunchRunnable launchRunnable) {
        launchRunnable.pending();
        synchronized (this) {
            runnablePool.put(launchRunnable.getId(), launchRunnable);
        }
        mThreadPool.execute(launchRunnable);

        final int CHECK_THRESHOLD_VALUE = 600;
        if (mIgnoreCheckTimes >= CHECK_THRESHOLD_VALUE) {
            filterOutNoExist();
            mIgnoreCheckTimes = 0;
        } else {
            mIgnoreCheckTimes++;
        }
    }

    public void cancel(final int id) {
        filterOutNoExist();
        synchronized (this) {
            DownloadLaunchRunnable r = runnablePool.get(id);
            if (r != null) {
                r.pause();
                boolean result = mThreadPool.remove(r);
                if (FileDownloadLog.NEED_LOG) {
                    // If {@code result} is false, must be: the Runnable has been running before
                    // invoke this method.
                    FileDownloadLog.d(this, "successful cancel %d %B", id, result);
                }
            }
            runnablePool.remove(id);
        }
    }


    private int mIgnoreCheckTimes = 0;

    private synchronized void filterOutNoExist() {
        SparseArray<DownloadLaunchRunnable> correctedRunnablePool = new SparseArray<>();
        final int size = runnablePool.size();
        for (int i = 0; i < size; i++) {
            final int key = runnablePool.keyAt(i);
            final DownloadLaunchRunnable runnable = runnablePool.get(key);
            if (runnable.isAlive()) {
                correctedRunnablePool.put(key, runnable);
            }
        }
        runnablePool = correctedRunnablePool;
    }

    public boolean isInThreadPool(final int downloadId) {
        final DownloadLaunchRunnable runnable = runnablePool.get(downloadId);
        return runnable != null && runnable.isAlive();
    }

    public int findRunningTaskIdBySameTempPath(String tempFilePath, int excludeId) {
        if (null == tempFilePath) {
            return 0;
        }

        final int size = runnablePool.size();
        for (int i = 0; i < size; i++) {
            final DownloadLaunchRunnable runnable = runnablePool.valueAt(i);
            // why not clone, no out-of-bounds exception? -- yes, we dig into SparseArray and find out
            // there are only two ways can change mValues: GrowingArrayUtils#insert and GrowingArrayUtils#append
            // they all only grow size, and valueAt only get value on mValues.
            if (runnable == null) {
                // why it is possible to occur null on here, because the value on  runnablePool can
                // be remove on #cancel method.
                continue;
            }

            if (runnable.isAlive() && runnable.getId() != excludeId &&
                    tempFilePath.equals(runnable.getTempFilePath())) {
                return runnable.getId();
            }
        }

        return 0;
    }

    public synchronized int exactSize() {
        filterOutNoExist();
        return runnablePool.size();
    }

    public synchronized List<Integer> getAllExactRunningDownloadIds() {
        filterOutNoExist();

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < runnablePool.size(); i++) {
            list.add(runnablePool.get(runnablePool.keyAt(i)).getId());
        }

        return list;
    }
}
