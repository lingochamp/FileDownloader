/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;

import com.liulishuo.filedownloader.R;
import com.liulishuo.filedownloader.util.FileDownloadLog;

@TargetApi(26)
public class ForegroundServiceConfig {
    private int notificationId;
    private String notificationChannelId;
    private String notificationChannelName;
    private Notification notification;
    private boolean needRecreateChannelId;

    private ForegroundServiceConfig() {
    }

    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "filedownloader_channel";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_NAME = "Filedownloader";
    private static final int DEFAULT_NOTIFICATION_ID = android.R.drawable.arrow_down_float;

    public int getNotificationId() {
        return notificationId;
    }

    public String getNotificationChannelId() {
        return notificationChannelId;
    }

    public String getNotificationChannelName() {
        return notificationChannelName;
    }

    public Notification getNotification(Context context) {
        if (notification == null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "build default notification");
            }
            notification = buildDefaultNotification(context);
        }
        return notification;
    }

    public boolean isNeedRecreateChannelId() {
        return needRecreateChannelId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public void setNotificationChannelId(String notificationChannelId) {
        this.notificationChannelId = notificationChannelId;
    }

    public void setNotificationChannelName(String notificationChannelName) {
        this.notificationChannelName = notificationChannelName;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public void setNeedRecreateChannelId(boolean needRecreateChannelId) {
        this.needRecreateChannelId = needRecreateChannelId;
    }

    private Notification buildDefaultNotification(Context context) {
        String title = context.getString(R.string.default_filedownloader_notification_title);
        String content =
                context.getString(R.string.default_filedownloader_notification_content);
        Notification.Builder builder = new Notification.Builder(context, notificationChannelId);
        builder.setContentTitle(title).setContentText(content)
                .setSmallIcon(DEFAULT_NOTIFICATION_ID);
        return builder.build();
    }

    @Override
    public String toString() {
        return "ForegroundServiceConfig{"
                + "notificationId=" + notificationId
                + ", notificationChannelId='" + notificationChannelId
                + '\''
                + ", notificationChannelName='" + notificationChannelName
                + '\''
                + ", notification=" + notification
                + ", needRecreateChannelId=" + needRecreateChannelId
                + '}';
    }

    public static class Builder {
        private int notificationId;
        private String notificationChannelId;
        private String notificationChannelName;
        private Notification notification;
        private boolean needRecreateChannelId;

        public Builder notificationId(int notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder notificationChannelId(String notificationChannelId) {
            this.notificationChannelId = notificationChannelId;
            return this;
        }

        public Builder notificationChannelName(String notificationChannelName) {
            this.notificationChannelName = notificationChannelName;
            return this;
        }

        public Builder notification(Notification notification) {
            this.notification = notification;
            return this;
        }

        public Builder needRecreateChannelId(boolean needRecreateChannelId) {
            this.needRecreateChannelId = needRecreateChannelId;
            return this;
        }

        public ForegroundServiceConfig build() {
            final ForegroundServiceConfig config = new ForegroundServiceConfig();
            config.setNotificationChannelId(notificationChannelId == null
                    ? DEFAULT_NOTIFICATION_CHANNEL_ID : notificationChannelId);
            config.setNotificationChannelName(notificationChannelName == null
                    ? DEFAULT_NOTIFICATION_CHANNEL_NAME : notificationChannelName);
            config.setNotificationId(notificationId == 0
                    ? DEFAULT_NOTIFICATION_ID : notificationId);
            config.setNeedRecreateChannelId(needRecreateChannelId);
            config.setNotification(notification);
            return config;
        }
    }
}
