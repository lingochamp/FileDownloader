package com.liulishuo.filedownloader.util;

import android.util.Log;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class FileDownloadLog {

    private final static String TAG = "FileDownloader";

    public static void e(Object o, Throwable e, String msg, Object... args) {
        Log.e(TAG + o.getClass().getSimpleName(), String.format(msg, args), e);
    }

    public static void e(Object o, String msg, Object... args) {
        Log.e(TAG + o.getClass().getSimpleName(), String.format(msg, args));
    }

    public static void i(Object o, String msg, Object... args) {
        Log.i(TAG + o.getClass().getSimpleName(), String.format(msg, args));
    }

    public static void d(Object o, String msg, Object... args) {
        Log.d(TAG + o.getClass().getSimpleName(), String.format(msg, args));
    }

    public static void w(Object o, String msg, Object... args) {
        Log.w(TAG + o.getClass().getSimpleName(), String.format(msg, args));
    }

    public static void v(Object o, String msg, Object... args) {
        Log.v(TAG + o.getClass().getSimpleName(), String.format(msg, args));
    }
}
