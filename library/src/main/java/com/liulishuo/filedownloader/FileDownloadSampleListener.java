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

package com.liulishuo.filedownloader;

/**
 * Simplify the {@link FileDownloadListener}.
 */
public class FileDownloadSampleListener extends FileDownloadListener {

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void blockComplete(BaseDownloadTask task) {

    }

    @Override
    protected void completed(BaseDownloadTask task) {

    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {

    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }
}
