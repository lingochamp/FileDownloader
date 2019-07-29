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

package com.liulishuo.filedownloader.demo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.notification.BaseNotificationItem;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationHelper;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationListener;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jacksgong on 2/2/16.
 */
public class NotificationSampleActivity extends AppCompatActivity {
    private final FileDownloadNotificationHelper<NotificationItem> notificationHelper =
            new FileDownloadNotificationHelper<>();
    private NotificationListener listener;

    private final String savePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "notification";
    private final String url = Constant.LIULISHUO_APK_URL;
    private final String channelId = "filedownloader_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_sample);

        assignViews();

        listener = new NotificationListener(new WeakReference<>(this), channelId);
        Utils.createNotificationChannel(channelId, "Filedownloader", getApplicationContext());
    }


    public void onClickStart(final View view) {
        startDownload.setEnabled(false);
        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(listener);
        final List<BaseDownloadTask> tasks = new ArrayList<>();
        final int taskCount = 5;
        for (int i = 0; i < taskCount; i++) {
            tasks.add(FileDownloader.getImpl()
                    .create(Constant.URLS[i])
                    .setTag(i + 1)
                    .setPath(savePath, true)
            );
        }
        queueSet.downloadTogether(tasks)
                .addTaskFinishListener(new BaseDownloadTask.FinishListener() {
                    final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public void over(BaseDownloadTask task) {
                        final int finishCount = counter.addAndGet(1);
                        if (finishCount == taskCount) startDownload.post(new Runnable() {
                            @Override
                            public void run() {
                                startDownload.setEnabled(true);
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress(1);
                                progressBar.setMax(1);
                            }
                        });
                    }
                })
                .start();
        progressBar.setIndeterminate(true);
    }

    public void onClickPause(final View view) {
        FileDownloader.getImpl().pause(listener);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onClickDelete(final View view) {
        FileDownloader.getImpl().pause(listener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File file = new File(savePath);
                if (file.isFile()) file.delete();
                if (file.exists()) {
                    final File[] files = file.listFiles();
                    if (files == null) return;
                    for (File f : files) {
                        f.delete();
                    }
                    file.delete();
                }
            }
        }).start();
    }

    private static class NotificationListener extends FileDownloadNotificationListener {

        private final String channelId;
        private final WeakReference<NotificationSampleActivity> wActivity;

        public NotificationListener(
                WeakReference<NotificationSampleActivity> wActivity,
                String channelId
        ) {
            super(wActivity.get().notificationHelper);
            this.wActivity = wActivity;
            this.channelId = channelId;
        }

        @Override
        protected BaseNotificationItem create(BaseDownloadTask task) {
            return new NotificationItem(
                    task.getId(),
                    "Task-" + task.getId(),
                    "",
                    channelId
            );
        }

        @Override
        public void addNotificationItem(BaseDownloadTask task) {
            super.addNotificationItem(task);
            if (wActivity.get() != null) {
                wActivity.get().showNotificationCb.setEnabled(false);
            }
        }

        @Override
        public void destroyNotification(BaseDownloadTask task) {
            super.destroyNotification(task);
            if (wActivity.get() != null) {
                wActivity.get().showNotificationCb.setEnabled(true);
            }
        }

        @Override
        protected boolean interceptCancel(BaseDownloadTask task,
                                          BaseNotificationItem n) {
            // in this demo, I don't want to cancel the notification, just show for the test
            // so return true
            return true;
        }

        @Override
        protected boolean disableNotification(BaseDownloadTask task) {
            if (wActivity.get() != null) {
                return !wActivity.get().showNotificationCb.isChecked();
            }

            return super.disableNotification(task);
        }
    }

    public static class NotificationItem extends BaseNotificationItem {

        private final NotificationCompat.Builder builder;

        private NotificationItem(int id, String title, String desc, String channelId) {
            super(id, title, desc);
            final Intent[] intents = new Intent[2];
            intents[0] = Intent.makeMainActivity(
                    new ComponentName(DemoApplication.CONTEXT, MainActivity.class));
            intents[1] = new Intent(DemoApplication.CONTEXT, NotificationSampleActivity.class);
            final PendingIntent pendingIntent = PendingIntent.getActivities(
                    DemoApplication.CONTEXT, 0, intents,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new NotificationCompat.Builder(
                        FileDownloadHelper.getAppContext(),
                        channelId);
            } else {
                //noinspection deprecation
                builder = new NotificationCompat.Builder(FileDownloadHelper.getAppContext())
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setPriority(NotificationCompat.PRIORITY_MIN);
            }

            builder.setContentTitle(getTitle())
                    .setContentText(desc)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher);
        }

        @Override
        public void show(boolean statusChanged, int status, boolean isShowProgress) {
            String desc = "";
            switch (status) {
                case FileDownloadStatus.pending:
                    desc += " pending";
                    builder.setProgress(getTotal(), getSofar(), true);
                    break;
                case FileDownloadStatus.started:
                    desc += " started";
                    builder.setProgress(getTotal(), getSofar(), true);
                    break;
                case FileDownloadStatus.progress:
                    desc += " progress";
                    builder.setProgress(getTotal(), getSofar(), getTotal() <= 0);
                    break;
                case FileDownloadStatus.retry:
                    desc += " retry";
                    builder.setProgress(getTotal(), getSofar(), true);
                    break;
                case FileDownloadStatus.error:
                    desc += " error";
                    builder.setProgress(getTotal(), getSofar(), false);
                    break;
                case FileDownloadStatus.paused:
                    desc += " paused";
                    builder.setProgress(getTotal(), getSofar(), false);
                    break;
                case FileDownloadStatus.completed:
                    desc += " completed";
                    builder.setProgress(getTotal(), getSofar(), false);
                    break;
                case FileDownloadStatus.warn:
                    desc += " warn";
                    builder.setProgress(0, 0, true);
                    break;
            }

            builder.setContentTitle(getTitle()).setContentText(desc);
            getManager().notify(getId(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationHelper.clear();
        Utils.deleteNotificationChannel(channelId, getApplicationContext());
    }

    private CheckBox showNotificationCb;
    private ProgressBar progressBar;
    private View startDownload;

    private void assignViews() {
        showNotificationCb = findViewById(R.id.show_notification_cb);
        progressBar = findViewById(R.id.progressBar);
        startDownload = findViewById(R.id.view_start);
    }
}