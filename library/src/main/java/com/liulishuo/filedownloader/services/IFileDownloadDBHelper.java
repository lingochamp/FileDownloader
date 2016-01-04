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


import com.liulishuo.filedownloader.model.FileDownloadModel;

import java.util.Set;

/**
 * Created by Jacksgong on 9/24/15.
 */
interface IFileDownloadDBHelper {

    Set<FileDownloadModel> getAllUnComplete();

    Set<FileDownloadModel> getAllCompleted();

    void refreshDataFromDB();

    /**
     * @param id download id
     */
    FileDownloadModel find(final int id);


    void insert(final FileDownloadModel downloadModel);

    void update(final FileDownloadModel downloadModel);

    void remove(final int id);

    void update(final int id, final byte status, final long soFar, final long total);

    void updateHeader(final int id, final String etag);

    void updateError(final int id, final String errMsg);

    void updateRetry(final int id, final String errMsg, final int retryingTimes);

    void updateComplete(final int id, final long total);

    void updatePause(final int id);

    void updatePending(final int id);
}
