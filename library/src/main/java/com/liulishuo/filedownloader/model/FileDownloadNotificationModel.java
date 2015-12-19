package com.liulishuo.filedownloader.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jacksgong on 9/23/15.
 */
public class FileDownloadNotificationModel implements Parcelable {

    private boolean isNeed = false;
    private String title;
    private String desc;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isNeed ? (byte) 1 : (byte) 0);
        dest.writeString(this.title);
        dest.writeString(this.desc);
    }

    public FileDownloadNotificationModel(final boolean isNeed, final String title, final String desc) {
        this.isNeed = isNeed;
        this.title = title;
        this.desc = desc;
    }

    protected FileDownloadNotificationModel(Parcel in) {
        this.isNeed = in.readByte() != 0;
        this.title = in.readString();
        this.desc = in.readString();
    }

    public static final Parcelable.Creator<FileDownloadNotificationModel> CREATOR = new Parcelable.Creator<FileDownloadNotificationModel>() {
        public FileDownloadNotificationModel createFromParcel(Parcel source) {
            return new FileDownloadNotificationModel(source);
        }

        public FileDownloadNotificationModel[] newArray(int size) {
            return new FileDownloadNotificationModel[size];
        }
    };

    public boolean isNeed() {
        return isNeed;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }
}
