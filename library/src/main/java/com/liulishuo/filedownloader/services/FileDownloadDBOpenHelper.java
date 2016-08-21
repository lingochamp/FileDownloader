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
 * The filedownloader database, what is used for storing the {@link FileDownloadModel}.
 * <p/>
 * The filedownloader database is used for judging whether the task can resume from the breakpoint.
 * <p>
 * The data of task can store in this database must be in downloading processing or doesn't finished,
 * if the task has already finished, its data is no use of resuming from the breakpoint, so we will
 * remove it from the database when the downloader service is launching automatically.
 *
 * @see FileDownloadDBHelper
 * @see FileDownloadMgr#isBreakpointAvailable(int, FileDownloadModel)
 */
class FileDownloadDBOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "filedownloader.db";
    private static final int DATABASE_VERSION = 2;

    public FileDownloadDBOpenHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                FileDownloadDBHelper.TABLE_NAME + "( " +
                FileDownloadModel.ID + " INTEGER PRIMARY KEY, " + // id
                FileDownloadModel.URL + " VARCHAR, " + // url
                FileDownloadModel.PATH + " VARCHAR, " + // path
                FileDownloadModel.STATUS + " TINYINT(7), " + // status ,ps SQLite will auto change to integer.
                FileDownloadModel.SOFAR + " INTEGER, " +// so far
                FileDownloadModel.TOTAL + " INTEGER, " +// total
                FileDownloadModel.ERR_MSG + " VARCHAR, " + // error message
                FileDownloadModel.ETAG + " VARCHAR, " +// e tag
                FileDownloadModel.PATH_AS_DIRECTORY + " TINYINT(1) DEFAULT 0, " +// path as directory
                FileDownloadModel.FILENAME + " VARCHAR" +// path as directory
                ")");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            String addAsDirectoryColumn = "ALTER TABLE " + FileDownloadDBHelper.TABLE_NAME +
                    " ADD COLUMN " + FileDownloadModel.PATH_AS_DIRECTORY +
                    " TINYINT(1) DEFAULT 0";
            db.execSQL(addAsDirectoryColumn);

            String addFilenameColumn = "ALTER TABLE " + FileDownloadDBHelper.TABLE_NAME +
                    " ADD COLUMN " + FileDownloadModel.FILENAME +
                    " VARCHAR";
            db.execSQL(addFilenameColumn);
        }
    }
}
