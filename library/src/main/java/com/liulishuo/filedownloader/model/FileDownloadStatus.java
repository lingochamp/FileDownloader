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
    // [-2^7, 2^7 -1]
    byte pending = 1;
    byte connected = 2;
    byte progress = 3;
    byte blockComplete = 4;
    byte retry = 5;
    byte error = -1;
    byte paused = -2;
    byte completed = -3;
    byte warn = -4;

    byte MAX_INT = 5;
    byte MIN_INT = -4;
    byte INVALID_STATUS = 0;
}
