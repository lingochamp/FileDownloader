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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.SparseArray;

import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persist data to SQLite database.
 * <p>
 * You can valid this database implementation through:
 * <p>
 * class MyApplication extends Application {
 *     ...
 *     public void onCreate() {
 *         ...
 *         FileDownloader.setupOnApplicationOnCreate(this)
 *             .database(SqliteDatabaseImpl.createMaker())
 *             ...
 *             .commit();
 *         ...
 *     }
 *     ...
 * }
 */
public class SqliteDatabaseImpl implements FileDownloadDatabase {

    private final SQLiteDatabase db;

    public static final String TABLE_NAME = "filedownloader";
    public static final String CONNECTION_TABLE_NAME = "filedownloaderConnection";

    public static Maker createMaker() {
        return new Maker();
    }

    public SqliteDatabaseImpl() {
        SqliteDatabaseOpenHelper openHelper = new SqliteDatabaseOpenHelper(
                FileDownloadHelper.getAppContext());

        db = openHelper.getWritableDatabase();
    }

    @Override public void onTaskStart(int id) {
    }

    @Override
    public FileDownloadModel find(final int id) {
        Cursor c = null;
        try {
            c = db.rawQuery(FileDownloadUtils.formatString("SELECT * FROM %s WHERE %s = ?",
                    TABLE_NAME, FileDownloadModel.ID), new String[]{Integer.toString(id)});

            if (c.moveToNext()) return createFromCursor(c);

        } finally {
            if (c != null) c.close();
        }

        return null;
    }

    @Override
    public List<ConnectionModel> findConnectionModel(int id) {
        final List<ConnectionModel> resultList = new ArrayList<>();

        Cursor c = null;
        try {
            c = db.rawQuery(FileDownloadUtils.formatString("SELECT * FROM %s WHERE %s = ?",
                    CONNECTION_TABLE_NAME, ConnectionModel.ID), new String[]{Integer.toString(id)});

            while (c.moveToNext()) {
                final ConnectionModel model = new ConnectionModel();
                model.setId(id);
                model.setIndex(c.getInt(c.getColumnIndex(ConnectionModel.INDEX)));
                model.setStartOffset(c.getLong(c.getColumnIndex(ConnectionModel.START_OFFSET)));
                model.setCurrentOffset(c.getLong(c.getColumnIndex(ConnectionModel.CURRENT_OFFSET)));
                model.setEndOffset(c.getLong(c.getColumnIndex(ConnectionModel.END_OFFSET)));

                resultList.add(model);
            }
        } finally {
            if (c != null) c.close();
        }

        return resultList;
    }

    @Override
    public void removeConnections(int id) {
        db.execSQL("DELETE FROM " + CONNECTION_TABLE_NAME + " WHERE "
                + ConnectionModel.ID + " = " + id);
    }

    @Override
    public void insertConnectionModel(ConnectionModel model) {
        db.insert(CONNECTION_TABLE_NAME, null, model.toContentValues());
    }

    @Override
    public void updateConnectionModel(int id, int index, long currentOffset) {
        final ContentValues values = new ContentValues();
        values.put(ConnectionModel.CURRENT_OFFSET, currentOffset);
        db.update(CONNECTION_TABLE_NAME, values,
                ConnectionModel.ID + " = ? AND " + ConnectionModel.INDEX + " = ?",
                new String[]{Integer.toString(id), Integer.toString(index)});
    }

    @Override
    public void updateConnectionCount(int id, int count) {
        ContentValues values = new ContentValues();
        values.put(FileDownloadModel.CONNECTION_COUNT, count);
        db.update(TABLE_NAME, values,
                FileDownloadModel.ID + " = ? ", new String[]{Integer.toString(id)});
    }

    @Override
    public void insert(FileDownloadModel downloadModel) {
        db.insert(TABLE_NAME, null, downloadModel.toContentValues());
    }

