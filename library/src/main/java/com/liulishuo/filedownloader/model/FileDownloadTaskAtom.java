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

import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.List;

/**
 * The Minimal unit for a task.
 * <p/>
 * Used for telling the FileDownloader Engine that a task was downloaded by the other ways.
 *
 * @see com.liulishuo.filedownloader.FileDownloader#setTaskCompleted(List)
 * @deprecated No used. {@link com.liulishuo.filedownloader.FileDownloader#setTaskCompleted(String, String, long)}
 */
@SuppressWarnings({"WeakerAccess", "deprecation", "DeprecatedIsStillUsed"})
public class FileDownloadTaskAtom implements Parcelable {
    private String url;
    private String path;
    private long totalBytes;

    public FileDownloadTaskAtom(String url, String path, long totalBytes) {
        setUrl(url);
        setPath(path);
        setTotalBytes(totalBytes);
    }

    private int id;

    public int getId() {
        if (id != 0) {
            return id;
        }

        return id = FileDownloadUtils.generateId(getUrl(), getPath());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.path);
        dest.writeLong(this.totalBytes);
    }

    protected FileDownloadTaskAtom(Parcel in) {
        this.url = in.readString();
        this.path = in.readString();
        this.totalBytes = in.readLong();
    }

    public static final Parcelable.Creator<FileDownloadTaskAtom> CREATOR = new Parcelable.Creator<FileDownloadTaskAtom>() {
        @Override
        public FileDownloadTaskAtom createFromParcel(Parcel source) {
            return new FileDownloadTaskAtom(source);
        }

        @Override
        public FileDownloadTaskAtom[] newArray(int size) {
            return new FileDownloadTaskAtom[size];
        }
    };
}
