package com.liulishuo.filedownloader.demo;

import android.app.Application;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadLog;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 下面这句为了测试，正常使用的时候不用添加
        FileDownloadLog.NEED_LOG = BuildConfig.DOWNLOAD_NEED_LOG;

        // 不耗时，做一些简单初始化准备工作，不会启动下载进程
        FileDownloader.init(this);
    }
}
