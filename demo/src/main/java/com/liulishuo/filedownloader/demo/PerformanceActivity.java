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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * Created by Jacksgong on 12/25/15.
 */
public class PerformanceActivity extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);
        assignViews();

        actionBtn.setTag(true);
        actionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean toStart = (boolean) actionBtn.getTag();
                if (toStart) {
                    if (start()) {
                        actionBtn.setText("Pause");
                        actionBtn.setTag(false);
                    }
                } else {
                    actionBtn.setText("Start");
                    pause();
                    actionBtn.setTag(true);
                }

            }
        });

        taskCountSb.setMax(Constant.URLS.length);
        taskCountSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                taskCountTv.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        deleteAllFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = 0;
                File file = new File(FileDownloadUtils.getDefaultSaveRootPath());
                do {
                    if (!file.exists()) {
                        break;
                    }

                    if (!file.isDirectory()) {
                        break;
                    }

                    File[] files = file.listFiles();

                    if (files == null) {
                        break;
                    }

                    for (File file1 : files) {
                        count++;
                        file1.delete();
                    }

                } while (false);

                Toast.makeText(PerformanceActivity.this, String.format("Complete delete %d files", count), Toast.LENGTH_LONG).show();

            }
        });

    }

    private long start = 0;

    private boolean start() {
        final int count = Integer.valueOf(taskCountTv.getText().toString());
        if (count <= 0) {
            return false;
        }

        pendingPb.setMax(count);
        connectedPb.setMax(count);
        progressPb.setMax(count);
        errorPb.setMax(count);
        pausedPb.setMax(count);
        completedWidthOldPb.setMax(count);
        completedPb.setMax(count);
        warnPb.setMax(count);

        pendingPb.setProgress(0);
        connectedPb.setProgress(0);
        progressPb.setProgress(0);
        errorPb.setProgress(0);
        pausedPb.setProgress(0);
        completedWidthOldPb.setProgress(0);
        completedPb.setProgress(0);
        warnPb.setProgress(0);

        pendingTv.setText("pending: " + 0);
        connectedTv.setText("connected: " + 0);
        progressTv.setText("progress: " + 0);
        errorTv.setText("error: " + 0);
        pausedTv.setText("paused: " + 0);
        completedWidthOldTv.setText("completed reuse old file: " + 0);
        completedTv.setText("completed width download: " + 0);
        warnTv.setText("warn: " + 0);

        overTaskPb.setMax(count);
        overTaskPb.setProgress(0);

        downloadListener = createLis();
        for (int i = 0; i < count; i++) {
            final String url = Constant.URLS[i];
            FileDownloader.getImpl().create(url)
                    .setListener(downloadListener)
                    .setCallbackProgressTimes(1)
                    .ready();
        }

        isStopTimer = false;
        timeConsumeTv.setTag(0);
        goTimeCount();
        start = System.currentTimeMillis();

        FileDownloader.getImpl().start(downloadListener, serialRbtn.isChecked());

        return true;
    }

    private FileDownloadListener downloadListener;

    private void pause() {
        FileDownloader.getImpl().pause(downloadListener);
        stopTimer();
    }

    private void stopTimer() {
        isStopTimer = true;
        final long consume = System.currentTimeMillis() - start;
        if (timeConsumeTv != null) {
            timeConsumeTv.setText(String.valueOf(consume / 1000f));
        }
    }

    private FileDownloadListener createLis() {
        return new FileDownloadListener() {

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (task.getListener() != downloadListener) {
                    return;
                }
                pendingPb.setProgress(pendingPb.getProgress() + 1);
                pendingTv.setText("pending: " + pendingPb.getProgress());
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                if (task.getListener() != downloadListener) {
                    return;
                }

                connectedPb.setProgress(connectedPb.getProgress() + 1);
                connectedTv.setText("connected: " + connectedPb.getProgress());
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (task.getListener() != downloadListener) {
                    return;
                }
//                progressPb.setProgress(progressPb.getProgress() + 1);
//                progressTv.setText("progress: " + progressPb.getProgress());
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }
            }


            @Override
            protected void completed(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }
                if (task.isReusedOldFile()) {
                    completedWidthOldPb.setProgress(completedWidthOldPb.getProgress() + 1);
                    completedWidthOldTv.setText("completed reuse old file: " + completedWidthOldPb.getProgress());
                } else {
                    completedPb.setProgress(completedPb.getProgress() + 1);
                    completedTv.setText("completed width download: " + completedPb.getProgress());
                }

                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
                checkEndAll();
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (task.getListener() != downloadListener) {
                    return;
                }
                pausedPb.setProgress(pausedPb.getProgress() + 1);
                pausedTv.setText("paused: " + pausedPb.getProgress());
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                if (task.getListener() != downloadListener) {
                    return;
                }
                errorPb.setProgress(errorPb.getProgress() + 1);
                errorTv.setText("error: " + errorPb.getProgress());
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
                checkEndAll();
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }

                warnPb.setProgress(warnPb.getProgress() + 1);
                warnTv.setText("warn: " + warnPb.getProgress());
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
                checkEndAll();
            }
        };
    }

    private void checkEndAll() {
        final boolean isEndAll = overTaskPb.getProgress() >= Integer.valueOf(taskCountTv.getText().toString());
        if (isEndAll) {
            stopTimer();
            actionBtn.setTag(true);
            actionBtn.setText("Start");
        }
    }

    private boolean isStopTimer = true;

    private void goTimeCount() {
        final int time = (int) timeConsumeTv.getTag();
        timeConsumeTv.setText(String.valueOf(time));
        timeConsumeTv.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isStopTimer) {
                    return;
                }
                timeConsumeTv.setTag(time + 1);
                goTimeCount();
            }
        }, 1000);
    }

    private SeekBar taskCountSb;
    private TextView taskCountTv;
    private TextView timeConsumeTv;
    private RadioGroup wayRgp;
    private RadioButton serialRbtn;
    private RadioButton parallelRbtn;
    private ProgressBar overTaskPb;
    private Button actionBtn;
    private TextView pendingTv;
    private ProgressBar pendingPb;
    private TextView connectedTv;
    private ProgressBar connectedPb;
    private TextView progressTv;
    private ProgressBar progressPb;
    private TextView errorTv;
    private ProgressBar errorPb;
    private TextView pausedTv;
    private ProgressBar pausedPb;
    private TextView completedWidthOldTv;
    private ProgressBar completedWidthOldPb;
    private TextView completedTv;
    private ProgressBar completedPb;
    private TextView warnTv;
    private ProgressBar warnPb;
    private Button deleteAllFileBtn;

    private void assignViews() {
        taskCountSb = (SeekBar) findViewById(R.id.task_count_sb);
        taskCountTv = (TextView) findViewById(R.id.task_count_tv);
        timeConsumeTv = (TextView) findViewById(R.id.time_consume_tv);
        wayRgp = (RadioGroup) findViewById(R.id.way_rgp);
        serialRbtn = (RadioButton) findViewById(R.id.serial_rbtn);
        parallelRbtn = (RadioButton) findViewById(R.id.parallel_rbtn);
        overTaskPb = (ProgressBar) findViewById(R.id.over_task_pb);
        actionBtn = (Button) findViewById(R.id.action_btn);
        pendingTv = (TextView) findViewById(R.id.pending_tv);
        pendingPb = (ProgressBar) findViewById(R.id.pending_pb);
        connectedTv = (TextView) findViewById(R.id.connected_tv);
        connectedPb = (ProgressBar) findViewById(R.id.connected_pb);
        progressTv = (TextView) findViewById(R.id.progress_tv);
        progressPb = (ProgressBar) findViewById(R.id.progress_pb);
        errorTv = (TextView) findViewById(R.id.error_tv);
        errorPb = (ProgressBar) findViewById(R.id.error_pb);
        pausedTv = (TextView) findViewById(R.id.paused_tv);
        pausedPb = (ProgressBar) findViewById(R.id.paused_pb);
        completedWidthOldTv = (TextView) findViewById(R.id.completed_width_old_tv);
        completedWidthOldPb = (ProgressBar) findViewById(R.id.completed_width_old_pb);
        completedTv = (TextView) findViewById(R.id.completed_tv);
        completedPb = (ProgressBar) findViewById(R.id.completed_pb);
        warnTv = (TextView) findViewById(R.id.warn_tv);
        warnPb = (ProgressBar) findViewById(R.id.warn_pb);
        deleteAllFileBtn = (Button) findViewById(R.id.delete_all_file_btn);
    }


}
