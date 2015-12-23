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

package com.liulishuo.filedownloader.model;

/**
 * Created by Jacksgong on 11/26/15.
 *
 * @see com.liulishuo.filedownloader.IFileDownloadMessage
 */
public interface FileDownloadStatus {
    int pending = 1;
    int connected = 2;
    int progress = 3;
    int blockComplete = 4;
    int error = -1;
    int paused = -2;
    int completed = -3;
    int warn = -4;

    int MAX_INT = 4;
    int MIN_INT = -4;
    int INVALID_STATUS = 0;
}
