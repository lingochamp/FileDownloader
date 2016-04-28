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
 * Created by Jacksgong on 1/22/16.
 * <p/>
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
    private Integer callbackProgressTimes;
    private Object tag;

    private BaseDownloadTask[] tasks;

    /**
     * @param target for all tasks callback status change
     */
    public FileDownloadQueueSet(FileDownloadListener target) {
        // TODO, support target is null
        if (target == null) {
            throw new IllegalArgumentException("create FileDownloadQueueSet must with valid target!");
        }
        this.target = target;
    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} in parallel
     */
    public FileDownloadQueueSet downloadTogether(BaseDownloadTask... tasks) {
        this.isSerial = false;
        this.tasks = tasks;

        return this;

    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} in parallel
     */
    public FileDownloadQueueSet downloadTogether(List<BaseDownloadTask> tasks) {
        this.isSerial = false;
        this.tasks = new BaseDownloadTask[tasks.size()];
        tasks.toArray(this.tasks);

        return this;

    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} linearly
     */
    public FileDownloadQueueSet downloadSequentially(BaseDownloadTask... tasks) {
        this.isSerial = true;
        this.tasks = tasks;

        return this;
    }

    /**
     * Form a queue with same {@link #target} and will {@link #start()} linearly
     */
    public FileDownloadQueueSet downloadSequentially(List<BaseDownloadTask> tasks) {
        this.isSerial = true;
        this.tasks = new BaseDownloadTask[tasks.size()];
        tasks.toArray(this.tasks);

        return this;
    }

    /**
     * Execute tasks
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

            if (tag != null) {
                task.setTag(tag);
            }

            if (taskFinishListenerList != null) {
                for (BaseDownloadTask.FinishListener finishListener : taskFinishListenerList) {
                    task.addFinishListener(finishListener);
                }
            }

            task.ready();
        }

        FileDownloader.getImpl().start(target, isSerial);
    }

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

}
