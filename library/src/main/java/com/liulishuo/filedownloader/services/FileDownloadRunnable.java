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

import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException;
import com.liulishuo.filedownloader.exception.FileDownloadHttpException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import com.liulishuo.filedownloader.message.MessageSnapshotTaker;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Jacksgong on 9/24/15.
 * <p/>
 * An atom download runnable for one task.
 *
 * @see #loop(FileDownloadModel)
 * @see #fetch(Response, boolean, long, long)
 * @see FileDownloadThreadPool
 */
@SuppressWarnings("WeakerAccess")
public class FileDownloadRunnable implements Runnable {

    private static final int BUFFER_SIZE = 1024 * 4;

    private long progressThresholdBytes;
    private int maxProgressCount = 0;
    private boolean isResumeDownloadAvailable;
    private boolean isResuming;
    private Throwable throwable;
    private int retryingTimes;

    private FileDownloadModel model;

    private volatile boolean isRunning = false;
    private volatile boolean isPending = false;

    private final IFileDownloadDBHelper helper;
    private final OkHttpClient client;
    private final int autoRetryTimes;

    private final FileDownloadHeader header;

    private volatile boolean isCanceled = false;

    public FileDownloadRunnable(final OkHttpClient client, final FileDownloadModel model,
                                final IFileDownloadDBHelper helper, final int autoRetryTimes,
                                final FileDownloadHeader header) {
        isPending = true;
        isRunning = false;

        this.client = client;
        this.helper = helper;
        this.header = header;

        maxProgressCount = model.getCallbackProgressTimes();
        maxProgressCount = maxProgressCount <= 0 ? 0 : maxProgressCount;

        this.isResumeDownloadAvailable = false;

        this.model = model;

        this.autoRetryTimes = autoRetryTimes;
    }

    public int getId() {
        return model.getId();
    }

    public boolean isExist() {
        return isPending || isRunning;
    }

    public boolean isResuming() {
        return isResuming;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public int getRetryingTimes() {
        return retryingTimes;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        isPending = false;
        isRunning = true;

        try {
            // Step 1, check model
            if (model == null) {
                FileDownloadLog.e(this, "start runnable but model == null?? %s", getId());

                this.model = helper.find(getId());

                if (this.model == null) {
                    FileDownloadLog.e(this, "start runnable but downloadMode == null?? %s", getId());
                    return;
                }
            }

            // Step 2, check status
            if (model.getStatus() != FileDownloadStatus.pending) {
                if (model.getStatus() == FileDownloadStatus.paused) {
                    if (FileDownloadLog.NEED_LOG) {
                        /**
                         * @see FileDownloadThreadPool#cancel(int), the invoking simultaneously
                         * with here. And this area is invoking before there, so, {@code cancel(int)}
                         * is fail.
                         *
                         * High concurrent cause.
                         */
                        FileDownloadLog.d(this, "High concurrent cause, start runnable but " +
                                "already paused %d", getId());
                    }
                } else {
                    FileDownloadLog.e(this, "start runnable but status err %s %d",
                            model.getStatus(), getId());
                    // 极低概率事件，相同url与path的任务被放到了线程池中(目前在入池之前是有检测的，但是还是存在极低概率的同步问题) 执行的时候有可能会遇到
                    onError(new RuntimeException(
                            FileDownloadUtils.formatString("start runnable but status err %s %d",
                                    model.getStatus())));
                }

                return;
            }

            onStarted();

            // Step 3, start download
            loop(model);

        } finally {
            isRunning = false;
        }


    }

    private void loop(FileDownloadModel model) {
        int retryingTimes = 0;

        do {
            // loop for retry
            Response response = null;
            long soFar = 0;
            try {

                // Step 1, check is paused
                if (isCancelled()) {
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog.d(this, "already canceled %d %d", model.getId(), model.getStatus());
                    }
                    onPause();
                    break;
                }

                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadRunnable.class, "start download %s %s", getId(), model.getUrl());
                }

                // Step 2, handle resume from breakpoint
                checkIsResumeAvailable();

                Request.Builder requestBuilder = new Request.Builder().url(model.getUrl());
                addHeader(requestBuilder);
                requestBuilder.tag(this.getId());
                // 目前没有指定cache，下载任务非普通REST请求，用户已经有了存储的地方
                requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);

