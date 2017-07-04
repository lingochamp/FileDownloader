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

package com.liulishuo.filedownloader.services;

import android.content.Intent;

import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

/**
 * The handler broadcast from filedownloader.
 */
public class FileDownloadBroadcastHandler {
    public final static String ACTION_COMPLETED = "filedownloader.intent.action.completed";
    public final static String KEY_MODEL = "model";

    /**
     * Parse the {@code intent} from the filedownloader broadcast.
     *
     * @param intent the intent from the broadcast.
     * @return the file download model.
     */
    public static FileDownloadModel parseIntent(Intent intent) {
        if (!ACTION_COMPLETED.equals(intent.getAction())) {
            throw new IllegalArgumentException(FileDownloadUtils.
                    formatString("can't recognize the intent with action %s, on the current" +
                            " version we only support action [%s]", intent.getAction(), ACTION_COMPLETED));
        }

        return intent.getParcelableExtra(KEY_MODEL);
    }

    public static void sendCompletedBroadcast(FileDownloadModel model) {
        if (model == null) throw new IllegalArgumentException();
        if (model.getStatus() != FileDownloadStatus.completed) throw new IllegalStateException();

        final Intent intent = new Intent(ACTION_COMPLETED);
        intent.putExtra(KEY_MODEL, model);

        FileDownloadHelper.getAppContext().sendBroadcast(intent);
    }
}
