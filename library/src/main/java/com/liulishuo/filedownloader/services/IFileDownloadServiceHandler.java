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
package com.liulishuo.filedownloader.services;

import android.content.Intent;
import android.os.IBinder;

import com.liulishuo.filedownloader.services.FDServiceSharedHandler.FileDownloadServiceSharedConnection;

/**
 * The handler for {@link FileDownloadService}.
 *
 * @see FileDownloadManager
 */
@SuppressWarnings("UnusedParameters")
interface IFileDownloadServiceHandler {
    /**
     * Will used to handling the onConnected in {@link FileDownloadServiceSharedConnection}.
     * <p/>
     * Called by the system every time a client explicitly starts the service by calling
     * {@link android.content.Context#startService}.
     */
    void onStartCommand(Intent intent, int flags, int startId);

    /**
     * Will establish the connection with binder in {@link FDServiceSeparateHandler}
     *
     * @return Return the communication channel to the service. Nullable.
     */
    IBinder onBind(Intent intent);

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     *
     * @see FileDownloadServiceSharedConnection
     * @see FDServiceSeparateHandler
     */
    void onDestroy();
}
