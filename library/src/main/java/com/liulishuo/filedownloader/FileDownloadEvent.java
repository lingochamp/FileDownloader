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


import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 9/7/15.
 */
class FileDownloadEvent extends IDownloadEvent {

    public FileDownloadEvent(final BaseDownloadTask downloader) {
        super(downloader.generateEventId());
        this.downloader = downloader;
    }

    private final BaseDownloadTask downloader;

    private byte status;

    public FileDownloadEvent progress() {
        this.status = FileDownloadStatus.progress;

        return this;
    }

    public FileDownloadEvent complete() {
        this.status = FileDownloadStatus.completed;
        return this;
    }

    public FileDownloadEvent error() {
        this.status = FileDownloadStatus.error;
        return this;
    }


    public FileDownloadEvent pause() {
        this.status = FileDownloadStatus.paused;
        return this;
    }

    public FileDownloadEvent pending() {
        this.status = FileDownloadStatus.pending;
        return this;
    }

    public FileDownloadEvent connected() {
        this.status = FileDownloadStatus.connected;
        return this;
    }

    public FileDownloadEvent blockComplete() {
        this.status = FileDownloadStatus.blockComplete;
        return this;
    }

    public FileDownloadEvent retry(){
        this.status = FileDownloadStatus.retry;
        return this;
    }

    public FileDownloadEvent warn() {
        this.status = FileDownloadStatus.warn;
        return this;
    }

    public FileDownloadEvent callback(Runnable runnable) {
        this.callback = runnable;
        return this;
    }

    public BaseDownloadTask getDownloader() {
        return this.downloader;
    }

    public byte getStatus() {
        return this.status;
    }
}
