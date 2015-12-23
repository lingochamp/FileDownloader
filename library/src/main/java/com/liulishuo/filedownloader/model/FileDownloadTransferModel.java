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

package com.liulishuo.filedownloader.model;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by Jacksgong on 9/23/15.
 * <p/>
 * ui进程与:downloader进程 相互通信对象
 */
public class FileDownloadTransferModel implements Parcelable {

    private int status;
    private int downloadId;
    private int soFarBytes;

    // ----  只有在连接上的时候带回
    // 总大小
    private int totalBytes;
    // 是否是断点续传
    private boolean isContinue;
    // ETag
    private String etag;
    // ----

    // ---- 只在错误的时候带回
    // 错误
    private Throwable throwable;

    public FileDownloadTransferModel(final FileDownloadModel model) {
        this.status = model.getStatus();
        this.downloadId = model.getId();
        this.soFarBytes = model.getSoFar();
        this.totalBytes = model.getTotal();
        this.etag = model.getETag();
    }

    public boolean isContinue() {
        return isContinue;
    }

    public void setIsContinue(boolean isContinue) {
        this.isContinue = isContinue;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    public int getSoFarBytes() {
        return soFarBytes;
    }

    public void setSoFarBytes(int soFarBytes) {
        this.soFarBytes = soFarBytes;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(int totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public FileDownloadTransferModel() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @see com.liulishuo.filedownloader.BaseDownloadTask#update(FileDownloadTransferModel)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.status);
        dest.writeInt(this.downloadId);

        // 为了频繁拷贝的时候不带上
        switch (this.status) {
            case FileDownloadStatus.pending:
                dest.writeInt(this.soFarBytes);
                dest.writeInt(this.totalBytes);
                break;
            case FileDownloadStatus.connected:
                dest.writeInt(this.soFarBytes);
                dest.writeInt(this.totalBytes);
                dest.writeString(this.etag);
                dest.writeByte(isContinue ? (byte) 1 : (byte) 0);
                break;
            case FileDownloadStatus.progress:
                dest.writeInt(this.soFarBytes);
                break;
            case FileDownloadStatus.error:
                dest.writeInt(this.soFarBytes);
                dest.writeSerializable(this.throwable);
                break;
            case FileDownloadStatus.completed:
                dest.writeInt(this.totalBytes);
                break;
        }
    }

    /**
     * @see com.liulishuo.filedownloader.BaseDownloadTask#update(FileDownloadTransferModel)
     */
    protected FileDownloadTransferModel(Parcel in) {
        this.status = in.readInt();
        this.downloadId = in.readInt();

        // 为了频繁拷贝的时候不带上
        switch (this.status) {
            case FileDownloadStatus.pending:
                this.soFarBytes = in.readInt();
                this.totalBytes = in.readInt();
                break;
            case FileDownloadStatus.connected:
                this.soFarBytes = in.readInt();
                this.totalBytes = in.readInt();
                this.etag = in.readString();
                this.isContinue = in.readByte() == 1;
                break;
            case FileDownloadStatus.progress:
                this.soFarBytes = in.readInt();
                break;
            case FileDownloadStatus.error:
                this.soFarBytes = in.readInt();
                this.throwable = (Throwable) in.readSerializable();
                break;
            case FileDownloadStatus.completed:
                this.totalBytes = in.readInt();
                break;
        }
    }

    public static final Creator<FileDownloadTransferModel> CREATOR = new Creator<FileDownloadTransferModel>() {
        public FileDownloadTransferModel createFromParcel(Parcel source) {
            return new FileDownloadTransferModel(source);
        }

        public FileDownloadTransferModel[] newArray(int size) {
            return new FileDownloadTransferModel[size];
        }
    };
}
