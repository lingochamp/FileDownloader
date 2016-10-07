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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The FileDownloadOutputStream implemented using {@link BufferedOutputStream}.
 */

public class FileDownloadBufferedOutputStream implements FileDownloadOutputStream {

    private final BufferedOutputStream mStream;

    FileDownloadBufferedOutputStream(File file) throws FileNotFoundException {
        mStream = new BufferedOutputStream(new FileOutputStream(file, true));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        mStream.write(b, off, len);
    }

    @Override
    public void sync() throws IOException {
        mStream.flush();
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }

    @Override
    public void seek(long offset) throws IOException, IllegalAccessException {
        throw new IllegalAccessException("Can't support 'seek' in BufferedOutputStream.");
    }

    @Override
    public void setLength(long totalBytes) throws IOException, IllegalAccessException {
        throw new IllegalAccessException("Can't support 'setLength' in BufferedOutputStream.");
    }

    public static class Creator implements FileDownloadHelper.OutputStreamCreator {

        @Override
        public FileDownloadOutputStream create(File file) throws FileNotFoundException {
            return new FileDownloadBufferedOutputStream(file);
        }

        @Override
        public boolean supportSeek() {
            return false;
        }
    }
}
