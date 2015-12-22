package com.liulishuo.filedownloader.model;

/**
 * Created by Jacksgong on 11/26/15.
 *
 * @see com.liulishuo.filedownloader.IFileDownloadMessage
 */
public interface FileDownloadStatus {
    int pending = 1;
    int connected = 2;
    int progress = 3;
    int blockComplete = 4;
    int error = -1;
    int paused = -2;
    int completed = -3;
    int warn = -4;

    int MAX_INT = 4;
    int MIN_INT = -4;
    int INVALID_STATUS = 0;
}
