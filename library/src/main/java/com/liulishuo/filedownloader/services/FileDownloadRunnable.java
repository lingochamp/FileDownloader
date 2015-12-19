package com.liulishuo.filedownloader.services;

import android.os.Process;
import android.text.TextUtils;

import com.liulishuo.filedownloader.event.FileDownloadTransferEvent;
import com.liulishuo.filedownloader.event.FileEventPool;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;

/**
 * Created by Jacksgong on 9/24/15.
 */
class FileDownloadRunnable implements Runnable {

    private static final int BUFFER_SIZE = 1024 * 4;
    private FileDownloadTransferModel downloadTransfer;

    private String url;
    private String path;

    private IFileDownloadDBHelper helper;

    private int maxNotifyBytes;


    private int maxNotifyNums = 0;

    //tmp
    private boolean isContinueDownloadAvailable;

    // etag
    private String etag;

    private final FileDownloadModel downloadModel;

    public int getId() {
        return downloadModel.getId();
    }

    private volatile boolean isRunning = false;
    private volatile boolean isPending = false;

    public FileDownloadRunnable(final FileDownloadModel model, final IFileDownloadDBHelper helper) {
        isPending = true;
        isRunning = false;

        this.helper = helper;

        this.url = model.getUrl();
        this.path = model.getPath();

        downloadTransfer = new FileDownloadTransferModel();

        downloadTransfer.setDownloadId(model.getId());
        downloadTransfer.setStatus(model.getStatus());
        downloadTransfer.setSofarBytes(model.getSoFar());
        downloadTransfer.setTotalBytes(model.getTotal());

        maxNotifyNums = model.getProgressCallbackTimes();
        maxNotifyNums = maxNotifyNums <= 0 ? 0 : maxNotifyNums;

        this.isContinueDownloadAvailable = false;

        this.etag = model.geteTag();
        this.downloadModel = model;
    }

    public boolean isExist() {
        return isPending || isRunning;
    }

