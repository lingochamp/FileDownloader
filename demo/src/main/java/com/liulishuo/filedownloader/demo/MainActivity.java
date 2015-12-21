package com.liulishuo.filedownloader.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    public void onClickMix(final View view) {
        startActivity(new Intent(this, MixTestActivity.class));
    }

    public void onClickSingle(final View view) {
        startActivity(new Intent(this, SingleTaskActivity.class));
    }

}
