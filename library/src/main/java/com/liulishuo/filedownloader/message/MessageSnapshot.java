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
import android.os.Parcelable;

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * The message snapshot.
 */
public abstract class MessageSnapshot implements IMessageSnapshot, Parcelable {
    private final int id;
    protected boolean isLargeFile;

    MessageSnapshot(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Throwable getThrowable() {
        throw new NoFieldException("getThrowable", this);
    }

    @Override
    public int getRetryingTimes() {
        throw new NoFieldException("getRetryingTimes", this);
    }

    @Override
    public boolean isResuming() {
        throw new NoFieldException("isResuming", this);
    }

    @Override
    public String getEtag() {
        throw new NoFieldException("getEtag", this);
    }

    @Override
    public long getLargeSofarBytes() {
        throw new NoFieldException("getLargeSofarBytes", this);
    }

    @Override
    public long getLargeTotalBytes() {
        throw new NoFieldException("getLargeTotalBytes", this);
    }

    @Override
    public int getSmallSofarBytes() {
        throw new NoFieldException("getSmallSofarBytes", this);
    }

    @Override
    public int getSmallTotalBytes() {
        throw new NoFieldException("getSmallTotalBytes", this);
    }

    @Override
    public boolean isReusedDownloadedFile() {
        throw new NoFieldException("isReusedDownloadedFile", this);
    }

    @Override
    public String getFileName() {
        throw new NoFieldException("getFileName", this);
    }

    @Override
    public boolean isLargeFile() {
        return isLargeFile;
    }


    public interface IWarnMessageSnapshot {
        MessageSnapshot turnToPending();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isLargeFile ? 1 : 0));
        dest.writeByte(getStatus());
        // normal
        dest.writeInt(this.id);
    }

    MessageSnapshot(Parcel in) {
        this.id = in.readInt();
    }

    public static final Creator<MessageSnapshot> CREATOR = new Creator<MessageSnapshot>() {
        @Override
        public MessageSnapshot createFromParcel(Parcel source) {
            boolean largeFile = source.readByte() == 1;
            byte status = source.readByte();
            final MessageSnapshot snapshot;
            switch (status) {
                case FileDownloadStatus.pending:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.PendingMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.PendingMessageSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.started:
                    snapshot = new StartedMessageSnapshot(source);
                    break;
                case FileDownloadStatus.connected:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.ConnectedMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.ConnectedMessageSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.progress:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.ProgressMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.ProgressMessageSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.retry:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.RetryMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.RetryMessageSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.error:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.ErrorMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.ErrorMessageSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.completed:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.CompletedSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.CompletedSnapshot(source);
                    }
                    break;
                case FileDownloadStatus.warn:
                    if (largeFile) {
                        snapshot = new LargeMessageSnapshot.WarnMessageSnapshot(source);
                    } else {
                        snapshot = new SmallMessageSnapshot.WarnMessageSnapshot(source);
                    }
                    break;
                default:
                    snapshot = null;
            }

            if (snapshot != null) {
                snapshot.isLargeFile = largeFile;
            } else {
                throw new IllegalStateException("Can't restore the snapshot because unknown " +
                        "status: " + status);
            }

            return snapshot;
        }

        @Override
        public MessageSnapshot[] newArray(int size) {
            return new MessageSnapshot[size];
        }
    };

    public static class NoFieldException extends IllegalStateException {
        NoFieldException(String methodName, MessageSnapshot snapshot) {
            super(FileDownloadUtils.formatString("There isn't a field for '%s' in this message %d %d %s",
                    methodName, snapshot.getId(), snapshot.getStatus(), snapshot.getClass().getName()));
        }
    }

    // Started Snapshot
    public static class StartedMessageSnapshot extends MessageSnapshot {

        StartedMessageSnapshot(int id) {
            super(id);
        }

        StartedMessageSnapshot(Parcel in) {
            super(in);
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.started;
        }
    }
}
