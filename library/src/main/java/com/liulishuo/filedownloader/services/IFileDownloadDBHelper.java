package com.liulishuo.filedownloader.services;


import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.util.Set;

/**
 * Created by Jacksgong on 9/24/15.
 */
interface IFileDownloadDBHelper {

    Set<FileDownloadModel> getAllUnComplete();

    Set<FileDownloadModel> getAllCompleted();

    void refreshDataFromDB();
    /**
     *
     * @param id download id
     * @return
     */
    FileDownloadModel find(final int id);


    void insert(final FileDownloadModel downloadModel);

    void update(final FileDownloadModel downloadModel);

    void remove(final int id);

    void update(final int id, final FileDownloadStatus status, final int soFar, final int total);

    void updateHeader(final int id, final String etag);

    void updateError(final int id, final String errMsg);

    void updateComplete(final int id, final int total);

    void updatePause(final int id);

    void updatePending(final int id);
}
