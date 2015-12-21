package com.liulishuo.filedownloader;

/**
 * Created by Jacksgong on 12/21/15.
 */
public class FileDownloadSampleListener extends FileDownloadListener {

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        
    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void blockComplete(BaseDownloadTask task) {

    }

    @Override
    protected void completed(BaseDownloadTask task) {

    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {

    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }
}
