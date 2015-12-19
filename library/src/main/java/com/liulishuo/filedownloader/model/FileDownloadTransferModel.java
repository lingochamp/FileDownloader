package com.liulishuo.filedownloader.model;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by Jacksgong on 9/23/15.
 *
 * ui进程与:downloader进程 相互通信对象
 */
public class FileDownloadTransferModel implements Parcelable {
    // TODO 名称修改

    private int status;
    private int downloadId;
    private int sofarBytes;
    private int totalBytes;

    private Throwable throwable;

    public FileDownloadTransferModel(final FileDownloadModel model) {
        this.status = model.getStatus();
        this.downloadId = model.getId();
        this.sofarBytes = model.getSoFar();
        this.totalBytes = model.getTotal();
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

    public int getSofarBytes() {
        return sofarBytes;
    }

    public void setSofarBytes(int sofarBytes) {
        this.sofarBytes = sofarBytes;
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.status);
        dest.writeInt(this.downloadId);
        dest.writeInt(this.sofarBytes);
        dest.writeInt(this.totalBytes);
        dest.writeSerializable(this.throwable);
    }

    protected FileDownloadTransferModel(Parcel in) {
        this.status = in.readInt();
        this.downloadId = in.readInt();
        this.sofarBytes = in.readInt();
        this.totalBytes = in.readInt();
        this.throwable = (Throwable) in.readSerializable();
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
