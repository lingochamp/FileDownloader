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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.liulishuo.filedownloader.util.FileDownloadExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The message station to transfer task events to {@link FileDownloadListener}.
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloadMessageStation {

    private static final Executor BLOCK_COMPLETED_POOL = FileDownloadExecutors.
            newDefaultThreadPool(5, "BlockCompleted");

    private final Handler handler;
    private final LinkedBlockingQueue<IFileDownloadMessenger> waitingQueue;

    private static final class HolderClass {
        private static final FileDownloadMessageStation INSTANCE = new FileDownloadMessageStation();
    }

    public static FileDownloadMessageStation getImpl() {
        return HolderClass.INSTANCE;
    }

    private FileDownloadMessageStation() {
        handler = new Handler(Looper.getMainLooper(), new UIHandlerCallback());
        waitingQueue = new LinkedBlockingQueue<>();
    }

    void requestEnqueue(final IFileDownloadMessenger messenger) {
        requestEnqueue(messenger, false);
    }

    void requestEnqueue(final IFileDownloadMessenger messenger,
                        @SuppressWarnings("SameParameterValue") boolean immediately) {

        if (messenger.handoverDirectly()) {
            messenger.handoverMessage();
            return;
        }

        // check if messenger.mTask is null
        // if null, return; according to filedownloader/FileDownloadMessenger.java -> process()
        // line 200 to 210
        if (messenger instanceof FileDownloadMessenger
                && ((FileDownloadMessenger) messenger).hasTask()) {
            if (interceptBlockCompleteMessage(messenger)) {
                return;
            }
        } else {
            return;
        }


        if (!isIntervalValid()) {
            // invalid
            // clear all waiting queue.
            if (!waitingQueue.isEmpty()) {
                synchronized (queueLock) {
                    if (!waitingQueue.isEmpty()) {
                        for (IFileDownloadMessenger iFileDownloadMessenger : waitingQueue) {
                            handoverInUIThread(iFileDownloadMessenger);
                        }
                    }
                    waitingQueue.clear();
                }
            }
        }

        if (!isIntervalValid() || immediately) {
            // post to UI thread immediately.
            handoverInUIThread(messenger);
            return;
        }

        // enqueue.
        enqueue(messenger);
    }

    private void handoverInUIThread(IFileDownloadMessenger messenger) {
        handler.sendMessage(handler.obtainMessage(HANDOVER_A_MESSENGER, messenger));
    }

    /**
     * @param messenger {@link com.liulishuo.filedownloader.IFileDownloadMessenger}
     * @return true if the messenger is a block complete messenger and handle it through
     * {@linkplain FileDownloadMessageStation#BLOCK_COMPLETED_POOL} in the meantime.
     */
    private static boolean interceptBlockCompleteMessage(final IFileDownloadMessenger messenger) {
        if (messenger.isBlockingCompleted()) {
            BLOCK_COMPLETED_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    messenger.handoverMessage();
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private final Object queueLock = new Object();

    private void enqueue(final IFileDownloadMessenger messenger) {
        synchronized (queueLock) {
            waitingQueue.offer(messenger);
        }

        push();
    }

    private void push() {

        final int delayMillis;
        synchronized (queueLock) {
            if (!disposingList.isEmpty()) {
                // is disposing.
                return;
            }

            if (waitingQueue.isEmpty()) {
                // not messenger need be handled.
                return;
            }

            if (!isIntervalValid()) {
                waitingQueue.drainTo(disposingList);
                delayMillis = 0;
            } else {
                delayMillis = INTERVAL;
                final int size = Math.min(waitingQueue.size(), SUB_PACKAGE_SIZE);
                for (int i = 0; i < size; i++) {
                    disposingList.add(waitingQueue.remove());
                }
            }


        }

        handler.sendMessageDelayed(handler.obtainMessage(DISPOSE_MESSENGER_LIST, disposingList),
                delayMillis);
    }


    static final int HANDOVER_A_MESSENGER = 1;
    static final int DISPOSE_MESSENGER_LIST = 2;
    private final ArrayList<IFileDownloadMessenger> disposingList = new ArrayList<>();

    private static class UIHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == HANDOVER_A_MESSENGER) {
                ((IFileDownloadMessenger) msg.obj).handoverMessage();
            } else if (msg.what == DISPOSE_MESSENGER_LIST) {
                //noinspection unchecked
                dispose((ArrayList<IFileDownloadMessenger>) msg.obj);
                FileDownloadMessageStation.getImpl().push();
            }
            return true;
        }

        private void dispose(final ArrayList<IFileDownloadMessenger> disposingList) {
            // dispose Sub-package-size each time.
            for (IFileDownloadMessenger iFileDownloadMessenger : disposingList) {
                if (iFileDownloadMessenger instanceof FileDownloadMessenger &&
                        ((FileDownloadMessenger) iFileDownloadMessenger).hasTask()) {
                    if (interceptBlockCompleteMessage(iFileDownloadMessenger)) {
                        continue;
                    }
                    iFileDownloadMessenger.handoverMessage();
                }
            }

            disposingList.clear();
        }
    }


    // ----------------- WAIT AND FREE INTERNAL TECH，SPLIT AND SUB-PACKAGE TECH ----------------

    public static final int DEFAULT_INTERVAL = 10;
    public static final int DEFAULT_SUB_PACKAGE_SIZE = 5;

    /**
     * For avoid dropped ui refresh frame.
     * <p/>
     * Every {@code INTERVAL} milliseconds post 1 message to the ui thread at most,
     * and will handle up to {@link FileDownloadMessageStation#SUB_PACKAGE_SIZE} events on the ui
     * thread at most.
     */
    static int INTERVAL = DEFAULT_INTERVAL;
    // the size of one package for callback on the ui thread.
    static int SUB_PACKAGE_SIZE = DEFAULT_SUB_PACKAGE_SIZE;

    public static boolean isIntervalValid() {
        return INTERVAL > 0;
    }
}
