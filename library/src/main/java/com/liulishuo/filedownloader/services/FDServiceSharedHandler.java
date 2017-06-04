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

import com.liulishuo.filedownloader.FileDownloadServiceProxy;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadHeader;

import java.lang.ref.WeakReference;

/**
 * For handling the case of the FileDownloadService runs in shared the main process.
 */
public class FDServiceSharedHandler extends IFileDownloadIPCService.Stub
        implements IFileDownloadServiceHandler {
    private final FileDownloadManager downloadManager;
    private final WeakReference<FileDownloadService> wService;

    FDServiceSharedHandler(WeakReference<FileDownloadService> wService, FileDownloadManager manager) {
        this.wService = wService;
        this.downloadManager = manager;
    }

    @Override
    public void registerCallback(IFileDownloadIPCCallback callback) {
    }

    @Override
    public void unregisterCallback(IFileDownloadIPCCallback callback) {
    }

    @Override
    public boolean checkDownloading(String url, String path) {
        return downloadManager.isDownloading(url, path);
    }

    @Override
    public void start(String url, String path, boolean pathAsDirectory, int callbackProgressTimes,
                      int callbackProgressMinIntervalMillis, int autoRetryTimes, boolean forceReDownload,
                      FileDownloadHeader header, boolean isWifiRequired) {
        downloadManager.start(url, path, pathAsDirectory, callbackProgressTimes,
                callbackProgressMinIntervalMillis, autoRetryTimes, forceReDownload, header,
                isWifiRequired);
    }

    @Override
    public boolean pause(int downloadId) {
        return downloadManager.pause(downloadId);
    }

    @Override
    public void pauseAllTasks() {
        downloadManager.pauseAll();
    }

    @Override
    public boolean setMaxNetworkThreadCount(int count) {
        return downloadManager.setMaxNetworkThreadCount(count);
    }

    @Override
    public long getSofar(int downloadId) {
        return downloadManager.getSoFar(downloadId);
    }

    @Override
    public long getTotal(int downloadId) {
        return downloadManager.getTotal(downloadId);
    }

    @Override
    public byte getStatus(int downloadId) {
        return downloadManager.getStatus(downloadId);
    }

    @Override
    public boolean isIdle() {
        return downloadManager.isIdle();
    }

    @Override
    public void startForeground(int id, Notification notification) {
        if (this.wService != null && this.wService.get() != null) {
            this.wService.get().startForeground(id, notification);
        }
    }

    @Override
    public void stopForeground(boolean removeNotification) {
        if (this.wService != null && this.wService.get() != null) {
            this.wService.get().stopForeground(removeNotification);
        }
    }

    @Override
    public boolean clearTaskData(int id) {
        return downloadManager.clearTaskData(id);
    }

    @Override
    public void clearAllTaskData() {
        downloadManager.clearAllTaskData();
    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {
        //noinspection ConstantConditions
        FileDownloadServiceProxy.getConnectionListener().onConnected(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //noinspection ConstantConditions
        FileDownloadServiceProxy.getConnectionListener().onDisconnected();
    }

    public interface FileDownloadServiceSharedConnection {
        void onConnected(FDServiceSharedHandler handler);

        void onDisconnected();
    }
}
