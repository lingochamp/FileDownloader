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
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.notification.BaseNotificationItem;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationHelper;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * Created by Jacksgong on 2/2/16.
 */
public class NotificationDemoActivity extends AppCompatActivity {
    private FileDownloadNotificationHelper<NotificationItem> notificationHelper;

    String savePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_demo);
        notificationHelper = new FileDownloadNotificationHelper<>();

        assignViews();

        showNotificationCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    clear();
                }
            }
        });

    }

    private int downloadId = 0;

    public void onClickStart(final View view) {
        downloadId = FileDownloader.getImpl().create(Constant.BIG_FILE_URLS[2])
                .setPath(savePath)
                .setListener(new FileDownloadNotificationListener(notificationHelper) {
                    @Override
                    protected BaseNotificationItem create(
                            BaseDownloadTask task) {
                        return new NotificationItem(task.getDownloadId(), "demo title", "demo desc");
                    }

                    @Override
                    public void createNotification(BaseDownloadTask task) {
                        super.createNotification(task);
                        showNotificationCb.setEnabled(false);
                    }

                    @Override
                    public void destroyNotification(BaseDownloadTask task) {
                        super.destroyNotification(task);
                        showNotificationCb.setEnabled(true);
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
                        return !showNotificationCb.isChecked();
                    }

                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.pending(task, soFarBytes, totalBytes);
                        progressBar.setIndeterminate(true);
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.progress(task, soFarBytes, totalBytes);
                        progressBar.setIndeterminate(false);
                        progressBar.setMax(totalBytes);
                        progressBar.setProgress(soFarBytes);
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        super.completed(task);
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(task.getSmallFileTotalBytes());
                    }
                })
                .start();
    }

    public void onClickPause(final View view) {
        FileDownloader.getImpl().pause(downloadId);
    }

    public void onClickDelete(final View view) {
        final File file = new File(savePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public static class NotificationItem extends BaseNotificationItem {

        private NotificationItem(int id, String title, String desc) {
            super(id, title, desc);
        }

        @Override
        public void show(boolean statusChanged, int status, boolean isShowProgress) {
            NotificationCompat.Builder builder = new NotificationCompat.
                    Builder(FileDownloadHelper.getAppContext());

            String desc = getDesc();
            switch (status) {
                case FileDownloadStatus.pending:
                    desc += " pending";
                    break;
                case FileDownloadStatus.progress:
                    desc += " progress";
                    break;
                case FileDownloadStatus.retry:
                    desc += " retry";
                    break;
                case FileDownloadStatus.error:
                    desc += " error";
                    break;
                case FileDownloadStatus.paused:
                    desc += " paused";
                    break;
                case FileDownloadStatus.completed:
                    desc += " completed";
                    break;
                case FileDownloadStatus.warn:
                    desc += " warn";
                    break;
            }

            builder.setDefaults(Notification.DEFAULT_LIGHTS)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentTitle(getTitle())
                    .setContentText(desc)
                    .setSmallIcon(R.mipmap.ic_launcher);

            if (statusChanged) {
                builder.setTicker(desc);
            }

            builder.setProgress(getTotal(), getSofar(), !isShowProgress);
            getManager().notify(getId(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        this.notificationHelper.clear();
        clear();
        super.onDestroy();
    }

    private void clear() {
        if (downloadId == 0) {
            return;
        }
        /**
         * why not use {@link FileDownloadNotificationHelper#clear()} directly?
         * @see FileDownloadNotificationListener#interceptCancel(BaseDownloadTask, BaseNotificationItem)
         */
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                cancel(downloadId);
    }

    private CheckBox showNotificationCb;
    private ProgressBar progressBar;

    private void assignViews() {
        showNotificationCb = (CheckBox) findViewById(R.id.show_notification_cb);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

}
