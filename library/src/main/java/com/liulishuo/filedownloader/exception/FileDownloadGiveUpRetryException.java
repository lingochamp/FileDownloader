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

import com.liulishuo.filedownloader.model.FileDownloadModel;

/**
 * Created by Jacksgong on 4/4/16.
 * <p/>
 * Throw this exception, when can't know the size of the download file, and its Transfer-Encoding
 * is not Chunked either.
 * <p/>
 * And With this exception, will ignore all retry-chances.
 *
 * @see com.liulishuo.filedownloader.services.FileDownloadRunnable#loop(FileDownloadModel)
 */
public class FileDownloadGiveUpRetryException extends RuntimeException {
    public FileDownloadGiveUpRetryException(final String detailMessage) {
        super(detailMessage);
    }
}