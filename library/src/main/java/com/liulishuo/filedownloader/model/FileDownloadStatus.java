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
    public final static byte pending = 1;
    public final static byte started = 6;
    public final static byte connected = 2;
    public final static byte progress = 3;
    /**
     * Just for event
     **/
    public final static byte blockComplete = 4;
    public final static byte retry = 5;

    public final static byte error = -1;
    public final static byte paused = -2;
    public final static byte completed = -3;
    public final static byte warn = -4;

    public final static byte MAX_INT = 6;
    public final static byte MIN_INT = -4;
    public final static byte INVALID_STATUS = 0;

    public static boolean isOver(final int status) {
        return status < 0;
    }

    public static boolean isIng(final int status) {
        return status >= pending && status <= started;
    }

    public static boolean isKeepAhead(final int status, final int nextStatus) {
        if (status != progress && status != retry && status == nextStatus) {
            return false;
        }

        if (isOver(status)) {
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
            case INVALID_STATUS:
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
