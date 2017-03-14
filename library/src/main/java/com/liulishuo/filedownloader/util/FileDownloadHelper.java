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

package com.liulishuo.filedownloader.util;

import android.annotation.SuppressLint;
import android.content.Context;

import com.liulishuo.filedownloader.IThreadPoolMonitor;
import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.exception.PathConflictException;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.message.MessageSnapshotTaker;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.services.FileDownloadDatabase;
import com.liulishuo.filedownloader.stream.FileDownloadBufferedOutputStream;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.stream.FileDownloadRandomAccessFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The helper for cache the {@code APP_CONTEXT} and {@code OK_HTTP_CLIENT} for the main process and
 * the filedownloader process.
 */
public class FileDownloadHelper {

    @SuppressLint("StaticFieldLeak")
    private static Context APP_CONTEXT;

    public static void holdContext(final Context context) {
        APP_CONTEXT = context;
    }

    public static Context getAppContext() {
        return APP_CONTEXT;
    }

    public interface DatabaseCustomMaker {
        /**
         * The database is used for storing the {@link FileDownloadModel}.
         * <p/>
         * The data stored in the database is only used for task resumes from the breakpoint.
         * <p>
         * The task of the data stored in the database must be a task that has not finished downloading yet,
         * and if the task has finished downloading, its data will be
         * {@link FileDownloadDatabase#remove(int)} from the database, since that data is no longer
         * available for resumption of its task pass.
         *
         * @return Nullable, Customize {@link FileDownloadDatabase} which will be used for storing
         * downloading model.
         * @see com.liulishuo.filedownloader.services.DefaultDatabaseImpl
         */
        FileDownloadDatabase customMake();
    }

    public interface OutputStreamCreator {
        /**
         * The output stream creator is used for creating {@link FileDownloadOutputStream} which is
         * used to write the input stream to the file for downloading.
         * <p>
         * <strong>Note:</strong> please create a output stream which append the content to the
         * exist file, which means that bytes would be written to the end of the file rather than
         * the beginning.
         *
         * @param file the file will used for storing the downloading content.
         * @return The output stream used to write downloading byte array to the {@code file}.
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason
         * @see FileDownloadRandomAccessFile.Creator
         * @see FileDownloadBufferedOutputStream.Creator
         */
        FileDownloadOutputStream create(File file) throws FileNotFoundException;

        /**
         * @return {@code true} if the {@link FileDownloadOutputStream} is created through
         * {@link #create(File)} support {@link FileDownloadOutputStream#seek(long)} function.
         * If the {@link FileDownloadOutputStream} is created through {@link #create(File)} doesn't
         * support {@link FileDownloadOutputStream#seek(long)}, please return {@code false}, in
         * order to let the internal mechanism can predict this situation, and handle it smoothly.
         */
        boolean supportSeek();
    }

    public interface ConnectionCreator {
        /**
         * The connection creator is used for creating {@link FileDownloadConnection} component which
         * is used to use some protocol to connect to the remote server.
         *
         * @param url the uniform resource locator, which direct the aim resource we need to connect.
         * @return The connection creator.
         * @throws IOException if an I/O exception occurs.
         */
        FileDownloadConnection create(String url) throws IOException;
    }

    /**
     * @param id              the {@code id} used for filter out which task would be notified the
     *                        'completed' message if need.
     * @param path            if the file with {@code path} is exist it means the relate task would
     *                        be completed.
     * @param forceReDownload whether the task is force to redownload ignore whether the file has
     *                        been exist or not.
     * @param flowDirectly    {@code true} if flow the message if need directly without throw to the
     *                        message-queue.
     * @return whether the task with {@code id} has been downloaded.
     */
    public static boolean inspectAndInflowDownloaded(int id, String path, boolean forceReDownload,
                                                     boolean flowDirectly) {
        if (forceReDownload) {
            return false;
        }

        if (path != null) {
            final File file = new File(path);
            if (file.exists()) {
                MessageSnapshotFlow.getImpl().inflow(MessageSnapshotTaker.
                        catchCanReusedOldFile(id, file, flowDirectly));
                return true;
            }
        }

        return false;
    }

    /**
     * @param id           the {@code id} used for filter out which task would be notified the
     *                     'warn' message if need.
     * @param model        the target model will be checked for judging whether it is downloading.
     * @param monitor      the monitor for download-thread.
     * @param flowDirectly {@code true} if flow the message if need directly without throw to the
     *                     message-queue.
     * @return whether the {@code model} is downloading.
     */
    public static boolean inspectAndInflowDownloading(int id, FileDownloadModel model,
                                                      IThreadPoolMonitor monitor,
                                                      boolean flowDirectly) {
        if (monitor.isDownloading(model)) {
            MessageSnapshotFlow.getImpl().
                    inflow(MessageSnapshotTaker.catchWarn(id, model.getSoFar(), model.getTotal(),
                            flowDirectly));
            return true;
        }

        return false;
    }

    /**
     * @param id             the {@code id} used for filter out which task would be notified the
     *                       'error' message if need.
     * @param sofar          the so far bytes of the current checking-task.
     * @param tempFilePath   the temp file path(file path used for downloading) for the current
     *                       checking-task.
     * @param targetFilePath the target file path for the current checking-task.
     * @param monitor        the monitor for download-thread.
     * @return whether the task with {@code id} is refused to start, because of there is an another
     * running task with the same {@code tempFilePath}.
     */
    public static boolean inspectAndInflowConflictPath(int id, long sofar,
                                                       String tempFilePath, String targetFilePath,
                                                       IThreadPoolMonitor monitor) {
        if (targetFilePath != null && tempFilePath != null) {
            final int anotherSameTempPathTaskId = monitor.findRunningTaskIdBySameTempPath(tempFilePath, id);
            if (anotherSameTempPathTaskId != 0) {
                MessageSnapshotFlow.getImpl().
                        inflow(MessageSnapshotTaker.catchException(id, sofar,
                                new PathConflictException(anotherSameTempPathTaskId, tempFilePath,
                                        targetFilePath)));
                return true;
            }
        }

        return false;
    }
}

