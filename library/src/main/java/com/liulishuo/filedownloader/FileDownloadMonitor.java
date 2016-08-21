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
 * The FileDownloader global monitor, monitor the begin、over for all tasks.
 *
 * @see BaseDownloadTask.LifeCycleCallback#onBegin()
 * @see BaseDownloadTask.LifeCycleCallback#onOver() ()
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
     * The interface used to monitor all tasks's status change in the FileDownloader.
     * <p/>
     * All method in this interface will be invoked synchronous, recommend don't to hold the thread
     * of invoking the method.
     *
     * @see FileDownloadMonitor#setGlobalMonitor(IMonitor)
     */
    public interface IMonitor {
        /**
         * Request to start multi-tasks manually.
         *
         * @param count  The count of tasks will start.
         * @param serial Tasks will be started in serial or parallel.
         * @param lis    The listener.
         */
        void onRequestStart(int count, boolean serial, FileDownloadListener lis);

        /**
         * Request to start a task.
         *
         * @param task The task will start.
         */
        void onRequestStart(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is beginning.
         *
         * @param task The task is received to start internally.
         */
        void onTaskBegin(BaseDownloadTask task);

        /**
         * The method will be invoked when the download runnable of the task has started running.
         *
         * @param task The task finish pending and start download runnable.
         */
        void onTaskStarted(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is over.
         *
         * @param task The task is over.
         */
        void onTaskOver(BaseDownloadTask task);
    }

}
