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

package com.liulishuo.filedownloader;

import android.app.Notification;
import android.os.IBinder;
import android.os.RemoteException;

import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.BaseFileServiceUIGuard;
import com.liulishuo.filedownloader.services.FileDownloadService.SeparateProcessService;
import com.liulishuo.filedownloader.util.DownloadServiceNotConnectedHelper;


/**
 * The UI-Guard for FileDownloader-Process.
 * <p/>
 * The only Class can access the FileDownload-Process, and the only Class can receive the event from
 * the FileDownloader-Process through Binder.
 * <p/>
 * We will use this UIGuard as default, because the FileDownloadService runs in the separate process
 * `:filedownloader` as default, If you want to share the main process to run the FileDownloadService,
 * just add a command `process.non-separate=true` in `/filedownloader.properties`.
 *
 * @see FileDownloadServiceSharedTransmit
 */
class FileDownloadServiceUIGuard extends
        BaseFileServiceUIGuard<FileDownloadServiceUIGuard.FileDownloadServiceCallback,
                IFileDownloadIPCService> {

    FileDownloadServiceUIGuard() {
        super(SeparateProcessService.class);
    }

    @Override
    protected FileDownloadServiceCallback createCallback() {
        return new FileDownloadServiceCallback();
    }

    @Override
    protected IFileDownloadIPCService asInterface(IBinder service) {
        return IFileDownloadIPCService.Stub.asInterface(service);
    }

    @Override
    protected void registerCallback(IFileDownloadIPCService service, FileDownloadServiceCallback fileDownloadServiceCallback) throws RemoteException {
        service.registerCallback(fileDownloadServiceCallback);
    }

    @Override
    protected void unregisterCallback(IFileDownloadIPCService service, FileDownloadServiceCallback fileDownloadServiceCallback) throws RemoteException {
        service.unregisterCallback(fileDownloadServiceCallback);
    }

    protected static class FileDownloadServiceCallback extends IFileDownloadIPCCallback.Stub {

        @Override
        public void callback(MessageSnapshot snapshot) throws RemoteException {
            MessageSnapshotFlow.getImpl().inflow(snapshot);
        }
    }

    /**
     * @param url                   for download
     * @param path                  for save download file
     * @param callbackProgressTimes for callback progress times
     * @param autoRetryTimes        for auto retry times when error
     * @param header                for http header
     */
    @Override
    public boolean start(final String url, final String path, final boolean pathAsDirectory,
                         final int callbackProgressTimes,
                         final int callbackProgressMinIntervalMillis,
                         final int autoRetryTimes, final boolean forceReDownload,
                         final FileDownloadHeader header, final boolean isWifiRequired) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.start(url, path, pathAsDirectory);
        }

        try {
            getService().start(url, path, pathAsDirectory, callbackProgressTimes,
                    callbackProgressMinIntervalMillis, autoRetryTimes, forceReDownload, header,
                    isWifiRequired);
        } catch (RemoteException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    @Override
    public boolean pause(final int id) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.pause(id);
        }

        try {
            return getService().pause(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean isDownloading(final String url, final String path) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.isDownloading(url, path);
        }

        try {
            return getService().checkDownloading(url, path);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public long getSofar(final int id) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.getSofar(id);
        }

        long val = 0;
        try {
            val = getService().getSofar(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return val;
    }

    @Override
    public long getTotal(final int id) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.getTotal(id);
        }

        long val = 0;
        try {
            val = getService().getTotal(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return val;
    }

    @Override
    public byte getStatus(final int id) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.getStatus(id);
        }

        byte status = FileDownloadStatus.INVALID_STATUS;
        try {
            status = getService().getStatus(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return status;
    }

    @Override
    public void pauseAllTasks() {
        if (!isConnected()) {
            DownloadServiceNotConnectedHelper.pauseAllTasks();
            return;
        }

        try {
            getService().pauseAllTasks();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return any error, will return true
     */
    @Override
    public boolean isIdle() {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.isIdle();
        }

        try {
            getService().isIdle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void startForeground(int notificationId, Notification notification) {
        if (!isConnected()) {
            DownloadServiceNotConnectedHelper.startForeground(notificationId, notification);
            return;
        }

        try {
            getService().startForeground(notificationId, notification);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopForeground(boolean removeNotification) {
        if (!isConnected()) {
            DownloadServiceNotConnectedHelper.stopForeground(removeNotification);
            return;
        }

        try {
            getService().stopForeground(removeNotification);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setMaxNetworkThreadCount(int count) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.setMaxNetworkThreadCount(count);
        }

        try {
            return getService().setMaxNetworkThreadCount(count);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean clearTaskData(int id) {
        if (!isConnected()) {
            return DownloadServiceNotConnectedHelper.clearTaskData(id);
        }

        try {
            return getService().clearTaskData(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void clearAllTaskData() {
        if (!isConnected()) {
            DownloadServiceNotConnectedHelper.clearAllTaskData();
            return;
        }

        try {
            getService().clearAllTaskData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
