/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

public class ServiceStatusModel implements Parcelable {

    private boolean isHandled;
    private boolean runServiceForeground;

    public ServiceStatusModel() {
    }

    public boolean isHandled() {
        return isHandled;
    }

    public void setHandled(boolean handled) {
        isHandled = handled;
    }

    public boolean isRunServiceForeground() {
        return runServiceForeground;
    }

    public void setRunServiceForeground(boolean runServiceForeground) {
        this.runServiceForeground = runServiceForeground;
    }

    protected ServiceStatusModel(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<ServiceStatusModel> CREATOR = new Creator<ServiceStatusModel>() {
        @Override
        public ServiceStatusModel createFromParcel(Parcel in) {
            return new ServiceStatusModel(in);
        }

        @Override
        public ServiceStatusModel[] newArray(int size) {
            return new ServiceStatusModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isHandled ? 1 : 0));
        dest.writeByte((byte) (runServiceForeground ? 1 : 0));
    }

    public void readFromParcel(Parcel in) {
        isHandled = in.readByte() != 0;
        runServiceForeground = in.readByte() != 0;
    }
}
