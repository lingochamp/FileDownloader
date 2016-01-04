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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.liulishuo.filedownloader.model.FileDownloadModel;


/**
 * Created by Jacksgong on 9/25/15.
 */
class FileDownloadDBOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "filedownloader.db";
    private static final int DATABASE_VERSION = 1;

    public FileDownloadDBOpenHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                FileDownloadDBHelper.TABLE_NAME +
                String.format(
                        "(" +
                                "%s INTEGER PRIMARY KEY, " + // id
                                "%s VARCHAR, " + //url
                                "%s VARCHAR, " + // path
                                "%s INTEGER, " + // callbackProgressTimes
                                "%s TINYINT, " + // status ,ps SQLite will auto change to integer.
                                "%s INTEGER, " + // so far
                                "%s INTEGER, " + // total
                                "%s VARCHAR, " + // err msg
                                "%s VARCHAR" + // e tag
                                ")",
                        FileDownloadModel.ID,
                        FileDownloadModel.URL,
                        FileDownloadModel.PATH,
                        FileDownloadModel.CALLBACK_PROGRESS_TIMES,
                        FileDownloadModel.STATUS,
                        FileDownloadModel.SOFAR,
                        FileDownloadModel.TOTAL,
                        FileDownloadModel.ERR_MSG,
                        FileDownloadModel.ETAG));

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
