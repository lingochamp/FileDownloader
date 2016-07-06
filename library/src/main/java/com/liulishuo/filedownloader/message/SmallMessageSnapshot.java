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
 * Created by Jacksgong on 5/1/16.
 * <p/>
 * A message snapshot for a small file(the length is less than 2G).
 */
public class SmallMessageSnapshot extends MessageSnapshot {

    SmallMessageSnapshot(int id, byte status) {
        super(id, status);
        isLargeFile = false;
    }

    SmallMessageSnapshot(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public long getLargeTotalBytes() {
        return getSmallTotalBytes();
    }

    @Override
    public long getLargeSofarBytes() {
        return getSmallSofarBytes();
    }

    public static class PendingMessageSnapshot extends SmallMessageSnapshot {

        private final int sofarBytes, totalBytes;

        PendingMessageSnapshot(int id, byte status, int sofarBytes, int totalBytes) {
            super(id, status);
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
        public int getSmallSofarBytes() {
            return sofarBytes;
        }

        @Override
        public int getSmallTotalBytes() {
            return totalBytes;
        }
    }

    public static class ConnectedMessageSnapshot extends SmallMessageSnapshot {
        private final boolean resuming;
        private final int totalBytes;
        private final String etag;
        private final String fileName;

        ConnectedMessageSnapshot(int id, byte status, boolean resuming, int totalBytes,
                                 String etag, String fileName) {
            super(id, status);
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

    public static class ProgressMessageSnapshot extends SmallMessageSnapshot {
        private final int sofarBytes;

        ProgressMessageSnapshot(int id, byte status, int sofarBytes) {
            super(id, status);
            this.sofarBytes = sofarBytes;
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

    public static class CompletedSnapshot extends SmallMessageSnapshot {
        private boolean reusedDownloadedFile;
        private int totalBytes;

        CompletedSnapshot(int id, byte status, boolean reusedDownloadedFile,
                          int totalBytes) {
            super(id, status);
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
        public int getSmallTotalBytes() {
            return totalBytes;
        }

        @Override
        public boolean isReusedDownloadedFile() {
            return reusedDownloadedFile;
        }
    }

    public static class ErrorMessageSnapshot extends SmallMessageSnapshot {
        private final int sofarBytes;
        private final Throwable throwable;

        ErrorMessageSnapshot(int id, byte status, int sofarBytes, Throwable throwable) {
            super(id, status);
            this.sofarBytes = sofarBytes;
            this.throwable = throwable;
        }

        @Override
        public int getSmallSofarBytes() {
            return sofarBytes;
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

    public static class RetryMessageSnapshot extends ErrorMessageSnapshot {
        private final int retryingTimes;

        RetryMessageSnapshot(int id, byte status, int sofarBytes, Throwable throwable,
                             int retryingTimes) {
            super(id, status, sofarBytes, throwable);
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
    }

    public static class WarnMessageSnapshot extends PendingMessageSnapshot implements
            IWarnMessageSnapshot {

        WarnMessageSnapshot(int id, byte status, int sofarBytes, int totalBytes) {
            super(id, status, sofarBytes, totalBytes);
        }

        WarnMessageSnapshot(Parcel in) {
            super(in);
        }

        @Override
        public void turnToPending() {
            this.status = FileDownloadStatus.pending;
        }
    }

    public static class PausedSnapshot extends PendingMessageSnapshot {
        PausedSnapshot(int id, byte status, int sofarBytes, int totalBytes) {
            super(id, status, sofarBytes, totalBytes);
        }
    }
}