    @Override
    public void run() {
        isPending = false;
        isRunning = true;
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            FileDownloadModel model = helper.find(getId());

            if (model == null) {
                FileDownloadLog.e(this, "start runnable but model == null?? %s", getId());

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

            FileDownloadLog.d(FileDownloadRunnable.class, "start download %s %s", getId(), model.getUrl());

            OkHttpClient httpClient = new OkHttpClient();

            checkIsContinueAvailable();

            Request.Builder headerBuilder = new Request.Builder().url(url);
            addHeader(headerBuilder);

            Call call = httpClient.newCall(headerBuilder.get().build());

            Response response = call.execute();

            final boolean isSuccedStart = response.code() == 200;
            final boolean isSucceedContinue = response.code() == 206 && isContinueDownloadAvailable;

            if (isSuccedStart || isSucceedContinue) {
                int total = downloadTransfer.getTotalBytes();
                int sofar = 0;
                if (isSuccedStart || total == 0) {
                    // TODO 目前没有对 2^31-1bit以上大小支持，未来会开发一个对应的库
                    total = (int) response.body().contentLength();
                }

                if (isSucceedContinue) {
                    sofar = downloadTransfer.getSofarBytes();
                    FileDownloadLog.d(this, "add range %d %d", downloadTransfer.getSofarBytes(), downloadTransfer.getTotalBytes());
                }

                InputStream inputStream = null;
                RandomAccessFile accessFile = getRandomAccessFile(isSucceedContinue);
                try {
                    inputStream = response.body().byteStream();
                    byte[] buff = new byte[BUFFER_SIZE];
                    maxNotifyBytes = maxNotifyNums <= 0 ? -1 : total / maxNotifyNums;

                    onProcess(sofar, total);

                    updateHeader(response);

                    do {
                        int readed = inputStream.read(buff);
                        if (readed == -1) {
                            break;
                        }

                        accessFile.write(buff, 0, readed);

                        //write buff
                        sofar += readed;
                        if (accessFile.length() != sofar) {
                            // 文件大小必须会等于正在写入的大小
                            onError(new RuntimeException("file be changed by others when downloading"));
                            return;
                        } else {
                            onProcess(sofar, total);
                        }

                        if (isCancelled()) {
                            onPause();
                            return;
                        }

                    } while (true);


                    if (sofar == total) {
                        onComplete(total);
                    } else {
                        onError(new RuntimeException(
                                String.format("sofar[%d] not equal total[%d]", sofar, total)
                        ));
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
            onError(ex);
        } finally {
            isRunning = false;
        }


    }

    private void addHeader(Request.Builder builder) {
        if (isContinueDownloadAvailable) {
            builder.addHeader("If-Match", this.etag);
            builder.addHeader("Range", String.format("bytes=%d-", downloadTransfer.getSofarBytes()));
        }
    }

    private void updateHeader(Response response) {
        if (response == null) {
            throw new RuntimeException("response is null when updateHeader");
        }

        boolean needRefresh = false;
        final String oldEtag = this.etag;
        final String newEtag = response.header("Etag");

        if (oldEtag == null && newEtag != null) {
            FileDownloadLog.w(this, "no etag find by header");
            needRefresh = true;
        } else if (oldEtag != null && newEtag != null && !oldEtag.equals(newEtag)) {
            needRefresh = true;
        }

        if (needRefresh) {
            helper.updateHeader(downloadTransfer.getDownloadId(), newEtag);
        }

    }

    private long lastNotifiedSofar = 0;

    private void onProcess(final int sofar, final int total) {
        if (maxNotifyBytes < 0 || sofar - lastNotifiedSofar < maxNotifyBytes && sofar != total) {
            return;
        }
        lastNotifiedSofar = sofar;
        FileDownloadLog.d(this, "On progress %d %d %d", downloadTransfer.getDownloadId(), sofar, total);

        downloadTransfer.setSofarBytes(sofar);
        downloadTransfer.setTotalBytes(total);
        downloadTransfer.setStatus(FileDownloadStatus.progress);

        helper.update(downloadTransfer.getDownloadId(), FileDownloadStatus.progress, sofar, total);

        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    private void onError(Throwable ex) {
        FileDownloadLog.e(this, ex, "On error %d %s", downloadTransfer.getDownloadId(), ex.getMessage());

        if (TextUtils.isEmpty(ex.getMessage())) {
            if (ex instanceof SocketTimeoutException) {
                ex = new RuntimeException(ex.getClass().getSimpleName(), ex);
            }
        }

        downloadTransfer.setStatus(FileDownloadStatus.error);
        downloadTransfer.setThrowable(ex);


        helper.updateError(downloadTransfer.getDownloadId(), ex.getMessage());

        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    private void onComplete(final int total) {
        FileDownloadLog.d(this, "On completed %d %d", downloadTransfer.getDownloadId(), total);
        downloadTransfer.setStatus(FileDownloadStatus.completed);

        helper.updateComplete(downloadTransfer.getDownloadId(), total);

        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    private void onPause() {
        FileDownloadLog.d(this, "On paused %d %d %d", downloadTransfer.getDownloadId(), downloadTransfer.getSofarBytes(), downloadTransfer.getTotalBytes());
        downloadTransfer.setStatus(FileDownloadStatus.paused);

        helper.updatePause(downloadTransfer.getDownloadId());

        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    public void onResume() {
        FileDownloadLog.d(this, "On resume %d", downloadTransfer.getDownloadId());
        downloadTransfer.setStatus(FileDownloadStatus.pending);
        this.isPending = true;

        helper.updatePending(downloadTransfer.getDownloadId());

        FileEventPool.getImpl().asyncPublishInNewThread(new FileDownloadTransferEvent(downloadTransfer));
    }

    private boolean isCancelled() {
        return this.downloadModel.isCancel();
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
            file.createNewFile();
        }

        RandomAccessFile outFd = new RandomAccessFile(file, "rw");
        if (append) {
            outFd.seek(downloadTransfer.getSofarBytes());
        }
        return outFd;
//        return new FileOutputStream(file, append);
    }

    private void checkIsContinueAvailable() {
        File file = new File(path);
        if (file.exists()) {
            final long fileLength = file.length();
            if (fileLength >= downloadTransfer.getSofarBytes() && this.etag != null) {
                FileDownloadLog.d(this, "adjust sofar old[%d] new[%d]", downloadTransfer.getSofarBytes(), fileLength);

                // 如果fileLength >= total bytes 视为脏数据，从头开始下载
                this.isContinueDownloadAvailable = fileLength < downloadTransfer.getTotalBytes();
            } else {
                file.delete();
            }
        }
    }
}
