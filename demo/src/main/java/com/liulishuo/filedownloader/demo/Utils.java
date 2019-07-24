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

package com.liulishuo.filedownloader.demo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

public class Utils {
    public static void createNotificationChannel(
            String channelId,
            String channelName,
            Context context
    ) {
        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void deleteNotificationChannel(String channelId, Context context) {
        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId);
        }
    }

    @Nullable
    public static NotificationManager getNotificationManager(Context context) {
        return ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }
}
