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

import com.liulishuo.filedownloader.FileDownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Jacksgong on 4/17/16.
 * <p/>
 * For handling the case of the FileDownloadService runs in separate `:filedownloader` process.
 */
public class FDServiceSeparateHandler extends IFileDownloadIPCService.Stub
        implements DownloadEventSampleListener.IEventListener, IFileDownloadServiceHandler {

    private final RemoteCallbackList<IFileDownloadIPCCallback> callbackList = new RemoteCallbackList<>();
    private final FileDownloadMgr downloadManager;
    private DownloadEventSampleListener mListener;
    private WeakReference<FileDownloadService> wService;

    protected synchronized int callback(FileDownloadTransferModel transfer) {
        final int n = callbackList.beginBroadcast();
        try {
            for (int i = 0; i < n; i++) {
                callbackList.getBroadcastItem(i).callback(transfer);
            }
        } catch (RemoteException e) {
            FileDownloadLog.e(this, e, "callback error");
        } finally {
            callbackList.finishBroadcast();
        }

        return n;
    }

    FDServiceSeparateHandler( WeakReference<FileDownloadService> wService) {
        this.wService = wService;
        this.downloadManager = new FileDownloadMgr(FileDownloadHelper.getOkHttpClient());

        mListener = new DownloadEventSampleListener(this);
        FileDownloadEventPool.getImpl().addListener(DownloadTransferEvent.ID, mListener);
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
    public FileDownloadTransferModel checkReuse(String url, String path) throws RemoteException {
        return downloadManager.checkReuse(FileDownloadUtils.generateId(url, path));
    }

    @Override
    public FileDownloadTransferModel checkReuse2(int id) throws RemoteException {
        return downloadManager.checkReuse(id);
    }

    @Override
    public boolean checkDownloading(String url, String path) throws RemoteException {
        return downloadManager.checkDownloading(url, path);
    }

    @Override
    public void start(String url, String path, int callbackProgressTimes, int autoRetryTimes,
                      FileDownloadHeader header) throws RemoteException {
        downloadManager.start(url, path, callbackProgressTimes, autoRetryTimes, header);
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
    public long getSofar(int downloadId) throws RemoteException {
        return downloadManager.getSoFar(downloadId);
    }

    @Override
    public long getTotal(int downloadId) throws RemoteException {
        return downloadManager.getTotal(downloadId);
    }

    @Override
    public int getStatus(int downloadId) throws RemoteException {
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
    public boolean setTaskCompleted(String url, String path, long totalBytes) throws RemoteException {
        return downloadManager.setTaskCompleted(url, path, totalBytes);
    }

    @Override
    public boolean setTaskCompleted1(List<FileDownloadTaskAtom> taskList) throws RemoteException {
        return downloadManager.setTaskCompleted(taskList);
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        callback(((DownloadTransferEvent) event).getTransfer());
        return false;
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
        FileDownloadEventPool.getImpl().removeListener(DownloadTransferEvent.ID, mListener);
    }
}
