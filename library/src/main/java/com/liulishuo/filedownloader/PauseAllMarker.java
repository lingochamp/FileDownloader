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

package com.liulishuo.filedownloader;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;

import com.liulishuo.filedownloader.i.IFileDownloadIPCService;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.io.File;
import java.io.IOException;

public class PauseAllMarker implements Handler.Callback {

    private static final String MAKER_FILE_NAME = ".filedownloader_pause_all_marker.b";
    private static File markerFile;
    private static final Long PAUSE_ALL_CHECKER_PERIOD = 1000L; // 1 second
    private static final int PAUSE_ALL_CHECKER_WHAT = 0;
    private HandlerThread pauseAllChecker;
    private Handler pauseAllHandler;
    private final IFileDownloadIPCService serviceHandler;

    public PauseAllMarker(IFileDownloadIPCService serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createMarker() {
        final File markerFile = markerFile();
        if (!markerFile.getParentFile().exists()) markerFile.getParentFile().mkdirs();
        if (markerFile.exists()) {
            FileDownloadLog.w(PauseAllMarker.class, "marker file " + markerFile.getAbsolutePath()
                    + " exists");
            return;
        }
        try {
            boolean success = markerFile.createNewFile();
            FileDownloadLog.d(PauseAllMarker.class, "create marker file"
                    + markerFile.getAbsolutePath() + " " + success);
        } catch (IOException e) {
            FileDownloadLog.e(PauseAllMarker.class, "create marker file failed", e);
        }
    }

    private static File markerFile() {
        if (markerFile == null) {
            final Context context = FileDownloadHelper.getAppContext();
            markerFile = new File(context.getCacheDir() + File.separator + MAKER_FILE_NAME);
        }
        return markerFile;
    }

    private static boolean isMarked() {
        return markerFile().exists();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void clearMarker() {
        final File file = markerFile();
        if (file.exists()) {
            FileDownloadLog.d(PauseAllMarker.class, "delete marker file " + file.delete());
        }
    }

    public void startPauseAllLooperCheck() {
        pauseAllChecker = new HandlerThread("PauseAllChecker");
        pauseAllChecker.start();
        pauseAllHandler = new Handler(pauseAllChecker.getLooper(), this);
        pauseAllHandler.sendEmptyMessageDelayed(PAUSE_ALL_CHECKER_WHAT, PAUSE_ALL_CHECKER_PERIOD);
    }

    public void stopPauseAllLooperCheck() {
        pauseAllHandler.removeMessages(PAUSE_ALL_CHECKER_WHAT);
        pauseAllChecker.quit();
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (PauseAllMarker.isMarked()) {
            try {
                serviceHandler.pauseAllTasks();
            } catch (RemoteException e) {
                FileDownloadLog.e(this, e, "pause all failed");
            } finally {
                PauseAllMarker.clearMarker();
            }
        }
        pauseAllHandler.sendEmptyMessageDelayed(0, PAUSE_ALL_CHECKER_PERIOD);
        return true;
    }
}
