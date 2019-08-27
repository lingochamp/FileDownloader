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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import com.liulishuo.filedownloader.FileDownloadEventPool;
import com.liulishuo.filedownloader.IFileDownloadServiceProxy;
import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.util.ExtraKeys;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A UI-Guard in Main-Process for IPC, which is the only Object can access the other process in
 * Main-Process with Binder.
 */
public abstract class BaseFileServiceUIGuard<CALLBACK extends Binder, INTERFACE extends IInterface>
        implements IFileDownloadServiceProxy, ServiceConnection {

    private final CALLBACK callback;
    private volatile INTERFACE service;
    private final Class<?> serviceClass;
    protected boolean runServiceForeground = false;

    private final HashMap<String, Object> uiCacheMap = new HashMap<>();

    protected CALLBACK getCallback() {
        return this.callback;
    }

    protected INTERFACE getService() {
        return this.service;
    }

    protected BaseFileServiceUIGuard(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
        this.callback = createCallback();
    }

    protected abstract CALLBACK createCallback();

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = asInterface(service);

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "onServiceConnected %s %s", name, this.service);
        }

        try {
            registerCallback(this.service, this.callback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        @SuppressWarnings("unchecked") final List<Runnable> runnableList =
                (List<Runnable>) connectedRunnableList.clone();
        connectedRunnableList.clear();
        for (Runnable runnable : runnableList) {
            runnable.run();
        }

        FileDownloadEventPool.getImpl().
                asyncPublishInNewThread(new DownloadServiceConnectChangedEvent(
                        DownloadServiceConnectChangedEvent.ConnectStatus.connected, serviceClass));

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "onServiceDisconnected %s %s", name, this.service);
        }
        releaseConnect(true);
    }

    private void releaseConnect(final boolean isLost) {
        if (!isLost && this.service != null) {
            try {
                unregisterCallback(this.service, this.callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "release connect resources %s", this.service);
        }
        this.service = null;

        FileDownloadEventPool.getImpl().
                asyncPublishInNewThread(new DownloadServiceConnectChangedEvent(
                        isLost ? DownloadServiceConnectChangedEvent.ConnectStatus.lost
                                : DownloadServiceConnectChangedEvent.ConnectStatus.disconnected,
                        serviceClass));
    }

    private final List<Context> bindContexts = new ArrayList<>();
    private final ArrayList<Runnable> connectedRunnableList = new ArrayList<>();

    @Override
    public void bindStartByContext(final Context context) {
        bindStartByContext(context, null);
    }

    @Override
    public void bindStartByContext(final Context context, final Runnable connectedRunnable) {
        if (FileDownloadUtils.isDownloaderProcess(context)) {
            throw new IllegalStateException("Fatal-Exception: You can't bind the "
                    + "FileDownloadService in :filedownloader process.\n It's the invalid operation"
                    + " and is likely to cause unexpected problems.\n Maybe you want to use"
                    + " non-separate process mode for FileDownloader, More detail about "
                    + "non-separate mode, please move to wiki manually:"
                    + " https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties"
            );
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "bindStartByContext %s", context.getClass().getSimpleName());
        }

        Intent i = new Intent(context, serviceClass);
        if (connectedRunnable != null) {
            if (!connectedRunnableList.contains(connectedRunnable)) {
                connectedRunnableList.add(connectedRunnable);
            }
        }

        if (!bindContexts.contains(context)) {
            // 对称,只有一次remove，防止内存泄漏
            bindContexts.add(context);
        }

        runServiceForeground = FileDownloadUtils.needMakeServiceForeground(context);
        i.putExtra(ExtraKeys.IS_FOREGROUND, runServiceForeground);
        context.bindService(i, this, Context.BIND_AUTO_CREATE);
        if (runServiceForeground) {
            if (FileDownloadLog.NEED_LOG) FileDownloadLog.d(this, "start foreground service");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    @Override
    public void unbindByContext(final Context context) {
        if (!bindContexts.contains(context)) {
            return;
        }

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "unbindByContext %s", context);
        }

        bindContexts.remove(context);


        if (bindContexts.isEmpty()) {
            releaseConnect(false);
        }

        Intent i = new Intent(context, serviceClass);
        context.unbindService(this);
        context.stopService(i);
    }

    @Override
    public boolean isRunServiceForeground() {
        return runServiceForeground;
    }

    protected abstract INTERFACE asInterface(IBinder service);

    protected abstract void registerCallback(final INTERFACE service, final CALLBACK callback)
            throws RemoteException;

    protected abstract void unregisterCallback(final INTERFACE service, final CALLBACK callback)
            throws RemoteException;


    protected Object popCache(final String key) {
        return uiCacheMap.remove(key);
    }

    protected String putCache(final Object object) {
        if (object == null) {
            return null;
        }
        final String key = object.toString();
        uiCacheMap.put(key, object);
        return key;
    }

    @Override
    public boolean isConnected() {
        return getService() != null;
    }


}
