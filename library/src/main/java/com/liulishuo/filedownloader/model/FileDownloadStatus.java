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

package com.liulishuo.filedownloader.model;

import com.liulishuo.filedownloader.BaseDownloadTask;

/**
 * The downloading status.
 *
 * @see com.liulishuo.filedownloader.IFileDownloadMessenger
 * @see <a href="https://raw.githubusercontent.com/lingochamp/FileDownloader/master/art/filedownloadlistener_callback_flow.png">Callback-Flow</a>
 */
public class FileDownloadStatus {
    // [-2^7, 2^7 -1]
    // by very beginning
    /**
     * When the task on {@code toLaunchPool} status, it means that the task is just into the
     * LaunchPool and is scheduled for launch.
     * <p>
     * The task is scheduled for launch and it isn't on the FileDownloadService yet.
     */
    public final static byte toLaunchPool = 10;
    /**
     * When the task on {@code toFileDownloadService} status, it means that the task is just post to
     * the FileDownloadService.
     * <p>
     * The task is posting to the FileDownloadService and after this status, this task can start.
     */
    public final static byte toFileDownloadService = 11;

    // by FileDownloadService
    /**
     * When the task on {@code pending} status, it means that the task is in the list on the
     * FileDownloadService and just waiting for start.
     * <p>
     * The task is waiting on the FileDownloadService.
     * <p>
     * The count of downloading simultaneously, you can configure in filedownloader.properties.
     */
    public final static byte pending = 1;
    /**
     * When the task on {@code started} status, it means that the network access thread of
     * downloading this task is started.
     * <p>
     * The task is downloading on the FileDownloadService.
     */
    public final static byte started = 6;
    /**
     * When the task on {@code connected} status, it means that the task is successfully connected
     * to the back-end.
     * <p>
     * The task is downloading on the FileDownloadService.
     */
    public final static byte connected = 2;
    /**
     * When the task on {@code progress} status, it means that the task is fetching data from the
     * back-end.
     * <p>
     * The task is downloading on the FileDownloadService.
     */
    public final static byte progress = 3;
    /**
     * When the task on {@code blockComplete} status, it means that the task has been completed
     * downloading successfully.
     * <p>
     * The task is completed downloading successfully and the action-flow is blocked for doing
     * something before callback completed method.
     */
    public final static byte blockComplete = 4;
    /**
     * When the task on {@code retry} status, it means that the task must occur some error, but
     * there is a valid chance to retry, so the task is retry to download again.
     * <p>
     * The task is restarting on the FileDownloadService.
     */
    public final static byte retry = 5;

    /**
     * When the task on {@code error} status, it means that the task must occur some error and there
     * isn't any valid chance to retry, so the task is finished with error.
     * <p>
     * The task is finished with an error.
     */
    public final static byte error = -1;
    /**
     * When the task on {@code paused} status, it means that the task is paused manually.
     * <p>
     * The task is finished with the pause action.
     */
    public final static byte paused = -2;
    /**
     * When the task on {@code completed} status, it means that the task is completed downloading
     * successfully.
     * <p>
     * The task is finished with completed downloading successfully.
     */
    public final static byte completed = -3;
    /**
     * When the task on {@code warn} status, it means that there is another same task(same url,
     * same path to store content) is running.
     * <p>
     * The task is finished with the warn status.
     */
    public final static byte warn = -4;

    /**
     * When the task on {@code INVALID_STATUS} status, it means that the task is IDLE.
     * <p>
     * The task is clear and it isn't launched.
     */
    public final static byte INVALID_STATUS = 0;

    public static boolean isOver(final int status) {
        return status < 0;
    }

    public static boolean isIng(final int status) {
        return status > 0;
    }

    public static boolean isKeepAhead(final int status, final int nextStatus) {
        if (status != progress && status != retry && status == nextStatus) {
            return false;
        }

        if (isOver(status)) {
            return false;
        }

        if (status >= pending && status <= started /** in FileDownloadService **/
                && nextStatus >= toLaunchPool && nextStatus <= toFileDownloadService) {
            return false;
        }

        switch (status) {
            case pending:
                switch (nextStatus) {
                    case INVALID_STATUS:
                        return false;
                    default:
                        return true;
                }
            case started:
                switch (nextStatus) {
                    case INVALID_STATUS:
                    case pending:
                        return false;
                    default:
                        return true;
                }

            case connected:
                switch (nextStatus) {
                    case INVALID_STATUS:
                    case pending:
                    case started:
                        return false;
                    default:
                        return true;
                }
            case progress:
                switch (nextStatus) {
                    case INVALID_STATUS:
                    case pending:
                    case started:
                    case connected:
                        return false;
                    default:
                        return true;
                }

            case retry:
                switch (nextStatus) {
                    case pending:
                    case started:
                        return false;
                    default:
                        return true;
                }

            default:
                return true;
        }

    }

    public static boolean isKeepFlow(final int status, final int nextStatus) {
        if (status != progress && status != retry && status == nextStatus) {
            return false;
        }

        if (isOver(status)) {
            return false;
        }

        if (nextStatus == paused) {
            return true;
        }

        if (nextStatus == error) {
            return true;
        }

        switch (status) {
            case INVALID_STATUS: {
                switch (nextStatus) {
                    case toLaunchPool:
                        return true;
                    default:
                        return false;
                }
            }
            case toLaunchPool:
                switch (nextStatus) {
                    case toFileDownloadService:
                        return true;
                    default:
                        return false;
                }
            case toFileDownloadService:
                switch (nextStatus) {
                    case pending:
                    case warn:
                    case completed:
                        return true;
                    default:
                        return false;
                }
            case pending:
                switch (nextStatus) {
                    case started:
                        return true;
                    default:
                        return false;
                }
            case retry:
            case started:
                switch (nextStatus) {
                    case retry:
                    case connected:
                        return true;
                    default:
                        return false;
                }
            case connected:
            case progress:
                switch (nextStatus) {
                    case progress:
                    case completed:
                    case retry:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }

    }

    public static boolean isMoreLikelyCompleted(BaseDownloadTask task) {
        return task.getStatus() == INVALID_STATUS || task.getStatus() == progress;
    }
}
