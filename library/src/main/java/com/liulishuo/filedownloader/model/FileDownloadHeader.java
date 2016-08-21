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

import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * We have already handled Etag internal for guaranteeing tasks resuming from the breakpoint, in
 * other words, if the task has downloaded and got Etag, we will add the 'If-Match' and the 'Range'
 * K-V to its request header automatically.
 */
public class FileDownloadHeader implements Parcelable {

    private Headers.Builder headerBuilder;

    private String nameAndValuesString;
    private String[] namesAndValues;

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#addHeader(Request.Builder)
     * @see okhttp3.Headers.Builder#add(String, String)
     */
    public void add(String name, String value) {
        if (headerBuilder == null) {
            headerBuilder = new Headers.Builder();
        }

        headerBuilder.add(name, value);
    }

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#addHeader(Request.Builder)
     * @see okhttp3.Headers.Builder#add(String, String)
     */
    public void add(String line) {
        if (headerBuilder == null) {
            headerBuilder = new Headers.Builder();
        }
        headerBuilder.add(line);
    }

    /**
     * @see okhttp3.Headers.Builder#removeAll(String)
     */
    public void removeAll(String name) {
        if (headerBuilder == null) {
            return;
        }

        headerBuilder.removeAll(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        checkAndInitValues();
        dest.writeString(nameAndValuesString);
    }

    private void checkAndInitValues() {
        if (headerBuilder != null) {
            nameAndValuesString = headerBuilder.build().toString();
        }
    }

    public Headers getHeaders() {
        if (!FileDownloadProperties.getImpl().PROCESS_NON_SEPARATE) {
            throw new IllegalStateException("the headers object isn't accessible, when the " +
                    "FileDownloadService in the separate process to UI process.");
        }

        return headerBuilder == null ? null : headerBuilder.build();
    }

    public FileDownloadHeader() {
    }

    protected FileDownloadHeader(Parcel in) {
        this.nameAndValuesString = in.readString();
    }

    /**
     * Invoke by :filedownloader progress
     *
     * @return for {@link Headers#Headers(String[])}
     * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#addHeader(Request.Builder)
     */
    public String[] getNamesAndValues() {

        do {
            if (namesAndValues != null) {
                // has already converted.
                break;
            }

            if (nameAndValuesString == null) {
                // give up
                break;
            }

            synchronized (this) {
                if (namesAndValues != null) {
                    break;
                }

                namesAndValues = FileDownloadUtils.convertHeaderString(nameAndValuesString);
            }


        } while (false);

        return namesAndValues;
    }

    public static final Creator<FileDownloadHeader> CREATOR = new Creator<FileDownloadHeader>() {
        public FileDownloadHeader createFromParcel(Parcel source) {
            return new FileDownloadHeader(source);
        }

        public FileDownloadHeader[] newArray(int size) {
            return new FileDownloadHeader[size];
        }
    };

    @Override
    public String toString() {
        return this.nameAndValuesString;
    }
}
