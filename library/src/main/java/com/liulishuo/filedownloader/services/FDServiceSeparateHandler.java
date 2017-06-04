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

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.lang.ref.WeakReference;

/**
 * For handling the case of the FileDownloadService runs in separate `:filedownloader` process.
 */
public class FDServiceSeparateHandler extends IFileDownloadIPCService.Stub
        implements MessageSnapshotFlow.MessageReceiver, IFileDownloadServiceHandler {

    private final RemoteCallbackList<IFileDownloadIPCCallback> callbackList = new RemoteCallbackList<>();
    private final FileDownloadManager downloadManager;
    private final WeakReference<FileDownloadService> wService;

    @SuppressWarnings("UnusedReturnValue")
    private synchronized int callback(MessageSnapshot snapShot) {
        final int n = callbackList.beginBroadcast();
        try {
            for (int i = 0; i < n; i++) {
                callbackList.getBroadcastItem(i).callback(snapShot);
            }
        } catch (RemoteException e) {
            FileDownloadLog.e(this, e, "callback error");
        } finally {
            callbackList.finishBroadcast();
        }

        return n;
    }

    FDServiceSeparateHandler(WeakReference<FileDownloadService> wService, FileDownloadManager manager) {
        this.wService = wService;
        this.downloadManager = manager;

        MessageSnapshotFlow.getImpl().setReceiver(this);
    }

    @Override
    public void registerCallback(IFileDownloadIPCCallback callback) throws RemoteException {
        callbackList.register(callback);
    }

    @Override
    public void unregisterCallback(IFileDownloadIPCCallback callback) throws RemoteException {
        callbackList.unregister(callback);
    }

    @Override
    public boolean checkDownloading(String url, String path) throws RemoteException {
        return downloadManager.isDownloading(url, path);
    }

    @Override
    public void start(String url, String path, boolean pathAsDirectory, int callbackProgressTimes,
                      int callbackProgressMinIntervalMillis, int autoRetryTimes, boolean forceReDownload,
                      FileDownloadHeader header, boolean isWifiRequired) throws RemoteException {
        downloadManager.start(url, path, pathAsDirectory, callbackProgressTimes,
                callbackProgressMinIntervalMillis, autoRetryTimes, forceReDownload, header,
                isWifiRequired);
    }

    @Override
    public boolean pause(int downloadId) throws RemoteException {
        return downloadManager.pause(downloadId);
    }

    @Override
    public void pauseAllTasks() throws RemoteException {
        downloadManager.pauseAll();
    }

    @Override
    public boolean setMaxNetworkThreadCount(int count) throws RemoteException {
        return downloadManager.setMaxNetworkThreadCount(count);
    }

    @Override
    public long getSofar(int downloadId) throws RemoteException {
        return downloadManager.getSoFar(downloadId);
    }

    @Override
    public long getTotal(int downloadId) throws RemoteException {
        return downloadManager.getTotal(downloadId);
    }

    @Override
    public byte getStatus(int downloadId) throws RemoteException {
        return downloadManager.getStatus(downloadId);
    }

    @Override
    public boolean isIdle() throws RemoteException {
        return downloadManager.isIdle();
    }

    @Override
    public void startForeground(int id, Notification notification) throws RemoteException {
        if (this.wService != null && this.wService.get() != null) {
            this.wService.get().startForeground(id, notification);
        }
    }

    @Override
    public void stopForeground(boolean removeNotification) throws RemoteException {
        if (this.wService != null && this.wService.get() != null) {
            this.wService.get().stopForeground(removeNotification);
        }
    }

    @Override
    public boolean clearTaskData(int id) throws RemoteException {
        return downloadManager.clearTaskData(id);
    }

    @Override
    public void clearAllTaskData() throws RemoteException {
        downloadManager.clearAllTaskData();
    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this;
    }

    @Override
    public void onDestroy() {
        MessageSnapshotFlow.getImpl().setReceiver(null);
    }

    @Override
    public void receive(MessageSnapshot snapShot) {
        callback(snapShot);
    }
}
