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
import com.liulishuo.filedownloader.database.FileDownloadDatabase;
import com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException;
import com.liulishuo.filedownloader.exception.FileDownloadHttpException;
import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * You can use this to launch downloading, on here the download will be launched separate following
 * steps:
 * <p/>
 * step 1. create the trial connection
 *          ( this trial connection is used for:
 *                  1. checkup the saved etag is overdue
 *                  2. checkup whether the partial-accept is supported
 *                  3. checkup whether the current connection is chunked. )
 *
 * step 2. if the saved etag is overdue -> jump to step 1 to checkup whether the partial-accept is
 * supported.
 * step 3. if (NOT chunked) & partial-accept & output stream support-seek:
 *              create multiple {@link DownloadTask} to download.
 *         else:
 *              create single first connection and use {@link FetchDataTask} to fetch data from the
 *              connection.
 * <p/>
 * We use {@link DownloadStatusCallback} to handle all events sync to DB/filesystem and callback to
 * user.
 */
public class DownloadLaunchRunnable implements Runnable, ProcessCallback {

    private final DownloadStatusCallback statusCallback;
    private final int defaultConnectionCount = 5;
    private final FileDownloadModel model;
    private final FileDownloadHeader userRequestHeader;
    private final boolean isForceReDownload;
    private final boolean isWifiRequired;

    private final FileDownloadDatabase database;
    private final IThreadPoolMonitor threadPoolMonitor;

    private boolean isTriedFixRangeNotSatisfiable;

    int validRetryTimes;

    /**
     * None of the ranges in the request's Range header field overlap the current extent of the
     * selected resource or that the set of ranges requested has been rejected due to invalid
     * ranges or an excessive request of small or overlapping ranges.
     */
    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int TOTAL_VALUE_IN_CHUNKED_RESOURCE = -1;

    private boolean isNeedForceDiscardRange = false;

    private final boolean supportSeek;

    private final ArrayList<DownloadRunnable> downloadRunnableList = new ArrayList<>(
            defaultConnectionCount);
    private DownloadRunnable singleDownloadRunnable;
    private boolean isSingleConnection;

    private static final ThreadPoolExecutor DOWNLOAD_EXECUTOR = FileDownloadExecutors
            .newFixedThreadPool("ConnectionBlock");

    private boolean isResumeAvailableOnDB;
    private boolean acceptPartial;
    private boolean isChunked;

    private final AtomicBoolean alive;
    private volatile boolean paused;
    private volatile boolean error;
    private volatile Exception errorException;

    private String redirectedUrl;

