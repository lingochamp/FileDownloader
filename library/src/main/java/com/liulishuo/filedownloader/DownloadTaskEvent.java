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

package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.event.IDownloadEvent;

/**
 * Created by Jacksgong on 12/23/15.
 */
public class DownloadTaskEvent extends IDownloadEvent {

    public final static String ID = "event.download.task";

    private BaseDownloadTask task;

    public DownloadTaskEvent(BaseDownloadTask task) {
        super(ID);
        this.task = task;
    }

    /**
     * @return 消耗掉并返回本事件的Task
     */
    public BaseDownloadTask consume() {
        final BaseDownloadTask task = this.task;
        this.task = null;
        return task;
    }

    public FileDownloadListener getTaskListener() {
        return this.task == null ? null : this.task.getListener();
    }

    private int operate;

    public DownloadTaskEvent requestStart() {
        this.operate = Operate.REQUEST_START;
        return this;
    }

    public int getOperate() {
        return this.operate;
    }


    public interface Operate {
        // request start download
        int REQUEST_START = 1;
    }
}