    @Override
    public void update(FileDownloadModel downloadModel) {
        if (downloadModel == null) {
            FileDownloadLog.w(this, "update but model == null!");
            return;
        }

        if (find(downloadModel.getId()) != null) {
            // db
            ContentValues cv = downloadModel.toContentValues();
            db.update(TABLE_NAME, cv, FileDownloadModel.ID + " = ? ",
                    new String[]{String.valueOf(downloadModel.getId())});
        } else {
            insert(downloadModel);
        }
    }

    @Override
    public boolean remove(int id) {
        return db
                .delete(TABLE_NAME, FileDownloadModel.ID + " = ?", new String[]{String.valueOf(id)})
                != 0;
    }

    @Override
    public void clear() {
        db.delete(TABLE_NAME, null, null);
        db.delete(CONNECTION_TABLE_NAME, null, null);
    }

    @Override
    public void updateOldEtagOverdue(int id, String newEtag, long sofar, long total,
                                     int connectionCount) {
        ContentValues values = new ContentValues();
        values.put(FileDownloadModel.SOFAR, sofar);
        values.put(FileDownloadModel.TOTAL, total);
        values.put(FileDownloadModel.ETAG, newEtag);
        values.put(FileDownloadModel.CONNECTION_COUNT, connectionCount);

        update(id, values);

    }

    @Override
    public void updateConnected(int id, long total, String etag, String filename) {
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.connected);
        cv.put(FileDownloadModel.TOTAL, total);
        cv.put(FileDownloadModel.ETAG, etag); // maybe null.
        cv.put(FileDownloadModel.FILENAME, filename); // maybe null.