                // start download----------------
                // Step 3, init request
                final Request request = requestBuilder.get().build();
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "%s request header %s", getId(), request.headers());
                }

                Call call = client.newCall(request);

                // Step 4, build connect
                response = call.execute();

                final boolean isSucceedStart = response.code() == HttpURLConnection.HTTP_OK;
                final boolean isSucceedResume = response.code() == HttpURLConnection.HTTP_PARTIAL &&
                        isResumeDownloadAvailable;

                if (isResumeDownloadAvailable && !isSucceedResume) {
                    FileDownloadLog.w(this, "tried to resume from the break point[%d], but the " +
                                    "response code is %d, not 206(PARTIAL).", model.getSoFar(),
                            response.code());
                }

                if (isSucceedStart || isSucceedResume) {
                    long total = model.getTotal();
                    final String transferEncoding = response.header("Transfer-Encoding");

                    // Step 5, check response's header
                    if (isSucceedStart || total <= 0) {
                        if (transferEncoding == null) {
                            total = response.body().contentLength();
                        } else {
                            // if transfer not nil, ignore content-length
                            total = -1;
                        }
                    }

                    // TODO consider if not is chunked & http 1.0/(>=http1.1 & connect not be keep live) may not give content-length
                    if (total < 0) {
                        // invalid total length
                        final boolean isEncodingChunked = transferEncoding != null
                                && transferEncoding.equals("chunked");
                        if (!isEncodingChunked) {
                            // not chunked transfer encoding data
                            if (FileDownloadProperties.getImpl().HTTP_LENIENT) {
                                // do not response content-length either not chunk transfer encoding,
                                // but HTTP lenient is true, so handle as the case of transfer encoding chunk
                                total = -1;
                                if (FileDownloadLog.NEED_LOG) {
                                    FileDownloadLog.d(this, "%d response header is not legal but " +
                                            "HTTP lenient is true, so handle as the case of " +
                                            "transfer encoding chunk", getId());
                                }
                            } else {
                                throw new FileDownloadGiveUpRetryException("can't know the size of the " +
                                        "download file, and its Transfer-Encoding is not Chunked " +
                                        "either.\nyou can ignore such exception by add " +
                                        "http.lenient=true to the filedownloader.properties");
                            }
                        }
                    }

                    if (isSucceedResume) {
                        soFar = model.getSoFar();
                    }

                    // Step 6, callback on connected, and update header to db. for save etag.
                    onConnected(isSucceedResume, total, findEtag(response));

                    // Step 7, start fetch datum from input stream & write to file
                    if (fetch(response, isSucceedResume, soFar, total)) {
                        break;
                    }

                } else {
                    throw new FileDownloadHttpException(request, response);
                }


            } catch (Throwable ex) {
                // TODO 决策是否需要重试，是否是用户决定，或者根据错误码处理
                if (autoRetryTimes > retryingTimes++
                        && !(ex instanceof FileDownloadGiveUpRetryException)) {
                    // retry
                    onRetry(ex, retryingTimes);
                } else {


                    // error
                    onError(ex);
                    break;
                }
            } finally {
                if (response != null && response.body() != null) {
                    response.body().close();
                }
            }

        } while (true);
    }

    /**
     * @return Whether finish looper or not.
     */
    @SuppressWarnings("SameReturnValue")
    private boolean fetch(Response response, boolean isSucceedContinue,
                          long soFar, long total) throws Throwable {
        // fetching datum
        InputStream inputStream = null;
        final RandomAccessFile accessFile = getRandomAccessFile(isSucceedContinue, total);
        final FileDescriptor fd = accessFile.getFD();
        try {
            // Step 1, get input stream
            inputStream = response.body().byteStream();
            byte[] buff = new byte[BUFFER_SIZE];
            progressThresholdBytes = maxProgressCount <= 0 ? -1 : total / (maxProgressCount + 1);

            // enter fetching loop(Step 2->6)
            do {
                // Step 2, read from input stream.
                int byteCount = inputStream.read(buff);
                if (byteCount == -1) {
                    break;
                }

                // Step 3, writ to file
                accessFile.write(buff, 0, byteCount);

                // Step 4, adapter sofar
                soFar += byteCount;

                // Step 5, check whether file is changed by others
                if (accessFile.length() < soFar) {
                    throw new RuntimeException(
                            FileDownloadUtils.formatString("the file was changed by others when" +
                                    " downloading. %d %d", accessFile.length(), soFar));
                } else {
                    // callback on progressing
                    onProgress(soFar, total, fd);
                }

                // Step 6, check pause
                if (isCancelled()) {
                    // callback on paused
                    onPause();
                    return true;
                }

            } while (true);


            // Step 7, adapter chunked transfer encoding
            if (total == -1) {
                total = soFar;
            }

            // Step 8, complete download
            if (soFar == total) {
                // callback on completed
                onComplete(total);

                return true;
            } else {
                throw new RuntimeException(
                        FileDownloadUtils.formatString("sofar[%d] not equal total[%d]", soFar, total));
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            //noinspection ConstantConditions
            if (accessFile != null) {
                accessFile.close();
            }
        }
    }

    private void addHeader(Request.Builder builder) {
        if (header != null && header.getNamesAndValues() != null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.v(this, "%d add outside header: %s", getId(), header);
            }
            builder.headers(Headers.of(header.getNamesAndValues()));
        }

        if (isResumeDownloadAvailable) {
            if (!TextUtils.isEmpty(model.getETag())) {
                builder.addHeader("If-Match", model.getETag());
            }
            builder.addHeader("Range", FileDownloadUtils.formatString("bytes=%d-", model.getSoFar()));
        }
    }

    private String findEtag(Response response) {
        if (response == null) {
            throw new RuntimeException("response is null when findEtag");
        }

        final String newEtag = response.header("Etag");

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "etag find by header %d %s", getId(), newEtag);
        }

        return newEtag;
    }

    public void cancelRunnable() {
        this.isCanceled = true;
        onPause();
    }

    private void onConnected(final boolean resuming, final long total, final String etag) {
        helper.updateConnected(model, total, etag);

        this.isResuming = resuming;

        onStatusChanged(model.getStatus());
    }

    private long lastProgressBytes = 0;

    private long lastUpdateBytes = 0;
    private long lastUpdateTime = 0;


    private void onProgress(final long soFar, final long total, final FileDescriptor fd) {
        if (soFar == total) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long bytesDelta = soFar - lastUpdateBytes;
        long timeDelta = now - lastUpdateTime;

        if (bytesDelta > FileDownloadUtils.getMinProgressStep() &&
                timeDelta > FileDownloadUtils.getMinProgressTime()) {
            try {
                fd.sync();
            } catch (SyncFailedException e) {
                e.printStackTrace();
            }
            helper.updateProgress(model, soFar);
            lastUpdateBytes = soFar;
            lastUpdateTime = now;
        } else {
            if (model.getStatus() != FileDownloadStatus.progress) {
                model.setStatus(FileDownloadStatus.progress);
            }
            model.setSoFar(soFar);
        }

        if (progressThresholdBytes < 0 ||
                soFar - lastProgressBytes < progressThresholdBytes) {
            return;
        }

        lastProgressBytes = soFar;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On progress %d %d %d", getId(), soFar, total);
        }

        onStatusChanged(model.getStatus());

    }

    private void onRetry(Throwable ex, final int retryTimes) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On retry %d %s %d %d", getId(), ex,
                    retryTimes, autoRetryTimes);
        }

        ex = exFiltrate(ex);
        helper.updateRetry(model, ex.getMessage());

        this.throwable = ex;
        this.retryingTimes = retryTimes;

        onStatusChanged(model.getStatus());
    }

    private void onError(Throwable ex) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On error %d %s", getId(), ex);
        }

        ex = exFiltrate(ex);
        helper.updateError(model, ex.getMessage(), model.getSoFar());

        this.throwable = ex;

        onStatusChanged(model.getStatus());
    }

    private void onComplete(final long total) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On completed %d %d %B", getId(), total, isCancelled());
        }
        helper.updateComplete(model, total);

        onStatusChanged(model.getStatus());
    }

    private void onPause() {
        this.isRunning = false;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On paused %d %d %d", getId(),
                    model.getSoFar(), model.getTotal());
        }

        helper.updatePause(model, model.getSoFar());

        onStatusChanged(model.getStatus());
    }

    private void onStarted() {
        model.setStatus(FileDownloadStatus.started);
        onStatusChanged(model.getStatus());
    }

    public void onPending() {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "On resume %d", getId());
        }

        this.isPending = true;

        helper.updatePending(model);

        onStatusChanged(model.getStatus());
    }

    private final Object statusChangedNotifyLock = new Object();

    @SuppressWarnings("UnusedParameters")
    private void onStatusChanged(final byte status) {
        // In current situation, it maybe invoke this method simultaneously between #onPause() and
        // others.
        synchronized (statusChangedNotifyLock) {
            if (model.getStatus() == FileDownloadStatus.paused) {
                if (FileDownloadLog.NEED_LOG) {
                    /**
                     * Already paused or the current status is paused.
                     *
                     * We don't need to call-back to Task in here, because the pause status has
                     * already handled by {@link BaseDownloadTask#pause()} manually.
                     *
                     * In some case, it will arrive here by High concurrent cause.  For performance
                     * more make sense.
                     *
                     * High concurrent cause.
                     */
                    FileDownloadLog.d(this, "High concurrent cause, Already paused and we don't " +
                            "need to call-back to Task in here, %d", getId());
                }
                return;
            }

            MessageSnapshotFlow.getImpl().
                    inflow(MessageSnapshotTaker.take(status, model, this));
        }
    }

    private boolean isCancelled() {
        return isCanceled;
    }

    // ----------------------------------
    private RandomAccessFile getRandomAccessFile(final boolean append, final long totalBytes)
            throws IOException {
        final String path = model.getPath();
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("found invalid internal destination path, empty");
        }

        //noinspection ConstantConditions
        if (!FileDownloadUtils.isFilenameValid(path)) {
            throw new RuntimeException(
                    FileDownloadUtils.formatString("found invalid internal destination filename" +
                            " %s", path));
        }

        File file = new File(path);

        if (file.exists() && file.isDirectory()) {
            throw new RuntimeException(
                    FileDownloadUtils.formatString("found invalid internal destination path[%s]," +
                            " & path is directory[%B]", path, file.isDirectory()));
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException(
                        FileDownloadUtils.formatString("create new file error  %s",
                                file.getAbsolutePath()));
            }
        }

        RandomAccessFile outFd = new RandomAccessFile(file, "rw");

        // check the available space bytes whether enough or not.
        if (totalBytes > 0) {
            final long breakpointBytes = outFd.length();
            final long requiredSpaceBytes = totalBytes - breakpointBytes;

            final long freeSpaceBytes = FileDownloadUtils.getFreeSpaceBytes(path);

            if (freeSpaceBytes < requiredSpaceBytes) {
                outFd.close();
                // throw a out of space exception.
                throw new FileDownloadOutOfSpaceException(freeSpaceBytes,
                        requiredSpaceBytes, breakpointBytes);
            } else {
                // pre allocate.
                outFd.setLength(totalBytes);
            }
        }

        if (append) {
            outFd.seek(model.getSoFar());
        }

        return outFd;
    }

    private void checkIsResumeAvailable() {
        if (FileDownloadMgr.checkBreakpointAvailable(getId(), this.model)) {
            this.isResumeDownloadAvailable = true;
        } else {
            this.isResumeDownloadAvailable = false;
            // delete dirty file
            File file = new File(model.getPath());
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private Throwable exFiltrate(Throwable ex) {
        /**
         * Only handle the case of Chunked resource, if it is not chunked, has already been handled
         * in {@link #getRandomAccessFile(boolean, long)}.
         */
        if (model.getTotal() == -1 && ex instanceof IOException &&
                new File(model.getPath()).exists()) {
            // chunked
            final long freeSpaceBytes = FileDownloadUtils.
                    getFreeSpaceBytes(model.getPath());
            if (freeSpaceBytes <= BUFFER_SIZE) {
                // free space is not enough.
                long downloadedSize = 0;
                final File file = new File(model.getPath());
                if (!file.exists()) {
                    FileDownloadLog.e(FileDownloadRunnable.class, ex, "Exception with: free " +
                            "space isn't enough, and the target file not exist.");
                } else {
                    downloadedSize = file.length();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    ex = new FileDownloadOutOfSpaceException(freeSpaceBytes, BUFFER_SIZE,
                            downloadedSize, ex);
                } else {
                    ex = new FileDownloadOutOfSpaceException(freeSpaceBytes, BUFFER_SIZE,
                            downloadedSize);
                }

            }
        }

        // Provide the exception message.
        else if (TextUtils.isEmpty(ex.getMessage())) {
            if (ex instanceof SocketTimeoutException) {
                ex = new RuntimeException(ex.getClass().getSimpleName(), ex);
            }
        }

        return ex;
    }
}
