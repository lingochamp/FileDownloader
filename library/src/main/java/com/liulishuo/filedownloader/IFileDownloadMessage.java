package com.liulishuo.filedownloader;

/**
 * Created by Jacksgong on 12/21/15.
 * @see com.liulishuo.filedownloader.model.FileDownloadStatus
 */
interface IFileDownloadMessage {


    // 开始态，入队列
    void notifyStarted();

    // 中间态
    void notifyPending();

    void notifyProgress();

    void notifyBlockComplete();

    // 结束态，异或
    void notifyWarn();

    void notifyError();

    void notifyPaused();

    void notifyCompleted();
}
