package com.liulishuo.filedownloader.services;

import android.os.RemoteException;

import com.liulishuo.filedownloader.event.DownloadEventPool;
import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

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
public class FileDownloadService extends BaseFileService<IFileDownloadIPCCallback, FileDownloadService.FileDownloadServiceBinder> implements DownloadEventSampleListener.IEventListener {

    private DownloadEventSampleListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mListener = new DownloadEventSampleListener(this);

        DownloadEventPool.getImpl().addListener(DownloadTransferEvent.ID, mListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        DownloadEventPool.getImpl().removeListener(DownloadTransferEvent.ID, mListener);
    }

    @Override
    protected FileDownloadServiceBinder createBinder() {
        return new FileDownloadServiceBinder();
    }

    @Override
    protected boolean handleCallback(int cmd, IFileDownloadIPCCallback IFileDownloadIPCCallback, Object... objects) throws RemoteException {
        IFileDownloadIPCCallback.callback(((DownloadTransferEvent) objects[0]).getTransfer());
        return false;
    }

    @Override
    public boolean callback(IDownloadEvent event) {

        if (event instanceof DownloadTransferEvent) {
            callback(0, event);
        }
        return false;
    }

    protected class FileDownloadServiceBinder extends IFileDownloadIPCService.Stub {


        private FileDownloadMgr downloadManager;

        private FileDownloadServiceBinder() {
            downloadManager = new FileDownloadMgr();
        }

        @Override
        public void registerCallback(IFileDownloadIPCCallback callback) throws RemoteException {
            register(callback);
        }

        @Override
        public void unregisterCallback(IFileDownloadIPCCallback callback) throws RemoteException {
            unregister(callback);
        }

        @Override
        public FileDownloadTransferModel checkReuse(String url, String path) throws RemoteException {
            return downloadManager.checkReuse(url, path);
        }

        @Override
        public boolean checkDownloading(String url, String path) throws RemoteException {
            return downloadManager.checkDownloading(url, path);
        }

        @Override
        public int start(String url, String path, FileDownloadNotificationModel notificaitonData, int callbackProgressTimes) throws RemoteException {
            final int downloadId = downloadManager.start(url, path, notificaitonData, callbackProgressTimes);

            return downloadId;
        }

        @Override
        public boolean resume(int downloadId) throws RemoteException {
            return downloadManager.resume(downloadId);
        }

        @Override
        public boolean pause(int downloadId) throws RemoteException {
            return downloadManager.pause(downloadId);
        }

        @Override
        public boolean remove(int downloadId) throws RemoteException {
            return downloadManager.remove(downloadId);
        }

        @Override
        public int getSofar(int downloadId) throws RemoteException {
            return downloadManager.getSofar(downloadId);
        }

        @Override
        public int getTotal(int downloadId) throws RemoteException {
            return downloadManager.getTotal(downloadId);
        }
    }
}
