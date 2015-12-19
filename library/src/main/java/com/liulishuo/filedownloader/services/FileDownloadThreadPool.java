package com.liulishuo.filedownloader.services;

import android.util.SparseArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jacksgong on 9/25/15.
 */
class FileDownloadThreadPool {

    private SparseArray<FileDownloadRunnable> poolRunnables = new SparseArray<>();

    private ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public void execute(FileDownloadRunnable runnable) {
        runnable.onResume();
        threadPool.execute(runnable);
        poolRunnables.put(runnable.getId(), runnable);

        if (mIgnoreCheckTimes >= CHECK_THRESHOLD_VALUE) {
            checkNoExist();
            mIgnoreCheckTimes = 0;
        } else {
            mIgnoreCheckTimes++;
        }
    }


    private final int CHECK_THRESHOLD_VALUE = 600;
    private int mIgnoreCheckTimes = 0;

    private synchronized void checkNoExist() {
        SparseArray<FileDownloadRunnable> newRunnables = new SparseArray<>();
        for (int i = 0; i < poolRunnables.size(); i++) {
            final int key = poolRunnables.keyAt(i);
            final FileDownloadRunnable runnable = poolRunnables.get(key);
            if (runnable.isExist()) {
                newRunnables.put(key, runnable);
            }
        }
        poolRunnables = newRunnables;

    }

    public boolean isInThreadPool(final int downloadId) {
        final FileDownloadRunnable runnable = poolRunnables.get(downloadId);
        return runnable != null && runnable.isExist();
    }
}
