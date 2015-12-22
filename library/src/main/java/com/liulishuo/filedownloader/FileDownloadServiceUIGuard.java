package com.liulishuo.filedownloader;

import android.os.IBinder;
import android.os.RemoteException;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.services.BaseFileServiceUIGuard;
import com.liulishuo.filedownloader.services.FileDownloadService;


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
 *
 * Created by Jacksgong on 9/23/15.
 */
class FileDownloadServiceUIGuard extends BaseFileServiceUIGuard<FileDownloadServiceUIGuard.FileDownloadServiceCallback, IFileDownloadIPCService> {
    // TODO 优化 回调回来由对象改为 目标小原子对象

    private final static class HolderClass {
        private final static FileDownloadServiceUIGuard INSTANCE = new FileDownloadServiceUIGuard();
    }

    public static FileDownloadServiceUIGuard getImpl() {
        return HolderClass.INSTANCE;
    }

    protected FileDownloadServiceUIGuard() {
        super(FileDownloadService.class);
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

    public static class FileDownloadServiceCallback extends IFileDownloadIPCCallback.Stub {

        @Override
        public void callback(FileDownloadTransferModel transfer) throws RemoteException {
            DownloadEventPool.getImpl().asyncPublishInNewThread(new DownloadTransferEvent(transfer));
        }
    }

    public int startDownloader(final String url, final String path, final FileDownloadNotificationModel notificationData, final int progressCallbackTimes) {
        int result = 0;

        if (getService() == null) {
            return result;
        }

        try {
            result = getService().start(url, path, notificationData, progressCallbackTimes);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

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

    public boolean resumeDownloader(final int downloadId) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().resume(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

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

    public int getSofar(final int downloadId) {
        int val = 0;
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

    public int getTotal(final int downloadId) {
        int val = 0;
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

    public boolean removeDownloader(final int downloadId) {
        if (getService() == null) {
            return false;
        }

        try {
            return getService().remove(downloadId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }
}
