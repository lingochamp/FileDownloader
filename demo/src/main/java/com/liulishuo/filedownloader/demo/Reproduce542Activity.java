/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

/**
 * Created by Jacksgong on 17/06/2017.
 */

public class Reproduce542Activity extends AppCompatActivity {
    public static final String APP_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cy/app";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reprocude_542);

        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reproduceNoFileAfterCompleted();
            }
        });
    }

    private void reproduceNoFileAfterCompleted() {

        FileDownloader.getImpl().create("https://dldir1.qq.com/music/clntupate/QQMusicSetup.exe")
                .setPath(APP_DIR)
                .setListener(new FileDownloadListener() {

                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        LogUtil.d("downloadAPP pending 等待，已经进入下载队列");
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                        LogUtil.d("downloadAPP connected 已经连接上");
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        LogUtil.d("downloadAPP  下载进度回调 progress soFarBytes：" + soFarBytes + ",totalBytes：" + totalBytes);
                    }

                    @Override
                    protected void blockComplete(BaseDownloadTask task) {
                        LogUtil.d("downloadAPP blockComplete 在完成前同步调用该方法，此时已经下载完成");
                    }

                    @Override
                    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
                        LogUtil.d("downloadAPP retry 重试之前把将要重试是第几次回调回来");
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        LogUtil.d("downloadAPP completed 完成整个下载过程");
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        LogUtil.d("downloadAPP paused 暂停下载");
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        LogUtil.d("downloadAPP error" + e.getMessage());
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        LogUtil.d("downloadAPP warn");
                    }
                }).start();
    }

    static class LogUtil {
        static void d(String msg) {
            Log.d("Reproduce542", msg);
        }
    }
}
