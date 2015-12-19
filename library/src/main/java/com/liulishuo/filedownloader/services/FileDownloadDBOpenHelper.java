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
                                "%s TINYINT, " + // needNotification 0(false)
                                "%s VARCHAR, " + // title
                                "%s VARCHAR, " + // desc
                                "%s INTEGER, " + // progressCallbackTimes
                                "%s SMALLINT, " + // status
                                "%s INTEGER, " + // so far
                                "%s INTEGER, " + // total
                                "%s VARCHAR, " + // err msg
                                "%s VARCHAR" + // e tag
                                ")",
                        FileDownloadModel.ID,
                        FileDownloadModel.URL,
                        FileDownloadModel.PATH,
                        FileDownloadModel.NEED_NOTIFICATION,
                        FileDownloadModel.TITLE,
                        FileDownloadModel.DESC,
                        FileDownloadModel.PROGRESS_CALLBACK_TIMES,
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
