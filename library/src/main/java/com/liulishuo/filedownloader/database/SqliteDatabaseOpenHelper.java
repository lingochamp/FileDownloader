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

package com.liulishuo.filedownloader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadModel;


/**
 * The default opener of the filedownloader database helper.
 */
public class SqliteDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "filedownloader.db";
    private static final int DATABASE_VERSION = 4;

    public SqliteDatabaseOpenHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            db.enableWriteAheadLogging();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + SqliteDatabaseImpl.TABLE_NAME + "( "
                + FileDownloadModel.ID + " INTEGER PRIMARY KEY, "  // id
                + FileDownloadModel.URL + " VARCHAR, "  // url
                + FileDownloadModel.PATH + " VARCHAR, "  // path
                + FileDownloadModel.STATUS + " TINYINT(7), "  // status
                + FileDownloadModel.SOFAR + " INTEGER, " // so far bytes
                + FileDownloadModel.TOTAL + " INTEGER, " // total bytes
                + FileDownloadModel.ERR_MSG + " VARCHAR, "  // error message
                + FileDownloadModel.ETAG + " VARCHAR, " // etag
                + FileDownloadModel.PATH_AS_DIRECTORY + " TINYINT(1) DEFAULT 0, "//path as directory
                + FileDownloadModel.FILENAME + " VARCHAR, " // path as directory
                + FileDownloadModel.CONNECTION_COUNT + " INTEGER DEFAULT 1" // connection count
                + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + SqliteDatabaseImpl.CONNECTION_TABLE_NAME + "( "
                + ConnectionModel.ID + " INTEGER, "
                + ConnectionModel.INDEX + " INTEGER, "
                + ConnectionModel.START_OFFSET + " INTEGER, "
                + ConnectionModel.CURRENT_OFFSET + " INTEGER, "
                + ConnectionModel.END_OFFSET + " INTEGER, "
                + "PRIMARY KEY ( " + ConnectionModel.ID + ", " + ConnectionModel.INDEX + " )"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {
            String addAsDirectoryColumn = "ALTER TABLE " + SqliteDatabaseImpl.TABLE_NAME
                    + " ADD COLUMN " + FileDownloadModel.PATH_AS_DIRECTORY
                    + " TINYINT(1) DEFAULT 0";
            db.execSQL(addAsDirectoryColumn);

            String addFilenameColumn = "ALTER TABLE " + SqliteDatabaseImpl.TABLE_NAME
                    + " ADD COLUMN " + FileDownloadModel.FILENAME
                    + " VARCHAR";
            db.execSQL(addFilenameColumn);
        }

        if (oldVersion < 3) {
            final String addConnectionCount = "ALTER TABLE " + SqliteDatabaseImpl.TABLE_NAME
                    + " ADD COLUMN " + FileDownloadModel.CONNECTION_COUNT
                    + " INTEGER DEFAULT 1";
            db.execSQL(addConnectionCount);

            db.execSQL("CREATE TABLE IF NOT EXISTS "
                    + SqliteDatabaseImpl.CONNECTION_TABLE_NAME + "( "
                    + ConnectionModel.ID + " INTEGER, "
                    + ConnectionModel.INDEX + " INTEGER, "
                    + ConnectionModel.START_OFFSET + " INTEGER, "
                    + ConnectionModel.CURRENT_OFFSET + " INTEGER, "
                    + ConnectionModel.END_OFFSET + " INTEGER, "
                    + "PRIMARY KEY ( " + ConnectionModel.ID + ", " + ConnectionModel.INDEX  + " )"
                    + ")");
        }

        if (oldVersion < 4) {
            ContentValues values = new ContentValues();
            values.put(ConnectionModel.END_OFFSET, -1);
            String whereClause = ConnectionModel.END_OFFSET + " = ? AND "
                    + ConnectionModel.START_OFFSET + " > ?";
            db.update(SqliteDatabaseImpl.CONNECTION_TABLE_NAME, values,
                    whereClause, new String[]{"0", "0"});
        }
    }

    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.delete(SqliteDatabaseImpl.TABLE_NAME, null, null);
        db.delete(SqliteDatabaseImpl.CONNECTION_TABLE_NAME, null, null);
    }
}
