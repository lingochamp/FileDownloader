package com.liulishuo.filedownloader.model;

/**
 * Created by Jacksgong on 11/26/15.
 */
public interface FileDownloadStatus {
    int pending = 1;
    int progress = 2;
    int preCompleteOnNewThread = 3;
    int error = -1;
    int paused = -2;
    int completed = -3;
    int warn = -4;

    int MAX_INT = 3;
    int MIN_INT = -4;
    int INVALID_STATUS = 0;
}
