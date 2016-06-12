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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.SparseArray;

import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 9/24/15.
 * <p/>
 * For storing and updating the {@link FileDownloadModel} to DB.
 * And will maintain the DB automatically, when FileDownloader-Process is launching.
 */
class FileDownloadDBHelper implements IFileDownloadDBHelper {

    private final SQLiteDatabase db;

    public final static String TABLE_NAME = "filedownloader";

    private final SparseArray<FileDownloadModel> downloaderModelMap = new SparseArray<>();

    public FileDownloadDBHelper() {
        FileDownloadDBOpenHelper openHelper = new FileDownloadDBOpenHelper(FileDownloadHelper.getAppContext());

        db = openHelper.getWritableDatabase();

        refreshDataFromDB();
    }

    @Override
    public void refreshDataFromDB() {
        // TODO 优化，分段加载，数据多了以后
        // TODO 自动清理一个月前的数据
        long start = System.currentTimeMillis();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        List<Integer> dirtyList = new ArrayList<>();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (c.moveToNext()) {
                FileDownloadModel model = new FileDownloadModel();
                model.setId(c.getInt(c.getColumnIndex(FileDownloadModel.ID)));
                model.setUrl(c.getString(c.getColumnIndex(FileDownloadModel.URL)));
                model.setPath(c.getString(c.getColumnIndex(FileDownloadModel.PATH)));
//                model.setCallbackProgressTimes(c.getInt(c.getColumnIndex(FileDownloadModel.CALLBACK_PROGRESS_TIMES)));
                model.setStatus((byte) c.getShort(c.getColumnIndex(FileDownloadModel.STATUS)));
                model.setSoFar(c.getLong(c.getColumnIndex(FileDownloadModel.SOFAR)));
                model.setTotal(c.getLong(c.getColumnIndex(FileDownloadModel.TOTAL)));
                model.setErrMsg(c.getString(c.getColumnIndex(FileDownloadModel.ERR_MSG)));
                model.setETag(c.getString(c.getColumnIndex(FileDownloadModel.ETAG)));
                if (model.getStatus() == FileDownloadStatus.progress ||
                        model.getStatus() == FileDownloadStatus.connected ||
                        model.getStatus() == FileDownloadStatus.error) {
                    // 保证断点续传可以覆盖到
                    model.setStatus(FileDownloadStatus.paused);
                }

                // consider check in new thread, but SQLite lock | file lock aways effect, so sync
                if (model.getStatus() == FileDownloadStatus.pending) {
                    //脏数据 在数据库中是pending或是progress，说明是之前
                    dirtyList.add(model.getId());
                } else if (!FileDownloadMgr.checkReuse(model.getId(), model)
                        && !FileDownloadMgr.checkBreakpointAvailable(model.getId(), model)) {
                    // can't use to reuse old file & can't use to resume form break point
                    // = dirty
                    dirtyList.add(model.getId());
                }
                downloaderModelMap.put(model.getId(), model);
            }
        } finally {
            c.close();

            for (Integer integer : dirtyList) {
                downloaderModelMap.remove(integer);
            }

            // db
            if (dirtyList.size() > 0) {
                String args = TextUtils.join(", ", dirtyList);
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "delete %s", args);
                }
                db.execSQL(FileDownloadUtils.formatString("DELETE FROM %s WHERE %s IN (%s);",
                        TABLE_NAME, FileDownloadModel.ID, args));
            }

            // 566 data consumes about 140ms
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "refresh data %d , will delete: %d consume %d",
                        downloaderModelMap.size(), dirtyList.size(), System.currentTimeMillis() - start);
            }
        }

    }

    @Override
    public FileDownloadModel find(final int id) {
        return downloaderModelMap.get(id);
    }

    @Override
    public void insert(FileDownloadModel downloadModel) {
        downloaderModelMap.put(downloadModel.getId(), downloadModel);

        // db
        db.insert(TABLE_NAME, null, downloadModel.toContentValues());
    }

    @Override
    public void update(FileDownloadModel downloadModel) {
        if (downloadModel == null) {
            FileDownloadLog.w(this, "update but model == null!");
            return;
        }

        if (find(downloadModel.getId()) != null) {
            // 替换
            downloaderModelMap.remove(downloadModel.getId());
            downloaderModelMap.put(downloadModel.getId(), downloadModel);

            // db
            ContentValues cv = downloadModel.toContentValues();
            db.update(TABLE_NAME, cv, FileDownloadModel.ID + " = ? ", new String[]{String.valueOf(downloadModel.getId())});
        } else {
            insert(downloadModel);
        }
    }

    @Override
    public void update(List<FileDownloadModel> downloadModelList) {
        if (downloadModelList == null) {
            FileDownloadLog.w(this, "update a download list, but list == null!");
            return;
        }

        db.beginTransaction();

        try {
            for (FileDownloadModel model : downloadModelList) {
                if (find(model.getId()) != null) {
                    // replace
                    downloaderModelMap.remove(model.getId());
                    downloaderModelMap.put(model.getId(), model);

                    db.update(TABLE_NAME, model.toContentValues(), FileDownloadModel.ID + " = ? ",
                            new String[]{String.valueOf(model.getId())});
                } else {
                    // insert new one.
                    downloaderModelMap.put(model.getId(), model);

                    db.insert(TABLE_NAME, null, model.toContentValues());
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void remove(int id) {
        downloaderModelMap.remove(id);

        // db
        db.delete(TABLE_NAME, FileDownloadModel.ID + " = ?", new String[]{String.valueOf(id)});
    }

    @Override
    public void updateConnected(FileDownloadModel model, long total, String etag) {
        model.setStatus(FileDownloadStatus.connected);


        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.connected);

        final long oldTotal = model.getTotal();
        if (oldTotal != total) {
            model.setTotal(total);
            cv.put(FileDownloadModel.TOTAL, total);
        }

        final String oldEtag = model.getETag();
        if ((etag != null && !etag.equals(oldEtag)) ||
                (oldEtag != null && !oldEtag.equals(etag))) {
            model.setETag(etag);
            cv.put(FileDownloadModel.ETAG, etag);
        }

        update(model.getId(), cv);
    }

    @Override
    public void updateProgress(FileDownloadModel model, long soFar) {
        model.setStatus(FileDownloadStatus.progress);
        model.setSoFar(soFar);

        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.progress);
        cv.put(FileDownloadModel.SOFAR, soFar);
        update(model.getId(), cv);
    }

    @Override
    public void updateError(FileDownloadModel model, Throwable throwable, long sofar) {
        final String errMsg = throwable.toString();

        model.setStatus(FileDownloadStatus.error);
        model.setErrMsg(errMsg);
        model.setSoFar(sofar);

        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.ERR_MSG, errMsg);
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.error);
        cv.put(FileDownloadModel.SOFAR, sofar);
        update(model.getId(), cv);
    }

    @Override
    public void updateRetry(FileDownloadModel model, Throwable throwable) {
        final String errMsg = throwable.toString();

        model.setStatus(FileDownloadStatus.retry);
        model.setErrMsg(errMsg);

        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.ERR_MSG, errMsg);
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.retry);
        update(model.getId(), cv);
    }

    @Override
    public void updateComplete(FileDownloadModel model, final long total) {
        model.setStatus(FileDownloadStatus.completed);
        model.setSoFar(total);
        model.setTotal(total);

        //db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.completed);
        cv.put(FileDownloadModel.TOTAL, total);
        cv.put(FileDownloadModel.SOFAR, total);
        update(model.getId(), cv);
    }

    @Override
    public void updatePause(FileDownloadModel model, long sofar) {
        model.setStatus(FileDownloadStatus.paused);
        model.setSoFar(sofar);

        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.paused);
        cv.put(FileDownloadModel.SOFAR, sofar);
        update(model.getId(), cv);
    }

    @Override
    public void updatePending(FileDownloadModel model) {
        model.setStatus(FileDownloadStatus.pending);

        // db
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.pending);
        update(model.getId(), cv);
    }

    private void update(final int id, final ContentValues cv) {
        db.update(TABLE_NAME, cv, FileDownloadModel.ID + " = ? ", new String[]{String.valueOf(id)});
    }
}
