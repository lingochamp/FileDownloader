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
 * The handler for handing the case of the connect with the downloader service is lost when tasks is
 * running.
 */

public interface ILostServiceConnectedHandler {

    /**
     * @return Whether in the waiting list, what is waiting for the downloader service reconnected,
     * and when it reconnected, all tasks in the waiting list will be started.
     */
    boolean isInWaitingList(BaseDownloadTask.IRunningTask task);

    /**
     * @param task is works well, so it will be removed from the waiting list.
     */
    void taskWorkFine(BaseDownloadTask.IRunningTask task);

    /**
     * @return {@code true} if the start action was dispatched.
     */
    boolean dispatchTaskStart(BaseDownloadTask.IRunningTask task);
}
