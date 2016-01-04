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

package com.liulishuo.filedownloader.demo.performance;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jacksgong on 1/4/16.
 */
public class LongParcel implements Parcelable {

    private long v1 = Integer.MAX_VALUE;
    private long v2 = Integer.MAX_VALUE;
    private long v3 = Integer.MAX_VALUE;

    public void operate() {
        v1 -= 11;
        v2 -= 12;
        v3 -= 13;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.v1);
        dest.writeLong(this.v2);
        dest.writeLong(this.v3);
    }

    public LongParcel() {
    }

    protected LongParcel(Parcel in) {
        this.v1 = in.readLong();
        this.v2 = in.readLong();
        this.v3 = in.readLong();
    }

    public static final Creator<LongParcel> CREATOR = new Creator<LongParcel>() {
        public LongParcel createFromParcel(Parcel source) {
            return new LongParcel(source);
        }

        public LongParcel[] newArray(int size) {
            return new LongParcel[size];
        }
    };
}
