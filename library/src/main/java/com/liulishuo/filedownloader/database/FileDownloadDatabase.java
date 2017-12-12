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

package com.liulishuo.filedownloader.database;


import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.List;

/**
 * The filedownloader database, what is used for storing the {@link FileDownloadModel}.
 * <p/>
 * The data stored in the database is only used for task resumes from the breakpoint.
 * <p>
 * The task of the data stored in the database must be a task that has not finished downloading yet,
 * and if the task has finished downloading, its data will be {@link #remove(int)} from the
 * database, since that data is no longer available for resumption of its task pass.
 *
 * @see SqliteDatabaseImpl
 * @see FileDownloadUtils#isBreakpointAvailable(int, FileDownloadModel)
 */
@SuppressWarnings("UnusedParameters")
public interface FileDownloadDatabase {

    /**
     * Invoked when task is started.
     *
     * @param id the download id.
     */
    void onTaskStart(final int id);

    /**
     * Find the model which identify is {@code id}.
     *
     * @param id the download id.
     */
    FileDownloadModel find(final int id);

    /**
     * Find the connection model which download identify is {@code id}
     *
     * @param id the download id.
     */
    List<ConnectionModel> findConnectionModel(int id);

    /**
     * Delete all connection model store on database through the download id.
     *
     * @param id the download id.
     */
    void removeConnections(int id);

    /**
     * Insert the {@code model} to connection table.
     *
     * @param model the connection model.
     */
    void insertConnectionModel(ConnectionModel model);

    /**
     * Update the currentOffset with {@code currentOffset} which id is {@code id}, index is
     * {@code index}
     *
     * @param id            the download id.
     * @param index         the connection index.
     * @param currentOffset the current offset.
     */
    void updateConnectionModel(int id, int index, long currentOffset);

    /**
     * Update the count of connection.
     *
     * @param count the connection count.
     */
    void updateConnectionCount(int id, int count);

    /**
     * Insert the model to the database.
     *
     * @param downloadModel the download model.
     */
    void insert(final FileDownloadModel downloadModel);

    /**
     * Update the data compare to the {@code downloadModel}
     *
     * @param downloadModel the download model.
     */
    void update(final FileDownloadModel downloadModel);

    /**
     * Remove the model which identify is {@code id}.
     *
     * @param id the download id.
     * @return {@code true} if succeed to remove model from the database.
     */
    boolean remove(final int id);

    /**
     * Clear all models in this database.
     */
    void clear();


    /**
     * Update when the old one is overdue.
     */
    void updateOldEtagOverdue(int id, String newEtag, long sofar, long total, int connectionCount);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#connected}.
     *
     * @param id       the download id.
     * @param total    the new total bytes.
     * @param etag     the new etag. this value will be {@code null} when we can't find it on
     *                 response header.
     * @param filename the new file name. this value will be {@code null} when its no need to store.
     */
    void updateConnected(int id, long total, String etag, String filename);

    /**
     * Update the sofar bytes with the status {@code progress}, so don't forget to store the
     * {@link FileDownloadStatus#progress} too.
     *
     * @param sofarBytes the current sofar bytes.
     */
    void updateProgress(int id, long sofarBytes);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#error}.
     *
     * @param id        the download id.
     * @param throwable the new exception.
     * @param sofar     the new so far bytes.
     */
    void updateError(int id, Throwable throwable, long sofar);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#retry}.
     *
     * @param id        the download id.
     * @param throwable the new exception.
     */
    void updateRetry(int id, Throwable throwable);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#completed}.
     * The latest version will remove model from DB.
     *
     * @param id    the download id.
     * @param total the new total bytes.
     */
    void updateCompleted(int id, final long total);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#paused}.
     *
     * @param id    the download id.
     * @param sofar the new so far bytes.
     */
    void updatePause(int id, final long sofar);

    /**
     * Update the data because of the download status alternative to
     * {@link FileDownloadStatus#pending}.
     *
     * @param id the download id.
     */
    void updatePending(int id);

    /**
     * Get the maintainer for the database, this maintainer will be used when the database is
     * initializing.
     * <p>
     * The maintainer will return all data on the database.
     * <p>
     * Demo: {@link SqliteDatabaseImpl.Maintainer}
     *
     * @return the maintainer for maintain the database.
     */
    Maintainer maintainer();

    /**
     * the maintainer for the database, this maintainer will be used when the database is
     * initializing.
     */
    @SuppressWarnings("EmptyMethod")
    interface Maintainer extends Iterable<FileDownloadModel> {
        /**
         * invoke this method when the operation for maintain is finished.
         */
        void onFinishMaintain();

        /**
         * invoke this method when the {@code model} is invalid and has been removed.
         *
         * @param model the removed invalid model.
         */
        void onRemovedInvalidData(FileDownloadModel model);

        /**
         * invoke this method when the {@code model} is valid to save and has been refreshed.
         *
         * @param model the refreshed valid model.
         */
        void onRefreshedValidData(FileDownloadModel model);

        /**
         * invoke this method when the {@link FileDownloadModel#id} is changed because of the
         * different {@link com.liulishuo.filedownloader.util.FileDownloadHelper.IdGenerator},
         * which generate the new id for the task.
         * <p>
         * tips: you need to update the filedownloader-table and the connection-table.
         *
         * @param oldId          the old id for the {@code modelWithNewId}
         * @param modelWithNewId the model with the new id.
         */
        void changeFileDownloadModelId(int oldId, FileDownloadModel modelWithNewId);
    }
}
