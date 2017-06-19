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

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * The model of the downloading task will be used in the filedownloader database.
 *
 * @see com.liulishuo.filedownloader.services.FileDownloadDatabase
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloadModel implements Parcelable {

    public static final int TOTAL_VALUE_IN_CHUNKED_RESOURCE = -1;
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


    private boolean pathAsDirectory;
    public final static String PATH_AS_DIRECTORY = "pathAsDirectory";

    private String filename;
    public final static String FILENAME = "filename";

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

    private int connectionCount;
    public final static String CONNECTION_COUNT = "connectionCount";

    public void setId(int id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPath(String path, boolean pathAsDirectory) {
        this.path = path;
        this.pathAsDirectory = pathAsDirectory;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public void setSoFar(long soFar) {
        this.soFar = soFar;
    }

    public void setTotal(long total) {
        this.isLargeFile = total > Integer.MAX_VALUE;
        this.total = total;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Get the path user set from {@link BaseDownloadTask#setPath(String)}
     *
     * @return the path user set from {@link BaseDownloadTask#setPath(String)}
     * @see #getTargetFilePath()
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the finally target file path is used for store the download file.
     * <p/>
     * This path is composited with {@link #path}、{@link #pathAsDirectory}、{@link #filename}.
     * <p/>
     * Why {@link #getPath()} may be not equal to getTargetFilePath()? this case only occurred
     * when the {@link #isPathAsDirectory()} is {@code true}, on this scenario the {@link #getPath()}
     * is directory, and the getTargetFilePath() is 'directory + "/" + filename'.
     *
     * @return the finally target file path.
     */
    public String getTargetFilePath() {
        return FileDownloadUtils.getTargetFilePath(getPath(), isPathAsDirectory(), getFilename());
    }

    public String getTempFilePath() {
        if (getTargetFilePath() == null) {
            return null;
        }
        return FileDownloadUtils.getTempPath(getTargetFilePath());
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

    public boolean isChunked() {
        return total == TOTAL_VALUE_IN_CHUNKED_RESOURCE;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isPathAsDirectory() {
        return pathAsDirectory;
    }

    public String getFilename() {
        return filename;
    }

    public void setConnectionCount(int connectionCount) {
        this.connectionCount = connectionCount;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    /**
     * reset the connection count to default value: 1.
     */
    public void resetConnectionCount() {
        this.connectionCount = 1;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(ID, getId());
        cv.put(URL, getUrl());
        cv.put(PATH, getPath());
        cv.put(STATUS, getStatus());
        cv.put(SOFAR, getSoFar());
        cv.put(TOTAL, getTotal());
        cv.put(ERR_MSG, getErrMsg());
        cv.put(ETAG, getETag());
        cv.put(CONNECTION_COUNT, getConnectionCount());
        cv.put(PATH_AS_DIRECTORY, isPathAsDirectory());
        if (isPathAsDirectory() && getFilename() != null) {
            cv.put(FILENAME, getFilename());
        }

        return cv;
    }


    private boolean isLargeFile;

    public boolean isLargeFile() {
        return isLargeFile;
    }

    public void deleteTaskFiles() {
        deleteTempFile();
        deleteTargetFile();
    }

    public void deleteTempFile() {
        final String tempFilePath = getTempFilePath();

        if (tempFilePath != null) {
            final File tempFile = new File(tempFilePath);
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public void deleteTargetFile() {
        final String targetFilePath = getTargetFilePath();
        if (targetFilePath != null) {
            final File targetFile = new File(targetFilePath);
            if (targetFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
            }
        }
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("id[%d], url[%s], path[%s], status[%d], sofar[%d]," +
                        " total[%d], etag[%s], %s", id, url, path, status, soFar, total, eTag,
                super.toString());
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
        dest.writeByte(this.pathAsDirectory ? (byte) 1 : (byte) 0);
        dest.writeString(this.filename);
        dest.writeByte(this.status);
        dest.writeLong(this.soFar);
        dest.writeLong(this.total);
        dest.writeString(this.errMsg);
        dest.writeString(this.eTag);
        dest.writeInt(this.connectionCount);
        dest.writeByte(this.isLargeFile ? (byte) 1 : (byte) 0);
    }

    public FileDownloadModel() {
    }

    protected FileDownloadModel(Parcel in) {
        this.id = in.readInt();
        this.url = in.readString();
        this.path = in.readString();
        this.pathAsDirectory = in.readByte() != 0;
        this.filename = in.readString();
        this.status = in.readByte();
        this.soFar = in.readLong();
        this.total = in.readLong();
        this.errMsg = in.readString();
        this.eTag = in.readString();
        this.connectionCount = in.readInt();
        this.isLargeFile = in.readByte() != 0;
    }

    public static final Parcelable.Creator<FileDownloadModel> CREATOR = new Parcelable.Creator<FileDownloadModel>() {
        @Override
        public FileDownloadModel createFromParcel(Parcel source) {
            return new FileDownloadModel(source);
        }

        @Override
        public FileDownloadModel[] newArray(int size) {
            return new FileDownloadModel[size];
        }
    };
}
