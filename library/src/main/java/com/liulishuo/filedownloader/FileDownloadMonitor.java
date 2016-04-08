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

/**
 * Created by Jacksgong on 1/19/16.
 * <p/>
 * The FileDownloader global monitor, monitor the begin、over for all tasks.
 *
 * @see BaseDownloadTask#begin()
 * @see BaseDownloadTask#over()
 * @see BaseDownloadTask#start()
 * @see FileDownloader#start(FileDownloadListener, boolean)
 */
public class FileDownloadMonitor {
    private static IMonitor MONITOR;

    public static void setGlobalMonitor(final IMonitor monitor) {
        MONITOR = monitor;
    }

    public static void releaseGlobalMonitor() {
        MONITOR = null;
    }

    public static IMonitor getMonitor() {
        return MONITOR;
    }

    public static boolean isValid() {
        return getMonitor() != null;
    }


    /**
     * Interface used to monitor all tasks's status change in the FileDownloader.
     * <p/>
     * all sync, Do not hold on.
     *
     * @see FileDownloadMonitor#setGlobalMonitor(IMonitor)
     */
    public interface IMonitor {
        /**
         * Request to start multi-tasks manually.
         * <p/>
         * Sync invoke, do not hold on.
         *
         * @param count  the count of tasks will start.
         * @param serial is in serial or parallel.
         * @param lis    target for binding queue.
         */
        void onRequestStart(int count, boolean serial, FileDownloadListener lis);

        /**
         * Request to start the task.
         * <p/>
         * Sync invoke, do not hold on.
         *
         * @param task The task will start.
         */
        void onRequestStart(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is begin.
         * <p/>
         * Sync invoke, do not hold on.
         *
         * @param task The task is just received to handle in the internal.
         */
        void onTaskBegin(BaseDownloadTask task);

        /**
         * The method will be invoked when the download runnable of the task has started running.
         * <p/>
         * Sync invoke, do not hold on.
         *
         * @param task The task finish pending and start download runnable.
         */
        void onTaskStarted(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is over.
         * <p/>
         * Sync invoke, do not hold on.
         *
         * @param task The task is over.
         */
        void onTaskOver(BaseDownloadTask task);
    }

}
