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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * For storing and updating the {@link FileDownloadModel} to the filedownloader database, and also
 * maintain the database when FileDownloader-Process is launching automatically.
 */
class DefaultDatabaseImpl implements FileDownloadDatabase {

    private final SQLiteDatabase db;

    public final static String TABLE_NAME = "filedownloader";

    private final SparseArray<FileDownloadModel> downloaderModelMap = new SparseArray<>();

    public DefaultDatabaseImpl() {
        DefaultDatabaseOpenHelper openHelper = new DefaultDatabaseOpenHelper(FileDownloadHelper.getAppContext());

        db = openHelper.getWritableDatabase();

        refreshDataFromDB();
    }

    private void refreshDataFromDB() {
        long start = System.currentTimeMillis();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        List<Integer> dirtyList = new ArrayList<>();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (c.moveToNext()) {
                FileDownloadModel model = new FileDownloadModel();
                model.setId(c.getInt(c.getColumnIndex(FileDownloadModel.ID)));
                model.setUrl(c.getString(c.getColumnIndex(FileDownloadModel.URL)));
                model.setPath(c.getString(c.getColumnIndex(FileDownloadModel.PATH)),
                        c.getShort(c.getColumnIndex(FileDownloadModel.PATH_AS_DIRECTORY)) == 1);
                model.setStatus((byte) c.getShort(c.getColumnIndex(FileDownloadModel.STATUS)));
                model.setSoFar(c.getLong(c.getColumnIndex(FileDownloadModel.SOFAR)));
                model.setTotal(c.getLong(c.getColumnIndex(FileDownloadModel.TOTAL)));
                model.setErrMsg(c.getString(c.getColumnIndex(FileDownloadModel.ERR_MSG)));
                model.setETag(c.getString(c.getColumnIndex(FileDownloadModel.ETAG)));
                model.setFilename(c.getString(c.getColumnIndex(FileDownloadModel.FILENAME)));
                if (model.getStatus() == FileDownloadStatus.progress ||
                        model.getStatus() == FileDownloadStatus.connected ||
                        model.getStatus() == FileDownloadStatus.error ||
                        (model.getStatus() == FileDownloadStatus.pending && model.getSoFar() > 0)
                        ) {
                    // Ensure can be covered by RESUME FROM BREAKPOINT.
                    model.setStatus(FileDownloadStatus.paused);
                }

                final String targetFilePath = model.getTargetFilePath();
                if (targetFilePath == null) {
                    // no target file path, can't used to resume from breakpoint.
                    dirtyList.add(model.getId());
                    continue;
                }

                final File targetFile = new File(targetFilePath);

                // consider check in new thread, but SQLite lock | file lock aways effect, so sync
                if (model.getStatus() == FileDownloadStatus.paused &&
                        FileDownloadMgr.isBreakpointAvailable(model.getId(), model,
                                model.getPath(), null)) {
                    // can be reused in the old mechanism(no-temp-file).

                    final File tempFile = new File(model.getTempFilePath());

                    if (!tempFile.exists() && targetFile.exists()) {
                        final boolean successRename = targetFile.renameTo(tempFile);
                        if (FileDownloadLog.NEED_LOG) {
                            FileDownloadLog.d(this,
                                    "resume from the old no-temp-file architecture [%B], [%s]->[%s]",
                                    successRename, targetFile.getPath(), tempFile.getPath());

                        }
                    }
                }

                /**
                 * Remove {@code model} from DB if it can't used for judging whether the
                 * old-downloaded file is valid for reused & it can't used for resuming from
                 * BREAKPOINT, In other words, {@code model} is no use anymore for FileDownloader.
                 */
                if (model.getStatus() == FileDownloadStatus.pending && model.getSoFar() <= 0) {
                    // This model is redundant.
                    dirtyList.add(model.getId());
                } else if (!FileDownloadMgr.isBreakpointAvailable(model.getId(), model)) {
                    // It can't used to resuming from breakpoint.
                    dirtyList.add(model.getId());
                } else if (targetFile.exists()) {
                    // It has already completed downloading.
                    dirtyList.add(model.getId());
                } else {
                    downloaderModelMap.put(model.getId(), model);
                }

            }
        } finally {
            c.close();
            FileDownloadUtils.markConverted(FileDownloadHelper.getAppContext());

            // db
            if (dirtyList.size() > 0) {
                String args = TextUtils.join(", ", dirtyList);
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "delete %s", args);
                }
                //noinspection ThrowFromFinallyBlock
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
    public boolean remove(int id) {
        downloaderModelMap.remove(id);

        // db
        return db.delete(TABLE_NAME, FileDownloadModel.ID + " = ?", new String[]{String.valueOf(id)})
                != 0;
    }

    @Override
    public void clear() {
        downloaderModelMap.clear();

        db.delete(TABLE_NAME, null, null);
    }

    @Override
    public void updateConnected(FileDownloadModel model, long total, String etag, String filename) {
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

        if (model.isPathAsDirectory() &&
                model.getFilename() == null && filename != null) {
            model.setFilename(filename);

            cv.put(FileDownloadModel.FILENAME, filename);
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
