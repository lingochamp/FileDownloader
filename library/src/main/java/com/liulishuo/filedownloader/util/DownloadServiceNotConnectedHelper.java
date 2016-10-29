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

import android.app.Notification;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * The helper for handling the case of requesting do something in the downloader service but the
 * downloader service isn't connected yet.
 *
 * @see FileDownloader#insureServiceBind()
 * @see FileDownloader#insureServiceBindAsync()
 */
public class DownloadServiceNotConnectedHelper {

    public static boolean start(final String url, final String path, final boolean pathAsDirectory) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request start the task([%s], [%s], [%B]) in the download service," +
                        " but the download service isn't connected yet.", url, path, pathAsDirectory);
        return false;
    }

    public static boolean pause(final int id) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request pause the task[%d] in the download service," +
                        " but the download service isn't connected yet.", id);
        return false;
    }

    public static boolean isDownloading(final String url, final String path) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request check the task([%s], [%s]) is downloading in the download service," +
                        " but the download service isn't connected yet.", url, path);
        return false;
    }

    public static long getSofar(final int id) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request get the downloaded so far byte for the task[%d] in the download service," +
                        " but the download service isn't connected yet.", id);
        return 0;
    }

    public static long getTotal(final int id) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request get the total byte for the task[%d] in the download service," +
                        " but the download service isn't connected yet.", id);
        return 0;
    }

    public static byte getStatus(final int id) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request get the status for the task[%d] in the download service," +
                        " but the download service isn't connected yet.", id);
        return FileDownloadStatus.INVALID_STATUS;
    }

    public static void pauseAllTasks() {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request pause all tasks in the download service," +
                        " but the download service isn't connected yet.");
    }

    public static boolean isIdle() {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request check the download service is idle," +
                        " but the download service isn't connected yet.");
        return true;
    }

    public static void startForeground(int notificationId, Notification notification) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request set the download service as the foreground service([%d],[%s])," +
                        " but the download service isn't connected yet.", notificationId, notification);
    }

    public static void stopForeground(boolean removeNotification) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request cancel the foreground status[%B] for the download service," +
                        " but the download service isn't connected yet.", removeNotification);
    }

    public static boolean setMaxNetworkThreadCount(int count) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request set the max network thread count[%d] in the download service," +
                        " but the download service isn't connected yet.", count);
        return false;
    }

    public static boolean clearTaskData(int id) {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request clear the task[%d] data in the database," +
                        " but the download service isn't connected yet.", id);
        return false;
    }

    public static boolean clearAllTaskData() {
        FileDownloadLog.w(DownloadServiceNotConnectedHelper.class,
                "request clear all tasks data in the database," +
                        " but the download service isn't connected yet.");
        return false;
    }
}
