package com.liulishuo.filedownloader.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Copyright (c) 2015 LingoChamp Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
