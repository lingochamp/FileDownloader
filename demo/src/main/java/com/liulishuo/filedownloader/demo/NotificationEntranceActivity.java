package com.liulishuo.filedownloader.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class NotificationEntranceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_entrance);
    }

    public void onClickSample(final View view) {
        startActivity(new Intent(this, NotificationSampleActivity.class));
    }

    public void onClickMinSet(final View view) {
        startActivity(new Intent(this, NotificationMinSetActivity.class));
    }
}
