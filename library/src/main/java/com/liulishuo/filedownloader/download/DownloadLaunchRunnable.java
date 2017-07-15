/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.download;

import android.Manifest;
import android.os.Process;

import com.liulishuo.filedownloader.DownloadTask;
import com.liulishuo.filedownloader.IThreadPoolMonitor;
import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException;
import com.liulishuo.filedownloader.exception.FileDownloadHttpException;
import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.FileDownloadDatabase;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.util.FileDownloadExecutors;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * You can use this to launch downloading, on here the download will be launched separate following
 * steps:
 * <p/>
 * step 1. create the first connection
 *          ( this first connection is used for:
 *                  1. checkup the saved etag is overdue
 *                  2. checkup whether the partial-accept is supported
 *                  3. checkup whether the current connection is chunked. )
 *
 * step 2. if the saved etag is overdue -> jump to step 1 to checkup whether the partial-accept is supported.
 * step 3. if (NOT chunked) & partial-accept & output stream support-seek:
 *              create multiple {@link DownloadTask} to download.
 *         else:
 *              reuse the first connection and use {@link FetchDataTask} to fetch data from the connection.
 * <p/>
 * We use {@link DownloadStatusCallback} to handle all events sync to DB/filesystem and callback to user.
 */
public class DownloadLaunchRunnable implements Runnable, ProcessCallback {

    private final DownloadStatusCallback statusCallback;
    private final int DEFAULT_CONNECTION_COUNT = 5;
    private final FileDownloadModel model;
    private final FileDownloadHeader userRequestHeader;
    private final boolean isForceReDownload;
    private final boolean isWifiRequired;

    private final FileDownloadDatabase database;
    private final IThreadPoolMonitor threadPoolMonitor;

    private boolean isTriedFixRangeNotSatisfiable;

    private int validRetryTimes;

    /**
     * None of the ranges in the request's Range header field overlap the current extent of the
     * selected resource or that the set of ranges requested has been rejected due to invalid
     * ranges or an excessive request of small or overlapping ranges.
     */
    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int TOTAL_VALUE_IN_CHUNKED_RESOURCE = -1;


    private final boolean supportSeek;

    private final ArrayList<DownloadRunnable> downloadRunnableList = new ArrayList<>(DEFAULT_CONNECTION_COUNT);
    private FetchDataTask singleFetchDataTask;
    private boolean isSingleConnection;

    private final static ThreadPoolExecutor DOWNLOAD_EXECUTOR = FileDownloadExecutors
            .newDefaultThreadPool(Integer.MAX_VALUE, "download-executor");

    private boolean isResumeAvailableOnDB;
    private boolean acceptPartial;
    private boolean isChunked;

    private volatile boolean alive;
    private volatile boolean paused;

    private String redirectedUrl;

    private DownloadLaunchRunnable(FileDownloadModel model, FileDownloadHeader header,
                                   IThreadPoolMonitor threadPoolMonitor,
                                   final int minIntervalMillis, int callbackProgressMaxCount,
                                   boolean isForceReDownload, boolean isWifiRequired, int maxRetryTimes) {
        this.alive = true;
        this.paused = false;
        this.isTriedFixRangeNotSatisfiable = false;

        this.model = model;
        this.userRequestHeader = header;
        this.isForceReDownload = isForceReDownload;
        this.isWifiRequired = isWifiRequired;
        this.database = CustomComponentHolder.getImpl().getDatabaseInstance();
        this.supportSeek = CustomComponentHolder.getImpl().isSupportSeek();
        this.threadPoolMonitor = threadPoolMonitor;
        this.validRetryTimes = maxRetryTimes;

        this.statusCallback = new DownloadStatusCallback(model,
                maxRetryTimes, minIntervalMillis, callbackProgressMaxCount);
    }

