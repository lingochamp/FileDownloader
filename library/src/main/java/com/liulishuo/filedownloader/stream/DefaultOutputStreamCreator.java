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

import android.net.Uri;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadHelper.OutputStreamCreator;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The default output stream creator.
 */

public class DefaultOutputStreamCreator implements OutputStreamCreator {

    @Override
    public FileDownloadOutputStream create(File file) throws FileNotFoundException {
        return new FileDownloadRandomAccessFile(file);
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
