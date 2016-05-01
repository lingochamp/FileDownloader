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
 * A message snapshot for large file(the length is more than or equal to 2G).
 */
public class LargeMessageSnapshot extends MessageSnapshot {

    LargeMessageSnapshot(int id, byte status) {
        super(id, status);
        isLargeFile = true;
    }

    LargeMessageSnapshot(Parcel in) {
        super(in);
    }

    @Override
    public int getSmallSofarBytes() {
        if (getLargeSofarBytes() > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) getLargeSofarBytes();
    }

    @Override
    public int getSmallTotalBytes() {
        if (getLargeTotalBytes() > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) getLargeTotalBytes();
    }

    public static class PendingMessageSnapshot extends LargeMessageSnapshot {

        private final long sofarBytes, totalBytes;

        PendingMessageSnapshot(int id, byte status, long sofarBytes, long totalBytes) {
            super(id, status);
            this.sofarBytes = sofarBytes;
            this.totalBytes = totalBytes;
        }

        @Override
        public long getLargeSofarBytes() {
            return sofarBytes;
        }

        @Override
        public long getLargeTotalBytes() {
            return totalBytes;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(this.sofarBytes);
            dest.writeLong(this.totalBytes);
        }

        PendingMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readLong();
            this.totalBytes = in.readLong();
        }
    }

    public static class ConnectedMessageSnapshot extends LargeMessageSnapshot {
        private final boolean resuming;
        private final long totalBytes;
        private final String etag;

        ConnectedMessageSnapshot(int id, byte status, boolean resuming, long totalBytes,
                                 String etag) {
            super(id, status);
            this.resuming = resuming;
            this.totalBytes = totalBytes;
            this.etag = etag;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte(resuming ? (byte) 1 : (byte) 0);
            dest.writeLong(this.totalBytes);
            dest.writeString(this.etag);
        }

        ConnectedMessageSnapshot(Parcel in) {
            super(in);
            this.resuming = in.readByte() != 0;
            this.totalBytes = in.readLong();
            this.etag = in.readString();
        }

        @Override
        public boolean isResuming() {
            return resuming;
        }

        @Override
        public long getLargeTotalBytes() {
            return totalBytes;
        }

        @Override
        public String getEtag() {
            return etag;
        }
    }

    public static class ProgressMessageSnapshot extends LargeMessageSnapshot {
        private final long sofarBytes;

        ProgressMessageSnapshot(int id, byte status, long sofarBytes) {
            super(id, status);
            this.sofarBytes = sofarBytes;
        }

        @Override
        public long getLargeSofarBytes() {
            return sofarBytes;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(this.sofarBytes);
        }

        ProgressMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readLong();
        }
    }

    public static class CompletedSnapshot extends LargeMessageSnapshot {
        private boolean reusedDownloadedFile;
        private String etag;
        private long totalBytes;

        CompletedSnapshot(int id, byte status, boolean reusedDownloadedFile, String etag,
                          long totalBytes) {
            super(id, status);
            this.reusedDownloadedFile = reusedDownloadedFile;
            this.etag = etag;
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
            dest.writeString(this.etag);
            dest.writeLong(this.totalBytes);
        }

        CompletedSnapshot(Parcel in) {
            super(in);
            this.reusedDownloadedFile = in.readByte() != 0;
            this.etag = in.readString();
            this.totalBytes = in.readLong();
        }

        @Override
        public String getEtag() {
            return etag;
        }

        @Override
        public long getLargeTotalBytes() {
            return totalBytes;
        }

        @Override
        public boolean isReusedDownloadedFile() {
            return reusedDownloadedFile;
        }
    }

    public static class ErrorMessageSnapshot extends LargeMessageSnapshot {
        private final long sofarBytes;
        private final Throwable throwable;

        ErrorMessageSnapshot(int id, byte status, long sofarBytes, Throwable throwable) {
            super(id, status);
            this.sofarBytes = sofarBytes;
            this.throwable = throwable;
        }

        @Override
        public long getLargeSofarBytes() {
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
            dest.writeLong(this.sofarBytes);
            dest.writeSerializable(this.throwable);
        }

        ErrorMessageSnapshot(Parcel in) {
            super(in);
            this.sofarBytes = in.readLong();
            this.throwable = (Throwable) in.readSerializable();
        }
    }

    public static class RetryMessageSnapshot extends ErrorMessageSnapshot {
        private final int retryingTimes;

        RetryMessageSnapshot(int id, byte status, long sofarBytes, Throwable throwable,
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

        WarnMessageSnapshot(int id, byte status, long sofarBytes, long totalBytes) {
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
        PausedSnapshot(int id, byte status, long sofarBytes, long totalBytes) {
            super(id, status, sofarBytes, totalBytes);
        }
    }
}