    private DownloadLaunchRunnable(DownloadStatusCallback callback, FileDownloadModel model, FileDownloadHeader header,
                                   IThreadPoolMonitor threadPoolMonitor,
                                   final int minIntervalMillis, int callbackProgressMaxCount,
                                   boolean isForceReDownload, boolean isWifiRequired, int maxRetryTimes) {
        this.alive = true;
        this.paused = false;
        this.isTriedFixRangeNotSatisfiable = false;

        this.model = model;
        this.userRequestHeader = header;
        this.isForceReDownload = isForceReDownload;
        this.isWifiRequired = isWifiRequired;
        this.database = CustomComponentHolder.getImpl().getDatabaseInstance();
        this.supportSeek = CustomComponentHolder.getImpl().isSupportSeek();
        this.threadPoolMonitor = threadPoolMonitor;
        this.validRetryTimes = maxRetryTimes;

        this.statusCallback = callback;
    }

    static DownloadLaunchRunnable createForTest(DownloadStatusCallback callback,
                                                FileDownloadModel model, FileDownloadHeader header,
                                                IThreadPoolMonitor threadPoolMonitor,
                                                final int minIntervalMillis, int callbackProgressMaxCount,
                                                boolean isForceReDownload, boolean isWifiRequired, int maxRetryTimes) {
        return new DownloadLaunchRunnable(callback, model, header, threadPoolMonitor,
                minIntervalMillis, callbackProgressMaxCount, isForceReDownload, isWifiRequired,
                maxRetryTimes);
    }

    public void pause() {
        this.paused = true;

        if (singleFetchDataTask != null) singleFetchDataTask.pause();
        @SuppressWarnings("unchecked") ArrayList<DownloadRunnable> pauseList =
                (ArrayList<DownloadRunnable>) downloadRunnableList.clone();
        for (DownloadRunnable runnable : pauseList) {
            if (runnable != null) {
                runnable.pause();
                // if runnable is null, then that one must be completed and removed
            }
        }

        statusCallback.onPaused();
    }

    public void pending() {
        if (model.getConnectionCount() > 1) {
            final List<ConnectionModel> connectionOnDBList = database.findConnectionModel(model.getId());
            if (model.getConnectionCount() == connectionOnDBList.size()) {
                model.setSoFar(ConnectionModel.getTotalOffset(connectionOnDBList));
            } else {
                // dirty
                model.setSoFar(0);
                database.removeConnections(model.getId());
            }
        }

        statusCallback.onPending();
    }

