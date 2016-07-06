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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.liulishuo.filedownloader.IThreadPoolMonitor;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.message.MessageSnapshotTaker;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;

import java.io.File;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 12/17/15.
 * <p/>
 * The simple helper for cache the {@code APP_CONTEXT} and {@code OK_HTTP_CLIENT}.
 *
 * @see com.liulishuo.filedownloader.FileDownloader#init(Context, OkHttpClientCustomMaker)
 */
public class FileDownloadHelper {

    @SuppressLint("StaticFieldLeak")
    private static Context APP_CONTEXT;
    private static DownloadMgrInitialParams DOWNLOAD_MANAGER_INITIAL_PARAMS;

    public static void holdContext(final Context context) {
        APP_CONTEXT = context;
    }

    public static Context getAppContext() {
        return APP_CONTEXT;
    }

    public static void initializeDownloadMgrParams(final OkHttpClientCustomMaker maker,
                                                   final int maxNetworkThreadCount) {
        if (!FileDownloadUtils.isDownloaderProcess(FileDownloadHelper.getAppContext())) {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("the DownloadMgrInitialParams is only " +
                            "can be touched in the process which the download service settles on"));
        }

        DOWNLOAD_MANAGER_INITIAL_PARAMS = new DownloadMgrInitialParams(maker,
                maxNetworkThreadCount);
    }

    public static DownloadMgrInitialParams getDownloadMgrInitialParams() {
        return DOWNLOAD_MANAGER_INITIAL_PARAMS;
    }

    public interface OkHttpClientCustomMaker {

        /**
         * Only be invoked by the {@link Application#onCreate()} on the ':filedownloader' progress.
         * You can customize Timeout, Proxy, etc...
         *
         * @return Nullable, Customize {@link OkHttpClient}, will be used for downloading files.
         * @see com.liulishuo.filedownloader.FileDownloader#init(Application, OkHttpClientCustomMaker)
         * @see OkHttpClient
         */
        OkHttpClient customMake();
    }

    public static boolean inspectAndInflowDownloaded(int id, String path, boolean forceReDownload) {
        if (forceReDownload) {
            return false;
        }

        if (path != null) {
            final File file = new File(path);
            if (file.exists()) {
                MessageSnapshotFlow.getImpl().inflow(MessageSnapshotTaker.
                        catchCanReusedOldFile(id, file));
                return true;
            }
        }

        return false;
    }

    public static boolean inspectAndInflowDownloading(int id, FileDownloadModel model,
                                                      IThreadPoolMonitor monitor) {
        if (monitor.isDownloading(model)) {
            MessageSnapshotFlow.getImpl().
                    inflow(MessageSnapshotTaker.catchWarn(id, model.getSoFar(), model.getTotal()));
            return true;
        }

        return false;
    }
}

