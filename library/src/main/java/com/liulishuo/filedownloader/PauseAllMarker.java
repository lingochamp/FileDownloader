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

import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.io.File;
import java.io.IOException;

public class PauseAllMarker {

    private static final String MAKER_FILE_NAME = ".filedownloader_pause_all_marker.b";
    private static File markerFile;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createMarker(Context context) {
        final File markerFile = markerFile(context);
        if (!markerFile.getParentFile().exists()) markerFile.getParentFile().mkdirs();
        if (markerFile.exists()) {
            FileDownloadLog.w(PauseAllMarker.class, "marker file " + markerFile.getAbsolutePath() + " exists");
            return;
        }
        try {
            boolean success = markerFile.createNewFile();
            FileDownloadLog.d(PauseAllMarker.class, "create marker file" + markerFile.getAbsolutePath() + " " + success);
        } catch (IOException e) {
            FileDownloadLog.e(PauseAllMarker.class, "create marker file failed", e);
        }
    }

    private static File markerFile(Context context) {
        if (markerFile == null) {
            markerFile = new File(context.getCacheDir() + File.separator + MAKER_FILE_NAME);
        }
        return markerFile;
    }

    public static boolean isMarked(Context context) {
        return markerFile(context).exists();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void clearMarker(Context context) {
        final File file = markerFile(context);
        if (file.exists()) {
            FileDownloadLog.d(PauseAllMarker.class, "delete marker file " + file.delete());
        } else {
            FileDownloadLog.w(PauseAllMarker.class, "marker file doesn't exist");
        }
    }
}
