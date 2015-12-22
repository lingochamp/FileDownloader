package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * Created by Jacksgong on 12/21/15.
 */
public class SingleTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);

        savePath1 = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp1";
        savePath2 = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp2";

        assignViews();

        startBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId1 = FileDownloader.getImpl().create(Constant.BIG_FILE_URLS[2])
                        .setPath(savePath1)
                        .setCallbackProgressTimes(500)
                        .setListener(new FileDownloadSampleListener() {
                            @Override
                            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                super.progress(task, soFarBytes, totalBytes);
                                progressBar1.setMax(totalBytes);
                                progressBar1.setProgress(soFarBytes);
                            }

                            @Override
                            protected void completed(BaseDownloadTask task) {
                                super.completed(task);
                                progressBar1.setProgress(task.getSoFarBytes());
                                Toast.makeText(SingleTaskActivity.this, String.format("completed 1 %s", task.getPath()), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                super.paused(task, soFarBytes, totalBytes);
                                Toast.makeText(SingleTaskActivity.this, "paused 1", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void warn(BaseDownloadTask task) {
                                super.warn(task);
                                Toast.makeText(SingleTaskActivity.this, "warn 1", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void error(BaseDownloadTask task, Throwable e) {
                                super.error(task, e);
                                Toast.makeText(SingleTaskActivity.this, "error 1", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .start();
            }
        });

        pauseBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId1);
            }
        });

        deleteBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(savePath1).delete();
            }
        });

        startBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId2 = FileDownloader.getImpl().create(Constant.BIG_FILE_URLS[3])
                        .setPath(savePath2)
                        .setCallbackProgressTimes(500)
                        .setListener(new FileDownloadSampleListener() {
                            @Override
                            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                super.progress(task, soFarBytes, totalBytes);
                                progressBar2.setMax(totalBytes);
                                progressBar2.setProgress(soFarBytes);
                            }

                            @Override
                            protected void completed(BaseDownloadTask task) {
                                super.completed(task);
                                progressBar2.setProgress(task.getSoFarBytes());
                                Toast.makeText(SingleTaskActivity.this, String.format("completed 2 %s", task.getPath()), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                super.paused(task, soFarBytes, totalBytes);
                                Toast.makeText(SingleTaskActivity.this, "paused 2", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void warn(BaseDownloadTask task) {
                                super.warn(task);
                                Toast.makeText(SingleTaskActivity.this, "warn 2", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            protected void error(BaseDownloadTask task, Throwable e) {
                                super.error(task, e);
                                Toast.makeText(SingleTaskActivity.this, "error 2", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .start();
            }
        });

        pauseBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId2);
            }
        });

        deleteBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(savePath2).delete();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.getImpl().pause(downloadId1);
        FileDownloader.getImpl().pause(downloadId2);
    }

    private int downloadId1;
    private int downloadId2;


    private String savePath1;
    private String savePath2;

    private Button startBtn1;
    private Button pauseBtn1;
    private Button deleteBtn1;
    private ProgressBar progressBar1;
    private Button startBtn2;
    private Button pauseBtn2;
    private Button deleteBtn2;
    private ProgressBar progressBar2;

    private void assignViews() {
        startBtn1 = (Button) findViewById(R.id.start_btn_1);
        pauseBtn1 = (Button) findViewById(R.id.pause_btn_1);
        deleteBtn1 = (Button) findViewById(R.id.delete_btn_1);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar_1);
        startBtn2 = (Button) findViewById(R.id.start_btn_2);
        pauseBtn2 = (Button) findViewById(R.id.pause_btn_2);
        deleteBtn2 = (Button) findViewById(R.id.delete_btn_2);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar_2);
    }


}
