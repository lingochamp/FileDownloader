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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jacksgong on 9/25/15.
 */
class FileDownloadThreadPool {

    private SparseArray<FileDownloadRunnable> runnablePool = new SparseArray<>();

    // TODO 对用户开放线程池大小，全局并行下载数
    private final ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public void execute(FileDownloadRunnable runnable) {
        runnable.onResume();
        threadPool.execute(runnable);
        runnablePool.put(runnable.getId(), runnable);

        final int CHECK_THRESHOLD_VALUE = 600;
        if (mIgnoreCheckTimes >= CHECK_THRESHOLD_VALUE) {
            checkNoExist();
            mIgnoreCheckTimes = 0;
        } else {
            mIgnoreCheckTimes++;
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
}
