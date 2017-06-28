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

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * The output stream for uri.
 */

public class FileDownloadUriOutputStream implements FileDownloadOutputStream {

    private FileChannel mFileChannel;
    private ParcelFileDescriptor mPdf;

    public FileDownloadUriOutputStream(Uri uri, Context context) throws FileNotFoundException {
        mPdf = context.getContentResolver().openFileDescriptor(uri, "rw");
        if (mPdf == null) throw new IllegalArgumentException();

        final FileOutputStream fos = new FileOutputStream(mPdf.getFileDescriptor());
        mFileChannel = fos.getChannel();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        mFileChannel.write(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public void sync() throws IOException {
        mPdf.getFileDescriptor().sync();
    }

    @Override
    public void close() throws IOException {
        mFileChannel.close();
    }

    @Override
    public void seek(long offset) throws IOException, IllegalAccessException {
        mFileChannel.position(offset);
    }

    @Override
    public void setLength(long newLength) throws IOException, IllegalAccessException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.ftruncate(mPdf.getFileDescriptor(), newLength);
            } catch (ErrnoException e) {
                FileDownloadLog.w(this,
                        FileDownloadUtils.
                                formatString("it can't pre-allocate length(%d) on the sdk version(%d)," +
                                        " because of %s", newLength, Build.VERSION.SDK_INT, e));
                e.printStackTrace();
            }
        } else {
            FileDownloadLog.w(this,
                    FileDownloadUtils.
                            formatString("it can't pre-allocate length(%d) on the sdk version(%d)",
                                    newLength, Build.VERSION.SDK_INT));
        }
    }

    public static class Creator implements FileDownloadHelper.OutputStreamCreator {

        @Override
        public FileDownloadOutputStream create(File file) throws FileNotFoundException {
            return new FileDownloadUriOutputStream(Uri.fromFile(file), FileDownloadHelper.getAppContext());
        }

        @Override
        public FileDownloadOutputStream create(Uri uri) throws FileNotFoundException {
            return new FileDownloadUriOutputStream(uri, FileDownloadHelper.getAppContext());
        }

        @Override
        public boolean supportSeek() {
            return true;
        }
    }
}
