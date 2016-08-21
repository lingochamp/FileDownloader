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

package com.liulishuo.filedownloader.message;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.FileDownloadRunnable;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * The factory for taking message snapshots.
 */
public class MessageSnapshotTaker {

    public static MessageSnapshot take(byte status, FileDownloadModel model) {
        return take(status, model, null);
    }

    public static MessageSnapshot catchCanReusedOldFile(int id, File oldFile, boolean flowDirectly) {
        final long totalBytes = oldFile.length();
        if (totalBytes > Integer.MAX_VALUE) {
            if (flowDirectly) {
                return new LargeMessageSnapshot.CompletedFlowDirectlySnapshot(id,
                        FileDownloadStatus.completed, true, totalBytes);
            } else {
                return new LargeMessageSnapshot.CompletedSnapshot(id,
                        FileDownloadStatus.completed, true, totalBytes);
            }
        } else {
            if (flowDirectly) {
                return new SmallMessageSnapshot.CompletedFlowDirectlySnapshot(id,
                        FileDownloadStatus.completed, true, (int) totalBytes);
            } else {
                return new SmallMessageSnapshot.CompletedSnapshot(id,
                        FileDownloadStatus.completed, true, (int) totalBytes);
            }
        }
    }

    public static MessageSnapshot catchWarn(int id, long sofar, long total, boolean flowDirectly) {
        if (total > Integer.MAX_VALUE) {
            if (flowDirectly) {
                return new LargeMessageSnapshot.WarnFlowDirectlySnapshot(id, FileDownloadStatus.warn,
                        sofar, total);
            } else {
                return new LargeMessageSnapshot.WarnMessageSnapshot(id, FileDownloadStatus.warn,
                        sofar, total);
            }
        } else {
            if (flowDirectly) {
                return new SmallMessageSnapshot.WarnFlowDirectlySnapshot(id, FileDownloadStatus.warn,
                        (int) sofar, (int) total);
            } else {
                return new SmallMessageSnapshot.WarnMessageSnapshot(id, FileDownloadStatus.warn,
                        (int) sofar, (int) total);
            }
        }
    }

    public static MessageSnapshot catchException(BaseDownloadTask task) {
        if (task.isLargeFile()) {
            return new LargeMessageSnapshot.ErrorMessageSnapshot(task.getId(),
                    FileDownloadStatus.error, task.getLargeFileSoFarBytes(), task.getErrorCause());
        } else {
            return new SmallMessageSnapshot.ErrorMessageSnapshot(task.getId(),
                    FileDownloadStatus.error, task.getSmallFileSoFarBytes(), task.getErrorCause());
        }
    }

    public static MessageSnapshot catchPause(BaseDownloadTask task) {
        if (task.isLargeFile()) {
            return new LargeMessageSnapshot.PausedSnapshot(task.getId(), FileDownloadStatus.paused,
                    task.getLargeFileSoFarBytes(), task.getLargeFileTotalBytes());
        } else {
            return new SmallMessageSnapshot.PausedSnapshot(task.getId(), FileDownloadStatus.paused,
                    task.getSmallFileSoFarBytes(), task.getSmallFileTotalBytes());
        }
    }

    public static MessageSnapshot takeBlockCompleted(MessageSnapshot snapshot) {
        if (snapshot.getStatus() != FileDownloadStatus.completed) {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("take block completed snapshot, must has " +
                            "already be completed. %d %d", snapshot.getId(), snapshot.getStatus()));
        }

        return new MessageSnapshot(snapshot.getId(), FileDownloadStatus.blockComplete);
    }

    public static MessageSnapshot take(byte status, FileDownloadModel model,
                                       FileDownloadRunnable runnable) {
        final MessageSnapshot snapShot;
        final int id = model.getId();
        if (status == FileDownloadStatus.warn) {
            throw new IllegalStateException(FileDownloadUtils.
                    formatString("please use #catchWarn instead %d", id));
        }

        switch (status) {
            case FileDownloadStatus.pending:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.PendingMessageSnapshot(id, status,
                            model.getSoFar(), model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.PendingMessageSnapshot(id, status,
                            (int) model.getSoFar(), (int) model.getTotal());
                }
                break;
            case FileDownloadStatus.started:
                snapShot = new MessageSnapshot(id, status);
                break;
            case FileDownloadStatus.connected:
                final String filename = model.isPathAsDirectory() ? model.getFilename() :
                        null;
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ConnectedMessageSnapshot(id, status,
                            runnable.isResuming(), model.getTotal(), model.getETag(), filename);
                } else {
                    snapShot = new SmallMessageSnapshot.ConnectedMessageSnapshot(id, status,
                            runnable.isResuming(), (int) model.getTotal(), model.getETag(), filename);
                }
                break;
            case FileDownloadStatus.progress:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ProgressMessageSnapshot(id, status,
                            model.getSoFar());
                } else {
                    snapShot = new SmallMessageSnapshot.ProgressMessageSnapshot(id, status,
                            (int) model.getSoFar());
                }
                break;
            case FileDownloadStatus.completed:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.CompletedSnapshot(id, status,
                            false, model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.CompletedSnapshot(id, status,
                            false, (int) model.getTotal());
                }
                break;
            case FileDownloadStatus.retry:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.RetryMessageSnapshot(id, status,
                            model.getSoFar(), runnable.getThrowable(), runnable.getRetryingTimes());
                } else {
                    snapShot = new SmallMessageSnapshot.RetryMessageSnapshot(id, status,
                            (int) model.getSoFar(), runnable.getThrowable(),
                            runnable.getRetryingTimes());
                }
                break;
            case FileDownloadStatus.error:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ErrorMessageSnapshot(id, status,
                            model.getSoFar(), runnable.getThrowable());
                } else {
                    snapShot = new SmallMessageSnapshot.ErrorMessageSnapshot(id, status,
                            (int) model.getSoFar(), runnable.getThrowable());
                }
                break;
            default:
                // deal with as error.
                final String message = FileDownloadUtils.
                        formatString("it can't takes a snapshot for the task(%s) when its status " +
                                "is %d,", model, status);

                FileDownloadLog.w(MessageSnapshotTaker.class, message);

                final Throwable throwable;
                if (runnable.getThrowable() != null) {
                    throwable = new IllegalStateException(message, runnable.getThrowable());
                } else {
                    throwable = new IllegalStateException(message);
                }

                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ErrorMessageSnapshot(id, status,
                            model.getSoFar(), throwable);
                } else {
                    snapShot = new SmallMessageSnapshot.ErrorMessageSnapshot(id, status,
                            (int) model.getSoFar(), throwable);
                }
                break;
        }

        return snapShot;
    }
}
