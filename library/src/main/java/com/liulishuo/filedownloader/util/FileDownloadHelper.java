package com.liulishuo.filedownloader.util;

import android.app.Application;
import android.content.Context;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class FileDownloadHelper {

    private static Context APP_CONTEXT;
    public static void initAppContext(final Application application) {
        APP_CONTEXT = application;
    }

    public static Context getAppContext(){
        return APP_CONTEXT;
    }
}

