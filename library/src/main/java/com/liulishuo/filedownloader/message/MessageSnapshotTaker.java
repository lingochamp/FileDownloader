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
import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * Created by Jacksgong on 5/1/16.
 * <p/>
 * The factory for take message snapshots.
 */
public class MessageSnapshotTaker {

    public static MessageSnapshot take(byte status, FileDownloadModel model) {
        return take(status, model, null, false);
    }

    public static MessageSnapshot take(byte status, FileDownloadModel model,
                                       FileDownloadRunnable runnable) {
        return take(status, model, runnable, false);
    }

    public static MessageSnapshot take(byte status, FileDownloadModel model,
                                       boolean reusedDownloadedFile) {
        return take(status, model, null, reusedDownloadedFile);
    }

    public static MessageSnapshot catchException(BaseDownloadTask task) {
        if (task.isLargeFile()) {
            return new LargeMessageSnapshot.ErrorMessageSnapshot(task.getId(),
                    FileDownloadStatus.error, task.getLargeFileSoFarBytes(), task.getEx());
        } else {
            return new SmallMessageSnapshot.ErrorMessageSnapshot(task.getId(),
                    FileDownloadStatus.error, task.getSmallFileSoFarBytes(), task.getEx());
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

    private static MessageSnapshot take(byte status, FileDownloadModel model,
                                        FileDownloadRunnable runnable,
                                        boolean reusedDownloadedFile) {
        final MessageSnapshot snapShot;
        final int id = model.getId();
        switch (status) {
            case FileDownloadStatus.warn:
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.WarnMessageSnapshot(id, status,
                            model.getSoFar(), model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.WarnMessageSnapshot(id, status,
                            (int) model.getSoFar(), (int) model.getTotal());
                }
                break;
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
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.ConnectedMessageSnapshot(id, status,
                            runnable.isResuming(), model.getTotal(), model.getETag());
                } else {
                    snapShot = new SmallMessageSnapshot.ConnectedMessageSnapshot(id, status,
                            runnable.isResuming(), (int) model.getTotal(), model.getETag());
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
                final String etag = reusedDownloadedFile ? model.getETag() : null;
                if (model.isLargeFile()) {
                    snapShot = new LargeMessageSnapshot.CompletedSnapshot(id, status,
                            reusedDownloadedFile, etag, model.getTotal());
                } else {
                    snapShot = new SmallMessageSnapshot.CompletedSnapshot(id, status,
                            reusedDownloadedFile, etag, (int) model.getTotal());
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
                snapShot = null;
        }

        return snapShot;
    }
}
