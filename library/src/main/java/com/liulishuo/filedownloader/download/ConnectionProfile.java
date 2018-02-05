/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.download;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.net.ProtocolException;

/**
 * The connection profile for {@link ConnectTask}.
 */
public class ConnectionProfile {

    static final int RANGE_INFINITE = -1;

    final long startOffset;
    final long currentOffset;
    final long endOffset;
    final long contentLength;

    private final boolean isForceNoRange;

    private final boolean isTrialConnect;

    /**
     * This construct is just for build trial connection profile.
     */
    private ConnectionProfile() {
        this.startOffset = 0;
        this.currentOffset = 0;
        this.endOffset = 0;
        this.contentLength = 0;

        this.isForceNoRange = false;
        this.isTrialConnect = true;
    }

    private ConnectionProfile(long startOffset, long currentOffset, long endOffset,
                              long contentLength) {
        this(startOffset, currentOffset, endOffset, contentLength, false);
    }

    private ConnectionProfile(long startOffset, long currentOffset, long endOffset,
                              long contentLength,
                              boolean isForceNoRange) {
        if ((startOffset != 0 || endOffset != 0) && isForceNoRange) {
            throw new IllegalArgumentException();
        }

        this.startOffset = startOffset;
        this.currentOffset = currentOffset;
        this.endOffset = endOffset;
        this.contentLength = contentLength;
        this.isForceNoRange = isForceNoRange;
        this.isTrialConnect = false;
    }

    public void processProfile(FileDownloadConnection connection) throws ProtocolException {
        if (isForceNoRange) return;

        if (isTrialConnect && FileDownloadProperties.getImpl().trialConnectionHeadMethod) {
            connection.setRequestMethod("HEAD");
        }

        final String range;
        if (endOffset == RANGE_INFINITE) {
            range = FileDownloadUtils.formatString("bytes=%d-", currentOffset);
        } else {
            range = FileDownloadUtils
                    .formatString("bytes=%d-%d", currentOffset, endOffset);
        }
        connection.addHeader("Range", range);
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("range[%d, %d) current offset[%d]",
                startOffset, endOffset, currentOffset);
    }

    public static class ConnectionProfileBuild {
        public static ConnectionProfile buildTrialConnectionProfile() {
            return new ConnectionProfile();
        }

        public static ConnectionProfile buildTrialConnectionProfileNoRange() {
            return new ConnectionProfile(0, 0, 0, 0, true);
        }

        public static ConnectionProfile buildBeginToEndConnectionProfile(long contentLength) {
            return new ConnectionProfile(0, 0, RANGE_INFINITE, contentLength);
        }

        public static ConnectionProfile buildToEndConnectionProfile(long startOffset,
                                                                    long currentOffset,
                                                                    long contentLength) {
            return new ConnectionProfile(startOffset, currentOffset, RANGE_INFINITE, contentLength);
        }

        public static ConnectionProfile buildConnectionProfile(long startOffset,
                                                               long currentOffset,
                                                               long endOffset,
                                                               long contentLength) {
            return new ConnectionProfile(startOffset, currentOffset, endOffset, contentLength);
        }
    }
}
