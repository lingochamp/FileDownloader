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
 * Created by Jacksgong on 5/1/16.
 * <p/>
 * A snapshot for wrapping a changed downloading statuses.
 */
public class MessageSnapshot implements IMessageSnapshot, Parcelable {
    private final int id;
    protected byte status;
    protected boolean isLargeFile;

    MessageSnapshot(int id, byte status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public byte getStatus() {
        return status;
    }

    @Override
    public Throwable getThrowable() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'exception' in this message %d %d", id, status));
    }

    @Override
    public int getRetryingTimes() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'retrying times' in this message %d %d", id,
                        status));
    }

    @Override
    public boolean isResuming() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'is resuming' in this message %d %d", id, status));
    }

    @Override
    public String getEtag() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'etag' in this message %d %d", id, status));
    }

    @Override
    public long getLargeSofarBytes() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'large sofar bytes' in this message %d %d",
                        id, status));
    }

    @Override
    public long getLargeTotalBytes() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'large total bytes' in this message %d %d",
                        id, status));
    }

    @Override
    public int getSmallSofarBytes() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'small sofar bytes' in this message %d %d",
                        id, status));
    }

    @Override
    public int getSmallTotalBytes() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No 'small total bytes' in this message %d %d",
                        id, status));
    }

    @Override
    public boolean isReusedDownloadedFile() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No reused downloaded file' in this message %d %d",
                        id, status));
    }

    @Override
    public String getFileName() {
        throw new IllegalStateException(
                FileDownloadUtils.formatString("No filename in this message %d %d",
                        id, status));
    }

    @Override
    public boolean isLargeFile() {
        return isLargeFile;
    }


    public interface IWarnMessageSnapshot {
        void turnToPending();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isLargeFile ? 1 : 0));
        dest.writeByte(this.status);
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
                    snapshot = new MessageSnapshot(source);
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
                    snapshot = new MessageSnapshot(source);
            }

            snapshot.isLargeFile = largeFile;
            snapshot.status = status;
            return snapshot;
        }

        @Override
        public MessageSnapshot[] newArray(int size) {
            return new MessageSnapshot[size];
        }
    };
}
