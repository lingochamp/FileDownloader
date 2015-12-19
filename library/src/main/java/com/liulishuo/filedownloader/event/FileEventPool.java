package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 * <p/>
 * 不同进程相互独立
 */
public class FileEventPool {
    public static IFileEventPool impl = null;

    public final static void setImpl(IFileEventPool impl) {
        FileEventPool.impl = impl;
    }

    public final static IFileEventPool getImpl() {
        return impl;
    }

}
