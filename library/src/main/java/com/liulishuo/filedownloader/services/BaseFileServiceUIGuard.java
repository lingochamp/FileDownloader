package com.liulishuo.filedownloader.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.event.FileServiceConnectChangedEvent;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jacksgong on 8/10/15.
 */
public abstract class BaseFileServiceUIGuard<CALLBACK extends Binder, INTERFACE extends IInterface> implements ServiceConnection {

    private CALLBACK callback;
    private INTERFACE service;
    private Class<?> serviceClass;

    private HashMap<String, Object> uiCacheMap = new HashMap<>();

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

        FileDownloadLog.d(this, "onServiceConnected %s %s", name, this.service);

        try {
            registerCallback(this.service, this.callback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        FileEventPool.getImpl().
                asyncPublishInNewThread(new FileServiceConnectChangedEvent(
                        FileServiceConnectChangedEvent.ConnectStatus.connected, serviceClass));

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (this.service != null) {
            try {
                unregisterCallback(this.service, this.callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        FileDownloadLog.d(this, "onServiceDisconnected %s %s", name, this.service);
        this.service = null;
        FileEventPool.getImpl().
                asyncPublishInNewThread(new FileServiceConnectChangedEvent(
                        FileServiceConnectChangedEvent.ConnectStatus.disconnected, serviceClass));
    }

    private List<Context> BIND_CONTEXTS = new ArrayList<>();

    public void bindStartByContext(final Context context) {
        FileDownloadLog.d(this, "bindStartByContext %s", context.getClass().getSimpleName());

        Intent i = new Intent(context, serviceClass);

        if (!BIND_CONTEXTS.contains(context)) {
            // 对称,只有一次remove，防止内存泄漏
            BIND_CONTEXTS.add(context);
        }

        context.bindService(i, this, Context.BIND_AUTO_CREATE);
        context.startService(i);
    }

    public void unbindByContext(final Context context) {
        if (!BIND_CONTEXTS.contains(context)) {
            return;
        }

        FileDownloadLog.d(this, "unbindByContext %s", context);

        BIND_CONTEXTS.remove(context);

        Intent i = new Intent(context, serviceClass);

        context.unbindService(this);
        context.stopService(i);
    }

    public void startService(final Context context) {

        Intent i = new Intent(context, serviceClass);
        context.startService(i);
    }

    protected abstract INTERFACE asInterface(IBinder service);

    protected abstract void registerCallback(final INTERFACE service, final CALLBACK callback) throws RemoteException;

    protected abstract void unregisterCallback(final INTERFACE service, final CALLBACK callback) throws RemoteException;


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

    public boolean isConnected() {
        return getService() != null;
    }


}
