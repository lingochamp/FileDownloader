/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader;


import com.liulishuo.filedownloader.event.IDownloadEvent;
import com.liulishuo.filedownloader.event.IDownloadListener;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
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


    /**
     * 进入队列
     *
     * @param task       Current task
     * @param soFarBytes 数据库中的当前已经下载了多少
     * @param totalBytes 数据库中的总字节数
     */
    protected abstract void pending(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * 连接上
     *
     * @param task       Current task
     * @param etag       ETag
     * @param isContinue 是否是断点续传继续下载
     * @param soFarBytes 已经下载了多少
     * @param totalBytes 总大小
     */
    protected void connected(final BaseDownloadTask task, final String etag, final boolean isContinue, final int soFarBytes, final int totalBytes) {

    }

    /**
     * @param task       Current task
     * @param soFarBytes 已经下载了的字节数
     * @param totalBytes 总字节数
     */
    protected abstract void progress(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * block completed in new thread
     *
     * @param task Current task
     */
    protected abstract void blockComplete(final BaseDownloadTask task);

    // final width below methods

    /**
     * succeed download
     *
     * @param task Current task
     */
    protected abstract void completed(final BaseDownloadTask task);

    /**
     * 下载被暂停
     *
     * @param task       Current task
     * @param soFarBytes 已经下载了的字节数
     * @param totalBytes 总字节数
     */
    protected abstract void paused(final BaseDownloadTask task, final int soFarBytes, final int totalBytes);

    /**
     * 下载出现错误
     *
     * @param task Current task
     * @param e    Any throwable on download pipeline
     */
    protected abstract void error(final BaseDownloadTask task, final Throwable e);

    /**
     * 相同任务(url与path相同)已经在队列中(等待中/正在下载中）
     *
     * @param task same download already start & pending/downloading
     */
    protected abstract void warn(final BaseDownloadTask task);

}
