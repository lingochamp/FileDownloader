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
import java.io.RandomAccessFile;

/**
 * The FileDownloadOutputStream implemented using {@link RandomAccessFile}.
 */

public class FileDownloadRandomAccessFile implements FileDownloadOutputStream {
    private final RandomAccessFile mAccessFile;

    FileDownloadRandomAccessFile(File file) throws FileNotFoundException {
        mAccessFile = new RandomAccessFile(file, "rw");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        mAccessFile.write(b, off, len);
    }

    @Override
    public void sync() throws IOException {
        mAccessFile.getFD().sync();
    }

    @Override
    public void close() throws IOException {
        mAccessFile.close();
    }

    @Override
    public void seek(long offset) throws IOException {
        mAccessFile.seek(offset);
    }

    @Override
    public void setLength(long totalBytes) throws IOException {
        mAccessFile.setLength(totalBytes);
    }

    public static class Creator implements FileDownloadHelper.OutputStreamCreator {

        @Override
        public FileDownloadOutputStream create(File file) throws FileNotFoundException {
            return new FileDownloadRandomAccessFile(file);
        }

        @Override
        public boolean supportSeek() {
            return true;
        }
    }
}
