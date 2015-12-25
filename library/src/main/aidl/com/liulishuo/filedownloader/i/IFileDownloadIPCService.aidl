package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

interface IFileDownloadIPCService {

    void registerCallback(in IFileDownloadIPCCallback callback);
    void unregisterCallback(in IFileDownloadIPCCallback callback);

    FileDownloadTransferModel checkReuse(String url, String path);
    boolean checkDownloading(String url, String path);
    int start(String url, String path, int callbackProgressTimes, int autoRetryTimes);
    boolean pause(int downloadId);

    int getSofar(int downloadId);
    int getTotal(int downloadId);
}
