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

package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.FileDownloadEventPool;
import com.liulishuo.filedownloader.util.FileDownloadLog;

/**
 * Created by Jacksgong on 12/21/15.
 */
class FileDownloadDriver implements IFileDownloadMessage {

    private final BaseDownloadTask download;

    FileDownloadDriver(final BaseDownloadTask download) {
        this.download = download;
    }

    // Start state, from FileDownloadList, to addEventListener ---------------
    @Override
    public void notifyStarted() {
        FileDownloadLog.d(this, "notify started %s", download);

        download.begin();
    }

    // in-between state, from BaseDownloadTask#update, to user ---------------------------
    @Override
    public void notifyPending() {
        FileDownloadLog.d(this, "notify pending %s", download);

        download.ing();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
                .pending());

    }

    @Override
    public void notifyConnected() {
        FileDownloadLog.d(this, "notify connected %s", download);

        download.ing();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
                .connected());

    }

    @Override
    public void notifyProgress() {
        FileDownloadLog.d(this, "notify progress %s %d %d", download, download.getLargeFileSoFarBytes(), download.getLargeFileTotalBytes());
        if (download.getCallbackProgressTimes() <= 0) {
            FileDownloadLog.d(this, "notify progress but client not request notify %s", download);
            return;
        }

        download.ing();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
                .progress());

    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete() {
        FileDownloadLog.d(this, "notify block completed %s %s", download, Thread.currentThread().getName());

        download.ing();

        FileDownloadEventPool.getImpl().publish(download.getIngEvent()
                .blockComplete());
    }

    @Override
    public void notifyRetry() {
        FileDownloadLog.d(this, "notify retry %s %d %d %s", download, download.getAutoRetryTimes(), download.getRetryingTimes(), download.getEx());

        download.ing();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getIngEvent()
                .retry());

    }

    // Over state, from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn() {
        FileDownloadLog.d(this, "notify warn %s", download);

        download.over();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent()
                .warn());

    }

    @Override
    public void notifyError() {
        FileDownloadLog.e(this, download.getEx(), "notify error %s", download);

        download.over();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent()
                .error());
    }

    @Override
    public void notifyPaused() {
        FileDownloadLog.d(this, "notify paused %s", download);

        download.over();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent()
                .pause());
    }

    @Override
    public void notifyCompleted() {
        FileDownloadLog.d(this, "notify completed %s", download);

        download.over();

        FileDownloadEventPool.getImpl().asyncPublishInMain(download.getOverEvent()
                .complete());
    }
}
