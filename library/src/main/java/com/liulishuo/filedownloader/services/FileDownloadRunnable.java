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

package com.liulishuo.filedownloader.services;

import android.os.Process;
import android.text.TextUtils;

import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadRunnable implements Runnable {

    private static final int BUFFER_SIZE = 1024 * 4;
    private final FileDownloadTransferModel downloadTransfer;

    private final String url;
    private final String path;

    private final IFileDownloadDBHelper helper;

    private int maxNotifyBytes;


    private int maxNotifyNums = 0;

    //tmp
    private boolean isContinueDownloadAvailable;

    // etag
    private String etag;

    private FileDownloadModel downloadModel;

    public int getId() {
        return downloadModel.getId();
    }

    private volatile boolean isRunning = false;
    private volatile boolean isPending = false;

    private final OkHttpClient client;
    private final int autoRetryTimes;

    public FileDownloadRunnable(final OkHttpClient client, final FileDownloadModel model, final IFileDownloadDBHelper helper, final int autoRetryTimes) {
        isPending = true;
        isRunning = false;

        this.client = client;
        this.helper = helper;

        this.url = model.getUrl();
        this.path = model.getPath();

        downloadTransfer = new FileDownloadTransferModel();

        downloadTransfer.setDownloadId(model.getId());
        downloadTransfer.setStatus(model.getStatus());
        downloadTransfer.setSoFarBytes(model.getSoFar());
        downloadTransfer.setTotalBytes(model.getTotal());

        maxNotifyNums = model.getCallbackProgressTimes();
        maxNotifyNums = maxNotifyNums <= 0 ? 0 : maxNotifyNums;

        this.isContinueDownloadAvailable = false;

        this.etag = model.getETag();
        this.downloadModel = model;

        this.autoRetryTimes = autoRetryTimes;
    }

    public boolean isExist() {
        return isPending || isRunning;
    }

    @Override
    public void run() {
        isPending = false;
        isRunning = true;
        int retryingTimes = 0;
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        FileDownloadModel model = this.downloadModel;

        if (model == null) {
            FileDownloadLog.e(this, "start runnable but model == null?? %s", getId());

            this.downloadModel = helper.find(getId());

            if (this.downloadModel == null) {
                FileDownloadLog.e(this, "start runnable but downloadMode == null?? %s", getId());
                return;
            }

            model = this.downloadModel;
        }

        if (model.getStatus() != FileDownloadStatus.pending) {
            FileDownloadLog.e(this, "start runnable but status err %s", model.getStatus());
            // 极低概率事件，相同url与path的任务被放到了线程池中(目前在入池之前是有检测的，但是还是存在极低概率的同步问题) 执行的时候有可能会遇到
            onError(new RuntimeException(String.format("start runnable but status err %s", model.getStatus())));

            return;
        }

        // 进入下载
        do {

            int soFar = 0;
            try {

                if (model.isCanceled()) {
                    FileDownloadLog.d(this, "already canceled %d %d", model.getId(), model.getStatus());
                    break;
                }

                FileDownloadLog.d(FileDownloadRunnable.class, "start download %s %s", getId(), model.getUrl());

                checkIsContinueAvailable();

                Request.Builder headerBuilder = new Request.Builder().url(url);
                addHeader(headerBuilder);
                headerBuilder.tag(this.getId());
                // 目前没有指定cache，下载任务非普通REST请求，用户已经有了存储的地方
                headerBuilder.cacheControl(CacheControl.FORCE_NETWORK);

                Call call = client.newCall(headerBuilder.get().build());

                Response response = call.execute();

                final boolean isSucceedStart = response.code() == 200;
                final boolean isSucceedContinue = response.code() == 206 && isContinueDownloadAvailable;

                if (isSucceedStart || isSucceedContinue) {
                    int total = downloadTransfer.getTotalBytes();
                    if (isSucceedStart || total == 0) {
                        // TODO 目前没有对 2^31-1bit以上大小支持，未来会开发一个对应的库
                        total = (int) response.body().contentLength();
                    }

                    if (isSucceedContinue) {
                        soFar = downloadTransfer.getSoFarBytes();
                        FileDownloadLog.d(this, "add range %d %d", downloadTransfer.getSoFarBytes(), downloadTransfer.getTotalBytes());
                    }

                    InputStream inputStream = null;
                    RandomAccessFile accessFile = getRandomAccessFile(isSucceedContinue);
                    try {
                        inputStream = response.body().byteStream();
                        byte[] buff = new byte[BUFFER_SIZE];
                        maxNotifyBytes = maxNotifyNums <= 0 ? -1 : total / maxNotifyNums;

                        updateHeader(response);
                        onConnected(isSucceedContinue, soFar, total);


                        do {
                            int readed = inputStream.read(buff);
                            if (readed == -1) {
                                break;
                            }

                            accessFile.write(buff, 0, readed);

                            //write buff
                            soFar += readed;
                            if (accessFile.length() < soFar) {
                                // 文件大小必须会等于正在写入的大小
                                throw new RuntimeException(String.format("file be changed by others when downloading %d %d", accessFile.length(), soFar));
                            } else {
                                onProcess(soFar, total);
                            }

                            if (isCancelled()) {
                                onPause();
                                return;
                            }

                        } while (true);


                        if (soFar == total) {
                            onComplete(total);

                            // 成功
                            break;
                        } else {
                            throw new RuntimeException(
                                    String.format("sofar[%d] not equal total[%d]", soFar, total));
                        }
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }

                        if (accessFile != null) {
                            accessFile.close();
                        }
                    }


                } else {
                    throw new RuntimeException(String.format("response code error: %d", response.code()));
                }


            } catch (Throwable ex) {
                // TODO 决策是否需要重试，是否是用户决定，或者根据错误码处理
                if (autoRetryTimes > retryingTimes++) {
                    // retry
                    onRetry(ex, retryingTimes, soFar);
                    continue;
                } else {
                    // error
                    onError(ex);
                    break;
                }
            } finally {
                isRunning = false;
            }

        } while (true);


    }

    private void addHeader(Request.Builder builder) {
        if (isContinueDownloadAvailable) {
            builder.addHeader("If-Match", this.etag);
            builder.addHeader("Range", String.format("bytes=%d-", downloadTransfer.getSoFarBytes()));
        }
    }

    private void updateHeader(Response response) {
        if (response == null) {
            throw new RuntimeException("response is null when updateHeader");
        }

        boolean needRefresh = false;
        final String oldEtag = this.etag;
        final String newEtag = response.header("Etag");

        FileDownloadLog.w(this, "etag find by header %s", newEtag);

        if (oldEtag == null && newEtag != null) {
            needRefresh = true;
        } else if (oldEtag != null && newEtag != null && !oldEtag.equals(newEtag)) {
            needRefresh = true;
        }

        if (needRefresh) {
            this.etag = newEtag;
            helper.updateHeader(downloadTransfer.getDownloadId(), newEtag);
        }

    }

    private final DownloadTransferEvent event = new DownloadTransferEvent(null);

    private void onConnected(final boolean isContinue, final int soFar, final int total) {
        downloadTransfer.setSoFarBytes(soFar);
        downloadTransfer.setTotalBytes(total);
        downloadTransfer.setEtag(this.etag);
        downloadTransfer.setIsContinue(isContinue);
        downloadTransfer.setStatus(FileDownloadStatus.connected);

        helper.update(downloadTransfer.getDownloadId(), FileDownloadStatus.connected, soFar, total);

        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(event.setTransfer(downloadTransfer.copy()));
    }

    private long lastNotifiedSoFar = 0;

    private void onProcess(final int soFar, final int total) {
        if (soFar != total) {
            downloadTransfer.setSoFarBytes(soFar);
            downloadTransfer.setTotalBytes(total);
            downloadTransfer.setStatus(FileDownloadStatus.progress);

            helper.update(downloadTransfer.getDownloadId(), FileDownloadStatus.progress, soFar, total);
        }

        if (maxNotifyBytes < 0 || soFar - lastNotifiedSoFar < maxNotifyBytes) {
            return;
        }

        lastNotifiedSoFar = soFar;
        FileDownloadLog.d(this, "On progress %d %d %d", downloadTransfer.getDownloadId(), soFar, total);


        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(event.setTransfer(downloadTransfer));

    }

    private void onRetry(Throwable ex, final int retryTimes, final int soFarBytes){
        FileDownloadLog.e(this, ex, "On retry %d %s %d %d", downloadTransfer.getDownloadId(), ex.getMessage(), retryTimes, autoRetryTimes);

        ex = exFiltrate(ex);
        downloadTransfer.setStatus(FileDownloadStatus.retry);
        downloadTransfer.setThrowable(ex);
        downloadTransfer.setRetryingTimes(retryTimes);
        downloadTransfer.setSoFarBytes(soFarBytes);
        // TODO 目前是做断点续传，实际还需要看情况而定

        helper.updateRetry(downloadTransfer.getDownloadId(), ex.getMessage(), retryTimes);

        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(
                new DownloadTransferEvent(downloadTransfer
                        .copy()// because we must make sure retry status no change by downloadTransfer reference
                ));
    }

    private void onError(Throwable ex) {
        FileDownloadLog.e(this, ex, "On error %d %s", downloadTransfer.getDownloadId(), ex.getMessage());

        ex = exFiltrate(ex);
        downloadTransfer.setStatus(FileDownloadStatus.error);
        downloadTransfer.setThrowable(ex);


        helper.updateError(downloadTransfer.getDownloadId(), ex.getMessage());

        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(event.setTransfer(downloadTransfer));
    }

    private void onComplete(final int total) {
        FileDownloadLog.d(this, "On completed %d %d", downloadTransfer.getDownloadId(), total);
        downloadTransfer.setStatus(FileDownloadStatus.completed);

        helper.updateComplete(downloadTransfer.getDownloadId(), total);

        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(event.setTransfer(downloadTransfer));
    }

    private void onPause() {
        this.isRunning = false;
        FileDownloadLog.d(this, "On paused %d %d %d", downloadTransfer.getDownloadId(), downloadTransfer.getSoFarBytes(), downloadTransfer.getTotalBytes());
        downloadTransfer.setStatus(FileDownloadStatus.paused);

        helper.updatePause(downloadTransfer.getDownloadId());

        // 这边没有必要从服务端再回调，由于直接调pause看是否已经成功
//        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    public void onResume() {
        FileDownloadLog.d(this, "On resume %d", downloadTransfer.getDownloadId());
        downloadTransfer.setStatus(FileDownloadStatus.pending);

        this.isPending = true;

        helper.updatePending(downloadTransfer.getDownloadId());

        FileDownloadProcessEventPool.getImpl().asyncPublishInNewThread(event.setTransfer(downloadTransfer));
    }

    private boolean isCancelled() {
        return this.downloadModel.isCanceled();
    }

    // ----------------------------------
    private RandomAccessFile getRandomAccessFile(final boolean append) throws Throwable {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException(String.format("found invalid internal destination path, empty"));
        }

        if (!FileDownloadUtils.isFilenameValid(path)) {
            throw new RuntimeException(String.format("found invalid internal destination filename %s", path));
        }

        File file = new File(path);

        if (file.exists() && file.isDirectory()) {
            throw new RuntimeException(String.format("found invalid internal destination path[%s], & path is directory[%B]", path, file.isDirectory()));
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException(String.format("create new file error  %s", file.getAbsolutePath()));
            }
        }

        RandomAccessFile outFd = new RandomAccessFile(file, "rw");
        if (append) {
            outFd.seek(downloadTransfer.getSoFarBytes());
        }
        return outFd;
//        return new FileOutputStream(file, append);
    }

    private void checkIsContinueAvailable() {
        File file = new File(path);
        if (file.exists()) {
            final long fileLength = file.length();
            if (fileLength >= downloadTransfer.getSoFarBytes() && this.etag != null && fileLength < downloadTransfer.getTotalBytes()) {
                // 如果fileLength >= total bytes 视为脏数据，从头开始下载
                FileDownloadLog.d(this, "adjust sofar old[%d] new[%d]", downloadTransfer.getSoFarBytes(), fileLength);

                this.isContinueDownloadAvailable = true;
            } else {
                final boolean result = file.delete();
                FileDownloadLog.d(this, "delete file for dirty file %B, fileLength[%d], sofar[%d] total[%d] etag", result, fileLength, downloadTransfer.getSoFarBytes(), downloadTransfer.getTotalBytes());
            }
        }
    }

    private Throwable exFiltrate(Throwable ex) {
        if (TextUtils.isEmpty(ex.getMessage())) {
            if (ex instanceof SocketTimeoutException) {
                ex = new RuntimeException(ex.getClass().getSimpleName(), ex);
            }
        }

        return ex;
    }
}
