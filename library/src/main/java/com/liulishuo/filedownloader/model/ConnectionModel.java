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

package com.liulishuo.filedownloader.model;

import android.content.ContentValues;

import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.List;

/**
 * The connection model used for record each connections on multiple connections case.
 */

public class ConnectionModel {
    public final static String ID = "id";
    private int id;

    public final static String INDEX = "connectionIndex";
    private int index;

    public final static String START_OFFSET = "startOffset";
    private long startOffset;

    public final static String CURRENT_OFFSET = "currentOffset";
    private long currentOffset;

    public final static String END_OFFSET = "endOffset";
    private long endOffset;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(long endOffset) {
        this.endOffset = endOffset;
    }

    public ContentValues toContentValues() {
        final ContentValues values = new ContentValues();
        values.put(ConnectionModel.ID, id);
        values.put(ConnectionModel.INDEX, index);
        values.put(ConnectionModel.START_OFFSET, startOffset);
        values.put(ConnectionModel.CURRENT_OFFSET, currentOffset);
        values.put(ConnectionModel.END_OFFSET, endOffset);
        return values;
    }

    public static long getTotalOffset(List<ConnectionModel> modelList) {
        long totalOffset = 0;
        for (ConnectionModel model : modelList) {
            totalOffset += (model.getCurrentOffset() - model.getStartOffset());
        }
        return totalOffset;
    }

    @Override
    public String toString() {
        return FileDownloadUtils.formatString("id[%d] index[%d] range[%d, %d) current offset(%d)",
                id, index, startOffset, endOffset, currentOffset);
    }
}
