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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The holder for supported custom components.
 */
public class CustomComponentHolder {
    private DownloadMgrInitialParams mMgrInitialParams;
    private FileDownloadDatabase database;

    private final static class LazyLoader {
        private final static CustomComponentHolder INSTANCE = new CustomComponentHolder();
    }

    public static CustomComponentHolder getImpl() {
        return LazyLoader.INSTANCE;
    }

    public void setInitCustomMaker(DownloadMgrInitialParams.InitCustomMaker initCustomMaker) {
        mMgrInitialParams = new DownloadMgrInitialParams(initCustomMaker);
    }

    private DownloadMgrInitialParams getDownloadMgrInitialParams() {
        if (mMgrInitialParams == null) mMgrInitialParams = new DownloadMgrInitialParams();
        return mMgrInitialParams;
    }

    public FileDownloadConnection createConnection(String url) throws IOException {
        return getDownloadMgrInitialParams().createConnectionCreator().create(url);
    }

    public FileDownloadOutputStream createOutputStream(File file) throws FileNotFoundException {
        return getDownloadMgrInitialParams().createOutputStreamCreator().create(file);
    }

    public FileDownloadDatabase getDatabaseInstance() {
        if (database != null) return database;

        synchronized (this) {
            if (database == null) {
                database = getDownloadMgrInitialParams().createDatabase();
            }
        }

        return database;
    }

    public int getMaxNetworkThreadCount() {
        return getDownloadMgrInitialParams().getMaxNetworkThreadCount();
    }

    public boolean isSupportSeek() {
        return getDownloadMgrInitialParams().createOutputStreamCreator().supportSeek();
    }
}
