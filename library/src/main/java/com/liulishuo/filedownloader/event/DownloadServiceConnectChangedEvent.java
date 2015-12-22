package com.liulishuo.filedownloader.event;


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
public class DownloadServiceConnectChangedEvent extends IDownloadEvent {
    public final static String ID = "event.service.connect.changed";

    public DownloadServiceConnectChangedEvent(final ConnectStatus status, final Class<?> serviceClass) {
        super(ID);

        this.status = status;
        this.serviceClass = serviceClass;
    }

    private ConnectStatus status;

    public enum ConnectStatus {
        connected, disconnected
    }

    public ConnectStatus getStatus() {
        return status;
    }


    private Class<?> serviceClass;

    public boolean isSuchService(final Class<?> serviceClass) {
        if (serviceClass == null) {
            return false;
        }

        return this.serviceClass.getName().equals(serviceClass.getName());

    }
}
