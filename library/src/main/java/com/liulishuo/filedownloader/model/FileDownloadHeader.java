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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * We have already handled Etag internal for guaranteeing tasks resuming from the breakpoint, in
 * other words, if the task has downloaded and got Etag, we will add the 'If-Match' and the 'Range'
 * K-V to its request header automatically.
 */
public class FileDownloadHeader implements Parcelable {

    private HashMap<String, List<String>> mHeaderMap;

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see com.liulishuo.filedownloader.download.ConnectTask#addUserRequiredHeader
     */
    public void add(String name, String value) {
        if (name == null) throw new NullPointerException("name == null");
        if (name.isEmpty()) throw new IllegalArgumentException("name is empty");
        if (value == null) throw new NullPointerException("value == null");

        if (mHeaderMap == null) {
            mHeaderMap = new HashMap<>();
        }

        List<String> values = mHeaderMap.get(name);
        if (values == null) {
            values = new ArrayList<>();
            mHeaderMap.put(name, values);
        }

        if (!values.contains(value)) {
            values.add(value);
        }
    }

    /**
     * We have already handled etag, and will add 'If-Match' & 'Range' value if it works.
     *
     * @see com.liulishuo.filedownloader.download.ConnectTask#addUserRequiredHeader
     */
    public void add(String line) {
        String[] parsed = line.split(":");
        final String name = parsed[0].trim();
        final String value = parsed[1].trim();

        add(name, value);
    }

    /**
     * Remove all files with the name.
     */
    public void removeAll(String name) {
        if (mHeaderMap == null) {
            return;
        }

        mHeaderMap.remove(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(mHeaderMap);
    }

    public HashMap<String, List<String>> getHeaders() {
        return mHeaderMap;
    }

    public FileDownloadHeader() {
    }

    protected FileDownloadHeader(Parcel in) {
        //noinspection unchecked
        this.mHeaderMap = in.readHashMap(String.class.getClassLoader());
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
        return mHeaderMap.toString();
    }
}
