package com.liulishuo.filedownloader.services;

import android.os.RemoteException;

import com.liulishuo.filedownloader.event.FileDownloadTransferEvent;
import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.event.FileEventSampleListener;
import com.liulishuo.filedownloader.event.IFileEvent;
import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.model.FileDownloadNotificationModel;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

/**
 * Created by Jacksgong on 9/23/15.
 */
public class FileDownloadService extends BaseFileService<IFileDownloadIPCCallback, FileDownloadService.FileDownloadServiceBinder> implements FileEventSampleListener.IEventListener {

    private FileEventSampleListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mListener = new FileEventSampleListener(this);

        FileEventPool.getImpl().addListener(FileDownloadTransferEvent.ID, mListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        FileEventPool.getImpl().removeListener(FileDownloadTransferEvent.ID, mListener);
    }

    @Override
    protected FileDownloadServiceBinder createBinder() {
        return new FileDownloadServiceBinder();
    }

    @Override
    protected boolean handleCallback(int cmd, IFileDownloadIPCCallback IFileDownloadIPCCallback, Object... objects) throws RemoteException {
        IFileDownloadIPCCallback.callback(((FileDownloadTransferEvent) objects[0]).getTransfer());
        return false;
    }

    @Override
    public boolean callback(IFileEvent event) {

        if (event instanceof FileDownloadTransferEvent) {
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
        public int start(String url, String path, FileDownloadNotificationModel notificaitonData, int progressNotifyNums) throws RemoteException {
            final int downloadId = downloadManager.start(url, path, notificaitonData, progressNotifyNums);

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
