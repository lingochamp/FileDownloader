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

package com.liulishuo.filedownloader.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.liulishuo.filedownloader.util.FileDownloadLog;


/**
 * Created by Jacksgong on 8/6/15.
 */
public abstract class BaseFileService<CALLBACK extends IInterface, BINDER extends Binder> extends Service {

    private final RemoteCallbackList<CALLBACK> callbackList = new RemoteCallbackList<>();
    private BINDER binder;

    protected BINDER getBinder() {
        return this.binder;
    }

    protected void register(CALLBACK callback) {
        FileDownloadLog.d(this, "register callback: %s", callback);
        if (callback != null) {
            callbackList.register(callback);
        }
    }

    protected void unregister(CALLBACK callback) {
        FileDownloadLog.d(this, "un register callback: %s", callback);
        if (callback != null) {
            callbackList.unregister(callback);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FileDownloadLog.d(this, "onCreate");
        binder = createBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileDownloadLog.d(this, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FileDownloadLog.d(this, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        FileDownloadLog.d(this, "onStart");
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        FileDownloadLog.d(this, "onBind %s", intent);
        return getBinder();
    }

    protected abstract BINDER createBinder();


    protected synchronized int callback(final int cmd, Object... objects) {
        final int n = callbackList.beginBroadcast();
        try {
            for (int i = 0; i < n; i++) {
                if (handleCallback(cmd, callbackList.getBroadcastItem(i), objects))
                    break;
            }

        } catch (RemoteException e) {
            FileDownloadLog.e(this, e, "callback error");
        } finally {
            callbackList.finishBroadcast();
        }

        return n;
    }

    /**
     * @param cmd     handle by cmd
     * @param objects params
     * @return is consume
     */
    protected abstract boolean handleCallback(final int cmd, final CALLBACK callback, Object... objects) throws RemoteException;

}
