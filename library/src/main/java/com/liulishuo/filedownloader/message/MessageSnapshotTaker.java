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
import com.liulishuo.filedownloader.download.DownloadStatusCallback;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
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

    public static MessageSnapshot catchCanReusedOldFile(int id, File oldFile,
                                                        boolean flowDirectly) {
        final long totalBytes = oldFile.length();
        if (totalBytes > Integer.MAX_VALUE) {
            if (flowDirectly) {
                return new LargeMessageSnapshot.CompletedFlowDirectlySnapshot(id, true, totalBytes);
            } else {
                return new LargeMessageSnapshot.CompletedSnapshot(id, true, totalBytes);
            }
        } else {
            if (flowDirectly) {
                return new SmallMessageSnapshot.CompletedFlowDirectlySnapshot(id, true,
                        (int) totalBytes);
            } else {
                return new SmallMessageSnapshot.CompletedSnapshot(id, true, (int) totalBytes);
            }
        }
    }

    public static MessageSnapshot catchWarn(int id, long sofar, long total, boolean flowDirectly) {
        if (total > Integer.MAX_VALUE) {
            if (flowDirectly) {
                return new LargeMessageSnapshot.WarnFlowDirectlySnapshot(id, sofar, total);
            } else {
                return new LargeMessageSnapshot.WarnMessageSnapshot(id, sofar, total);
            }
        } else {
            if (flowDirectly) {
                return new SmallMessageSnapshot.WarnFlowDirectlySnapshot(id, (int) sofar,
                        (int) total);
            } else {
                return new SmallMessageSnapshot.WarnMessageSnapshot(id, (int) sofar, (int) total);
            }
        }
    }

    public static MessageSnapshot catchException(int id, long sofar, Throwable error) {
        if (sofar > Integer.MAX_VALUE) {
            return new LargeMessageSnapshot.ErrorMessageSnapshot(id, sofar, error);
        } else {
            return new SmallMessageSnapshot.ErrorMessageSnapshot(id, (int) sofar, error);
        }
    }

    public static MessageSnapshot catchPause(BaseDownloadTask task) {
        if (task.isLargeFile()) {
            return new LargeMessageSnapshot.PausedSnapshot(task.getId(),
                    task.getLargeFileSoFarBytes(), task.getLargeFileTotalBytes());
        } else {
            return new SmallMessageSnapshot.PausedSnapshot(task.getId(),
                    task.getSmallFileSoFarBytes(), task.getSmallFileTotalBytes());
        }
    }

    public static MessageSnapshot takeBlockCompleted(MessageSnapshot snapshot) {
        if (snapshot.getStatus() != FileDownloadStatus.completed) {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("take block completed snapshot, must has "
                                    + "already be completed. %d %d",
                            snapshot.getId(), snapshot.getStatus()));
        }

        return new BlockCompleteMessage.BlockCompleteMessageImpl(snapshot);
    }

    public static MessageSnapshot take(byte status, FileDownloadModel model,
                                       DownloadStatusCallback.ProcessParams processParams) {
        final MessageSnapshot snapShot;
        final int id = model.getId();
        if (status == FileDownloadStatus.warn) {
            throw new IllegalStateException(FileDownloadUtils.
                    formatString("please use #catchWarn instead %d", id));
        }

        switch (status) {
            case FileDownloadStatus.pending:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.PendingMessageSnapshot(id,
                            model.getSoFar(), model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.PendingMessageSnapshot(id,
                            (int) model.getSoFar(), (int) model.getTotal());
                }
                break;
            case FileDownloadStatus.started:
                snapShot = new MessageSnapshot.StartedMessageSnapshot(id);
                break;
            case FileDownloadStatus.connected:
                final String filename = model.isPathAsDirectory() ? model.getFilename() : null;
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ConnectedMessageSnapshot(id,
                            processParams.isResuming(), model.getTotal(), model.getETag(),
                            filename);
                } else {
                    snapShot = new SmallMessageSnapshot.ConnectedMessageSnapshot(id,
                            processParams.isResuming(), (int) model.getTotal(), model.getETag(),
                            filename);
                }
                break;
            case FileDownloadStatus.progress:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.
                            ProgressMessageSnapshot(id, model.getSoFar());
                } else {
                    snapShot = new SmallMessageSnapshot.
                            ProgressMessageSnapshot(id, (int) model.getSoFar());
                }
                break;
            case FileDownloadStatus.completed:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.
                            CompletedSnapshot(id, false, model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.
                            CompletedSnapshot(id, false, (int) model.getTotal());
                }
                break;
            case FileDownloadStatus.retry:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.RetryMessageSnapshot(id,
                            model.getSoFar(), processParams.getException(),
                            processParams.getRetryingTimes());
                } else {
                    snapShot = new SmallMessageSnapshot.RetryMessageSnapshot(id,
                            (int) model.getSoFar(), processParams.getException(),
                            processParams.getRetryingTimes());
                }
                break;
            case FileDownloadStatus.error:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ErrorMessageSnapshot(id,
                            model.getSoFar(), processParams.getException());
                } else {
                    snapShot = new SmallMessageSnapshot.ErrorMessageSnapshot(id,
                            (int) model.getSoFar(), processParams.getException());
                }
                break;
            default:
                // deal with as error.
                final String message = FileDownloadUtils.
                        formatString(
                                "it can't takes a snapshot for the task(%s) when its status is %d,",
                                model, status);

                FileDownloadLog.w(MessageSnapshotTaker.class,
                        "it can't takes a snapshot for the task(%s) when its status is %d,", model,
                        status);

                final Throwable throwable;
                if (processParams.getException() != null) {
                    throwable = new IllegalStateException(message, processParams.getException());
                } else {
                    throwable = new IllegalStateException(message);
                }

                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ErrorMessageSnapshot(id,
                            model.getSoFar(), throwable);
                } else {
                    snapShot = new SmallMessageSnapshot.ErrorMessageSnapshot(id,
                            (int) model.getSoFar(), throwable);
                }
                break;
        }

        return snapShot;
    }
}
