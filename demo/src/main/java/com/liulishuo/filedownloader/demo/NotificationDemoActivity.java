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
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.notification.BaseNotificationItem;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationHelper;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationListener;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Jacksgong on 2/2/16.
 */
public class NotificationDemoActivity extends AppCompatActivity {
    private FileDownloadNotificationHelper<NotificationItem> notificationHelper;
    private NotificationListener listener;

    private final String savePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "notification";
    private final String url = Constant.BIG_FILE_URLS[4];

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
                    notificationHelper.clear();
                    clear();
                }
            }
        });

        listener = new NotificationListener(new WeakReference<>(this));
        downloadId = FileDownloader.getImpl().replaceListener(url, savePath, listener);
        if (downloadId != 0) {
            // Avoid the task has passed 'pending' status, so we must create notification manually.
            listener.addNotificationItem(downloadId);
        }
    }

    private int downloadId = 0;

    public void onClickStart(final View view) {

        if (downloadId == 0) {
            downloadId = FileDownloader.getImpl().create(url)
                    .setPath(savePath)
                    .setListener(listener)
                    .start();
        }
    }

    public void onClickPause(final View view) {
        if (downloadId != 0) {
            FileDownloader.getImpl().pause(downloadId);
            downloadId = 0;
        }
    }

    public void onClickDelete(final View view) {
        final File file = new File(savePath);
        if (file.exists()) {
            file.delete();
        }
        downloadId = 0;
    }

    private static class NotificationListener extends FileDownloadNotificationListener {

        private WeakReference<NotificationDemoActivity> wActivity;

        public NotificationListener(WeakReference<NotificationDemoActivity> wActivity) {
            super(wActivity.get().notificationHelper);
            this.wActivity = wActivity;
        }

        @Override
        protected BaseNotificationItem create(BaseDownloadTask task) {
            return new NotificationItem(task.getId(), "demo title", "demo desc");
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
                wActivity.get().downloadId = 0;
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

        @Override
        protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            super.pending(task, soFarBytes, totalBytes);
            if (wActivity.get() != null) {
                wActivity.get().progressBar.setIndeterminate(true);
            }
        }

        @Override
        protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            super.progress(task, soFarBytes, totalBytes);
            if (wActivity.get() != null) {
                wActivity.get().progressBar.setIndeterminate(false);
                wActivity.get().progressBar.setMax(totalBytes);
                wActivity.get().progressBar.setProgress(soFarBytes);
            }
        }

        @Override
        protected void completed(BaseDownloadTask task) {
            super.completed(task);
            if (wActivity.get() != null) {
                wActivity.get().progressBar.setIndeterminate(false);
                wActivity.get().progressBar.setProgress(task.getSmallFileTotalBytes());
            }
        }
    }

    public static class NotificationItem extends BaseNotificationItem {

        PendingIntent pendingIntent;
        private NotificationItem(int id, String title, String desc) {
            super(id, title, desc);
            Intent[] intents = new Intent[2];
            intents[0] = Intent.makeMainActivity(new ComponentName(DemoApplication.CONTEXT,
                    MainActivity.class));
            intents[1] = new Intent(DemoApplication.CONTEXT, NotificationDemoActivity.class);

            this.pendingIntent = PendingIntent.getActivities(DemoApplication.CONTEXT, 0, intents,
                    PendingIntent.FLAG_UPDATE_CURRENT);

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
                case FileDownloadStatus.started:
                    desc += " started";
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
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher);

            if (statusChanged) {
                builder.setTicker(desc);
            }

            builder.setProgress(getTotal(), getSofar(), !isShowProgress);
//            getManager().notify(getId(), builder.build());
            FileDownloader.getImpl().startForeground(getId(), builder.build());
        }

        @Override
        public void cancel() {
//            super.cancel();
            FileDownloader.getImpl().stopForeground(true);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (downloadId != 0) {
                FileDownloader.getImpl().pause(downloadId);
            }
            notificationHelper.clear();
            clear();
        }
        return super.dispatchKeyEvent(event);
    }

    private void clear() {
        /**
         * why not use {@link FileDownloadNotificationHelper#clear()} directly?
         */
//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
//                cancel(downloadId);
        FileDownloader.getImpl().stopForeground(true);
    }

    private CheckBox showNotificationCb;
    private ProgressBar progressBar;

    private void assignViews() {
        showNotificationCb = (CheckBox) findViewById(R.id.show_notification_cb);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

}
