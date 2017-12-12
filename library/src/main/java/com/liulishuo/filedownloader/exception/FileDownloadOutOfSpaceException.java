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
package com.liulishuo.filedownloader.exception;

import android.annotation.TargetApi;
import android.os.Build;

import com.liulishuo.filedownloader.download.DownloadStatusCallback;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.IOException;

/**
 * Throw this exception, when the downloading file is too large to store, in other words,
 * the free space is less than the length of the downloading file.
 * <p/>
 * When the resource is non-Chunked(normally), we will check the space and handle this problem
 * before fetch data from the input stream:
 * {@link FileDownloadUtils#createOutputStream(String)}
 * When the resource is chunked, we will handle this problem when the free space is not enough to
 * store the following chunk:
 * {@link DownloadStatusCallback#exFiltrate(Exception)}
 */
@SuppressWarnings("SameParameterValue")
public class FileDownloadOutOfSpaceException extends IOException {

    private long freeSpaceBytes, requiredSpaceBytes, breakpointBytes;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public FileDownloadOutOfSpaceException(long freeSpaceBytes, long requiredSpaceBytes,
                                           long breakpointBytes, Throwable cause) {
        super(FileDownloadUtils.formatString("The file is too large to store, breakpoint in bytes: "
                + " %d, required space in bytes: %d, but free space in bytes: "
                + "%d", breakpointBytes, requiredSpaceBytes, freeSpaceBytes), cause);

        init(freeSpaceBytes, requiredSpaceBytes, breakpointBytes);
    }

    public FileDownloadOutOfSpaceException(long freeSpaceBytes, long requiredSpaceBytes,
                                           long breakpointBytes) {
        super(FileDownloadUtils.formatString("The file is too large to store, breakpoint in bytes: "
                + " %d, required space in bytes: %d, but free space in bytes: "
                + "%d", breakpointBytes, requiredSpaceBytes, freeSpaceBytes));

        init(freeSpaceBytes, requiredSpaceBytes, breakpointBytes);

    }

    private void init(long freeSpaceBytes, long requiredSpaceBytes, long breakpointBytes) {
        this.freeSpaceBytes = freeSpaceBytes;
        this.requiredSpaceBytes = requiredSpaceBytes;
        this.breakpointBytes = breakpointBytes;
    }

    /**
     * @return The free space in bytes.
     */
    public long getFreeSpaceBytes() {
        return freeSpaceBytes;
    }

    /**
     * @return The required space in bytes use to store the datum will be fetched.
     */
    public long getRequiredSpaceBytes() {
        return requiredSpaceBytes;
    }

    /**
     * @return In normal Case: The value of breakpoint, which has already downloaded by past, if the
     * value is more than 0, it must be resuming from breakpoint.
     * For Chunked Resource(Streaming media):
     * The value would be the filled size.
     */
    public long getBreakpointBytes() {
        return breakpointBytes;
    }
}
