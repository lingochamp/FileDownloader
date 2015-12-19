package com.liulishuo.filedownloader.demo;

import android.app.Application;

import com.liulishuo.filedownloader.FileDownloader;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 不耗时，做一些简单初始化准备工作，不会启动下载进程
        FileDownloader.init(this);
    }
}
