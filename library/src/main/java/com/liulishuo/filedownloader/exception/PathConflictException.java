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

package com.liulishuo.filedownloader.exception;

import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * Throw this exception, when there is an another running task with the same path with the current
 * task, so if the current task is started, the path of the file is sure to be written by multiple
 * tasks, it is wrong, then we through this exception to avoid such conflict.
 */

public class PathConflictException extends IllegalAccessException {

    private final String mDownloadingConflictPath;
    private final String mTargetFilePath;
    private final int mAnotherSamePathTaskId;

    public PathConflictException(final int anotherSamePathTaskId, final String conflictPath,
                                 final String targetFilePath) {
        super(FileDownloadUtils.formatString("There is an another running task(%d) with the"
                        + " same downloading path(%s), because of they are with the same "
                        + "target-file-path(%s), so if the current task is started, the path of the"
                        + " file is sure to be written by multiple tasks, it is wrong, then you "
                        + "receive this exception to avoid such conflict.",
                anotherSamePathTaskId, conflictPath, targetFilePath));

        mAnotherSamePathTaskId = anotherSamePathTaskId;
        mDownloadingConflictPath = conflictPath;
        mTargetFilePath = targetFilePath;
    }

    /**
     * Get the conflict downloading file path, normally, this path is used for store the downloading
     * file relate with the {@link #mTargetFilePath}, and it would be generated from
     * {@link FileDownloadUtils#getTempPath(String)}.
     *
     * @return the conflict downloading file path.
     */
    public String getDownloadingConflictPath() {
        return mDownloadingConflictPath;
    }

    /**
     * Get the target file path, which downloading file path is conflict when downloading the task.
     *
     * @return the target file path, which downloading file path is conflict when downloading the
     * task.
     */
    public String getTargetFilePath() {
        return mTargetFilePath;
    }

    /**
     * Get the identify of another task which has the same path with the current task and its target
     * file path is the same to the current task too.
     *
     * @return the identify of another task which has the same path with the current task and its
     * target file path is the same to the current task too.
     */
    public int getAnotherSamePathTaskId() {
        return mAnotherSamePathTaskId;
    }
}
