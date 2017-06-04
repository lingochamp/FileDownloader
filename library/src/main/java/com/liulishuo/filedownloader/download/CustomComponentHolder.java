/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.download;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;
import com.liulishuo.filedownloader.services.FileDownloadDatabase;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The holder for supported custom components.
 */
public class CustomComponentHolder {
    private DownloadMgrInitialParams initialParams;

    private FileDownloadHelper.ConnectionCountAdapter connectionCountAdapter;
    private FileDownloadHelper.ConnectionCreator connectionCreator;
    private FileDownloadHelper.OutputStreamCreator outputStreamCreator;
    private FileDownloadDatabase database;

    private final static class LazyLoader {
        private final static CustomComponentHolder INSTANCE = new CustomComponentHolder();
    }

    public static CustomComponentHolder getImpl() {
        return LazyLoader.INSTANCE;
    }

    public void setInitCustomMaker(DownloadMgrInitialParams.InitCustomMaker initCustomMaker) {
        synchronized (this) {
            initialParams = new DownloadMgrInitialParams(initCustomMaker);
            connectionCreator = null;
            outputStreamCreator = null;
            database = null;
        }
    }

    public FileDownloadConnection createConnection(String url) throws IOException {
        return getConnectionCreator().create(url);
    }

    public FileDownloadOutputStream createOutputStream(File file) throws FileNotFoundException {
        return getOutputStreamCreator().create(file);
    }

    public FileDownloadDatabase getDatabaseInstance() {
        if (database != null) return database;

        synchronized (this) {
            if (database == null) database = getDownloadMgrInitialParams().createDatabase();
        }

        return database;
    }

    public int getMaxNetworkThreadCount() {
        return getDownloadMgrInitialParams().getMaxNetworkThreadCount();
    }

    public boolean isSupportSeek() {
        return getOutputStreamCreator().supportSeek();
    }

    public int determineConnectionCount(int downloadId, String url, String path, long totalLength) {
        return getConnectionCountAdapter().determineConnectionCount(downloadId, url, path, totalLength);
    }

    private FileDownloadHelper.ConnectionCountAdapter getConnectionCountAdapter() {
        if (connectionCountAdapter != null) return connectionCountAdapter;

        synchronized (this) {
            if (connectionCountAdapter == null)
                connectionCountAdapter = getDownloadMgrInitialParams().createConnectionCountAdapter();
        }

        return connectionCountAdapter;
    }

    private FileDownloadHelper.ConnectionCreator getConnectionCreator() {
        if (connectionCreator != null) return connectionCreator;

        synchronized (this) {
            if (connectionCreator == null)
                connectionCreator = getDownloadMgrInitialParams().createConnectionCreator();
        }

        return connectionCreator;
    }

    private FileDownloadHelper.OutputStreamCreator getOutputStreamCreator() {
        if (outputStreamCreator != null) return outputStreamCreator;

        synchronized (this) {
            if (outputStreamCreator == null)
                outputStreamCreator = getDownloadMgrInitialParams().createOutputStreamCreator();
        }

        return outputStreamCreator;
    }

    private DownloadMgrInitialParams getDownloadMgrInitialParams() {
        if (initialParams != null) return initialParams;

        synchronized (this) {
            if (initialParams == null) initialParams = new DownloadMgrInitialParams();
        }

        return initialParams;
    }

}
