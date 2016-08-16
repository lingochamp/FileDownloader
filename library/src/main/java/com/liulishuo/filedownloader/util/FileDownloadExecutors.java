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

package com.liulishuo.filedownloader.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jacksgong on 16/08/2016.
 * <p>
 * Executors are used in entire FileDownloader internal for managing different threads.
 */
public class FileDownloadExecutors {
    private final static String FILEDOWNLOADER_PREFIX = "FileDownloader";

    public static Executor newFixedThreadPool(int nThreads, String prefix) {
        return new FileDownloadExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                prefix);
    }

    public static class FileDownloadExecutor extends ThreadPoolExecutor {

        public FileDownloadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                    TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                    final String prefixName) {
            //noinspection NullableProblems
            super(corePoolSize, /** core **/
                    maximumPoolSize, keepAliveTime, unit, /** max, idle-time **/
                    workQueue,

                    new ThreadFactory() {

                        private final AtomicInteger noNameThreadNumber = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, FILEDOWNLOADER_PREFIX + "-" +
                                    prefixName + "-" + noNameThreadNumber.getAndIncrement());
                        }
                    }
            );

        }

        private final static String STATUS_SPLIT = "::";

        @Override
        protected void beforeExecute(Thread thread, Runnable r) {
            super.beforeExecute(thread, r);
            String[] nameArray = thread.getName().split(STATUS_SPLIT);
            thread.setName(nameArray[0] + STATUS_SPLIT + "running");
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            final Thread thread = Thread.currentThread();
            String[] nameArray = thread.getName().split(STATUS_SPLIT);
            thread.setName(nameArray[0] + STATUS_SPLIT + "idle");
        }
    }


}
