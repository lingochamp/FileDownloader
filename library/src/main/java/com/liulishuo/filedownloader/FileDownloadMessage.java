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

package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * Created by Jacksgong on 4/27/16.
 * <p/>
 * A Message for {@link com.liulishuo.filedownloader.FileDownloadListener}.
 */
public class FileDownloadMessage {

    public static FileDownloadMessage writeMessage(BaseDownloadTask task, final int status) {
        final MessageSnapShot snapShot;
        switch (task.getStatus()) {
            case FileDownloadStatus.pending:
            case FileDownloadStatus.progress:
                snapShot = new ProcessingMessageSnapshot(task, status);
                break;
            case FileDownloadStatus.started:
            case FileDownloadStatus.completed:
            case FileDownloadStatus.warn:
                snapShot = new MessageSnapShot(status);
                break;
            default:
                snapShot = new IntactMessageSnapshot(task, status);
        }

        return new FileDownloadMessage(task, snapShot);
    }

    public static FileDownloadMessage writeLargeFileMessage(BaseDownloadTask task, final int status) {
        final MessageSnapShot snapShot;
        switch (task.getStatus()) {
            case FileDownloadStatus.pending:
            case FileDownloadStatus.progress:
                snapShot = new ProcessingLargeMessageSnapshot(task, status);
                break;
            case FileDownloadStatus.started:
            case FileDownloadStatus.completed:
            case FileDownloadStatus.warn:
                snapShot = new MessageSnapShot(status);
                break;
            default:
                snapShot = new IntactLargeMessageSnapshot(task, status);
        }

        return new FileDownloadMessage(task, snapShot);
    }

    private final BaseDownloadTask task;

    private final MessageSnapShot snapshot;

    private FileDownloadMessage(BaseDownloadTask task, MessageSnapShot snapshot) {
        this.task = task;
        this.snapshot = snapshot;
    }

    public MessageSnapShot getSnapshot() {
        return this.snapshot;
    }


    public BaseDownloadTask getTask() {
        return this.task;
    }

    public static class MessageSnapShot implements IMessageSnapshot {
        private final int status;

        public MessageSnapShot(int status) {
            this.status = status;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public Throwable getException() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No exception in this message %d", status));
        }

        @Override
        public int getRetryingTimes() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No retrying times in this message %d", status));
        }

        @Override
        public boolean isResuming() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is resuming in this message %d", status));
        }

        @Override
        public String getEtag() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is etag in this message %d", status));
        }

        @Override
        public int getSmallSoFarBytes() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is small sofar bytes in this message %d",
                            status));
        }

        @Override
        public int getSmallTotalBytes() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is small total bytes in this message %d",
                            status));
        }

        @Override
        public long getLargeSoFarBytes() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is large sofar bytes in this message %d",
                            status));
        }

        @Override
        public long getLargeTotalBytes() {
            throw new IllegalStateException(
                    FileDownloadUtils.formatString("No is large total bytes in this message %d",
                            status));
        }
    }

    public static class ProcessingLargeMessageSnapshot extends MessageSnapShot {

        private final long sofarBytes, totalBytes;

        public ProcessingLargeMessageSnapshot(BaseDownloadTask task, int status) {
            super(status);
            this.sofarBytes = task.getLargeFileSoFarBytes();
            this.totalBytes = task.getLargeFileTotalBytes();
        }

        @Override
        public long getLargeSoFarBytes() {
            return sofarBytes;
        }

        @Override
        public long getLargeTotalBytes() {
            return totalBytes;
        }

        @Override
        public int getSmallTotalBytes() {
            if (totalBytes > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) totalBytes;
        }

        @Override
        public int getSmallSoFarBytes() {
            if (sofarBytes > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) sofarBytes;
        }
    }

    public static class IntactLargeMessageSnapshot extends ProcessingLargeMessageSnapshot {
        private final Throwable exception;
        private final int retryingTimes;
        private final boolean isResuming;
        private final String etag;

        public IntactLargeMessageSnapshot(BaseDownloadTask task, int status) {
            super(task, status);
            this.exception = task.getEx();
            this.retryingTimes = task.getRetryingTimes();
            this.isResuming = task.isResuming();
            this.etag = task.getEtag();
        }

        @Override
        public Throwable getException() {
            return exception;
        }

        @Override
        public int getRetryingTimes() {
            return retryingTimes;
        }

        @Override
        public boolean isResuming() {
            return isResuming;
        }

        @Override
        public String getEtag() {
            return etag;
        }
    }

    public static class ProcessingMessageSnapshot extends MessageSnapShot {
        private final int sofarBytes, totalBytes;

        public ProcessingMessageSnapshot(BaseDownloadTask task, int status) {
            super(status);
            this.sofarBytes = task.getSmallFileSoFarBytes();
            this.totalBytes = task.getSmallFileTotalBytes();
        }

        @Override
        public int getSmallSoFarBytes() {
            return sofarBytes;
        }

        @Override
        public int getSmallTotalBytes() {
            return totalBytes;
        }
    }

    public static class IntactMessageSnapshot extends ProcessingMessageSnapshot {
        private final Throwable exception;
        private final int retryingTimes;
        private final boolean isResuming;
        private final String etag;

        public IntactMessageSnapshot(BaseDownloadTask task, int status) {
            super(task, status);
            this.exception = task.getEx();
            this.retryingTimes = task.getRetryingTimes();
            this.isResuming = task.isResuming();
            this.etag = task.getEtag();
        }

        @Override
        public Throwable getException() {
            return exception;
        }

        @Override
        public int getRetryingTimes() {
            return retryingTimes;
        }

        @Override
        public boolean isResuming() {
            return isResuming;
        }

        @Override
        public String getEtag() {
            return etag;
        }
    }

    private interface IMessageSnapshot {
        int getStatus();

        Throwable getException();

        int getRetryingTimes();

        boolean isResuming();

        String getEtag();

        int getSmallSoFarBytes();

        int getSmallTotalBytes();

        long getLargeSoFarBytes();

        long getLargeTotalBytes();

    }
}