    private DownloadLaunchRunnable(FileDownloadModel model, FileDownloadHeader header,
                                   IThreadPoolMonitor threadPoolMonitor,
                                   final int minIntervalMillis, int callbackProgressMaxCount,
                                   boolean isForceReDownload, boolean isWifiRequired,
                                   int maxRetryTimes) {
        this.alive = new AtomicBoolean(true);
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

    private DownloadLaunchRunnable(DownloadStatusCallback callback, FileDownloadModel model,
                                   FileDownloadHeader header,
                                   IThreadPoolMonitor threadPoolMonitor,
                                   final int minIntervalMillis, int callbackProgressMaxCount,
                                   boolean isForceReDownload, boolean isWifiRequired,
                                   int maxRetryTimes) {
        this.alive = new AtomicBoolean(true);
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
                                                final int minIntervalMillis,
                                                int callbackProgressMaxCount,
                                                boolean isForceReDownload, boolean isWifiRequired,
                                                int maxRetryTimes) {
        return new DownloadLaunchRunnable(callback, model, header, threadPoolMonitor,
                minIntervalMillis, callbackProgressMaxCount, isForceReDownload, isWifiRequired,
                maxRetryTimes);
    }

    public void pause() {
        this.paused = true;

        if (singleDownloadRunnable != null) singleDownloadRunnable.pause();
        @SuppressWarnings("unchecked") ArrayList<DownloadRunnable> pauseList =
                (ArrayList<DownloadRunnable>) downloadRunnableList.clone();
        for (DownloadRunnable runnable : pauseList) {
            if (runnable != null) {
                runnable.pause();
                // if runnable is null, then that one must be completed and removed
            }
        }
    }

    public void pending() {
        final List<ConnectionModel> connectionOnDBList = database
                .findConnectionModel(model.getId());
        //inspect model can be resumed or not, if false, the previous sofar cannot be used
        inspectTaskModelResumeAvailableOnDB(connectionOnDBList);
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
                         * with here. And this area is invoking before there, so,
                         * {@code cancel(int)} is fail.
                         *
                         * High concurrent cause.
                         */
                        FileDownloadLog.d(this, "High concurrent cause, start runnable but "
                                + "already paused %d", model.getId());
                    }

                } else {
                    onError(new RuntimeException(
                            FileDownloadUtils.formatString("Task[%d] can't start the download"
                                            + " runnable, because its status is %d not %d",
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
                         * with here. And this area is invoking before there, so,
                         * {@code cancel(int)} is fail.
                         *
                         * High concurrent cause.
                         */
                        FileDownloadLog.d(this, "High concurrent cause, start runnable but "
                                + "already paused %d", model.getId());
                    }
                    return;
                }

                try {
                    // 1. check env state
                    checkupBeforeConnect();

                    // 2. trial connect
                    trialConnect();

                    // 3. reuse same task
                    checkupAfterGetFilename();

                    // 4. check local resume model
                    final List<ConnectionModel> connectionOnDBList = database
                            .findConnectionModel(model.getId());
                    inspectTaskModelResumeAvailableOnDB(connectionOnDBList);

                    if (paused) {
                        model.setStatus(FileDownloadStatus.paused);
                        return;
                    }

                    final long totalLength = model.getTotal();

                    // 5. pre-allocate if need.
                    handlePreAllocate(totalLength, model.getTempFilePath());

                    // 6. calculate block count
                    final int connectionCount = calcConnectionCount(totalLength);
                    if (connectionCount <= 0) {
                        throw new IllegalAccessException(FileDownloadUtils
                                .formatString("invalid connection count %d, the connection count"
                                        + " must be larger than 0", connectionCount));
                    }

                    if (totalLength == 0) {
                        return;
                    }

                    if (paused) {
                        model.setStatus(FileDownloadStatus.paused);
                        return;
                    }

                    // 7. start real connect and fetch to local filesystem
                    isSingleConnection = connectionCount == 1;
                    if (isSingleConnection) {
                        // single connection
                        realDownloadWithSingleConnection(totalLength);
                    } else {
                        // multiple connection
                        statusCallback.onMultiConnection();
                        if (isResumeAvailableOnDB) {
                            realDownloadWithMultiConnectionFromResume(connectionCount,
                                    connectionOnDBList);
                        } else {
                            realDownloadWithMultiConnectionFromBeginning(totalLength,
                                    connectionCount);
                        }
                    }

                } catch (IOException | IllegalAccessException
                        | InterruptedException | IllegalArgumentException
                        | FileDownloadGiveUpRetryException e) {
                    if (isRetry(e)) {
                        onRetry(e);
                        continue;
                    } else {
                        onError(e);
                    }
                } catch (DiscardSafely discardSafely) {
                    return;
                } catch (RetryDirectly retryDirectly) {
                    model.setStatus(FileDownloadStatus.retry);
                    continue;
                }

                break;
            } while (true);
        } finally {
            statusCallback.discardAllMessage();

            if (paused) {
                statusCallback.onPausedDirectly();
            } else if (error) {
                statusCallback.onErrorDirectly(errorException);
            } else {
                try {
                    statusCallback.onCompletedDirectly();
                } catch (IOException e) {
                    statusCallback.onErrorDirectly(e);
                }
            }

            alive.set(false);
        }
    }

    private int calcConnectionCount(long totalLength) {
        if (isMultiConnectionAvailable()) {
            if (isResumeAvailableOnDB) {
                return model.getConnectionCount();
            } else {
                return CustomComponentHolder.getImpl()
                        .determineConnectionCount(model.getId(), model.getUrl(),
                                model.getPath(), totalLength);
            }
        } else {
            return 1;
        }
    }

    // the trial connection is for: 1. etag verify; 2. partial support verify.
    private void trialConnect() throws IOException, RetryDirectly, IllegalAccessException {
        FileDownloadConnection trialConnection = null;
        try {
            final ConnectionProfile trialConnectionProfile;
            if (isNeedForceDiscardRange) {
                trialConnectionProfile = ConnectionProfile.ConnectionProfileBuild
                        .buildTrialConnectionProfileNoRange();
            } else {
                trialConnectionProfile = ConnectionProfile.ConnectionProfileBuild
                        .buildTrialConnectionProfile();
            }
            final ConnectTask trialConnectTask = new ConnectTask.Builder()
                    .setDownloadId(model.getId())
                    .setUrl(model.getUrl())
                    .setEtag(model.getETag())
                    .setHeader(userRequestHeader)
                    .setConnectionProfile(trialConnectionProfile)
                    .build();
            trialConnection = trialConnectTask.connect();
            handleTrialConnectResult(trialConnectTask.getRequestHeader(),
                    trialConnectTask, trialConnection);

        } finally {
            if (trialConnection != null) trialConnection.ending();
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
        return defaultConnectionCount;
    }

    void inspectTaskModelResumeAvailableOnDB(List<ConnectionModel> connectionOnDBList) {
        // check resume available
        final long offset;
        final int connectionCount = model.getConnectionCount();
        final String tempFilePath = model.getTempFilePath();
        final String targetFilePath = model.getTargetFilePath();
        final boolean isMultiConnection = connectionCount > 1;
        if (isNeedForceDiscardRange) {
            offset = 0;
        } else if (isMultiConnection && !supportSeek) {
            // can't support seek for multi-connection is fatal problem, so discard resume.
            offset = 0;
        } else {
            final boolean resumeAvailable = FileDownloadUtils
                    .isBreakpointAvailable(model.getId(), model);
            if (resumeAvailable) {
                if (!supportSeek) {
                    offset = new File(tempFilePath).length();
                } else {
                    if (isMultiConnection) {
                        // when it is multi connections, the offset would be 0,
                        // because it only store on the connection table.
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
    }

    private void handleTrialConnectResult(Map<String, List<String>> requestHeader,
                                          ConnectTask connectTask,
                                          FileDownloadConnection connection)
            throws IOException, RetryDirectly, IllegalArgumentException {
        final int id = model.getId();
        final int code = connection.getResponseCode();

        acceptPartial = FileDownloadUtils.isAcceptRange(code, connection);
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
                // The request has been fulfilled and has resulted in one or more new resources
                // being created. mark this case is precondition failed for
                // 1. checkout whether accept partial
                // 2. 201 means new resources so range must be from beginning otherwise it can't
                // match local range.
                isPreconditionFailed = true;
                break;
            }

            if (code == HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                if (model.getSoFar() > 0) {
                    // On the first connection range not satisfiable, there must something wrong,
                    // so have to retry.
                    isPreconditionFailed = true;
                    break;
                } else {
                    // range is right, but get 416
                    if (!isNeedForceDiscardRange) {
                        // if range is still added, but range is right with 416 response, so we
                        // discard range on header and try again
                        isNeedForceDiscardRange = true;
                        isPreconditionFailed = true;
                    }
                }
            }

        } while (false);


        if (isPreconditionFailed) {
            // the file on remote is changed
            if (isResumeAvailableOnDB) {
                FileDownloadLog.w(this, "there is precondition failed on this request[%d] "
                                + "with old etag[%s]、new etag[%s]、response code is %d",
                        id, oldEtag, newEtag, code);
            }

            database.removeConnections(model.getId());
            FileDownloadUtils.deleteTaskFiles(model.getTargetFilePath(), model.getTempFilePath());
            isResumeAvailableOnDB = false;

            if (oldEtag != null && oldEtag.equals(newEtag)) {
                FileDownloadLog.w(this, "the old etag[%s] is the same to the new etag[%s], "
                                + "but the response status code is %d not Partial(206), so wo have"
                                + " to start this task from very beginning for task[%d]!",
                        oldEtag, newEtag, code, id);
                newEtag = null;
            }

            model.setSoFar(0);
            model.setTotal(0);
            model.setETag(newEtag);
            model.resetConnectionCount();

            database.updateOldEtagOverdue(id, model.getETag(), model.getSoFar(), model.getTotal(),
                    model.getConnectionCount());

            // retry to check whether support partial or not.
            throw new RetryDirectly();
        }

        redirectedUrl = connectTask.getFinalRedirectedUrl();
        if (acceptPartial || onlyFromBeginning) {
            final long totalLength = FileDownloadUtils.findInstanceLengthForTrial(connection);

            // update model
            String fileName = null;
            if (model.isPathAsDirectory()) {
                // filename
                fileName = FileDownloadUtils.findFilename(connection, model.getUrl());
            }
            isChunked = (totalLength == TOTAL_VALUE_IN_CHUNKED_RESOURCE);

            // callback
            statusCallback.onConnected(isResumeAvailableOnDB && acceptPartial,
                    totalLength, newEtag, fileName);

        } else {
            throw new FileDownloadHttpException(code,
                    requestHeader, connection.getResponseHeaderFields());
        }
    }

    private void realDownloadWithSingleConnection(final long totalLength)
            throws IOException, IllegalAccessException {

        // connect
        final ConnectionProfile profile;
        if (!acceptPartial) {
            model.setSoFar(0);
            profile = ConnectionProfile.ConnectionProfileBuild
                    .buildBeginToEndConnectionProfile(totalLength);
        } else {
            profile = ConnectionProfile.ConnectionProfileBuild
                    .buildToEndConnectionProfile(model.getSoFar(), model.getSoFar(),
                            totalLength - model.getSoFar());
        }

        singleDownloadRunnable = new DownloadRunnable.Builder()
                .setId(model.getId())
                .setConnectionIndex(-1)
                .setCallback(this)
                .setUrl(model.getUrl())
                .setEtag(model.getETag())
                .setHeader(userRequestHeader)
                .setWifiRequired(isWifiRequired)
                .setConnectionModel(profile)
                .setPath(model.getTempFilePath())
                .build();

        model.setConnectionCount(1);
        database.updateConnectionCount(model.getId(), 1);
        if (paused) {
            model.setStatus(FileDownloadStatus.paused);
            singleDownloadRunnable.pause();
        } else {
            singleDownloadRunnable.run();
        }
    }

    private void realDownloadWithMultiConnectionFromResume(final int connectionCount,
                                                           List<ConnectionModel> modelList)
            throws InterruptedException {
        if (connectionCount <= 1 || modelList.size() != connectionCount) {
            throw new IllegalArgumentException();
        }

        fetchWithMultipleConnection(modelList, model.getTotal());
    }

    private void realDownloadWithMultiConnectionFromBeginning(final long totalLength,
                                                              final int connectionCount)
            throws InterruptedException {
        long startOffset = 0;
        final long eachRegion = totalLength / connectionCount;
        final int id = model.getId();

        final List<ConnectionModel> connectionModelList = new ArrayList<>();

        for (int i = 0; i < connectionCount; i++) {

            final long endOffset;
            if (i == connectionCount - 1) {
                // avoid float precision error
                endOffset = ConnectionProfile.RANGE_INFINITE;
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


    private void fetchWithMultipleConnection(final List<ConnectionModel> connectionModelList,
                                             final long totalLength) throws InterruptedException {
        final int id = model.getId();
        final String etag = model.getETag();
        final String url = redirectedUrl != null ? redirectedUrl : model.getUrl();
        final String path = model.getTempFilePath();

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this,
                    "fetch data with multiple connection(count: [%d]) for task[%d] totalLength[%d]",
                    connectionModelList.size(), id, totalLength);
        }

        long totalOffset = 0;

        // why not with etag when not resume from the database? because do this can avoid
        // precondition failed on separate downloading.
        final boolean withEtag = isResumeAvailableOnDB;
        for (ConnectionModel connectionModel : connectionModelList) {
            final long contentLength;
            if (connectionModel.getEndOffset() == ConnectionProfile.RANGE_INFINITE) {
                // must be the last one
                contentLength = totalLength - connectionModel.getCurrentOffset();
            } else {
                contentLength = connectionModel.getEndOffset() - connectionModel
                        .getCurrentOffset() + 1;
            }

            totalOffset += (connectionModel.getCurrentOffset() - connectionModel.getStartOffset());

            if (contentLength == 0) {
                // [start, end), offset contain the start one, so need - 1.
                // it has already done, so pass.
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "pass connection[%d-%d], because it has been completed",
                            connectionModel.getId(), connectionModel.getIndex());
                }
                continue;
            }

            final DownloadRunnable.Builder builder = new DownloadRunnable.Builder();

            final ConnectionProfile connectionProfile = ConnectionProfile.ConnectionProfileBuild
                    .buildConnectionProfile(
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

            if (runnable == null) {
                throw new IllegalArgumentException("the download runnable must not be null!");
            }

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

    private void handlePreAllocate(long totalLength, String path)
            throws IOException, IllegalAccessException {

        FileDownloadOutputStream outputStream = null;
        try {

            if (totalLength != TOTAL_VALUE_IN_CHUNKED_RESOURCE) {
                outputStream = FileDownloadUtils.createOutputStream(model.getTempFilePath());
                final long breakpointBytes = new File(path).length();
                final long requiredSpaceBytes = totalLength - breakpointBytes;

                final long freeSpaceBytes = FileDownloadUtils.getFreeSpaceBytes(path);

                if (freeSpaceBytes < requiredSpaceBytes) {
                    // throw a out of space exception.
                    throw new FileDownloadOutOfSpaceException(freeSpaceBytes,
                            requiredSpaceBytes, breakpointBytes);
                } else if (!FileDownloadProperties.getImpl().fileNonPreAllocation) {
                    // pre allocate.
                    outputStream.setLength(totalLength);
                }
            }
        } finally {
            if (outputStream != null) outputStream.close();
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
    public void onCompleted(DownloadRunnable doneRunnable, long startOffset, long endOffset) {
        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the"
                        + " completed callback", model.getId());
            }
            return;
        }

        final int doneConnectionIndex = doneRunnable.connectionIndex;
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "the connection has been completed(%d): [%d, %d)  %d",
                    doneConnectionIndex, startOffset, endOffset, model.getTotal());
        }

        if (isSingleConnection) {
            if (startOffset != 0 && endOffset != model.getTotal()) {
                FileDownloadLog.e(this, "the single task not completed corrected(%d, %d != %d) "
                        + "for task(%d)", startOffset, endOffset, model.getTotal(), model.getId());
            }
        } else {
            synchronized (downloadRunnableList) {
                downloadRunnableList.remove(doneRunnable);
            }
        }
    }

    @Override
    public boolean isRetry(Exception exception) {
        if (exception instanceof FileDownloadHttpException) {
            final FileDownloadHttpException httpException = (FileDownloadHttpException) exception;

            final int code = httpException.getCode();

            if (isSingleConnection && code == HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                if (!isTriedFixRangeNotSatisfiable) {
                    FileDownloadUtils
                            .deleteTaskFiles(model.getTargetFilePath(), model.getTempFilePath());
                    isTriedFixRangeNotSatisfiable = true;
                    return true;
                }
            }
        }

        return validRetryTimes > 0 && !(exception instanceof FileDownloadGiveUpRetryException);
    }

    @Override
    public void onError(Exception exception) {
        error = true;
        errorException = exception;

        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the"
                        + " error callback", model.getId());
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
    }

    @Override
    public void onRetry(Exception exception) {
        if (paused) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "the task[%d] has already been paused, so pass the"
                        + " retry callback", model.getId());
            }
            return;
        }

        if (validRetryTimes-- < 0) {
            FileDownloadLog.e(this, "valid retry times is less than 0(%d) for download task(%d)",
                    validRetryTimes, model.getId());
        }

        statusCallback.onRetry(exception, validRetryTimes);
    }

    @Override
    public void syncProgressFromCache() {
        database.updateProgress(model.getId(), model.getSoFar());
    }

    private void checkupBeforeConnect()
            throws FileDownloadGiveUpRetryException {

        // 1. check whether need access-network-state permission?
        if (isWifiRequired
                && !FileDownloadUtils.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            throw new FileDownloadGiveUpRetryException(
                    FileDownloadUtils.formatString("Task[%d] can't start the download runnable,"
                                    + " because this task require wifi, but user application "
                                    + "nor current process has %s, so we can't check whether "
                                    + "the network type connection.", model.getId(),
                            Manifest.permission.ACCESS_NETWORK_STATE));
        }

        // 2. check whether need wifi to download?
        if (isWifiRequired && FileDownloadUtils.isNetworkNotOnWifiType()) {
            throw new FileDownloadNetworkPolicyException();
        }
    }

    private void checkupAfterGetFilename() throws RetryDirectly, DiscardSafely {
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

                final List<ConnectionModel> connectionModelList = database
                        .findConnectionModel(fileCaseId);

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
        return alive.get() || this.statusCallback.isAlive();
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
            if (model == null || threadPoolMonitor == null
                    || minIntervalMillis == null || callbackProgressMaxCount == null
                    || isForceReDownload == null || isWifiRequired == null
                    || maxRetryTimes == null) {
                throw new IllegalArgumentException();
            }

            return new DownloadLaunchRunnable(model, header, threadPoolMonitor,
                    minIntervalMillis, callbackProgressMaxCount,
                    isForceReDownload, isWifiRequired, maxRetryTimes);
        }
    }
}
