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

import com.liulishuo.filedownloader.util.FileDownloadProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Jacksgong on 9/25/15.
 * <p/>
 * The thread pool for driving the download runnable.
 */
class FileDownloadThreadPool {

    private SparseArray<FileDownloadRunnable> runnablePool = new SparseArray<>();

    private final ThreadPoolExecutor threadPool =
            (ThreadPoolExecutor) Executors.
                    newFixedThreadPool(FileDownloadProperties.getImpl().
                            DOWNLOAD_MAX_NETWORK_THREAD_COUNT);

    public void execute(FileDownloadRunnable runnable) {
        runnable.onPending();
        threadPool.execute(runnable);
        synchronized (this) {
            runnablePool.put(runnable.getId(), runnable);
        }

        final int CHECK_THRESHOLD_VALUE = 600;
        if (mIgnoreCheckTimes >= CHECK_THRESHOLD_VALUE) {
            checkNoExist();
            mIgnoreCheckTimes = 0;
        } else {
            mIgnoreCheckTimes++;
        }
    }

    public void cancel(final int id) {
        checkNoExist();
        synchronized (this) {
            FileDownloadRunnable r = runnablePool.get(id);
            if (r != null) {
                r.cancelRunnable();
                threadPool.remove(r);
            }
            runnablePool.remove(id);
        }
    }


    private int mIgnoreCheckTimes = 0;

    private synchronized void checkNoExist() {
        SparseArray<FileDownloadRunnable> correctedRunnablePool = new SparseArray<>();
        for (int i = 0; i < runnablePool.size(); i++) {
            final int key = runnablePool.keyAt(i);
            final FileDownloadRunnable runnable = runnablePool.get(key);
            if (runnable.isExist()) {
                correctedRunnablePool.put(key, runnable);
            }
        }
        runnablePool = correctedRunnablePool;

    }

    public boolean isInThreadPool(final int downloadId) {
        final FileDownloadRunnable runnable = runnablePool.get(downloadId);
        return runnable != null && runnable.isExist();
    }

    public synchronized int exactSize(){
        checkNoExist();
        return runnablePool.size();
    }

    public synchronized List<Integer> getAllExactRunningDownloadIds() {
        checkNoExist();

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < runnablePool.size(); i++) {
            list.add(runnablePool.get(runnablePool.keyAt(i)).getId());
        }

        return list;
    }
}
