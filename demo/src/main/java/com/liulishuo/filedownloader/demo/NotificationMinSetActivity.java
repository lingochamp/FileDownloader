package com.liulishuo.filedownloader.demo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.notification.BaseNotificationItem;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationHelper;
import com.liulishuo.filedownloader.notification.FileDownloadNotificationListener;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

public class NotificationMinSetActivity extends AppCompatActivity {


    private final String savePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator +
            "minSetNotification";
    private final String url = Constant.LIULISHUO_APK_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_minset);
    }

    private int downloadId = 0;

    public void onClickStart(final View view) {

        if (downloadId == 0) {
            downloadId = FileDownloader.getImpl().create(url)
                    .setPath(savePath)
                    .setListener(new NotificationListener(new FileDownloadNotificationHelper<NotificationItem>()))
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (downloadId != 0) {
            FileDownloader.getImpl().pause(downloadId);
        }
    }

    private class NotificationListener extends FileDownloadNotificationListener {

        private static final String TAG = "NotificationListener";

        public NotificationListener(FileDownloadNotificationHelper helper) {
            super(helper);
        }

        @Override
        protected BaseNotificationItem create(BaseDownloadTask task) {
            return new NotificationMinSetActivity.NotificationItem(task.getId(),
                    "min set demo title", " min set demo desc");
        }

        @Override
        public void destroyNotification(BaseDownloadTask task) {
            super.destroyNotification(task);
            Toast.makeText(NotificationMinSetActivity.this, "destroyNotification() called with: status "
                    + task.getStatus(), Toast.LENGTH_LONG).show();
            downloadId = 0;
        }
    }

    public static class NotificationItem extends BaseNotificationItem {

        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;

        private NotificationItem(int id, String title, String desc) {
            super(id, title, desc);
            Intent[] intents = new Intent[2];
            intents[0] = Intent.makeMainActivity(new ComponentName(DemoApplication.CONTEXT,
                    MainActivity.class));
            intents[1] = new Intent(DemoApplication.CONTEXT, NotificationSampleActivity.class);

            this.pendingIntent = PendingIntent.getActivities(DemoApplication.CONTEXT, 0, intents,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder = new NotificationCompat.
                    Builder(FileDownloadHelper.getAppContext());

            builder.setDefaults(Notification.DEFAULT_LIGHTS)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentTitle(getTitle())
                    .setContentText(desc)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher);

        }

        @Override
        public void show(boolean statusChanged, int status, boolean isShowProgress) {

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

            builder.setContentTitle(getTitle())
                    .setContentText(desc);


            if (statusChanged) {
                builder.setTicker(desc);
            }

            builder.setProgress(getTotal(), getSofar(), !isShowProgress);
            getManager().notify(getId(), builder.build());
        }

    }


}
