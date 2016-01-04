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

import android.app.NotificationManager;
import android.content.Context;
import android.util.SparseArray;

import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 9/28/15.
 */
public class FileDownloadNotificationHelper<T extends FileDownloadNotificationHelper.FileDownloadNotification> {

    private final SparseArray<T> notificationMap = new SparseArray<>();

    /**
     * @param id Download id
     */
    public T get(final int id) {
        return notificationMap.get(id);
    }

    public boolean contains(final int id) {
        return get(id) != null;
    }

    public T remove(final int id) {
        final T n = get(id);
        if (n != null) {
            notificationMap.remove(id);
            return n;
        }

        return null;
    }

    /**
     * Add a notification object
     * 新增 一个通知对象
     */
    public void add(T notification) {

        notificationMap.remove(notification.getId());
        notificationMap.put(notification.getId(), notification);
    }

    /**
     * progress
     * <p/>
     * 显示一个有进度变化的notification
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
     * pending/(paused)
     * <p/>
     * 显示一个没有进度变化的notification
     *
     * @param id     download id
     * @param status {@link FileDownloadStatus}
     */
    public void showNoProgress(final int id, int status) {
        final FileDownloadNotification notification = get(id);

        if (notification == null) {
            return;
        }

        notification.updateStatus(status);
        notification.show(false);
    }

    /**
     * warn/error/completed/(paused)
     * <p/>
     * Has finished downloading
     *
     * @param id download id
     */
    public void cancel(final int id) {
        final FileDownloadNotification notification = remove(id);

        if (notification == null) {
            return;
        }

        notification.cancel();
    }

    public static abstract class FileDownloadNotification {

        private int id, sofar, total;
        private String title, desc;

        private int status;

        private FileDownloadNotification(final int id, final String title, final String desc) {
            this.id = id;

            this.title = title;
            this.desc = desc;
        }

        /**
         * @param isShowProgress Whether there is a need to show the progress schedule changes
         */
        public abstract void show(final boolean isShowProgress);

        public void update(final int sofar, final int total) {
            this.sofar = sofar;
            this.total = total;
            show(true);
        }

        public void updateStatus(final int status) {
            this.status = status;
        }

        public void cancel() {
            final NotificationManager manager = (NotificationManager) FileDownloadHelper.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(id);
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getSofar() {
            return sofar;
        }

        public void setSofar(int sofar) {
            this.sofar = sofar;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }
}

