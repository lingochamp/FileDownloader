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

import com.liulishuo.filedownloader.util.FileDownloadHelper;

import okhttp3.OkHttpClient;

/**
 * Created by Jacksgong on 6/9/16.
 * <p/>
 * Params in this class is used in the download manager, and would be used for initialize the
 * download manager in the process which the download service settled on.
 */
public class DownloadMgrInitialParams {

    private FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker = null;
    private int maxNetworkThreadCount = 0;

    public DownloadMgrInitialParams(FileDownloadHelper.OkHttpClientCustomMaker maker,
                                    int maxNetworkThreadCount) {
        this.okHttpClientCustomMaker = maker;
        this.maxNetworkThreadCount = maxNetworkThreadCount;
    }

    OkHttpClient makeCustomOkHttpClient() {
        return this.okHttpClientCustomMaker != null ? this.okHttpClientCustomMaker.customMake() : null;
    }

    int getMaxNetworkThreadCount() {
        return this.maxNetworkThreadCount;
    }
}
