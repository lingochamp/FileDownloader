/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader;

import android.app.Notification;
import android.os.Looper;

import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.io.File;

/**
 * The FileDownload synchronous line.
 *
 * @see FileDownloader#insureServiceBind()
 * @see #wait(ConnectSubscriber)
 */

public class FileDownloadLine {

    /**
     * The {@link FileDownloader#startForeground(int, Notification)} request.
     */
    public void startForeground(final int id, final Notification notification) {
        if (FileDownloader.getImpl().isServiceConnected()) {
            FileDownloader.getImpl().startForeground(id, notification);
            return;
        }

        final ConnectSubscriber subscriber = new ConnectSubscriber() {
            @Override
            public void connected() {
                FileDownloader.getImpl().startForeground(id, notification);
            }

            @Override
            public Object getValue() {
                return null;
            }
        };

        wait(subscriber);
    }

    /**
     * The {@link FileDownloader#getSoFar(int)} request.
     */
    public long getSoFar(final int id) {
        if (FileDownloader.getImpl().isServiceConnected()) {
            return FileDownloader.getImpl().getSoFar(id);
        }

        final ConnectSubscriber subscriber = new ConnectSubscriber() {
            private long mValue;

            @Override
            public void connected() {
                mValue = FileDownloader.getImpl().getSoFar(id);
            }

            @Override
            public Object getValue() {
                return mValue;
            }
        };

        wait(subscriber);

        return (long) subscriber.getValue();
    }

    /**
     * The {@link FileDownloader#getTotal(int)} request.
     */
    public long getTotal(final int id) {
        if (FileDownloader.getImpl().isServiceConnected()) {
            return FileDownloader.getImpl().getTotal(id);
        }

        final ConnectSubscriber subscriber = new ConnectSubscriber() {
            private long mValue;

            @Override
            public void connected() {
                mValue = FileDownloader.getImpl().getTotal(id);
            }

            @Override
            public Object getValue() {
                return mValue;
            }
        };

        wait(subscriber);

        return (long) subscriber.getValue();
    }

    /**
     * The {@link FileDownloader#getStatus(int, String)} request.
     */
    public byte getStatus(final int id, final String path) {
        if (FileDownloader.getImpl().isServiceConnected()) {
            return FileDownloader.getImpl().getStatus(id, path);
        }

        if (path != null && new File(path).exists()) {
            return FileDownloadStatus.completed;
        }

        final ConnectSubscriber subscriber = new ConnectSubscriber() {
            private byte mValue;

            @Override
            public void connected() {
                mValue = FileDownloader.getImpl().getStatus(id, path);
            }

            @Override
            public Object getValue() {
                return mValue;
            }
        };

        wait(subscriber);

        return (byte) subscriber.getValue();
    }

    private void wait(final ConnectSubscriber subscriber) {
        final ConnectListener connectListener = new ConnectListener(subscriber);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (connectListener) {
            FileDownloader.getImpl().bindService(connectListener);

            if (!connectListener.isFinished()) {

                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    throw new IllegalThreadStateException("Sorry, FileDownloader can not block the "
                            + "main thread, because the system is also  callbacks "
                            + "ServiceConnection#onServiceConnected method in the main thread.");
                }

                try {
                    connectListener.wait(200 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ConnectListener implements Runnable {
        private boolean mIsFinished = false;
        private final ConnectSubscriber mSubscriber;

        ConnectListener(ConnectSubscriber subscriber) {
            this.mSubscriber = subscriber;
        }

        public boolean isFinished() {
            return mIsFinished;
        }

        @Override
        public void run() {
            synchronized (this) {
                mSubscriber.connected();
                mIsFinished = true;
                notifyAll();
            }
        }
    }

    interface ConnectSubscriber {
        void connected();

        Object getValue();
    }
}
