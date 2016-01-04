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

package com.liulishuo.filedownloader.demo.performance;

import android.os.Bundle;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.liulishuo.filedownloader.demo.R;

/**
 * Created by Jacksgong on 1/4/16.
 */
public class PerformanceTestActivity extends AppCompatActivity {

    private final static int TIMES = 100000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);
        assignViews();

        setTitle(getString(R.string.performance_test_with_times_title, TIMES));

    }

    public void onClickLongOperate(final View view) {
        final long start = System.currentTimeMillis();

        final LongParcel longParcel = new LongParcel();
        for (int i = 0; i < TIMES; i++) {
            longParcel.operate();
        }

        InfoAppend("Long Operate", start);
    }

    public void onClickLongParcel(final View view) {
        final long start = System.currentTimeMillis();

        final LongParcel longParcel = new LongParcel();
        for (int i = 0; i < TIMES; i++) {
            Parcel p = Parcel.obtain();
            longParcel.writeToParcel(p, 0);
            LongParcel longParcelCopy = new LongParcel(p);
        }

        InfoAppend("Long Parcel and Alloc [and GC]", start);
    }

    public void onClickIntOperate(final View view) {
        final long start = System.currentTimeMillis();

        final IntParcel intParcel = new IntParcel();
        for (int i = 0; i < TIMES; i++) {
            intParcel.operate();
        }

        InfoAppend("Int Operate", start);
    }

    public void onClickIntParcel(final View view) {
        final long start = System.currentTimeMillis();

        final IntParcel intParcel = new IntParcel();
        for (int i = 0; i < TIMES; i++) {
            Parcel p = Parcel.obtain();
            intParcel.writeToParcel(p, 0);
            IntParcel intParcelCopy = new IntParcel(p);
        }

        InfoAppend("Int Parcel and Alloc [and GC]", start);
    }

    private void InfoAppend(final String msg, final long start) {
        infoTv.append(String.format(" %s: %d\n", msg, System.currentTimeMillis() - start));
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (scrollView != null) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }

    private ScrollView scrollView;
    private TextView infoTv;

    private void assignViews() {
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        infoTv = (TextView) findViewById(R.id.info_tv);
    }

}
