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

package com.liulishuo.filedownloader.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import okhttp3.Headers;


/**
 * Created by Jacksgong on 9/25/15.
 * <p/>
 * Wrapping some static utils for FileDownloader.
 */
public class FileDownloadUtils {

    private static int MIN_PROGRESS_STEP = 65536;
    private static long MIN_PROGRESS_TIME = 2000;

    /**
     * @param minProgressStep The min buffered so far bytes.
     *                        Used for adjudging whether is time to sync the downloaded so far bytes
     *                        to database and make sure sync the downloaded buffer to local file.
     *                        <p/>
     *                        More smaller more frequently, then download more slowly, but will more
     *                        safer in scene of the process is killed unexpected.
     *                        <p/>
     *                        Default 65536, which follow the value in
     *                        com.android.providers.downloads.Constants.
     * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#onProgress(long, long, FileDescriptor)
     * @see #setMinProgressTime(long)
     */
    public static void setMinProgressStep(int minProgressStep) throws IllegalAccessException {
        if (isDownloaderProcess(FileDownloadHelper.getAppContext())) {
            MIN_PROGRESS_STEP = minProgressStep;
        } else {
            throw new IllegalAccessException("This value is used in the :filedownloader process," +
                    " so set this value in your process is without effect. You can add " +
                    "'process.non-separate=true' in 'filedownloader.properties' to share the main " +
                    "process to FileDownloadService. Or you can configure this value in " +
                    "'filedownloader.properties' by 'download.min-progress-step'.");
        }
    }

    /**
     * @param minProgressTime The min buffered millisecond.
     *                        Used for adjudging whether is time to sync the downloaded so far bytes
     *                        to database and make sure sync the downloaded buffer to local file.
     *                        <p/>
     *                        More smaller more frequently, then download more slowly, but will more
     *                        safer in scene of the process is killed unexpected.
     *                        <p/>
     *                        Default 2000, which follow the value in
     *                        com.android.providers.downloads.Constants.
     * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#onProgress(long, long, FileDescriptor)
     * @see #setMinProgressStep(int)
     */
    public static void setMinProgressTime(long minProgressTime) throws IllegalAccessException {
        if (isDownloaderProcess(FileDownloadHelper.getAppContext())) {
            MIN_PROGRESS_TIME = minProgressTime;
        } else {
            throw new IllegalAccessException("This value is used in the :filedownloader process," +
                    " so set this value in your process is without effect. You can add " +
                    "'process.non-separate=true' in 'filedownloader.properties' to share the main " +
                    "process to FileDownloadService. Or you can configure this value in " +
                    "'filedownloader.properties' by 'download.min-progress-time'.");
        }
    }

    public static int getMinProgressStep() {
        return MIN_PROGRESS_STEP;
    }

    public static long getMinProgressTime() {
        return MIN_PROGRESS_TIME;
    }

    /**
     * Checks whether the filename looks legitimate
     */
    public static boolean isFilenameValid(String filename) {
//        filename = filename.replaceFirst("/+", "/"); // normalize leading
        // slashes
//        return filename.startsWith(Environment.getDownloadCacheDirectory()
//                .toString())
//                || filename.startsWith(Environment
//                .getExternalStorageDirectory().toString());
        return true;
    }

    private static String DEFAULT_SAVE_ROOT_PATH;

    public static String getDefaultSaveRootPath() {
        if (!TextUtils.isEmpty(DEFAULT_SAVE_ROOT_PATH)) {
            return DEFAULT_SAVE_ROOT_PATH;
        }

        if (FileDownloadHelper.getAppContext().getExternalCacheDir() == null) {
            return Environment.getDownloadCacheDirectory().getAbsolutePath();
        } else {
            return FileDownloadHelper.getAppContext().getExternalCacheDir().getAbsolutePath();
        }
    }

    public static String getDefaultSaveFilePath(final String url) {
        return formatString("%s%s%s", getDefaultSaveRootPath(), File.separator, md5(url));
    }

    /**
     * The path is used as Root Path in the case of task without setting path in the entire Download Engine
     * {@link com.liulishuo.filedownloader.BaseDownloadTask#setPath(String)}
     *
     * @param path default root path for save download file.
     */
    public static void setDefaultSaveRootPath(final String path) {
        DEFAULT_SAVE_ROOT_PATH = path;
    }

//    public static Integer getActiveNetworkType(final Context context) {
//        ConnectivityManager connectivity = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivity == null) {
//            return null;
//        }
//
//        NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
//        if (activeInfo == null) {
//            return null;
//        }
//        return activeInfo.getType();
//    }

    public static int generateId(final String url, final String path) {
        return md5(formatString("%sp%s", url, path)).hashCode();
    }

    private static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }


    // stack

    public static String getStack() {
        return getStack(true);
    }

    public static String getStack(final boolean printLine) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return getStack(stackTrace, printLine);
    }

    public static String getStack(final StackTraceElement[] stackTrace, final boolean printLine) {
        if ((stackTrace == null) || (stackTrace.length < 4)) {
            return "";
        }

        StringBuilder t = new StringBuilder();

        for (int i = 3; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().contains("com.liulishuo.filedownloader")) {
                continue;
            }
            t.append("[");
            t.append(stackTrace[i].getClassName().substring("com.liulishuo.filedownloader".length()));
            t.append(":");
            t.append(stackTrace[i].getMethodName());
            if (printLine) {
                t.append("(").append(stackTrace[i].getLineNumber()).append(")]");
            } else {
                t.append("]");
            }
        }
        return t.toString();
    }

    public static boolean isDownloaderProcess(final Context context) {
        if (FileDownloadProperties.getImpl().PROCESS_NON_SEPARATE) {
            return true;
        }

        int pid = android.os.Process.myPid();
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : activityManager.getRunningAppProcesses()) {
            if (runningAppProcessInfo.pid == pid) {
                return runningAppProcessInfo.processName.endsWith(":filedownloader");
            }
        }

        return false;
    }

    public static String[] convertHeaderString(final String nameAndValuesString) {
        final String[] lineString = nameAndValuesString.split("\n");
        final String[] namesAndValues = new String[lineString.length * 2];

        for (int i = 0; i < lineString.length; i++) {
            final String[] nameAndValue = lineString[i].split(": ");
            /**
             * @see Headers#toString()
             * @see Headers#name(int)
             * @see Headers#value(int)
             */
            namesAndValues[i * 2] = nameAndValue[0];
            namesAndValues[i * 2 + 1] = nameAndValue[1];
        }

        return namesAndValues;
    }

    public static long getFreeSpaceBytes(final String path) {
        long freeSpaceBytes;
        final StatFs statFs = new StatFs(path);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpaceBytes = statFs.getAvailableBytes();
        } else {
            freeSpaceBytes = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }

        return freeSpaceBytes;
    }

    public static String formatString(final String msg, Object... args) {
        return String.format(Locale.ENGLISH, msg, args);
    }
}
