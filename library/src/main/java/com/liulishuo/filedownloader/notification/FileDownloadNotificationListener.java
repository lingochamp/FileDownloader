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

package com.liulishuo.filedownloader.notification;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadList;
import com.liulishuo.filedownloader.FileDownloadListener;

import junit.framework.Assert;

/**
 * Created by Jacksgong on 2/2/16.
 *
 * @see FileDownloadNotificationHelper
 * @see BaseNotificationItem
 */
public abstract class FileDownloadNotificationListener extends FileDownloadListener {
    private final FileDownloadNotificationHelper helper;

    public FileDownloadNotificationListener(FileDownloadNotificationHelper helper) {
        Assert.assertNotNull("FileDownloadNotificationHelper must not null", helper);
        this.helper = helper;
    }

    public FileDownloadNotificationHelper getHelper() {
        return helper;
    }


    public void addNotificationItem(int downloadId) {
        if (downloadId == 0) {
            return;
        }

        BaseDownloadTask task = FileDownloadList.getImpl().get(downloadId);
        if (task != null) {
            addNotificationItem(task);
        }
    }

    public void addNotificationItem(BaseDownloadTask task) {
        if (disableNotification(task)) {
            return;
        }

        final BaseNotificationItem n = create(task);
        if (n != null) {
            this.helper.add(n);
        }
    }

    /**
     * remove notification item from NotificationHelper
     *
     * @param task the current task
     */
    public void destroyNotification(BaseDownloadTask task) {
        if (disableNotification(task)) {
            return;
        }

        this.helper.showIndeterminate(task.getDownloadId(), task.getStatus());

        final BaseNotificationItem n = this.helper.
                remove(task.getDownloadId());
        if (!interceptCancel(task, n) && n != null) {
            n.cancel();
        }
    }

    public void showIndeterminate(BaseDownloadTask task) {
        if (disableNotification(task)) {
            return;
        }

        this.helper.showIndeterminate(task.getDownloadId(), task.getStatus());
    }

    public void showProgress(BaseDownloadTask task, int soFarBytes,
                             int totalBytes) {
        if (disableNotification(task)) {
            return;
        }

        this.helper.showProgress(task.getDownloadId(), task.getSmallFileSoFarBytes(),
                task.getSmallFileTotalBytes());
    }

    /**
     * @param task the current task
     * @return create a Notification Item on the basis of the current task
     */
    protected abstract BaseNotificationItem create(BaseDownloadTask task);

    /**
     * @param task the current task
     * @param n    the current notification item
     * @return whether intercept canceling the Notification Item which no more be updated
     * Notification Item, if true, the notification will not be canceled, but will not exist
     * in NotificationHelper
     * @see #destroyNotification(BaseDownloadTask)
     */
    protected boolean interceptCancel(BaseDownloadTask task,
                                      BaseNotificationItem n) {
        return false;
    }

    /**
     * @param task the current task
     * @return whether disable handle notification internal mechanism or not, if true, the method of
     * the notification lifecycle will do nothing internal
     */
    protected boolean disableNotification(final BaseDownloadTask task) {
        return false;
    }

    // ---------------------------------------

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        addNotificationItem(task);
        showIndeterminate(task);
    }

    @Override
    protected void started(BaseDownloadTask task) {
        super.started(task);
        showIndeterminate(task);
    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        showProgress(task, soFarBytes, totalBytes);
    }

    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
        super.retry(task, ex, retryingTimes, soFarBytes);
        showIndeterminate(task);
    }

    @Override
    protected void blockComplete(BaseDownloadTask task) {
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        destroyNotification(task);
    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        destroyNotification(task);
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
        destroyNotification(task);
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        // ignore
        // do not handle the case of the same URL and path task(the same download id), which
        // will share the same notification item
//        if (!disableNotification(task)) {
//            destroyNotification(task);
//        }
    }
}
