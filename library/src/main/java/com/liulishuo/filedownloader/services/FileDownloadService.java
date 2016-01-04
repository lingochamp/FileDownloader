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

import android.os.RemoteException;

import com.liulishuo.filedownloader.event.DownloadEventSampleListener;
import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

/**
 * Created by Jacksgong on 9/23/15.
 */
public class FileDownloadService extends BaseFileService<IFileDownloadIPCCallback, FileDownloadService.FileDownloadServiceBinder> implements DownloadEventSampleListener.IEventListener {

    private DownloadEventSampleListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mListener = new DownloadEventSampleListener(this);

        FileDownloadProcessEventPool.getImpl().addListener(DownloadTransferEvent.ID, mListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        FileDownloadProcessEventPool.getImpl().removeListener(DownloadTransferEvent.ID, mListener);
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


        private final FileDownloadMgr downloadManager;

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
        public int start(String url, String path, int callbackProgressTimes, int autoRetryTimes) throws RemoteException {

            return downloadManager.start(url, path, callbackProgressTimes, autoRetryTimes);
        }

        @Override
        public boolean pause(int downloadId) throws RemoteException {
            return downloadManager.pause(downloadId);
        }

        @Override
        public long getSofar(int downloadId) throws RemoteException {
            return downloadManager.getSoFar(downloadId);
        }

        @Override
        public long getTotal(int downloadId) throws RemoteException {
            return downloadManager.getTotal(downloadId);
        }
    }
}
