package com.liulishuo.filedownloader.services;

import android.app.NotificationManager;
import android.content.Context;

import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Jacksgong on 9/28/15.
 */
class FileDownloadNotificationMgr {

    private Map<Integer, FileDownloadNotification> notificationMap = new HashMap<>();

    /**
     * @param id Download id
     * @return
     */
    public FileDownloadNotification get(final int id) {
        return notificationMap.get(id);
    }

    public FileDownloadNotification update(final FileDownloadModel model) {
        if (model == null || model.getId() == 0) {
            return null;
        }

        if (notificationMap.get(model.getId()) != null) {
            return notificationMap.get(model.getId());
        }

        FileDownloadNotification notification = new FileDownloadNotification(model);

        notificationMap.put(model.getId(), notification);
        return notification;
    }

    /**
     * progress
     * @param id
     * @param sofar
     * @param total
     */
    public void showProgress(final int id, final int sofar, final int total) {
        final FileDownloadNotification notification = get(id);

        if (notification == null) {
            return;
        }

        notification.updateStatus(FileDownloadStatus.progress);
        notification.update(sofar, total);
    }

    /**
     * paused/pending
     * @param id
     * @param status
     */
    public void showNoProgress(final int id, int status) {
        final FileDownloadNotification notification = get(id);

        if (notification == null) {
            return;
        }

        notification.updateStatus(status);
        notification.show();
    }

    /**
     * warn/error/completed
     * @param id
     */
    public void cancel(final int id) {
        final FileDownloadNotification notification = get(id);

        if (notification == null) {
            return;
        }

        notification.cancel();
    }

    public static class FileDownloadNotification {

        private int id, sofar, total;
        private String title, desc;

        private int status;

        private FileDownloadNotification(final FileDownloadModel model) {
            this.id = model.getId();
            this.sofar = model.getSoFar();
            this.total = model.getTotal();

//            this.title = model.getTitle();
//            this.desc = model.getDesc();
        }

        public void show() {
            show(false);
        }

        public void show(final boolean isShowProgress) {
            final NotificationManager manager = (NotificationManager) FileDownloadHelper.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (!isShowProgress) {
                manager.cancel(id);
            }

//            String tmpTitle = title;

//            switch (this.status) {
//                case FileDownloadStatus.paused:
//                    tmpTitle = String.format("%s %s",
//                            FileDownloadHelper.getAppContext().getString(R.string.downloadfile_notification_title_pause),
//                            this.title);
//                    break;
//                case FileDownloadStatus.completed:
//                    tmpTitle = String.format("%s %s",
//                            FileDownloadHelper.getAppContext().getString(R.string.downloadfile_notification_title_complete),
//                            this.title);
//                    break;
//                case FileDownloadStatus.error:
//                    tmpTitle = String.format("%s %s",
//                            this.title, FileDownloadHelper.getAppContext().getString(R.string.downloadfile_notification_title_error));
//                    break;
//                case FileDownloadStatus.pending:
//                    tmpTitle = String.format("%s %s",
//                            this.title, "正在队列中");
//                    break;
//            }


//            Notification notification;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                Notification.Builder builder = new Notification.Builder(FileDownloadHelper.getAppContext());
//                builder.setDefaults(Notification.DEFAULT_LIGHTS)
//                        .setOngoing(true)
//                        .setContentTitle(tmpTitle)
//                        .setContentText(desc)
//                        .setSmallIcon(R.mipmap.downloadfile_default_icon);
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                    builder.setProgress(total, sofar, !isShowProgress);
//                } else {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                        // always true, for build compile
//                        builder.setContentText(String.format("%s %d/%d", desc, sofar, total));
//                    }
//                }
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                    notification = builder.build();
//                } else {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                        // always true, for build compile
//                        notification = builder.getNotification();
//                    }
//                }
//
//            } else {
                // new notification
//                notification = new Notification();
//                notification.icon = R.mipmap.downloadfile_default_icon;
//                notification.defaults = Notification.DEFAULT_LIGHTS;
//                notification.tickerText = String.format("%s %s %d/%d", title, desc, sofar, total);

//                notification.setLatestEventInfo(null, null, null, null);
//            }

//            Notification notification = new Notification.Builder(FileDownloadHelper.getAppContext()).setTicker(tmpTitle)
//                    .setContentTitle(tmpTitle)
//                    .setContentText(desc)
//                    .setOngoing(true)
//                    .setDefaults(Notification.DEFAULT_LIGHTS)
//                    .setPriority(Notification.PRIORITY_DEFAULT)
//                            // TODO 改用外界传入
//                    .setSmallIcon(R.mipmap.downloadfile_default_icon)
//                    .setProgress(total, sofar, !isShowProgress).build();
//
//
//            notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;
//            manager.notify(id, notification);
        }

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
    }
}

