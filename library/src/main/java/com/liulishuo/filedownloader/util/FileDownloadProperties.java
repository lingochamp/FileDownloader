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

import com.liulishuo.filedownloader.services.FileDownloadBroadcastHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * You can customize the FileDownloader Engine by add filedownloader.properties file in your assets
 * folder. Example: /demo/src/main/assets/filedownloader.properties
 * <p/>
 * Rules: key=value
 * <p/>
 * Supported keys:
 * <p/>
 * Key {@code http.lenient}
 * Value: {@code true} or {@code false}
 * Default: {@code false}.
 * Such as: http.lenient=false
 * Description:
 * If you occur exception: 'can't know the size of the download file, and its Transfer-Encoding is
 * not Chunked either', but you want to ignore such exception, set true, will deal with it as the
 * case of transfer encoding chunk.
 * If true, will ignore HTTP response header does not has content-length either not chunk transfer
 * encoding.
 * <p/>
 * Key {@code process.non-separate}
 * Value: {@code true} or {@code false}
 * Default: {@code false}.
 * Such as: process.non-separate=false
 * Description:
 * The FileDownloadService runs in the separate process ':filedownloader' as default, if you want
 * to run the FileDownloadService in the main process, just set true.
 * <p/>
 * Key {@code download.min-progress-step}
 * Value: [0, {@link Integer#MAX_VALUE}]
 * Default: 65536, which follow the value in com.android.providers.downloads.Constants.
 * Such as: download.min-progress-step=65536
 * Description:
 * The min buffered so far bytes.
 * Used for adjudging whether is time to sync the downloaded so far bytes to database and make sure
 * sync the downloaded buffer to local file.
 * More smaller more frequently, then download more slowly, but will more safer in scene of the
 * process is killed unexpectedly.
 * <p/>
 * Key {@code download.min-progress-time}
 * Value: [0, {@link Long#MAX_VALUE}]
 * Default: 2000, which follow the value in com.android.providers.downloads.Constants.
 * Such as: download.min-progress-time=2000
 * Description:
 * The min buffered millisecond.
 * Used for adjudging whether is time to sync the downloaded so far bytes to database and make sure
 * sync the downloaded buffer to local file.
 * More smaller more frequently, then download more slowly, but will more safer in scene of the
 * process is killed unexpectedly.
 * <p/>
 * Key {@code download.max-network-thread-count}
 * Value: [1, 12]
 * Default: 3.
 * Such as: download.max-network-thread-count=3
 * Description:
 * The maximum network thread count for downloading simultaneously.
 * FileDownloader is designed to download 3 files simultaneously as maximum size as default, and the
 * rest of the task is in the FIFO(First In First Out) pending queue.
 * Because the network resource is limited to one device, it means if FileDownloader start
 * downloading tasks unlimited simultaneously, it will be blocked by lack of the network resource,
 * and more useless CPU occupy.
 * The relative efficiency of 3 is higher than others(As Fresco or Picasso do), But for case by case
 * FileDownloader is support to configure for this.
 * Max 12, min 1. If the value more than {@code max} will be replaced with {@code max}; If the value
 * less than {@code min} will be replaced with {@code min}.
 * <p/>
 * Key {@code file.non-pre-allocation}
 * Value: {@code true} or {@code false}
 * Default: {@code false}.
 * Such as: file.non-pre-allocation=false
 * Description:
 * FileDownloader is designed to create the file and pre-allocates the 'content-length' space for it
 * when start downloading.Because FileDownloader want to prevent the space is not enough to store
 * coming data in downloading state as default.
 * <p/>
 * Key {@code broadcast.completed}
 * Value: {@code true} or {@code false}
 * Default: {@code false}.
 * Such as: broadcast.completed=false
 * Description:
 * Whether need to post an broadcast when downloading is completed.
 * This option is very useful when you download something silent on the background on the filedownloader
 * process, and the main process is killed, but you want to do something on the main process when tasks
 * are completed downloading on the filedownloader process, so you can set this one to `true`, then
 * when a task is completed task, you will receive the broadcast, and the main process will be relaunched
 * to handle the broadcast.
 * <p>
 * If you want to receive such broadcast, you also need to register receiver with
 * 'filedownloader.intent.action.completed' action name on 'AndroidManifest.xml'.
 * <p>
 * You can use {@link FileDownloadBroadcastHandler} class to parse the received intent.
 */
public class FileDownloadProperties {

    private final static String KEY_HTTP_LENIENT = "http.lenient";
    private final static String KEY_PROCESS_NON_SEPARATE = "process.non-separate";
    private final static String KEY_DOWNLOAD_MIN_PROGRESS_STEP = "download.min-progress-step";
    private final static String KEY_DOWNLOAD_MIN_PROGRESS_TIME = "download.min-progress-time";
    private final static String KEY_DOWNLOAD_MAX_NETWORK_THREAD_COUNT = "download.max-network-thread-count";
    private final static String KEY_FILE_NON_PRE_ALLOCATION = "file.non-pre-allocation";
    private final static String KEY_BROADCAST_COMPLETED = "broadcast.completed";

    public final int DOWNLOAD_MIN_PROGRESS_STEP;
    public final long DOWNLOAD_MIN_PROGRESS_TIME;
    public final boolean HTTP_LENIENT;
    public final boolean PROCESS_NON_SEPARATE;
    public final int DOWNLOAD_MAX_NETWORK_THREAD_COUNT;
    public final boolean FILE_NON_PRE_ALLOCATION;
    public final boolean BROADCAST_COMPLETED;

    public static class HolderClass {
        private final static FileDownloadProperties INSTANCE = new FileDownloadProperties();
    }

    public static FileDownloadProperties getImpl() {
        return HolderClass.INSTANCE;
    }

    private final static String TRUE_STRING = "true";
    private final static String FALSE_STRING = "false";

    // init properties, normally consume <= 2ms
    private FileDownloadProperties() {
        if (FileDownloadHelper.getAppContext() == null) {
            throw new IllegalStateException("Please invoke the 'FileDownloader#setup' before using " +
                    "FileDownloader. If you want to register some components on FileDownloader " +
                    "please invoke the 'FileDownloader#setupOnApplicationOnCreate' on the " +
                    "'Application#onCreate' first.");
        }

        final long start = System.currentTimeMillis();
        String httpLenient = null;
        String processNonSeparate = null;
        String downloadMinProgressStep = null;
        String downloadMinProgressTime = null;
        String downloadMaxNetworkThreadCount = null;
        String fileNonPreAllocation = null;
        String broadcastCompleted = null;

        Properties p = new Properties();
        InputStream inputStream = null;

        try {
            inputStream = FileDownloadHelper.getAppContext().getAssets().
                    open("filedownloader.properties");
            if (inputStream != null) {
                p.load(inputStream);
                httpLenient = p.getProperty(KEY_HTTP_LENIENT);
                processNonSeparate = p.getProperty(KEY_PROCESS_NON_SEPARATE);
                downloadMinProgressStep = p.getProperty(KEY_DOWNLOAD_MIN_PROGRESS_STEP);
                downloadMinProgressTime = p.getProperty(KEY_DOWNLOAD_MIN_PROGRESS_TIME);
                downloadMaxNetworkThreadCount = p.getProperty(KEY_DOWNLOAD_MAX_NETWORK_THREAD_COUNT);
                fileNonPreAllocation = p.getProperty(KEY_FILE_NON_PRE_ALLOCATION);
                broadcastCompleted = p.getProperty(KEY_BROADCAST_COMPLETED);
            }
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadProperties.class, "not found filedownloader.properties");
                }
            } else {
                e.printStackTrace();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        //http.lenient
        if (httpLenient != null) {
            if (!httpLenient.equals(TRUE_STRING) && !httpLenient.equals(FALSE_STRING)) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the value of '%s' must be '%s' or '%s'",
                                KEY_HTTP_LENIENT, TRUE_STRING, FALSE_STRING));
            }
            HTTP_LENIENT = httpLenient.equals(TRUE_STRING);
        } else {
            HTTP_LENIENT = false;
        }

        //process.non-separate
        if (processNonSeparate != null) {
            if (!processNonSeparate.equals(TRUE_STRING) &&
                    !processNonSeparate.equals(FALSE_STRING)) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the value of '%s' must be '%s' or '%s'",
                                KEY_PROCESS_NON_SEPARATE, TRUE_STRING, FALSE_STRING));
            }
            PROCESS_NON_SEPARATE = processNonSeparate.equals(TRUE_STRING);
        } else {
            PROCESS_NON_SEPARATE = false;
        }

        //download.min-progress-step
        if (downloadMinProgressStep != null) {
            int processDownloadMinProgressStep = Integer.valueOf(downloadMinProgressStep);
            processDownloadMinProgressStep = Math.max(0, processDownloadMinProgressStep);
            DOWNLOAD_MIN_PROGRESS_STEP = processDownloadMinProgressStep;
        } else {
            DOWNLOAD_MIN_PROGRESS_STEP = 65536;
        }

        //download.min-progress-time
        if (downloadMinProgressTime != null) {
            long processDownloadMinProgressTime = Long.valueOf(downloadMinProgressTime);
            processDownloadMinProgressTime = Math.max(0, processDownloadMinProgressTime);
            DOWNLOAD_MIN_PROGRESS_TIME = processDownloadMinProgressTime;
        } else {
            DOWNLOAD_MIN_PROGRESS_TIME = 2000L;
        }

        //download.max-network-thread-count
        if (downloadMaxNetworkThreadCount != null) {
            DOWNLOAD_MAX_NETWORK_THREAD_COUNT = getValidNetworkThreadCount(
                    Integer.valueOf(downloadMaxNetworkThreadCount));
        } else {
            DOWNLOAD_MAX_NETWORK_THREAD_COUNT = 3;
        }

        // file.non-pre-allocation
        if (fileNonPreAllocation != null) {
            if (!fileNonPreAllocation.equals(TRUE_STRING) &&
                    !fileNonPreAllocation.equals(FALSE_STRING)) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the value of '%s' must be '%s' or '%s'",
                                KEY_FILE_NON_PRE_ALLOCATION, TRUE_STRING, FALSE_STRING));
            }
            FILE_NON_PRE_ALLOCATION = fileNonPreAllocation.equals(TRUE_STRING);
        } else {
            FILE_NON_PRE_ALLOCATION = false;
        }

        if (broadcastCompleted != null) {
            if (!broadcastCompleted.equals(TRUE_STRING) &&
                    !broadcastCompleted.equals(FALSE_STRING)) {
                throw new IllegalStateException(
                        FileDownloadUtils.formatString("the value of '%s' must be '%s' or '%s'",
                                KEY_BROADCAST_COMPLETED, TRUE_STRING, FALSE_STRING));
            }
            BROADCAST_COMPLETED = broadcastCompleted.equals(TRUE_STRING);

        } else {
            BROADCAST_COMPLETED = false;
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.i(FileDownloadProperties.class, "init properties %d\n load properties:" +
                            " %s=%B; %s=%B; %s=%d; %s=%d; %s=%d",
                    System.currentTimeMillis() - start,
                    KEY_HTTP_LENIENT, HTTP_LENIENT,
                    KEY_PROCESS_NON_SEPARATE, PROCESS_NON_SEPARATE,
                    KEY_DOWNLOAD_MIN_PROGRESS_STEP, DOWNLOAD_MIN_PROGRESS_STEP,
                    KEY_DOWNLOAD_MIN_PROGRESS_TIME, DOWNLOAD_MIN_PROGRESS_TIME,
                    KEY_DOWNLOAD_MAX_NETWORK_THREAD_COUNT, DOWNLOAD_MAX_NETWORK_THREAD_COUNT);
        }
    }

    public static int getValidNetworkThreadCount(int requireCount) {
        int MAX_VALID_NETWORK_THREAD_COUNT = 12;
        int MIN_VALID_NETWORK_THREAD_COUNT = 1;

        if (requireCount > MAX_VALID_NETWORK_THREAD_COUNT) {
            FileDownloadLog.w(FileDownloadProperties.class, "require the count of network thread  " +
                            "is %d, what is more than the max valid count(%d), so adjust to %d auto",
                    requireCount, MAX_VALID_NETWORK_THREAD_COUNT, MAX_VALID_NETWORK_THREAD_COUNT);
            return MAX_VALID_NETWORK_THREAD_COUNT;
        } else if (requireCount < MIN_VALID_NETWORK_THREAD_COUNT) {
            FileDownloadLog.w(FileDownloadProperties.class, "require the count of network thread  " +
                            "is %d, what is less than the min valid count(%d), so adjust to %d auto",
                    requireCount, MIN_VALID_NETWORK_THREAD_COUNT, MIN_VALID_NETWORK_THREAD_COUNT);
            return MIN_VALID_NETWORK_THREAD_COUNT;
        }

        return requireCount;
    }
}