        update(id, cv);
    }

    @Override
    public void updateProgress(int id, long sofarBytes) {
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.progress);
        cv.put(FileDownloadModel.SOFAR, sofarBytes);

        update(id, cv);
    }

    @Override
    public void updateError(int id, Throwable throwable, long sofar) {
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.ERR_MSG, throwable.toString());
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.error);
        cv.put(FileDownloadModel.SOFAR, sofar);

        update(id, cv);
    }

    @Override
    public void updateRetry(int id, Throwable throwable) {
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.ERR_MSG, throwable.toString());
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.retry);

        update(id, cv);
    }

    @Override
    public void updateCompleted(int id, final long total) {
        remove(id);
    }

    @Override
    public void updatePause(int id, long sofar) {
        ContentValues cv = new ContentValues();
        cv.put(FileDownloadModel.STATUS, FileDownloadStatus.paused);
        cv.put(FileDownloadModel.SOFAR, sofar);

        update(id, cv);
    }

    @Override
    public void updatePending(int id) {
        // No need to persist pending status.
    }

    @Override
    public FileDownloadDatabase.Maintainer maintainer() {
        return new Maintainer();
    }

    public FileDownloadDatabase.Maintainer maintainer(
            SparseArray<FileDownloadModel> downloaderModelMap,
            SparseArray<List<ConnectionModel>> connectionModelListMap) {
        return new Maintainer(downloaderModelMap, connectionModelListMap);
    }

    private void update(final int id, final ContentValues cv) {
        db.update(TABLE_NAME, cv, FileDownloadModel.ID + " = ? ", new String[]{String.valueOf(id)});
    }

    public class Maintainer implements FileDownloadDatabase.Maintainer {

        private final SparseArray<FileDownloadModel> needChangeIdList = new SparseArray<>();
        private final SparseArray<FileDownloadModel> needRemoveList = new SparseArray<>();
        private MaintainerIterator currentIterator;

        private final SparseArray<FileDownloadModel> downloaderModelMap;
        private final SparseArray<List<ConnectionModel>> connectionModelListMap;

        Maintainer() {
            this(null, null);
        }

        Maintainer(SparseArray<FileDownloadModel> downloaderModelMap,
                   SparseArray<List<ConnectionModel>> connectionModelListMap) {
            this.downloaderModelMap = downloaderModelMap;
            this.connectionModelListMap = connectionModelListMap;
        }

        @Override
        public Iterator<FileDownloadModel> iterator() {
            return currentIterator = new MaintainerIterator();
        }

        @Override
        public void onFinishMaintain() {
            if (currentIterator != null) currentIterator.onFinishMaintain();

            final int length = needChangeIdList.size();
            if (length < 0) return;

            db.beginTransaction();
            try {
                for (int i = 0; i < length; i++) {
                    final int oldId = needChangeIdList.keyAt(i);
                    final FileDownloadModel modelWithNewId = needChangeIdList.get(oldId);
                    db.delete(TABLE_NAME, FileDownloadModel.ID + " = ?",
                            new String[]{String.valueOf(oldId)});
                    db.insert(TABLE_NAME, null, modelWithNewId.toContentValues());

                    if (modelWithNewId.getConnectionCount() > 1) {
                        List<ConnectionModel> connectionModelList = findConnectionModel(oldId);
                        if (connectionModelList.size() <= 0) continue;

                        db.delete(CONNECTION_TABLE_NAME, ConnectionModel.ID + " = ?",
                                new String[]{String.valueOf(oldId)});
                        for (ConnectionModel connectionModel : connectionModelList) {
                            connectionModel.setId(modelWithNewId.getId());
                            db.insert(CONNECTION_TABLE_NAME, null,
                                    connectionModel.toContentValues());
                        }
                    }
                }

                // remove invalid
                final int removeSize = needRemoveList.size();
                for (int i = 0; i < removeSize; i++) {
                    final int modelId = needRemoveList.keyAt(i);
                    db.delete(TABLE_NAME, FileDownloadModel.ID + " = ?",
                            new String[]{String.valueOf(modelId)});
                }

                // initial cache of connection model
                if (downloaderModelMap != null && connectionModelListMap != null) {
                    final int size = downloaderModelMap.size();
                    for (int i = 0; i < size; i++) {
                        final int id = downloaderModelMap.valueAt(i).getId();
                        final List<ConnectionModel> connectionModelList = findConnectionModel(id);

                        if (connectionModelList != null && connectionModelList.size() > 0) {
                            connectionModelListMap.put(id, connectionModelList);
                        }
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        @Override
        public void onRemovedInvalidData(FileDownloadModel model) {
            synchronized (needRemoveList) {
                needRemoveList.put(model.getId(), model);
            }
        }

        @Override
        public void onRefreshedValidData(FileDownloadModel model) {
            if (downloaderModelMap != null) {
                synchronized (downloaderModelMap) {
                    downloaderModelMap.put(model.getId(), model);
                }
            }
        }

        @Override
        public void changeFileDownloadModelId(int oldId, FileDownloadModel modelWithNewId) {
            synchronized (needChangeIdList) {
                needChangeIdList.put(oldId, modelWithNewId);
            }
        }

    }

    class MaintainerIterator implements Iterator<FileDownloadModel> {
        private final Cursor c;
        private final List<Integer> needRemoveId = new ArrayList<>();
        private int currentId;


        MaintainerIterator() {
            c = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        }

        @Override
        public boolean hasNext() {
            return c.moveToNext();
        }

        @Override
        public FileDownloadModel next() {
            final FileDownloadModel model = createFromCursor(c);

            currentId = model.getId();

            return model;
        }

        @Override
        public void remove() {
            needRemoveId.add(currentId);
        }

        void onFinishMaintain() {
            c.close();

            if (!needRemoveId.isEmpty()) {
                String args = TextUtils.join(", ", needRemoveId);
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "delete %s", args);
                }
                //noinspection ThrowFromFinallyBlock
                db.execSQL(FileDownloadUtils.formatString("DELETE FROM %s WHERE %s IN (%s);",
                        TABLE_NAME, FileDownloadModel.ID, args));
                db.execSQL(FileDownloadUtils.formatString("DELETE FROM %s WHERE %s IN (%s);",
                        CONNECTION_TABLE_NAME, ConnectionModel.ID, args));
            }
        }

    }

    private static FileDownloadModel createFromCursor(Cursor c) {
        final FileDownloadModel model = new FileDownloadModel();
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
        model.setConnectionCount(
                c.getInt(c.getColumnIndex(FileDownloadModel.CONNECTION_COUNT)));

        return model;
    }

    public static class Maker implements FileDownloadHelper.DatabaseCustomMaker {

        @Override
        public FileDownloadDatabase customMake() {
            return new SqliteDatabaseImpl();
        }
    }
}
