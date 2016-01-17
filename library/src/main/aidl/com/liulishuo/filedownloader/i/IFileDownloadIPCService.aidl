package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.model.FileDownloadHeader;

interface IFileDownloadIPCService {

    oneway void registerCallback(in IFileDownloadIPCCallback callback);
    oneway void unregisterCallback(in IFileDownloadIPCCallback callback);

    FileDownloadTransferModel checkReuse(String url, String path);
    FileDownloadTransferModel checkReuse2(int id);
    boolean checkDownloading(String url, String path);
    oneway void start(String url, String path, int callbackProgressTimes, int autoRetryTimes, in FileDownloadHeader header);
    boolean pause(int downloadId);
    void pauseAllTasks();

    long getSofar(int downloadId);
    long getTotal(int downloadId);
    int getStatus(int downloadId);
    boolean isIdle();
}
