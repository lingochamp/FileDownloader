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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 12/25/15.
 */
public class MultitaskTestActivity extends AppCompatActivity {

    private final static String TAG = "MultitaskTestActivity";

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mutitask_test);
        assignViews();
        resetDisplayData();

        actionBtn.setTag(true);
        actionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean toStart = (boolean) actionBtn.getTag();
                if (toStart) {
                    if (start()) {
                        actionBtn.setText(R.string.pause);
                        actionBtn.setTag(false);
                    }
                } else {
                    actionBtn.setText(R.string.start);
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

                Toast.makeText(MultitaskTestActivity.this,
                        String.format("Complete delete %d files", count), Toast.LENGTH_LONG).show();

            }
        });

        avoidMissFrameCb.setChecked(FileDownloader.isEnabledAvoidDropFrame());
        avoidMissFrameCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    FileDownloader.enableAvoidDropFrame();
                } else {
                    FileDownloader.disableAvoidDropFrame();
                }
            }
        });
    }

    private void resetDisplayData() {
        pendingPb.setProgress(0);
        connectedPb.setProgress(0);
        progressPb.setProgress(0);
        retryPb.setProgress(0);
        errorPb.setProgress(0);
        pausedPb.setProgress(0);
        completedReusedPb.setProgress(0);
        completedDownloadingPb.setProgress(0);
        warnPb.setProgress(0);
        overTaskPb.setProgress(0);

        pendingTv.setText(getString(R.string.multitask_test_pending, 0));
        connectedTv.setText(getString(R.string.multitask_test_connected, 0));
        progressTv.setText(getString(R.string.multitask_test_progress, 0));
        retryTv.setText(getString(R.string.multitask_test_retry, 0));
        errorTv.setText(getString(R.string.multitask_test_error, 0));
        pausedTv.setText(getString(R.string.multitask_test_paused, 0));
        completedReusedTv.setText(getString(R.string.multitask_test_completed_reused, 0));
        completedDownloadingTv.setText(getString(R.string.multitask_test_completed_downloading, 0));
        warnTv.setText(getString(R.string.multitask_test_warn, 0));

        pendingInfoTv.setText("");
        connectedInfoTv.setText("");
        retryInfoTv.setText("");
        progressInfoTv.setText("");
        errorInfoTv.setText("");
        pausedInfoTv.setText("");
        completedReusedInfoTv.setText("");
        completedDownloadingInfoTv.setText("");
        warnInfoTv.setText("");
    }

    private long start = 0;

    private boolean start() {
        final int count = Integer.valueOf(taskCountTv.getText().toString());
        if (count <= 0) {
            return false;
        }

        taskCountSb.setEnabled(false);

        pendingPb.setMax(count);
        connectedPb.setMax(count);
        progressPb.setMax(count);
        retryPb.setMax(count);
        errorPb.setMax(count);
        pausedPb.setMax(count);
        completedReusedPb.setMax(count);
        completedDownloadingPb.setMax(count);
        warnPb.setMax(count);
        overTaskPb.setMax(count);


        resetDisplayData();

        // 需要时再显示
        retryInfoTv.setVisibility(View.GONE);
        retryPb.setVisibility(View.GONE);
        retryTv.setVisibility(View.GONE);

        isStopTimer = false;
        timeConsumeTv.setTag(0);
        goTimeCount();


        start = System.currentTimeMillis();
        // =================== How to Download tasks: =============================
        downloadListener = createLis();
//        The first way-----------------------------:
//        for (int i = 0; i < count; i++) {
//            final String url = Constant.URLS[i];
//            FileDownloader.getImpl().create(url)
//                    .setListener(downloadListener)
//                    .setAutoRetryTimes(1)
//                    .setTag(i + 1)
//                    .setCallbackProgressTimes(0)
//                    .asInQueueTask()
//                    .enqueue();
//        }
//        FileDownloader.getImpl().start(downloadListener, serialRbtn.isChecked());

//        The second way----------------------------:

        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(downloadListener);

        final List<BaseDownloadTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(FileDownloader.getImpl().create(Constant.URLS[i]).setTag(i + 1));
        }
        queueSet.disableCallbackProgressTimes(); // do not want each task's download progress's callback,
        // we just consider which task will completed.

        // auto retry 1 time if download fail
        queueSet.setAutoRetryTimes(1);

        if (serialRbtn.isChecked()) {
            // start download in serial order
            queueSet.downloadSequentially(tasks);
            // if your tasks are not a list, invoke such following will more readable:
//            queueSet.downloadSequentially(
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).addHeader(...,...),
//                    FileDownloader.getImpl().create(url).setPath(...)
//            );
        } else {
            // start parallel download
            queueSet.downloadTogether(tasks);
            // if your tasks are not a list, invoke such following will more readable:
//            queueSet.downloadTogether(
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).setSyncCallback(true)
//            );
        }
        queueSet.start();

        return true;
    }

    private FileDownloadListener downloadListener;

    private void pause() {
        FileDownloader.getImpl().pause(downloadListener);
        stopTimeCount();
        taskCountSb.setEnabled(true);
    }

    private void stopTimeCount() {
        isStopTimer = true;
        timeConsumeTv.getHandler().removeCallbacks(timeCountRunnable);
        final long consume = System.currentTimeMillis() - start;
        if (timeConsumeTv != null) {
            timeConsumeTv.setText(String.valueOf(consume / 1000f));
        }
    }

    private FileDownloadListener createLis() {
        return new FileDownloadListener() {

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                // 之所以加这句判断，是因为有些异步任务在pause以后，会持续回调pause回来，而有些任务在pause之前已经完成，
                // 但是通知消息还在线程池中还未回调回来，这里可以优化
                // 后面所有在回调中加这句都是这个原因
                if (task.getListener() != downloadListener) {
                    return;
                }
                pendingPb.setProgress(pendingPb.getProgress() + 1);
                pendingTv.setText(getString(R.string.multitask_test_pending, pendingPb.getProgress()));
                pendingInfoTv.append((int) task.getTag() + " | ");
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue,
                                     int soFarBytes, int totalBytes) {

                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                if (task.getListener() != downloadListener) {
                    return;
                }

                connectedPb.setProgress(connectedPb.getProgress() + 1);
                connectedTv.setText(getString(R.string.multitask_test_connected,
                        connectedPb.getProgress()));

                connectedInfoTv.append((int) task.getTag() + " | ");
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (task.getListener() != downloadListener) {
                    return;
                }
//                progressPb.setProgress(progressPb.getProgress() + 1);
//                progressTv.setText("progress: " + progressPb.getProgress());
//                progressInfoTv.append((int)task.getTag() + " | ");
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }
            }

            @Override
            protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
                super.retry(task, ex, retryingTimes, soFarBytes);
                if (task.getListener() != downloadListener) {
                    return;
                }

                retryInfoTv.setVisibility(View.VISIBLE);
                retryPb.setVisibility(View.VISIBLE);
                retryTv.setVisibility(View.VISIBLE);

                retryPb.setProgress(retryPb.getProgress() + 1);
                retryTv.setText(getString(R.string.multitask_test_retry, retryPb.getProgress()));
                retryInfoTv.append((int)task.getTag() + " | ");
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }

                if (task.isReusedOldFile()) {
                    completedReusedPb.setProgress(completedReusedPb.getProgress() + 1);
                    completedReusedTv.setText(getString(R.string.multitask_test_completed_reused,
                            completedReusedPb.getProgress()));
                    completedReusedInfoTv.append((int) task.getTag() + " | ");
                } else {
                    completedDownloadingPb.setProgress(completedDownloadingPb.getProgress() + 1);
                    completedDownloadingTv.
                            setText(getString(R.string.multitask_test_completed_downloading,
                                    completedDownloadingPb.getProgress()));

                    completedDownloadingInfoTv.append((int) task.getTag() + " | ");
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
                pausedTv.setText(getString(R.string.multitask_test_paused, pausedPb.getProgress()));
                pausedInfoTv.append((int) task.getTag() + " | ");
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                if (task.getListener() != downloadListener) {
                    return;
                }
                errorPb.setProgress(errorPb.getProgress() + 1);
                errorTv.setText(getString(R.string.multitask_test_error, errorPb.getProgress()));
                errorInfoTv.append((int) task.getTag() + " | ");
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
                checkEndAll();
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                if (task.getListener() != downloadListener) {
                    return;
                }

                warnPb.setProgress(warnPb.getProgress() + 1);
                warnTv.setText(getString(R.string.multitask_test_warn, warnPb.getProgress()));
                warnInfoTv.append((int) task.getTag() + " | ");
                overTaskPb.setProgress(overTaskPb.getProgress() + 1);
                checkEndAll();
            }
        };
    }

    private void checkEndAll() {
        final boolean isEndAll = overTaskPb.getProgress() >= Integer.valueOf(taskCountTv.getText().toString());
        if (isEndAll) {

            Log.d(TAG, String.format("start[%d] over[%d]", GlobalMonitor.getImpl().getMarkStart(),
                    GlobalMonitor.getImpl().getMarkOver()));

            stopTimeCount();
            actionBtn.setTag(true);
            actionBtn.setText("Start");
            taskCountSb.setEnabled(true);
        }
    }

    private boolean isStopTimer = true;

    private void goTimeCount() {
        final int time = (int) timeConsumeTv.getTag();
        timeConsumeTv.setText(String.valueOf(time));
        timeConsumeTv.getHandler().postDelayed(timeCountRunnable, 1000);
    }

    private Runnable timeCountRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStopTimer) {
                return;
            }
            timeConsumeTv.setTag((int)timeConsumeTv.getTag() + 1);
            goTimeCount();
        }
    };

    private SeekBar taskCountSb;
    private TextView taskCountTv;
    private TextView timeConsumeTv;
    private RadioGroup wayRgp;
    private RadioButton serialRbtn;
    private RadioButton parallelRbtn;
    private CheckBox avoidMissFrameCb;
    private ProgressBar overTaskPb;
    private Button actionBtn;
    private TextView pendingTv;
    private TextView pendingInfoTv;
    private ProgressBar pendingPb;
    private TextView connectedTv;
    private TextView connectedInfoTv;
    private ProgressBar connectedPb;
    private TextView progressTv;
    private TextView progressInfoTv;
    private ProgressBar progressPb;
    private TextView retryTv;
    private TextView retryInfoTv;
    private ProgressBar retryPb;
    private TextView errorTv;
    private TextView errorInfoTv;
    private ProgressBar errorPb;
    private TextView pausedTv;
    private TextView pausedInfoTv;
    private ProgressBar pausedPb;
    private TextView completedReusedTv;
    private TextView completedReusedInfoTv;
    private ProgressBar completedReusedPb;
    private TextView completedDownloadingTv;
    private TextView completedDownloadingInfoTv;
    private ProgressBar completedDownloadingPb;
    private TextView warnTv;
    private TextView warnInfoTv;
    private ProgressBar warnPb;
    private Button deleteAllFileBtn;

    private void assignViews() {
        taskCountSb = (SeekBar) findViewById(R.id.task_count_sb);
        taskCountTv = (TextView) findViewById(R.id.task_count_tv);
        timeConsumeTv = (TextView) findViewById(R.id.time_consume_tv);
        wayRgp = (RadioGroup) findViewById(R.id.way_rgp);
        serialRbtn = (RadioButton) findViewById(R.id.serial_rbtn);
        parallelRbtn = (RadioButton) findViewById(R.id.parallel_rbtn);
        avoidMissFrameCb = (CheckBox) findViewById(R.id.avoid_miss_frame_cb);
        overTaskPb = (ProgressBar) findViewById(R.id.over_task_pb);
        actionBtn = (Button) findViewById(R.id.action_btn);
        pendingTv = (TextView) findViewById(R.id.pending_tv);
        pendingInfoTv = (TextView) findViewById(R.id.pending_info_tv);
        pendingPb = (ProgressBar) findViewById(R.id.pending_pb);
        connectedTv = (TextView) findViewById(R.id.connected_tv);
        connectedInfoTv = (TextView) findViewById(R.id.connected_info_tv);
        connectedPb = (ProgressBar) findViewById(R.id.connected_pb);
        progressTv = (TextView) findViewById(R.id.progress_tv);
        progressInfoTv = (TextView) findViewById(R.id.progress_info_tv);
        progressPb = (ProgressBar) findViewById(R.id.progress_pb);
        retryTv = (TextView) findViewById(R.id.retry_tv);
        retryInfoTv = (TextView) findViewById(R.id.retry_info_tv);
        retryPb = (ProgressBar) findViewById(R.id.retry_pb);
        errorTv = (TextView) findViewById(R.id.error_tv);
        errorInfoTv = (TextView) findViewById(R.id.error_info_tv);
        errorPb = (ProgressBar) findViewById(R.id.error_pb);
        pausedTv = (TextView) findViewById(R.id.paused_tv);
        pausedInfoTv = (TextView) findViewById(R.id.paused_info_tv);
        pausedPb = (ProgressBar) findViewById(R.id.paused_pb);
        completedReusedTv = (TextView) findViewById(R.id.completed_with_old_tv);
        completedReusedInfoTv = (TextView) findViewById(R.id.completed_with_old_info_tv);
        completedReusedPb = (ProgressBar) findViewById(R.id.completed_with_old_pb);
        completedDownloadingTv = (TextView) findViewById(R.id.completed_tv);
        completedDownloadingInfoTv = (TextView) findViewById(R.id.completed_info_tv);
        completedDownloadingPb = (ProgressBar) findViewById(R.id.completed_pb);
        warnTv = (TextView) findViewById(R.id.warn_tv);
        warnInfoTv = (TextView) findViewById(R.id.warn_info_tv);
        warnPb = (ProgressBar) findViewById(R.id.warn_pb);
        deleteAllFileBtn = (Button) findViewById(R.id.delete_all_file_btn);
    }


}
