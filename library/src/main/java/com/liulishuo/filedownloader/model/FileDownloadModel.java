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

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

/**
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

    private int callbackProgressTimes = DEFAULT_CALLBACK_PROGRESS_TIMES;
    public final static String CALLBACK_PROGRESS_TIMES = "callbackProgressTimes";

    private byte status;
    public final static String STATUS = "status";

    private long soFar;
    private long total;

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

    public void setStatus(byte status) {
        this.status = status;
    }

    public void setSoFar(long soFar) {
        this.soFar = soFar;
    }

    public void setTotal(long total) {
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

    public byte getStatus() {
        return status;
    }

    public long getSoFar() {
        return soFar;
    }

    public long getTotal() {
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

    public void setETag(String eTag) {
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
        dest.writeInt(this.callbackProgressTimes);
        dest.writeByte(this.status);
        dest.writeInt(this.status);
        dest.writeLong(this.soFar);
        dest.writeLong(this.total);
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
        this.callbackProgressTimes = in.readInt();
        this.status = in.readByte();
        this.soFar = in.readLong();
        this.total = in.readLong();
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
