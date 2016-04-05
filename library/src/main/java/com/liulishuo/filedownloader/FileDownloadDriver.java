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

import com.liulishuo.filedownloader.util.FileDownloadLog;

/**
 * Created by Jacksgong on 12/21/15.
 * <p/>
 * The driver is used to notify to the {@link FileDownloadListener}.
 */
class FileDownloadDriver implements IFileDownloadMessage {

    private final BaseDownloadTask download;

    FileDownloadDriver(final BaseDownloadTask download) {
        this.download = download;
    }

    // Start state, from FileDownloadList, to addEventListener ---------------
    @Override
    public void notifyStarted() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify started %s", download);
        }

        download.begin();
    }

    // in-between state, from BaseDownloadTask#update, to user ---------------------------
    @Override
    public void notifyPending() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify pending %s", download);
        }

        download.ing();

        final FileDownloadEvent event = download.getIngEvent().pending();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    @Override
    public void notifyConnected() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify connected %s", download);
        }

        download.ing();

        final FileDownloadEvent event = download.getIngEvent().connected();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    @Override
    public void notifyProgress() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify progress %s %d %d",
                    download, download.getLargeFileSoFarBytes(), download.getLargeFileTotalBytes());
        }
        if (download.getCallbackProgressTimes() <= 0) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "notify progress but client not request notify %s", download);
            }
            return;
        }

        download.ing();

        final FileDownloadEvent event = download.getIngEvent().progress();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    /**
     * sync
     */
    @Override
    public void notifyBlockComplete() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify block completed %s %s", download, Thread.currentThread().getName());
        }

        download.ing();

        FileDownloadEventPool.getImpl().publish(download.getIngEvent()
                .blockComplete());
    }

    @Override
    public void notifyRetry() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify retry %s %d %d %s", download,
                    download.getAutoRetryTimes(), download.getRetryingTimes(), download.getEx());
        }

        download.ing();

        final FileDownloadEvent event = download.getIngEvent().retry();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    // Over state, from FileDownloadList, to user -----------------------------
    @Override
    public void notifyWarn() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify warn %s", download);
        }

        download.over();

        final FileDownloadEvent event = download.getOverEvent().warn();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    @Override
    public void notifyError() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify error %s %s", download, download.getEx());
        }

        download.over();

        final FileDownloadEvent event = download.getOverEvent().error();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }

    }

    @Override
    public void notifyPaused() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify paused %s", download);
        }

        download.over();

        final FileDownloadEvent event = download.getOverEvent().pause();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }
    }

    @Override
    public void notifyCompleted() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "notify completed %s", download);
        }

        download.over();

        final FileDownloadEvent event = download.getOverEvent().complete();

        if (download.isSyncCallback()) {
            FileDownloadEventPool.getImpl().publish(event);
        } else {
            FileDownloadEventPool.getImpl().send2UIThread(event);
        }
    }
}
