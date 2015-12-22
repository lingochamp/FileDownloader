package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 *
 * 不同进程相互独立
 */
public class DownloadEventPool {
    public static IDownloadEventPool impl = null;

    public final static void setImpl(IDownloadEventPool impl) {
        DownloadEventPool.impl = impl;
    }

    public final static IDownloadEventPool getImpl() {
        return impl;
    }

}
