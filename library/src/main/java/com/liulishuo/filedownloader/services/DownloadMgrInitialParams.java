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
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.stream.FileDownloadRandomAccessFile;
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
        if (maker != null) {
            maker.securityCheck();
        }
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


    FileDownloadHelper.OutputStreamCreator createOutputStreamCreator() {
        if (mMaker == null) {
            return createDefaultOutputStreamCreator();
        }

        final FileDownloadHelper.OutputStreamCreator outputStreamCreator = mMaker.mOutputStreamCreator;
        if (outputStreamCreator != null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "initial FileDownloader manager with the customize " +
                        "output stream: %s", outputStreamCreator);
            }
            return outputStreamCreator;
        } else {
            return createDefaultOutputStreamCreator();
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

    private FileDownloadHelper.OutputStreamCreator createDefaultOutputStreamCreator() {
        return new FileDownloadRandomAccessFile.Creator();
    }

    public static class InitCustomMaker {
        FileDownloadHelper.DatabaseCustomMaker mDatabaseCustomMaker;
        FileDownloadHelper.OkHttpClientCustomMaker mOkHttpClientCustomMaker;
        Integer mMaxNetworkThreadCount;
        FileDownloadHelper.OutputStreamCreator mOutputStreamCreator;

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

        /**
         * @param creator The output stream creator is used for creating {@link FileDownloadOutputStream}
         *                which is used to write the input stream to the file for downloading.
         */
        public InitCustomMaker outputStreamCreator(FileDownloadHelper.OutputStreamCreator creator) {
            this.mOutputStreamCreator = creator;
            return this;
        }

        private void securityCheck() {
            if (mOutputStreamCreator != null && !mOutputStreamCreator.supportSeek()) {
                if (!FileDownloadProperties.getImpl().FILE_NON_PRE_ALLOCATION) {
                    throw new IllegalArgumentException("Since the provided FileDownloadOutputStream " +
                            "does not support the seek function, if FileDownloader pre-allocates " +
                            "file size at the beginning of the download, it will can not be resumed" +
                            " from the breakpoint. If you need to ensure that the resumption is" +
                            " available, please add and set the value of 'file.non-pre-allocation' " +
                            "field to 'true' in the 'filedownloader.properties' file which is in your" +
                            " application assets folder manually for resolving this problem.");
                }
            }
        }
    }
}
