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

package com.liulishuo.filedownloader.stream;

import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okio.Buffer;
import okio.Okio;
import okio.Sink;

/**
 * The FileDownloadOutputStream implemented using {@link Okio}.
 */

public class FileDownloadOkio implements FileDownloadOutputStream {
    private Sink mSink;
    private Buffer mBuffer;

    FileDownloadOkio(File file) throws FileNotFoundException {
        mSink = Okio.appendingSink(file);
        mBuffer = new Buffer();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        mBuffer.write(b, off, len);
        mSink.write(mBuffer, len);
    }

    @Override
    public void sync() throws IOException {
        mSink.flush();
    }

    @Override
    public void close() throws IOException {
        mBuffer.close();
        mSink.close();
    }

    @Override
    public void seek(long offset) throws IOException, IllegalAccessException {
        throw new IllegalAccessException("Can't support 'seek' in Okio.");
    }

    @Override
    public void setLength(long newLength) throws IOException, IllegalAccessException {
        throw new IllegalAccessException("Can't support 'setLength' in Okio.");
    }

    public static class Creator implements FileDownloadHelper.OutputStreamCreator {

        @Override
        public FileDownloadOutputStream create(File file) throws FileNotFoundException {
            return new FileDownloadOkio(file);
        }

        @Override
        public boolean supportSeek() {
            return false;
        }
    }
}
