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
public class IntParcel implements Parcelable {

    private int v1 = Integer.MAX_VALUE;
    private int v2 = Integer.MAX_VALUE;
    private int v3 = Integer.MAX_VALUE;


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
        dest.writeInt(this.v1);
        dest.writeInt(this.v2);
        dest.writeInt(this.v3);
    }

    public IntParcel() {
    }

    protected IntParcel(Parcel in) {
        this.v1 = in.readInt();
        this.v2 = in.readInt();
        this.v3 = in.readInt();
    }

    public static final Creator<IntParcel> CREATOR = new Creator<IntParcel>() {
        public IntParcel createFromParcel(Parcel source) {
            return new IntParcel(source);
        }

        public IntParcel[] newArray(int size) {
            return new IntParcel[size];
        }
    };
}
