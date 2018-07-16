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

package com.liulishuo.filedownloader.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;

/**
 * The service is running for FileDownloader.
 * <p/>
 * You can add a command `process.non-separate=true` to the `filedownloader.properties` asset file
 * to make the FileDownloadService runs in the main process, and by default the FileDownloadService
 * runs in the separate process(`:filedownloader`).
 */
@SuppressLint("Registered")
public class FileDownloadService extends Service {

    private IFileDownloadServiceHandler handler;
    private static final String NOTIFICATION_CHANNEL_ID = "filedownloader_channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Filedownloader";
    private static final String NOTIFICATION_TITLE = "Filedownloader";
    private static final String NOTIFICATION_CONTENT = "FileDownloader is running.";
    private static final int NOTIFICATION_ID = 6666;

    @Override
    public void onCreate() {
        super.onCreate();
        FileDownloadHelper.holdContext(this);

        try {
            FileDownloadUtils.setMinProgressStep(
                    FileDownloadProperties.getImpl().downloadMinProgressStep);
            FileDownloadUtils.setMinProgressTime(
                    FileDownloadProperties.getImpl().downloadMinProgressTime);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        final FileDownloadManager manager = new FileDownloadManager();

        if (FileDownloadProperties.getImpl().processNonSeparate) {
            handler = new FDServiceSharedHandler(new WeakReference<>(this), manager);
        } else {
            handler = new FDServiceSeparateHandler(new WeakReference<>(this), manager);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.onStartCommand(intent, flags, startId);
        if (FileDownloadUtils.mustPushServiceToForeground(this)) pushServiceToForegroundIfNeeded();
        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void pushServiceToForegroundIfNeeded() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "push download service to foreground");
        }
        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;
        notificationManager.createNotificationChannel(notificationChannel);

        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(NOTIFICATION_TITLE)
                .setSmallIcon(android.R.drawable.arrow_down_float)
                .setContentText(NOTIFICATION_CONTENT)
                .setLocalOnly(true);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        handler.onDestroy();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return handler.onBind(intent);
    }

    public static class SharedMainProcessService extends FileDownloadService {
    }

    public static class SeparateProcessService extends FileDownloadService {
    }
}
