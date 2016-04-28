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

import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.services.BaseFileServiceUIGuard;
import com.liulishuo.filedownloader.services.FileDownloadService.SeparateProcessService;

import java.util.List;


/**
 * Created by Jacksgong on 9/23/15.
 * <p/>
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
        public void callback(FileDownloadTransferModel transfer) throws RemoteException {
            FileDownloadEventPool.getImpl().asyncPublishInFlow(new DownloadTransferEvent(transfer));
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
    public boolean startDownloader(final String url, final String path,
                                   final int callbackProgressTimes, final int autoRetryTimes,
                                   final FileDownloadHeader header) {
        if (getService() == null) {
            return false;
        }

        try {
            getService().start(url, path, callbackProgressTimes, autoRetryTimes, header);
        } catch (RemoteException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    @Override
    public boolean pauseDownloader(final int downloadId) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().pause(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public FileDownloadTransferModel checkReuse(final String url, final String path) {
        if (getService() == null) {
            return null;
        }

        try {
            return getService().checkReuse(url, path);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public FileDownloadTransferModel checkReuse(final int id) {
        if (getService() == null) {
            return null;
        }

        try {
            return getService().checkReuse2(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean checkIsDownloading(final String url, final String path) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().checkDownloading(url, path);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public long getSofar(final int downloadId) {
        long val = 0;
        if (getService() == null) {
            return val;
        }

        try {
            val = getService().getSofar(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return val;
    }

    @Override
    public long getTotal(final int downloadId) {
        long val = 0;
        if (getService() == null) {
            return val;
        }

        try {
            val = getService().getTotal(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return val;
    }

    @Override
    public int getStatus(final int downloadId) {
        int status = FileDownloadStatus.INVALID_STATUS;
        if (getService() == null) {
            return status;
        }

        try {
            status = getService().getStatus(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return status;
    }

    @Override
    public void pauseAllTasks() {
        if (getService() == null) {
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
        if (getService() == null) {
            return true;
        }

        try {
            getService().isIdle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void startForeground(int id, Notification notification) {
        if (getService() == null) {
            return;
        }

        try {
            getService().startForeground(id, notification);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopForeground(boolean removeNotification) {
        if (getService() == null) {
            return;
        }

        try {
            getService().stopForeground(removeNotification);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setTaskCompleted(String url, String path, long totalBytes) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().setTaskCompleted(url, path, totalBytes);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean setTaskCompleted(List<FileDownloadTaskAtom> taskAtomList) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().setTaskCompleted1(taskAtomList);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }
}
