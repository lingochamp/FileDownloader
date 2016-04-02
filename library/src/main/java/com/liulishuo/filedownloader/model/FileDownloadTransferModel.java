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

import junit.framework.Assert;


/**
 * Created by Jacksgong on 9/23/15.
 * <p/>
 * ui进程与:downloader进程 相互通信对象
 */
public class FileDownloadTransferModel implements Parcelable {

    private byte status;
    private int downloadId;
    private long soFarBytes;

    // Total bytes
    private long totalBytes;
    // Whether Resume from the breakpoint
    private boolean isContinue;
    // ETag
    private String etag;

    // Error
    private Throwable throwable;

    // Number of times to try again, [1, &]
    private int retryingTimes;

    // reused old file
    private boolean reusedOldFile = false;

    public FileDownloadTransferModel(final FileDownloadModel model) {
        this.status = model.getStatus();
        this.downloadId = model.getId();
        this.soFarBytes = model.getSoFar();
        this.totalBytes = model.getTotal();
        this.etag = model.getETag();
    }

    public int getRetryingTimes() {
        return retryingTimes;
    }

    public void setRetryingTimes(int retryingTimes) {
        this.retryingTimes = retryingTimes;
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

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    public long getSoFarBytes() {
        return soFarBytes;
    }

    public void setSoFarBytes(long soFarBytes) {
        this.soFarBytes = soFarBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public boolean isReusedOldFile() {
        return reusedOldFile;
    }

    public void markAsReusedOldFile() {
        Assert.assertEquals("you can't mark the status as reused old file, when the status isn't" +
                " equal completed", getStatus(), FileDownloadStatus.completed);
        this.reusedOldFile = true;
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
        dest.writeByte(this.status);
        dest.writeInt(this.downloadId);

        // For fewer copies
        switch (this.status) {
            case FileDownloadStatus.pending:
                dest.writeLong(this.soFarBytes);
                dest.writeLong(this.totalBytes);
                break;
            case FileDownloadStatus.connected:
                dest.writeLong(this.soFarBytes);
                dest.writeLong(this.totalBytes);
                dest.writeString(this.etag);
                dest.writeByte(isContinue ? (byte) 1 : (byte) 0);
                break;
            case FileDownloadStatus.progress:
                dest.writeLong(this.soFarBytes);
                break;
            case FileDownloadStatus.error:
                dest.writeLong(this.soFarBytes);
                dest.writeSerializable(this.throwable);
                break;
            case FileDownloadStatus.retry:
                dest.writeLong(this.soFarBytes);
                dest.writeSerializable(this.throwable);
                dest.writeInt(this.retryingTimes);
                break;
            case FileDownloadStatus.completed:
                dest.writeLong(this.totalBytes);
                dest.writeByte(reusedOldFile ? (byte) 1 : (byte) 0);
                if (reusedOldFile) {
                    dest.writeString(this.etag);
                }
                break;
        }
    }

    /**
     * @see com.liulishuo.filedownloader.BaseDownloadTask#update(FileDownloadTransferModel)
     */
    protected FileDownloadTransferModel(Parcel in) {
        this.status = in.readByte();
        this.downloadId = in.readInt();

        // For fewer copies
        switch (this.status) {
            case FileDownloadStatus.pending:
                this.soFarBytes = in.readLong();
                this.totalBytes = in.readLong();
                break;
            case FileDownloadStatus.connected:
                this.soFarBytes = in.readLong();
                this.totalBytes = in.readLong();
                this.etag = in.readString();
                this.isContinue = in.readByte() == 1;
                break;
            case FileDownloadStatus.progress:
                this.soFarBytes = in.readLong();
                break;
            case FileDownloadStatus.error:
                this.soFarBytes = in.readLong();
                this.throwable = (Throwable) in.readSerializable();
                break;
            case FileDownloadStatus.retry:
                this.soFarBytes = in.readLong();
                this.throwable = (Throwable) in.readSerializable();
                this.retryingTimes = in.readInt();
                break;
            case FileDownloadStatus.completed:
                this.totalBytes = in.readLong();
                this.reusedOldFile = in.readByte() == 1;
                if (this.reusedOldFile) {
                    this.etag = in.readString();
                }
                break;
        }
    }

    public FileDownloadTransferModel copy() {
        final FileDownloadTransferModel model = new FileDownloadTransferModel();

        model.status = this.status;
        model.downloadId = this.downloadId;
        model.soFarBytes = this.soFarBytes;
        model.totalBytes = this.totalBytes;
        model.etag = this.etag;
        model.isContinue = this.isContinue;
        model.throwable = this.throwable;
        model.retryingTimes = this.retryingTimes;

        return model;
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
