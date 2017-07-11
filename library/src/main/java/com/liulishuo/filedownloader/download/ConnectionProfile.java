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

import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * The connection profile for {@link ConnectTask}.
 */

public class ConnectionProfile {

    final long startOffset;
    final long currentOffset;
    final long endOffset;
    final long contentLength;

    ConnectionProfile(long startOffset, long currentOffset, long endOffset, long contentLength) {
        this.startOffset = startOffset;
        this.currentOffset = currentOffset;
        this.endOffset = endOffset;
        this.contentLength = contentLength;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("range[%d, %d) current offset[%d]", startOffset, endOffset, currentOffset);
    }
}
