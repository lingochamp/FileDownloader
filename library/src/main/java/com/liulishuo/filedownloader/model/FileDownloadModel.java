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

import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * The model of the downloading task will be used in the filedownloader database.
 *
 * @see com.liulishuo.filedownloader.services.FileDownloadDatabase
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloadModel {

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

    public String getPath() {
        return path;
    }

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

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("id[%d], url[%s], path[%s], status[%d], sofar[%d]," +
                        " total[%d], etag[%s], %s", id, url, path, status, soFar, total, eTag,
                super.toString());
    }
}