    @Override
    public void run() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            // status checkout
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
                                "already paused %d", model.getId());
                    }

                } else {
                    onError(new RuntimeException(
                            FileDownloadUtils.formatString("Task[%d] can't start the download" +
                                            " runnable, because its status is %d not %d",
                                    model.getId(), model.getStatus(), FileDownloadStatus.pending)));
                }
                return;
            }

            if (!paused) {
                statusCallback.onStartThread();
            }

            do {
                if (paused) {
                    if (FileDownloadLog.NEED_LOG) {
                        /**
                         * @see FileDownloadThreadPool#cancel(int), the invoking simultaneously
                         * with here. And this area is invoking before there, so, {@code cancel(int)}
                         * is fail.
                         *
                         * High concurrent cause.
                         */
                        FileDownloadLog.d(this, "High concurrent cause, start runnable but " +
                                "already paused %d", model.getId());
                    }
                    return;
                }

                FileDownloadConnection connection = null;
                try {


                    // 1. connect
                    checkupBeforeConnect();

                    // the first connection is for: 1. etag verify; 2. first connect.
                    final List<ConnectionModel> connectionOnDBList = database.findConnectionModel(model.getId());
                    final ConnectionProfile connectionProfile = buildFirstConnectProfile(connectionOnDBList);
                    final ConnectTask.Builder build = new ConnectTask.Builder();
                    final ConnectTask firstConnectionTask = build.setDownloadId(model.getId())
                            .setUrl(model.getUrl())
                            .setEtag(model.getETag())
                            .setHeader(userRequestHeader)
                            .setConnectionProfile(connectionProfile)
                            .build();

                    connection = firstConnectionTask.connect();
                    handleFirstConnected(firstConnectionTask.getRequestHeader(),
                            firstConnectionTask, connection);

                    if (paused) {
                        model.setStatus(FileDownloadStatus.paused);
                        return;
                    }

                    // 2. fetch
                    checkupBeforeFetch();
                    final long totalLength = model.getTotal();
                    // pre-allocate if need.
                    handlePreAllocate(totalLength, model.getTempFilePath());

                    final int connectionCount;
                    // start fetching
                    if (isMultiConnectionAvailable()) {
                        if (isResumeAvailableOnDB) {
                            connectionCount = model.getConnectionCount();
                        } else {
                            connectionCount = CustomComponentHolder.getImpl()
                                    .determineConnectionCount(model.getId(), model.getUrl(), model.getPath(), totalLength);
                        }
                    } else {
                        connectionCount = 1;
                    }

                    if (connectionCount <= 0) {
                        throw new IllegalAccessException(FileDownloadUtils
                                .formatString("invalid connection count %d, the connection count" +
                                        " must be larger than 0", connection));
                    }

                    if (paused) {
                        model.setStatus(FileDownloadStatus.paused);
                        return;
                    }

                    isSingleConnection = connectionCount == 1;
                    if (isSingleConnection) {
                        // single connection
                        fetchWithSingleConnection(firstConnectionTask.getProfile(), connection);
                    } else {
                        // multiple connection
                        statusCallback.onMultiConnection();
                        if (isResumeAvailableOnDB) {
                            fetchWithMultipleConnectionFromResume(connectionCount, connectionOnDBList);
                        } else {
                            fetchWithMultipleConnectionFromBeginning(totalLength, connectionCount);
                        }
                    }

                } catch (IOException | IllegalAccessException | InterruptedException | IllegalArgumentException | FileDownloadGiveUpRetryException e) {
                    if (isRetry(e)) {
                        onRetry(e, 0);
                        continue;
                    } else {
                        onError(e);
                    }
                } catch (DiscardSafely discardSafely) {
                    return;
                } catch (RetryDirectly retryDirectly) {
                    model.setStatus(FileDownloadStatus.retry);
                    continue;
                } finally {
                    if (connection != null) connection.ending();
                }

                break;
            } while (true);
        } finally {
            alive = false;
        }
    }

    private boolean isMultiConnectionAvailable() {
        //noinspection SimplifiableIfStatement
        if (isResumeAvailableOnDB && model.getConnectionCount() <= 1) {
            return false;
        }

        return acceptPartial && supportSeek && !isChunked;
    }

    private int determineConnectionCount() {
        return DEFAULT_CONNECTION_COUNT;
    }

    private ConnectionProfile buildFirstConnectProfile(List<ConnectionModel> connectionOnDBList) {
        // check resume available
        final long offset;
        final int connectionCount = model.getConnectionCount();
        final String tempFilePath = model.getTempFilePath();
        final String targetFilePath = model.getTargetFilePath();
        final boolean isMultiConnection = connectionCount > 1;
        if (isMultiConnection && !supportSeek) {
            // can't support seek for multi-connection is fatal problem, so discard resume.
            offset = 0;
        } else {
            final boolean resumeAvailable = FileDownloadUtils.isBreakpointAvailable(model.getId(), model);
            if (resumeAvailable) {
                if (!supportSeek) {
                    offset = new File(tempFilePath).length();
                } else {
                    if (isMultiConnection) {
                        // when it is multi connections, the offset would be 0, because it only store on the connection table.
                        if (connectionCount != connectionOnDBList.size()) {
                            // dirty data
                            offset = 0;
                        } else {
                            offset = ConnectionModel.getTotalOffset(connectionOnDBList);
                        }
                    } else {
                        offset = model.getSoFar();
                    }

                }
            } else {
                offset = 0;
            }
        }

        model.setSoFar(offset);
        isResumeAvailableOnDB = offset > 0;
        if (!isResumeAvailableOnDB) {
            database.removeConnections(model.getId());
            FileDownloadUtils.deleteTaskFiles(targetFilePath, tempFilePath);
        }

        return new ConnectionProfile(0, offset, 0, model.getTotal() - offset);
    }

    private void handleFirstConnected(Map<String, List<String>> requestHeader,
                                      ConnectTask connectTask, FileDownloadConnection connection)
            throws IOException, RetryDirectly, IllegalArgumentException {
        final int id = model.getId();
        final int code = connection.getResponseCode();

        // if the response status code isn't point to PARTIAL/OFFSET, isSucceedResume will
        // be assigned to false, so filedownloader will download the file from very beginning.
        acceptPartial = (code == HttpURLConnection.HTTP_PARTIAL
                || code == FileDownloadConnection.RESPONSE_CODE_FROM_OFFSET);
        final boolean onlyFromBeginning = (code == HttpURLConnection.HTTP_OK
                || code == HttpURLConnection.HTTP_CREATED
                || code == FileDownloadConnection.NO_RESPONSE_CODE);

        final String oldEtag = model.getETag();
        String newEtag = FileDownloadUtils.findEtag(id, connection);

        // handle whether need retry because of etag is overdue
        boolean isPreconditionFailed = false;
        do {
            if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
                isPreconditionFailed = true;
                break;
            }

            if (oldEtag != null && !oldEtag.equals(newEtag)) {
                // etag changed.
                if (onlyFromBeginning || acceptPartial) {
                    // 200 or 206
                    isPreconditionFailed = true;
                    break;
                }
            }

            if (code == HttpURLConnection.HTTP_CREATED && connectTask.isRangeNotFromBeginning()) {
                // The request has been fulfilled and has resulted in one or more new resources being created.
                // mark this case is precondition failed for
                // 1. checkout whether accept partial
                // 2. 201 means new resources so range must be from beginning otherwise it can't match
                // local range.
                isPreconditionFailed = true;
                break;
            }

        } while (false);


        if (isPreconditionFailed) {
            // the file on remote is changed
            if (isResumeAvailableOnDB) {
                FileDownloadLog.w(this, "there is precondition failed on this request[%d] " +
                                "with old etag[%s]、new etag[%s]、response code is %d",
                        id, oldEtag, newEtag, code);
            }

            database.removeConnections(model.getId());
            FileDownloadUtils.deleteTaskFiles(model.getTargetFilePath(), model.getTempFilePath());
            isResumeAvailableOnDB = false;

            if (oldEtag != null && oldEtag.equals(newEtag)) {
                FileDownloadLog.w(this, "the old etag[%s] is the same to the new etag[%s], " +
                                "but the response status code is %d not Partial(206), so wo have to " +
                                "start this task from very beginning for task[%d]!",
                        oldEtag, newEtag, code, id);
                newEtag = null;
            }

            model.setSoFar(0);
            model.setTotal(0);
            model.setETag(newEtag);
            model.resetConnectionCount();

            database.updateOldEtagOverdue(id, model.getETag(), model.getSoFar(), model.getTotal(), model.getConnectionCount());

            // retry to check whether support partial or not.
            throw new RetryDirectly();
        }

        redirectedUrl = connectTask.getFinalRedirectedUrl();
        if (acceptPartial || onlyFromBeginning) {
            final long contentLength = FileDownloadUtils.findContentLength(id, connection);

            // update model
            String fileName = null;
            if (model.isPathAsDirectory()) {
                // filename
                fileName = FileDownloadUtils.findFilename(connection, model.getUrl());
            }
            isChunked = (contentLength == TOTAL_VALUE_IN_CHUNKED_RESOURCE);
            final long totalLength;
            if (!isChunked) {
                totalLength = model.getSoFar() + contentLength;
            } else {
                totalLength = contentLength;
            }

            // callback
            statusCallback.onConnected(isResumeAvailableOnDB && acceptPartial,
                    totalLength, newEtag, fileName);

        } else {
            throw new FileDownloadHttpException(code,
                    requestHeader, connection.getResponseHeaderFields());
        }
    }

    private void fetchWithSingleConnection(final ConnectionProfile firstConnectionProfile,
                                           FileDownloadConnection connection)
            throws IOException, IllegalAccessException {
        
        final ConnectionProfile profile;
        if (!acceptPartial) {
            model.setSoFar(0);

            profile = new ConnectionProfile(0, 0,
                    firstConnectionProfile.endOffset, firstConnectionProfile.contentLength);
        } else {
            profile = firstConnectionProfile;
        }

        final FetchDataTask.Builder builder = new FetchDataTask.Builder();
        builder.setCallback(this)
                .setDownloadId(model.getId())
                .setConnectionIndex(-1)
                .setWifiRequired(isWifiRequired)
                .setConnection(connection)
                .setConnectionProfile(profile)
                .setPath(model.getTempFilePath());

        model.setConnectionCount(1);
        database.updateConnectionCount(model.getId(), 1);
        singleFetchDataTask = builder.build();
        if (paused) {
            model.setStatus(FileDownloadStatus.paused);
            singleFetchDataTask.pause();
        } else {
            singleFetchDataTask.run();
        }
    }

    private void fetchWithMultipleConnectionFromResume(final int connectionCount, final List<ConnectionModel> connectionModelList) throws InterruptedException {
        if (connectionCount <= 1 || connectionModelList.size() != connectionCount)
            throw new IllegalArgumentException();

        fetchWithMultipleConnection(connectionModelList, model.getTotal());
    }

    private void fetchWithMultipleConnectionFromBeginning(final long totalLength, final int connectionCount) throws InterruptedException {
        long startOffset = 0;
        final long eachRegion = totalLength / connectionCount;
        final int id = model.getId();

        final List<ConnectionModel> connectionModelList = new ArrayList<>();

        for (int i = 0; i < connectionCount; i++) {

            final long endOffset;
            if (i == connectionCount - 1) {
                // avoid float precision error
                endOffset = 0;
            } else {
                // [startOffset, endOffset)
                endOffset = startOffset + eachRegion - 1;
            }

            final ConnectionModel connectionModel = new ConnectionModel();
            connectionModel.setId(id);
            connectionModel.setIndex(i);
            connectionModel.setStartOffset(startOffset);
            connectionModel.setCurrentOffset(startOffset);
            connectionModel.setEndOffset(endOffset);
            connectionModelList.add(connectionModel);

            database.insertConnectionModel(connectionModel);
            startOffset += eachRegion;
        }

        model.setConnectionCount(connectionCount);
        database.updateConnectionCount(id, connectionCount);

        fetchWithMultipleConnection(connectionModelList, totalLength);
    }


    private void fetchWithMultipleConnection(final List<ConnectionModel> connectionModelList, final long totalLength) throws InterruptedException {
        final int id = model.getId();
        final String etag = model.getETag();
        final String url = redirectedUrl != null ? redirectedUrl : model.getUrl();
        final String path = model.getTempFilePath();

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "fetch data with multiple connection(count: [%d]) for task[%d]",
                    connectionModelList.size(), id);
        }

        long totalOffset = 0;

        // why not with etag when not resume from the database? because do this can avoid
        // precondition failed on separate downloading.
        final boolean withEtag = isResumeAvailableOnDB;
        for (ConnectionModel connectionModel : connectionModelList) {
            final long contentLength;
            if (connectionModel.getEndOffset() == 0) {
                // must be the last one
                contentLength = totalLength - connectionModel.getCurrentOffset();
            } else {
                contentLength = connectionModel.getEndOffset() - connectionModel.getCurrentOffset() + 1;
            }

            totalOffset += (connectionModel.getCurrentOffset() - connectionModel.getStartOffset());

            if (connectionModel.getEndOffset() == connectionModel.getCurrentOffset() - 1) {
                // [start, end), offset contain the start one, so need - 1.
                // it has already done, so pass.
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "pass connection[%d], because it has been completed",
                            connectionModel.getId());
                }
                continue;
            }

            final DownloadRunnable.Builder builder = new DownloadRunnable.Builder();

            final ConnectionProfile connectionProfile = new ConnectionProfile(
                    connectionModel.getStartOffset(), connectionModel.getCurrentOffset(),
                    connectionModel.getEndOffset(), contentLength);

            final DownloadRunnable runnable = builder
                    .setId(id)
                    .setConnectionIndex(connectionModel.getIndex())
                    .setCallback(this)
                    .setUrl(url)
                    .setEtag(withEtag ? etag : null)
                    .setHeader(userRequestHeader)
                    .setWifiRequired(isWifiRequired)
                    .setConnectionModel(connectionProfile)
                    .setPath(path)
                    .build();

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "enable multiple connection: %s", connectionModel);
            }

            if (runnable == null)
                throw new IllegalArgumentException("the download runnable must not be null!");

            downloadRunnableList.add(runnable);
        }

        if (totalOffset != model.getSoFar()) {
            FileDownloadLog.w(this, "correct the sofar[%d] from connection table[%d]",
                    model.getSoFar(), totalOffset);
            model.setSoFar(totalOffset);
        }

        List<Callable<Object>> subTasks = new ArrayList<>(downloadRunnableList.size());
        for (DownloadRunnable runnable : downloadRunnableList) {
            if (paused) {
                runnable.pause();
                continue;
            }
            subTasks.add(Executors.callable(runnable));
        }
        if (paused) {
            model.setStatus(FileDownloadStatus.paused);
            return;
        }

        List<Future<Object>> subTaskFutures = DOWNLOAD_EXECUTOR.invokeAll(subTasks);
        if (FileDownloadLog.NEED_LOG) {
            for (Future<Object> future : subTaskFutures) {
                FileDownloadLog.d(this, "finish sub-task for [%d] %B %B",
                        id, future.isDone(), future.isCancelled());
            }
        }
    }

    private void handlePreAllocate(long contentLength, String path)
            throws IOException, IllegalAccessException {

        FileDownloadOutputStream outputStream = null;
        try {

            if (contentLength != TOTAL_VALUE_IN_CHUNKED_RESOURCE) {
                outputStream = FileDownloadUtils.createOutputStream(model.getTempFilePath());
                final long breakpointBytes = new File(path).length();
                final long requiredSpaceBytes = contentLength - breakpointBytes;

                final long freeSpaceBytes = FileDownloadUtils.getFreeSpaceBytes(path);

                if (freeSpaceBytes < requiredSpaceBytes) {
                    // throw a out of space exception.
                    throw new FileDownloadOutOfSpaceException(freeSpaceBytes,
                            requiredSpaceBytes, breakpointBytes);
                } else if (!FileDownloadProperties.getImpl().FILE_NON_PRE_ALLOCATION) {
                    // pre allocate.
                    outputStream.setLength(contentLength);
                }
            }
        } finally {
            if (outputStream != null)
                outputStream.close();
        }
    }

    private long lastCallbackBytes = 0;
    private long lastCallbackTimestamp = 0;

    private long lastUpdateBytes = 0;
    private long lastUpdateTimestamp = 0;

    @Override
    public void onProgress(long increaseBytes) {
        if (paused) return;

        statusCallback.onProgress(increaseBytes);
    }

    @Override
    public void onCompleted(DownloadRunnable doneRunnable, long startOffset, long endOffset)
            throws IOException {
        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the" +
                        " completed callback", model.getId());
            }
            return;
        }

        boolean allConnectionCompleted = false;

        final int doneConnectionIndex = doneRunnable == null ? -1 : doneRunnable.connectionIndex;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "the connection has been completed(%d): [%d, %d)  %d",
                    doneConnectionIndex, startOffset, endOffset, model.getTotal());
        }

        if (isSingleConnection) {
            if (startOffset != 0 && endOffset != model.getTotal()) {
                FileDownloadLog.e(this, "the single task not completed corrected(%d, %d != %d) " +
                        "for task(%d)", startOffset, endOffset, model.getTotal(), model.getId());
            }

            allConnectionCompleted = true;
        } else {
            synchronized (downloadRunnableList) {
                downloadRunnableList.remove(doneRunnable);
            }

            if (downloadRunnableList.size() <= 0) {
                allConnectionCompleted = true;
            }
        }

        if (allConnectionCompleted) {
            statusCallback.onCompleted();
        }
    }

    @Override
    public boolean isRetry(Exception exception) {
        if (exception instanceof FileDownloadHttpException) {
            final FileDownloadHttpException httpException = (FileDownloadHttpException) exception;

            final int code = httpException.getCode();

            if (isSingleConnection && code == HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                if (!isTriedFixRangeNotSatisfiable) {
                    FileDownloadUtils.deleteTaskFiles(model.getTargetFilePath(), model.getTempFilePath());
                    isTriedFixRangeNotSatisfiable = true;
                    return true;
                }
            }
        }

        return validRetryTimes > 0 && !(exception instanceof FileDownloadGiveUpRetryException);
    }

    @Override
    public void onError(Exception exception) {
        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the" +
                        " error callback", model.getId());
            }
            return;
        }

        // discard all
        @SuppressWarnings("unchecked") ArrayList<DownloadRunnable> discardList =
                (ArrayList<DownloadRunnable>) downloadRunnableList.clone();
        for (DownloadRunnable runnable : discardList) {
            if (runnable != null) {
                runnable.discard();
                // if runnable is null, then that one must be completed and removed
            }
        }

        statusCallback.onError(exception);
    }

    @Override
    public void onRetry(Exception exception, long invalidIncreaseBytes) {
        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the" +
                        " retry callback", model.getId());
            }
            return;
        }

        if (validRetryTimes-- < 0) {
            FileDownloadLog.e(this, "valid retry times is less than 0(%d) for download task(%d)",
                    validRetryTimes, model.getId());
        }

        statusCallback.onRetry(exception, validRetryTimes--, invalidIncreaseBytes);
    }

    @Override
    public void syncProgressFromCache() {
        database.updateProgress(model.getId(), model.getSoFar());
    }

    private void checkupBeforeConnect()
            throws FileDownloadGiveUpRetryException {

        // 1. check whether need access-network-state permission?
        if (isWifiRequired &&
                !FileDownloadUtils.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            throw new FileDownloadGiveUpRetryException(
                    FileDownloadUtils.formatString("Task[%d] can't start the download runnable," +
                                    " because this task require wifi, but user application " +
                                    "nor current process has %s, so we can't check whether " +
                                    "the network type connection.", model.getId(),
                            Manifest.permission.ACCESS_NETWORK_STATE));
        }

        // 2. check whether need wifi to download?
        if (isWifiRequired && FileDownloadUtils.isNetworkNotOnWifiType()) {
            throw new FileDownloadNetworkPolicyException();
        }
    }

    private void checkupBeforeFetch() throws RetryDirectly, DiscardSafely {
        final int id = model.getId();

        if (model.isPathAsDirectory()) {
            // this scope for caring about the case of there is another task is provided
            // the same path to store file and the same url.

            final String targetFilePath = model.getTargetFilePath();

            // get the ID after got the filename.
            final int fileCaseId = FileDownloadUtils.generateId(model.getUrl(),
                    targetFilePath);

            // whether the file with the filename has been existed.
            if (FileDownloadHelper.inspectAndInflowDownloaded(id,
                    targetFilePath, isForceReDownload, false)) {
                database.remove(id);
                database.removeConnections(id);
                throw new DiscardSafely();
            }

            final FileDownloadModel fileCaseModel = database.find(fileCaseId);

            if (fileCaseModel != null) {
                // the task with the same file name and url has been exist.

                // whether the another task with the same file and url is downloading.
                if (FileDownloadHelper.inspectAndInflowDownloading(id, fileCaseModel,
                        threadPoolMonitor, false)) {
                    //it has been post to upper layer the 'warn' message, so the current
                    // task no need to continue download.
                    database.remove(id);
                    database.removeConnections(id);
                    throw new DiscardSafely();
                }

                final List<ConnectionModel> connectionModelList = database.findConnectionModel(fileCaseId);

                // the another task with the same file name and url is paused
                database.remove(fileCaseId);
                database.removeConnections(fileCaseId);
                FileDownloadUtils.deleteTargetFile(model.getTargetFilePath());

                if (FileDownloadUtils.isBreakpointAvailable(fileCaseId, fileCaseModel)) {
                    model.setSoFar(fileCaseModel.getSoFar());
                    model.setTotal(fileCaseModel.getTotal());
                    model.setETag(fileCaseModel.getETag());
                    model.setConnectionCount(fileCaseModel.getConnectionCount());
                    database.update(model);

                    // re connect to resume from breakpoint.
                    if (connectionModelList != null) {
                        for (ConnectionModel connectionModel : connectionModelList) {
                            connectionModel.setId(id);
                            database.insertConnectionModel(connectionModel);
                        }
                    }

                    // retry
                    throw new RetryDirectly();
                }
            }

            // whether there is an another running task with the same target-file-path.
            if (FileDownloadHelper.inspectAndInflowConflictPath(id, model.getSoFar(),
                    model.getTempFilePath(),
                    targetFilePath,
                    threadPoolMonitor)) {
                database.remove(id);
                database.removeConnections(id);

                throw new DiscardSafely();
            }
        }
    }

    public int getId() {
        return model.getId();
    }

    public boolean isAlive() {
        return alive || this.statusCallback.isAlive();
    }

    public String getTempFilePath() {
        return model.getTempFilePath();
    }

    class RetryDirectly extends Throwable {
    }

    class DiscardSafely extends Throwable {
    }

    public static class Builder {
        private FileDownloadModel model;
        private FileDownloadHeader header;
        private IThreadPoolMonitor threadPoolMonitor;
        private Integer minIntervalMillis;
        private Integer callbackProgressMaxCount;
        private Boolean isForceReDownload;
        private Boolean isWifiRequired;
        private Integer maxRetryTimes;

        public Builder setModel(FileDownloadModel model) {
            this.model = model;
            return this;
        }

        public Builder setHeader(FileDownloadHeader header) {
            this.header = header;
            return this;
        }

        public Builder setThreadPoolMonitor(IThreadPoolMonitor threadPoolMonitor) {
            this.threadPoolMonitor = threadPoolMonitor;
            return this;
        }

        public Builder setMinIntervalMillis(Integer minIntervalMillis) {
            this.minIntervalMillis = minIntervalMillis;
            return this;
        }

        public Builder setCallbackProgressMaxCount(Integer callbackProgressMaxCount) {
            this.callbackProgressMaxCount = callbackProgressMaxCount;
            return this;
        }

        public Builder setForceReDownload(Boolean forceReDownload) {
            isForceReDownload = forceReDownload;
            return this;
        }

        public Builder setWifiRequired(Boolean wifiRequired) {
            isWifiRequired = wifiRequired;
            return this;
        }

        public Builder setMaxRetryTimes(Integer maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public DownloadLaunchRunnable build() {
            if (model == null || threadPoolMonitor == null ||
                    minIntervalMillis == null || callbackProgressMaxCount == null ||
                    isForceReDownload == null || isWifiRequired == null || maxRetryTimes == null)
                throw new IllegalArgumentException();

            return new DownloadLaunchRunnable(model, header, threadPoolMonitor,
                    minIntervalMillis, callbackProgressMaxCount,
                    isForceReDownload, isWifiRequired, maxRetryTimes);
        }
    }
}
