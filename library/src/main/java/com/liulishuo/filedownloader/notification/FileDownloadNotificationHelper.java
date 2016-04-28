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

import android.util.SparseArray;

import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 9/28/15.
 *
 * @see BaseNotificationItem
 * @see FileDownloadNotificationListener
 */
public class FileDownloadNotificationHelper<T extends BaseNotificationItem> {

    private final SparseArray<T> notificationArray = new SparseArray<>();

    /**
     * get {@link BaseNotificationItem} by the download id
     *
     * @param id download id
     */
    public T get(final int id) {
        return notificationArray.get(id);
    }

    public boolean contains(final int id) {
        return get(id) != null;
    }

    /**
     * remove the {@link BaseNotificationItem} by the download id
     *
     * @param id download id
     * @return removed {@link BaseNotificationItem}
     */
    public T remove(final int id) {
        final T n = get(id);
        if (n != null) {
            notificationArray.remove(id);
            return n;
        }

        return null;
    }

    /**
     * input a {@link BaseNotificationItem}
     */
    public void add(T notification) {
        notificationArray.remove(notification.getId());
        notificationArray.put(notification.getId(), notification);
    }

    /**
     * show the notification with the exact progress
     *
     * @param id    download id
     * @param sofar Number of bytes download so far
     * @param total Total bytes
     */
    public void showProgress(final int id, final int sofar, final int total) {
        final T notification = get(id);

        if (notification == null) {
            return;
        }

        notification.updateStatus(FileDownloadStatus.progress);
        notification.update(sofar, total);
    }

    /**
     * show the notification with indeterminate progress
     * <p/>
     * recommend invoke by pending/retry
     *
     * @param id     download id
     * @param status {@link FileDownloadStatus}
     */
    public void showIndeterminate(final int id, int status) {
        final BaseNotificationItem notification = get(id);

        if (notification == null) {
            return;
        }

        notification.updateStatus(status);
        notification.show(false);
    }

    /**
     * cancel the notification by notification id
     * <p/>
     * recommend invoke by warn/error/completed/(paused)
     *
     * @param id download id
     */
    public void cancel(final int id) {
        final BaseNotificationItem notification = remove(id);

        if (notification == null) {
            return;
        }

        notification.cancel();
    }

    /**
     * clear and cancel all notifications which inside this helper {@link #notificationArray}
     */
    public void clear() {
        @SuppressWarnings("unchecked") SparseArray<BaseNotificationItem> cloneArray =
                (SparseArray<BaseNotificationItem>) notificationArray.clone();
        notificationArray.clear();

        for (int i = 0; i < cloneArray.size(); i++) {
            final BaseNotificationItem n = cloneArray.get(cloneArray.keyAt(i));
            n.cancel();
        }

    }
}
