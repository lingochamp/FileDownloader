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

import android.util.SparseArray;

import com.liulishuo.filedownloader.model.ConnectionModel;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The no database implementation, this implementation no use database anymore, it just store the
 * data on the cache, which means when the process is killed or re-create everything would be gone
 * including all breakpoint data, and some mistake rescue will be can't be achieve.
 * <p>
 * but you can get following benefit:
 * <p>
 * 1. there isn't any database error anymore.
 * 2. if the process is alive the breakpoint is store on the cache, so the breakpoint is valid which
 * produce on the process living time.
 * 3. there isn't any cost from database work.
 * <p>
 * You can valid this database implementation through:
 * <p>
 * class MyApplication extends Application {
 *     ...
 *     public void onCreate() {
 *          ...
 *          FileDownloader.setupOnApplicationOnCreate(this)
 *              .database(NoDatabaseImpl.createMaker())
 *              ...
 *              .commit();
 *          ...
 *     }
 *     ...
 * }
 */
public class NoDatabaseImpl implements FileDownloadDatabase {

    final SparseArray<FileDownloadModel> downloaderModelMap = new SparseArray<>();
    final SparseArray<List<ConnectionModel>> connectionModelListMap = new SparseArray<>();

    public NoDatabaseImpl() {
    }

    public static Maker createMaker() {
        return new Maker();
    }

    @Override
    public void onTaskStart(int id) {
    }

    @Override
    public FileDownloadModel find(final int id) {
        synchronized (downloaderModelMap) {
            return downloaderModelMap.get(id);
        }
    }

    @Override
    public List<ConnectionModel> findConnectionModel(int id) {
        final List<ConnectionModel> resultList = new ArrayList<>();
        List<ConnectionModel> processList = null;
        synchronized (connectionModelListMap) {
            processList = connectionModelListMap.get(id);
        }
        if (processList != null) resultList.addAll(processList);
        return resultList;
    }

    @Override
    public void removeConnections(int id) {
        synchronized (connectionModelListMap) {
            connectionModelListMap.remove(id);
        }
    }

    @Override
    public void insertConnectionModel(ConnectionModel model) {
        final int id = model.getId();
        synchronized (connectionModelListMap) {
            List<ConnectionModel> processList = connectionModelListMap.get(id);
            if (processList == null) {
                processList = new ArrayList<>();
                connectionModelListMap.put(id, processList);
            }
            processList.add(model);
        }
    }

    @Override
    public void updateConnectionModel(int id, int index, long currentOffset) {
        synchronized (connectionModelListMap) {
            final List<ConnectionModel> processList = connectionModelListMap.get(id);
            if (processList == null) return;

            for (ConnectionModel connectionModel : processList) {
                if (connectionModel.getIndex() == index) {
                    connectionModel.setCurrentOffset(currentOffset);
                    return;
                }
            }
        }
    }

    @Override
    public void updateConnectionCount(int id, int count) {
    }

    @Override
    public void insert(FileDownloadModel downloadModel) {
        synchronized (downloaderModelMap) {
            downloaderModelMap.put(downloadModel.getId(), downloadModel);
        }
    }

    @Override
    public void update(FileDownloadModel downloadModel) {
        if (downloadModel == null) {
            FileDownloadLog.w(this, "update but model == null!");
            return;
        }

        if (find(downloadModel.getId()) != null) {
            // 替换
            synchronized (downloaderModelMap) {
                downloaderModelMap.remove(downloadModel.getId());
                downloaderModelMap.put(downloadModel.getId(), downloadModel);
            }
        } else {
            insert(downloadModel);
        }
    }

    @Override
    public boolean remove(int id) {
        synchronized (downloaderModelMap) {
            downloaderModelMap.remove(id);
        }
        return true;
    }

    @Override
    public void clear() {
        synchronized (downloaderModelMap) {
            downloaderModelMap.clear();
        }
    }

    @Override
    public void updateOldEtagOverdue(int id, String newEtag, long sofar, long total,
                                     int connectionCount) {
    }

    @Override
    public void updateConnected(int id, long total, String etag, String filename) {
    }

    @Override
    public void updateProgress(int id, long sofarBytes) {
    }

    @Override
    public void updateError(int id, Throwable throwable, long sofar) {
    }

    @Override
    public void updateRetry(int id, Throwable throwable) {
    }

    @Override
    public void updateCompleted(int id, final long total) {
        remove(id);
    }

    @Override
    public void updatePause(int id, long sofar) {
    }

    @Override
    public void updatePending(int id) {
    }

    @Override
    public FileDownloadDatabase.Maintainer maintainer() {
        return new Maintainer();
    }

    class Maintainer implements FileDownloadDatabase.Maintainer {

        @Override
        public Iterator<FileDownloadModel> iterator() {
            return new MaintainerIterator();
        }

        @Override
        public void onFinishMaintain() {
        }

        @Override
        public void onRemovedInvalidData(FileDownloadModel model) {
        }

        @Override
        public void onRefreshedValidData(FileDownloadModel model) {
        }

        @Override
        public void changeFileDownloadModelId(int oldId, FileDownloadModel modelWithNewId) {
        }

    }

    class MaintainerIterator implements Iterator<FileDownloadModel> {

        MaintainerIterator() {
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public FileDownloadModel next() {
            return null;
        }

        @Override
        public void remove() {
        }
    }

    public static class Maker implements FileDownloadHelper.DatabaseCustomMaker {

        @Override
        public FileDownloadDatabase customMake() {
            return new NoDatabaseImpl();
        }
    }
}
