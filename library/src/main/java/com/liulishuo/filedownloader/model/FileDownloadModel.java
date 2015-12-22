package com.liulishuo.filedownloader.model;

import android.content.ContentValues;
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
 * Created by Jacksgong on 9/24/15.
 */
public class FileDownloadModel implements Parcelable {

    public final static int DEFAULT_CALLBACK_PROGRESS_TIMES = 100;

    // download id
    private int id;
    public final static String ID = "_id";

    // download url
    private String url;
    public final static String URL = "url";

    // save path
    private String path;
    public final static String PATH = "path";

    private boolean needNotification;
    public final static String NEED_NOTIFICATION = "needNotification";

    // notification title
    private String title;
    public final static String TITLE = "title";

    // notification desc
    private String desc;
    public final static String DESC = "desc";

    private int callbackProgressTimes = DEFAULT_CALLBACK_PROGRESS_TIMES;
    public final static String CALLBACK_PROGRESS_TIMES = "callbackProgressTimes";

    private int status;
    public final static String STATUS = "status";

    private int soFar;
    private int total;

    public final static String SOFAR = "sofar";
    public final static String TOTAL = "total";

    private String errMsg;
    public final static String ERR_MSG = "errMsg";

    // header
    private String eTag;
    public final static String ETAG = "etag";

    private boolean isCanceled = false;


    public void setId(int id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setNeedNotification(boolean needNotification) {
        this.needNotification = needNotification;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setSoFar(int soFar) {
        this.soFar = soFar;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public boolean isNeedNotification() {
        return needNotification;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public int getStatus() {
        return status;
    }

    public int getSoFar() {
        return soFar;
    }

    public int getTotal() {
        return total;
    }

    public int getCallbackProgressTimes() {
        return callbackProgressTimes;
    }

    public void setCallbackProgressTimes(int callbackProgressTimes) {
        this.callbackProgressTimes = callbackProgressTimes;
    }

    public String getETag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setIsCancel(boolean isCancel) {
        this.isCanceled = isCancel;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(ID, id);
        cv.put(URL, url);
        cv.put(PATH, path);
        cv.put(NEED_NOTIFICATION, needNotification ? 1 : 0);
        cv.put(TITLE, title);
        cv.put(DESC, desc);
        cv.put(CALLBACK_PROGRESS_TIMES, callbackProgressTimes);
        cv.put(STATUS, status);
        cv.put(SOFAR, soFar);
        cv.put(TOTAL, total);
        cv.put(ERR_MSG, errMsg);
        cv.put(ETAG, eTag);
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.url);
        dest.writeString(this.path);
        dest.writeByte(needNotification ? (byte) 1 : (byte) 0);
        dest.writeString(this.title);
        dest.writeString(this.desc);
        dest.writeInt(this.callbackProgressTimes);
        dest.writeInt(this.status);
        dest.writeInt(this.soFar);
        dest.writeInt(this.total);
        dest.writeString(this.errMsg);
        dest.writeString(this.eTag);
        dest.writeByte(isCanceled ? (byte) 1 : (byte) 0);
    }

    public FileDownloadModel() {
    }

    protected FileDownloadModel(Parcel in) {
        this.id = in.readInt();
        this.url = in.readString();
        this.path = in.readString();
        this.needNotification = in.readByte() != 0;
        this.title = in.readString();
        this.desc = in.readString();
        this.callbackProgressTimes = in.readInt();
        this.status = in.readInt();
        this.soFar = in.readInt();
        this.total = in.readInt();
        this.errMsg = in.readString();
        this.eTag = in.readString();
        this.isCanceled = in.readByte() != 0;
    }

    public static final Creator<FileDownloadModel> CREATOR = new Creator<FileDownloadModel>() {
        public FileDownloadModel createFromParcel(Parcel source) {
            return new FileDownloadModel(source);
        }

        public FileDownloadModel[] newArray(int size) {
            return new FileDownloadModel[size];
        }
    };
}
