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

package com.liulishuo.filedownloader.services;

import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;

import okhttp3.OkHttpClient;

/**
 * Params in this class is used in the downloading manager, and would be used for initialize the
 * download manager in the process the downloader service settled on.
 */
public class DownloadMgrInitialParams {

    private final InitCustomMaker mMaker;

    public DownloadMgrInitialParams(InitCustomMaker maker) {
        this.mMaker = maker;
    }

    OkHttpClient createOkHttpClient() {
        if (mMaker == null || mMaker.mOkHttpClientCustomMaker == null) {
            return createDefaultOkHttpClient();
        }

        final OkHttpClient customOkHttpClient = mMaker.mOkHttpClientCustomMaker.customMake();

        if (customOkHttpClient != null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "initial FileDownloader manager with the customize " +
                        "okHttpClient: %s", customOkHttpClient);
            }
            return customOkHttpClient;
        } else {
            return createDefaultOkHttpClient();
        }
    }

    int getMaxNetworkThreadCount() {
        if (mMaker == null || mMaker.mMaxNetworkThreadCount == null) {
            return getDefaultMaxNetworkThreadCount();
        }

        final int customizeMaxNetworkThreadCount = mMaker.mMaxNetworkThreadCount;

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "initial FileDownloader manager with the customize " +
                    "maxNetworkThreadCount: %d", customizeMaxNetworkThreadCount);
        }

        return FileDownloadProperties.getValidNetworkThreadCount(customizeMaxNetworkThreadCount);
    }

    FileDownloadDatabase createDatabase() {
        if (mMaker == null || mMaker.mDatabaseCustomMaker == null) {
            return createDefaultDatabase();
        }
        final FileDownloadDatabase customDatabase = mMaker.mDatabaseCustomMaker.customMake();

        if (customDatabase != null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "initial FileDownloader manager with the customize " +
                        "database: %s", customDatabase);
            }
            return customDatabase;
        } else {
            return createDefaultDatabase();
        }
    }

    private OkHttpClient createDefaultOkHttpClient() {
        return new OkHttpClient();
    }

    private int getDefaultMaxNetworkThreadCount() {
        return FileDownloadProperties.getImpl().DOWNLOAD_MAX_NETWORK_THREAD_COUNT;
    }

    private FileDownloadDatabase createDefaultDatabase() {
        return new DefaultDatabaseImpl();
    }

    public static class InitCustomMaker {
        FileDownloadHelper.DatabaseCustomMaker mDatabaseCustomMaker;
        FileDownloadHelper.OkHttpClientCustomMaker mOkHttpClientCustomMaker;
        Integer mMaxNetworkThreadCount;

        /**
         * @param maker The database is used for storing the {@link FileDownloadModel}.
         *              <p>
         *              The data stored in the database is only used for task resumes from the
         *              breakpoint.
         *              <p>
         *              The task of the data stored in the database must be a task that has not
         *              finished downloading yet, and if the task has finished downloading, its data
         *              will be {@link FileDownloadDatabase#remove(int)} from the database, since
         *              that data is no longer available for resumption of its task pass.
         */
        public InitCustomMaker database(FileDownloadHelper.DatabaseCustomMaker maker) {
            this.mDatabaseCustomMaker = maker;
            return this;
        }

        /**
         * @param maker The okHttpClient customize maker, the okHttpClient will be used
         *              in the downloader service to downloading file.
         */
        public InitCustomMaker okHttpClient(FileDownloadHelper.OkHttpClientCustomMaker maker) {
            this.mOkHttpClientCustomMaker = maker;
            return this;
        }

        /**
         * @param maxNetworkThreadCount The maximum count of the network thread, what is the number of
         *                              simultaneous downloads in FileDownloader.
         *                              <p>
         *                              If this value is less than or equal to 0, the value will be
         *                              ignored and use
         *                              {@link FileDownloadProperties#DOWNLOAD_MAX_NETWORK_THREAD_COUNT}
         *                              which is defined in filedownloader.properties instead.
         */
        public InitCustomMaker maxNetworkThreadCount(int maxNetworkThreadCount) {
            if (maxNetworkThreadCount > 0) {
                this.mMaxNetworkThreadCount = maxNetworkThreadCount;
            }
            return this;
        }
    }
}
