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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadProperties;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * If one data insert/update and remove within 2 sec, which will do not effect on
 * {@code realDatabase}.
 */
public class RemitDatabase implements FileDownloadDatabase {

    private final NoDatabaseImpl cachedDatabase;
    private final SqliteDatabaseImpl realDatabase;


    private Handler handler;
    private final long minInterval;

    private final List<Integer> freeToDBIdList = new ArrayList<>();
    private AtomicInteger handlingId = new AtomicInteger();
    private volatile Thread parkThread;

    private static final int WHAT_CLEAN_LOCK = 0;

    public RemitDatabase() {
        this.cachedDatabase = new NoDatabaseImpl();
        this.realDatabase = new SqliteDatabaseImpl();
        this.minInterval = FileDownloadProperties.getImpl().downloadMinProgressTime;

        final HandlerThread thread = new HandlerThread(
                FileDownloadUtils.getThreadPoolName("RemitHandoverToDB"));
        thread.start();
        handler = new Handler(thread.getLooper(), new Handler.Callback() {
            @Override public boolean handleMessage(Message msg) {
                final int id = msg.what;
                if (id == WHAT_CLEAN_LOCK) {
                    if (parkThread != null) {
                        LockSupport.unpark(parkThread);
                        parkThread = null;
                    }
                    return false;
                }

                try {
                    handlingId.set(id);

                    syncCacheToDB(id);
                    freeToDBIdList.add(id);
                } finally {
                    handlingId.set(0);
                    if (parkThread != null) {
                        LockSupport.unpark(parkThread);
                        parkThread = null;
                    }
                }

                return false;
            }
        });
    }

    private void syncCacheToDB(int id) {
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "sync cache to db %d", id);
        }

        // need to attention that the `sofar` in `FileDownloadModel` database will
        // be updated even through this is a multiple connections task. But the
        // multi-connections task only concern about the `ConnectionModel` database,
        // so it doesn't matter in current.
        realDatabase.update(cachedDatabase.find(id));
        final List<ConnectionModel> modelList = cachedDatabase.findConnectionModel(id);
        realDatabase.removeConnections(id);
        for (ConnectionModel connectionModel : modelList) {
            realDatabase.insertConnectionModel(connectionModel);
        }
    }

    private boolean isNoNeedUpdateToRealDB(int id) {
        return !freeToDBIdList.contains(id);
    }

    @Override public void onTaskStart(int id) {
        handler.sendEmptyMessageDelayed(id, minInterval);
    }

    @Override public FileDownloadModel find(int id) {
        return this.cachedDatabase.find(id);
    }

    @Override public List<ConnectionModel> findConnectionModel(int id) {
        return this.cachedDatabase.findConnectionModel(id);
    }

    @Override public void removeConnections(int id) {
        this.cachedDatabase.removeConnections(id);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.removeConnections(id);

    }

    @Override public void insertConnectionModel(ConnectionModel model) {
        this.cachedDatabase.insertConnectionModel(model);
        final int id = model.getId();
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.insertConnectionModel(model);
    }

    @Override public void updateConnectionModel(int id, int index, long currentOffset) {
        this.cachedDatabase.updateConnectionModel(id, index, currentOffset);

        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateConnectionModel(id, index, currentOffset);
    }

    @Override public void updateProgress(int id, long sofarBytes) {
        this.cachedDatabase.updateProgress(id, sofarBytes);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateProgress(id, sofarBytes);
    }

    @Override public void updateConnectionCount(int id, int count) {
        this.cachedDatabase.updateConnectionCount(id, count);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateConnectionCount(id, count);

    }

    @Override public void insert(FileDownloadModel downloadModel) {
        this.cachedDatabase.insert(downloadModel);
        if (isNoNeedUpdateToRealDB(downloadModel.getId())) return;
        this.realDatabase.insert(downloadModel);
    }

    @Override public void update(FileDownloadModel downloadModel) {
        this.cachedDatabase.update(downloadModel);
        if (isNoNeedUpdateToRealDB(downloadModel.getId())) return;
        this.realDatabase.update(downloadModel);
    }

    @Override public boolean remove(int id) {
        this.realDatabase.remove(id);
        return this.cachedDatabase.remove(id);
    }

    @Override public void clear() {
        this.cachedDatabase.clear();
        this.realDatabase.clear();
    }

    @Override public void updateOldEtagOverdue(int id, String newEtag, long sofar, long total,
                                               int connectionCount) {
        this.cachedDatabase.updateOldEtagOverdue(id, newEtag, sofar, total, connectionCount);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateOldEtagOverdue(id, newEtag, sofar, total, connectionCount);
    }

    @Override public void updateConnected(int id, long total, String etag, String filename) {
        this.cachedDatabase.updateConnected(id, total, etag, filename);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateConnected(id, total, etag, filename);
    }

    @Override public void updatePending(int id) {
        this.cachedDatabase.updatePending(id);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updatePending(id);
    }

    @Override public void updateRetry(int id, Throwable throwable) {
        this.cachedDatabase.updateRetry(id, throwable);
        if (isNoNeedUpdateToRealDB(id)) return;
        this.realDatabase.updateRetry(id, throwable);
    }

    private void ensureCacheToDB(int id) {
        handler.removeMessages(id);
        if (handlingId.get() == id) {
            parkThread = Thread.currentThread();
            handler.sendEmptyMessage(WHAT_CLEAN_LOCK);
            LockSupport.park();
        } else {
            syncCacheToDB(id);
        }
    }

    @Override public void updateError(int id, Throwable throwable, long sofar) {
        this.cachedDatabase.updateError(id, throwable, sofar);
        if (isNoNeedUpdateToRealDB(id)) {
            ensureCacheToDB(id);
        }
        this.realDatabase.updateError(id, throwable, sofar);
        freeToDBIdList.remove((Integer) id);
    }

    @Override public void updateCompleted(int id, long total) {
        this.cachedDatabase.updateCompleted(id, total);
        if (isNoNeedUpdateToRealDB(id)) {
            handler.removeMessages(id);
            if (handlingId.get() == id) {
                parkThread = Thread.currentThread();
                handler.sendEmptyMessage(WHAT_CLEAN_LOCK);
                LockSupport.park();
                this.realDatabase.updateCompleted(id, total);
            }
        } else {
            this.realDatabase.updateCompleted(id, total);
        }
        freeToDBIdList.remove((Integer) id);
    }

    @Override public void updatePause(int id, long sofar) {
        this.cachedDatabase.updatePause(id, sofar);
        if (isNoNeedUpdateToRealDB(id)) {
            ensureCacheToDB(id);
        }
        this.realDatabase.updatePause(id, sofar);
        freeToDBIdList.remove((Integer) id);
    }


    @Override public Maintainer maintainer() {
        return this.realDatabase.maintainer(this.cachedDatabase.downloaderModelMap,
                this.cachedDatabase.connectionModelListMap);
    }

    public static class Maker implements FileDownloadHelper.DatabaseCustomMaker {

        @Override public FileDownloadDatabase customMake() {
            return new RemitDatabase();
        }
    }
}
