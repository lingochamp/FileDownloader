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

import java.util.ArrayList;
import java.util.List;

/**
 * The helper for start and config the task queue simply and quickly.
 *
 * @see FileDownloader#start(FileDownloadListener, boolean)
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class FileDownloadQueueSet {

    private FileDownloadListener target;
    private boolean isSerial;


    private List<BaseDownloadTask.FinishListener> taskFinishListenerList;
    private Integer autoRetryTimes;
    private Boolean syncCallback;
    private Boolean isForceReDownload;
    private Boolean isWifiRequired;
    private Integer callbackProgressTimes;
    private Integer callbackProgressMinIntervalMillis;
    private Object tag;
    private String directory;

    private BaseDownloadTask[] tasks;

    /**
     * @param target The download listener will be set to all tasks in this queue set.
     */
    public FileDownloadQueueSet(FileDownloadListener target) {
        if (target == null) {
            throw new IllegalArgumentException(
                    "create FileDownloadQueueSet must with valid target!");
        }
        this.target = target;
    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} in parallel.
     */
    public FileDownloadQueueSet downloadTogether(BaseDownloadTask... tasks) {
        this.isSerial = false;
        this.tasks = tasks;

        return this;

    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} in parallel.
     */
    public FileDownloadQueueSet downloadTogether(List<BaseDownloadTask> tasks) {
        this.isSerial = false;
        this.tasks = new BaseDownloadTask[tasks.size()];
        tasks.toArray(this.tasks);

        return this;

    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} linearly.
     */
    public FileDownloadQueueSet downloadSequentially(BaseDownloadTask... tasks) {
        this.isSerial = true;
        this.tasks = tasks;

        return this;
    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} linearly.
     */
    public FileDownloadQueueSet downloadSequentially(List<BaseDownloadTask> tasks) {
        this.isSerial = true;
        this.tasks = new BaseDownloadTask[tasks.size()];
        tasks.toArray(this.tasks);

        return this;
    }

    /**
     * Before starting downloading tasks in this queue-set, we will try to
     * {@link BaseDownloadTask#reuse} tasks first.
     */
    public void reuseAndStart() {
        for (BaseDownloadTask task : tasks) {
            task.reuse();
        }
        start();
    }

    /**
     * Start tasks in a queue.
     *
     * @see #downloadSequentially(BaseDownloadTask...)
     * @see #downloadSequentially(List)
     * @see #downloadTogether(BaseDownloadTask...)
     * @see #downloadTogether(List)
     */
    public void start() {
        for (BaseDownloadTask task : tasks) {
            task.setListener(target);

            if (autoRetryTimes != null) {
                task.setAutoRetryTimes(autoRetryTimes);
            }

            if (syncCallback != null) {
                task.setSyncCallback(syncCallback);
            }

            if (isForceReDownload != null) {
                task.setForceReDownload(isForceReDownload);
            }

            if (callbackProgressTimes != null) {
                task.setCallbackProgressTimes(callbackProgressTimes);
            }

            if (callbackProgressMinIntervalMillis != null) {
                task.setCallbackProgressMinInterval(callbackProgressMinIntervalMillis);
            }

            if (tag != null) {
                task.setTag(tag);
            }

            if (taskFinishListenerList != null) {
                for (BaseDownloadTask.FinishListener finishListener : taskFinishListenerList) {
                    task.addFinishListener(finishListener);
                }
            }

            if (this.directory != null) {
                task.setPath(this.directory, true);
            }

            if (this.isWifiRequired != null) {
                task.setWifiRequired(this.isWifiRequired);
            }

            task.asInQueueTask().enqueue();
        }

        FileDownloader.getImpl().start(target, isSerial);
    }

    /**
     * @param directory Set the {@code directory} to store files in this queue.
     *                  All tasks in this queue will be invoked
     *                  {@link BaseDownloadTask#setPath(String, boolean)} with params:
     *                  ({@code directory}, {@code true}).
     */
    public FileDownloadQueueSet setDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    /**
     * @see BaseDownloadTask#setAutoRetryTimes(int)
     */
    public FileDownloadQueueSet setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        return this;
    }

    /**
     * @see BaseDownloadTask#setSyncCallback(boolean)
     */
    public FileDownloadQueueSet setSyncCallback(final boolean syncCallback) {
        this.syncCallback = syncCallback;
        return this;
    }

    /**
     * @see BaseDownloadTask#setForceReDownload(boolean)
     */
    public FileDownloadQueueSet setForceReDownload(final boolean isForceReDownload) {
        this.isForceReDownload = isForceReDownload;
        return this;
    }

    /**
     * @see BaseDownloadTask#setCallbackProgressTimes(int)
     */
    public FileDownloadQueueSet setCallbackProgressTimes(final int callbackProgressTimes) {
        this.callbackProgressTimes = callbackProgressTimes;
        return this;
    }

    /**
     * @see BaseDownloadTask#setCallbackProgressMinInterval(int)
     */
    public FileDownloadQueueSet setCallbackProgressMinInterval(int minIntervalMillis) {
        this.callbackProgressMinIntervalMillis = minIntervalMillis;
        return this;
    }

    /**
     * @see BaseDownloadTask#setCallbackProgressIgnored()
     */
    public FileDownloadQueueSet ignoreEachTaskInternalProgress() {
        setCallbackProgressTimes(-1);
        return this;
    }

    /**
     * @see BaseDownloadTask#setCallbackProgressTimes(int)
     */
    public FileDownloadQueueSet disableCallbackProgressTimes() {
        return setCallbackProgressTimes(0);
    }

    /**
     * @see BaseDownloadTask#setTag(Object)
     */
    public FileDownloadQueueSet setTag(final Object tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @see BaseDownloadTask#addFinishListener(BaseDownloadTask.FinishListener)
     */
    public FileDownloadQueueSet addTaskFinishListener(
            final BaseDownloadTask.FinishListener finishListener) {
        if (this.taskFinishListenerList == null) {
            this.taskFinishListenerList = new ArrayList<>();
        }

        this.taskFinishListenerList.add(finishListener);
        return this;
    }

    /**
     * @see BaseDownloadTask#setWifiRequired(boolean)
     */
    public FileDownloadQueueSet setWifiRequired(boolean isWifiRequired) {
        this.isWifiRequired = isWifiRequired;
        return this;
    }

}
