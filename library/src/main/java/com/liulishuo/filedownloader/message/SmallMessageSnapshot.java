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

import android.os.Parcel;

import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * A message snapshot for a small file(the length is less than 2G).
 *
 * @see LargeMessageSnapshot
 * @see BlockCompleteMessage
 */
public abstract class SmallMessageSnapshot extends MessageSnapshot {

    SmallMessageSnapshot(int id) {
        super(id);
        isLargeFile = false;
    }

    SmallMessageSnapshot(Parcel in) {
        super(in);
    }

    @Override
    public long getLargeTotalBytes() {
        return getSmallTotalBytes();
    }

    @Override
    public long getLargeSofarBytes() {
        return getSmallSofarBytes();
    }

    // Pending Snapshot
    public static class PendingMessageSnapshot extends SmallMessageSnapshot {

        private final int sofarBytes, totalBytes;

        PendingMessageSnapshot(PendingMessageSnapshot snapshot) {
            this(snapshot.getId(), snapshot.getSmallSofarBytes(), snapshot.getSmallTotalBytes());
        }

        PendingMessageSnapshot(int id, int sofarBytes, int totalBytes) {
            super(id);
            this.sofarBytes = sofarBytes;
            this.totalBytes = totalBytes;
        }

        PendingMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readInt();
            this.totalBytes = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.sofarBytes);
            dest.writeInt(this.totalBytes);
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.pending;
        }

        @Override
        public int getSmallSofarBytes() {
            return sofarBytes;
        }

        @Override
        public int getSmallTotalBytes() {
            return totalBytes;
        }
    }


    // Connected Snapshot
    public static class ConnectedMessageSnapshot extends SmallMessageSnapshot {
        private final boolean resuming;
        private final int totalBytes;
        private final String etag;
        private final String fileName;

        ConnectedMessageSnapshot(int id, boolean resuming, int totalBytes,
                                 String etag, String fileName) {
            super(id);
            this.resuming = resuming;
            this.totalBytes = totalBytes;
            this.etag = etag;
            this.fileName = fileName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte(resuming ? (byte) 1 : (byte) 0);
            dest.writeInt(this.totalBytes);
            dest.writeString(this.etag);
            dest.writeString(this.fileName);
        }

        ConnectedMessageSnapshot(Parcel in) {
            super(in);
            this.resuming = in.readByte() != 0;
            this.totalBytes = in.readInt();
            this.etag = in.readString();
            this.fileName = in.readString();
        }

        @Override
        public String getFileName() {
            return fileName;
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.connected;
        }

        @Override
        public boolean isResuming() {
            return resuming;
        }

        @Override
        public int getSmallTotalBytes() {
            return totalBytes;
        }

        @Override
        public String getEtag() {
            return etag;
        }
    }

    // Progress Snapshot
    public static class ProgressMessageSnapshot extends SmallMessageSnapshot {
        private final int sofarBytes;

        ProgressMessageSnapshot(int id, int sofarBytes) {
            super(id);
            this.sofarBytes = sofarBytes;
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.progress;
        }

        @Override
        public int getSmallSofarBytes() {
            return sofarBytes;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.sofarBytes);
        }

        ProgressMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readInt();
        }
    }

    // Completed Snapshot
    public static class CompletedFlowDirectlySnapshot extends CompletedSnapshot implements
            IFlowDirectly {

        CompletedFlowDirectlySnapshot(int id, boolean reusedDownloadedFile,
                                      int totalBytes) {
            super(id, reusedDownloadedFile, totalBytes);
        }

        CompletedFlowDirectlySnapshot(Parcel in) {
            super(in);
        }
    }

    public static class CompletedSnapshot extends SmallMessageSnapshot {
        private final boolean reusedDownloadedFile;
        private final int totalBytes;

        CompletedSnapshot(int id, boolean reusedDownloadedFile,
                          int totalBytes) {
            super(id);
            this.reusedDownloadedFile = reusedDownloadedFile;
            this.totalBytes = totalBytes;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte(reusedDownloadedFile ? (byte) 1 : (byte) 0);
            dest.writeInt(this.totalBytes);
        }

        CompletedSnapshot(Parcel in) {
            super(in);
            this.reusedDownloadedFile = in.readByte() != 0;
            this.totalBytes = in.readInt();
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.completed;
        }

        @Override
        public int getSmallTotalBytes() {
            return totalBytes;
        }

        @Override
        public boolean isReusedDownloadedFile() {
            return reusedDownloadedFile;
        }
    }

    // Error Snapshot
    public static class ErrorMessageSnapshot extends SmallMessageSnapshot {
        private final int sofarBytes;
        private final Throwable throwable;

        ErrorMessageSnapshot(int id, int sofarBytes, Throwable throwable) {
            super(id);
            this.sofarBytes = sofarBytes;
            this.throwable = throwable;
        }

        @Override
        public int getSmallSofarBytes() {
            return sofarBytes;
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.error;
        }

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.sofarBytes);
            dest.writeSerializable(this.throwable);
        }

        ErrorMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readInt();
            this.throwable = (Throwable) in.readSerializable();
        }
    }

    // Retry Snapshot
    public static class RetryMessageSnapshot extends ErrorMessageSnapshot {
        private final int retryingTimes;

        RetryMessageSnapshot(int id, int sofarBytes, Throwable throwable,
                             int retryingTimes) {
            super(id, sofarBytes, throwable);
            this.retryingTimes = retryingTimes;
        }

        @Override
        public int getRetryingTimes() {
            return retryingTimes;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.retryingTimes);
        }

        RetryMessageSnapshot(Parcel in) {
            super(in);
            this.retryingTimes = in.readInt();
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.retry;
        }
    }


    // Warn Snapshot
    public static class WarnFlowDirectlySnapshot extends WarnMessageSnapshot implements
            IFlowDirectly {
        WarnFlowDirectlySnapshot(int id, int sofarBytes, int totalBytes) {
            super(id, sofarBytes, totalBytes);
        }

        WarnFlowDirectlySnapshot(Parcel in) {
            super(in);
        }
    }

    public static class WarnMessageSnapshot extends PendingMessageSnapshot implements
            IWarnMessageSnapshot {

        WarnMessageSnapshot(int id, int sofarBytes, int totalBytes) {
            super(id, sofarBytes, totalBytes);
        }

        WarnMessageSnapshot(Parcel in) {
            super(in);
        }

        @Override
        public MessageSnapshot turnToPending() {
            return new SmallMessageSnapshot.PendingMessageSnapshot(this);
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.warn;
        }
    }

    // Paused Snapshot
    public static class PausedSnapshot extends PendingMessageSnapshot {
        PausedSnapshot(int id, int sofarBytes, int totalBytes) {
            super(id, sofarBytes, totalBytes);
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.paused;
        }
    }
}
