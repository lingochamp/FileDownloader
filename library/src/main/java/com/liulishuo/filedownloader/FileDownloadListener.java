package com.liulishuo.filedownloader;


import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.event.IDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Copyright (c) 2015 LingoChamp Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Jacksgong on 9/7/15.
 * <p/>
 * normal chain {@link #pending} -> {@link #connected} -> {@link #progress}  -> {@link #blockComplete} -> {@link #completed}
 * may final width {@link #paused}/{@link #completed}/{@link #error}/{@link #warn}
 * if reuse just {@link #blockComplete} ->{@link #completed}
 */
public abstract class FileDownloadListener extends IDownloadListener {

    public FileDownloadListener() {
        this(0);
    }

    public FileDownloadListener(int priority) {
        super(priority);
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        if (!(event instanceof FileDownloadEvent)) {
            return false;
        }

        final FileDownloadEvent downloaderEvent = ((FileDownloadEvent) event);


        switch (downloaderEvent.getStatus()) {
            case FileDownloadStatus.pending:
                pending(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getSoFarBytes(),
                        downloaderEvent.getDownloader().getTotalBytes());
                break;
            case FileDownloadStatus.connected:
                connected(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEtag(),
                        downloaderEvent.getDownloader().isContinue(),
                        downloaderEvent.getDownloader().getSoFarBytes(),
                        downloaderEvent.getDownloader().getTotalBytes());
                break;
            case FileDownloadStatus.progress:
                progress(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getSoFarBytes(),
                        downloaderEvent.getDownloader().getTotalBytes());
                break;
            case FileDownloadStatus.paused:
                paused(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getSoFarBytes(),
                        downloaderEvent.getDownloader().getTotalBytes());
                break;

            case FileDownloadStatus.blockComplete:
                blockComplete(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.completed:
                completed(downloaderEvent.getDownloader());
                break;
            case FileDownloadStatus.error:
                error(downloaderEvent.getDownloader(),
                        downloaderEvent.getDownloader().getEx());
                break;
            case FileDownloadStatus.warn:
                // already same url & path in pending/running list
                warn(downloaderEvent.getDownloader());
                break;
        }

        return false;
    }


    protected abstract void pending(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * 连接上
     *
     * @param task
     * @param etag       ETag
     * @param isContinue 是否是断点续传继续下载
     * @param soFarBytes 已经下载了多少
     * @param totalBytes 总大小
     */
    protected void connected(final BaseDownloadTask task, final String etag, final boolean isContinue, final int soFarBytes, final int totalBytes) {

    }

    protected abstract void progress(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * block completed in new thread
     *
     * @param task
     */
    protected abstract void blockComplete(final BaseDownloadTask task);

    // final width below methods

    /**
     * succeed download
     *
     * @param task
     */
    protected abstract void completed(final BaseDownloadTask task);

    protected abstract void paused(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * @param task
     * @param e
     */
    protected abstract void error(final BaseDownloadTask task, final Throwable e);

    /**
     * @param task same download already start & pending/downloading
     */
    protected abstract void warn(final BaseDownloadTask task);

}
