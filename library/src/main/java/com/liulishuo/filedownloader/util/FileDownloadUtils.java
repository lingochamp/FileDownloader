package com.liulishuo.filedownloader.util;

import android.os.Environment;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by Jacksgong on 9/25/15.
 */
public class FileDownloadUtils {

    /**
     * Checks whether the filename looks legitimate
     */
    public static boolean isFilenameValid(String filename) {
        filename = filename.replaceFirst("/+", "/"); // normalize leading
        // slashes
        return filename.startsWith(Environment.getDownloadCacheDirectory()
                .toString())
                || filename.startsWith(Environment
                .getExternalStorageDirectory().toString());
    }

    public static String getDefaultSaveRootPath(){
        return FileDownloadHelper.getAppContext().getExternalCacheDir().getAbsolutePath();
    }

    public static String getDefaultSaveFilePath(final String url) {
        return String.format("%s%s%s", getDefaultSaveRootPath(), File.separator, md5(url));
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
        return md5(String.format("%sp%s", url, path)).hashCode();
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
        StackTraceElement[] stes = new Throwable().getStackTrace();
        return getStack(stes, printLine);
    }

    public static String getStack(final StackTraceElement[] stes, final boolean printLine) {
        if ((stes == null) || (stes.length < 4)) {
            return "";
        }

        StringBuilder t = new StringBuilder();

        for (int i = 3; i < stes.length; i++) {
            if (!stes[i].getClassName().contains("com.liulishuo.filedownloader")) {
                continue;
            }
            t.append("[");
            t.append(stes[i].getClassName().substring("com.liulishuo.filedownloader".length()));
            t.append(":");
            t.append(stes[i].getMethodName());
            if (printLine) {
                t.append("(" + stes[i].getLineNumber() + ")]");
            } else {
                t.append("]");
            }
        }
        return t.toString();
    }
}
