package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.i.IFileDownloadIPCCallback;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import android.app.Notification;

interface IFileDownloadIPCService {

    oneway void registerCallback(in IFileDownloadIPCCallback callback);
    oneway void unregisterCallback(in IFileDownloadIPCCallback callback);

    FileDownloadTransferModel checkReuse(String url, String path);
    FileDownloadTransferModel checkReuse2(int id);
    boolean checkDownloading(String url, String path);
    // why not use `oneway` to optimize the performance of the below `start` method? because if we
    // use `oneway` it will be very hard to decide how is the binder thread going according to the context.
    // and in this way(not is `oneway`), we can block the download before its launch only
    // by {@link FileDownloadEventPool#shutdownSendPool} according to the context, because it
    // will execute sync on the {@link FileDownloadEventPool#sendPool}
    void start(String url, String path, int callbackProgressTimes, int autoRetryTimes, in FileDownloadHeader header);
    boolean pause(int downloadId);
    void pauseAllTasks();

    long getSofar(int downloadId);
    long getTotal(int downloadId);
    int getStatus(int downloadId);
    boolean isIdle();

    oneway void startForeground(int id, in Notification notification);
    oneway void stopForeground(boolean removeNotification);

    boolean setTaskCompleted(String url, String path, long totalBytes);
    boolean setTaskCompleted1(in List<FileDownloadTaskAtom> taskList);
}
