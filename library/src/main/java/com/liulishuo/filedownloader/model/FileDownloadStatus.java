package com.liulishuo.filedownloader.model;

/**
 * Created by Jacksgong on 11/26/15.
 */
public enum FileDownloadStatus {
    progress,
    completed,
    error, //TODO more make sense
    paused,
    pending,
    preCompleteOnNewThread,
    warn
}
