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

import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.message.MessageSnapshotFlow;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadLog;

import java.util.List;

/**
 * The message snapshot gate beyond the downloader service.
 */

public class MessageSnapshotGate implements MessageSnapshotFlow.MessageReceiver {

    private boolean transmitMessage(List<BaseDownloadTask.IRunningTask> taskList,
                                    MessageSnapshot snapshot) {

        if (taskList.size() > 1 && snapshot.getStatus() == FileDownloadStatus.completed) {
            for (BaseDownloadTask.IRunningTask task : taskList) {
                synchronized (task.getPauseLock()) {
                    if (task.getMessageHandler().updateMoreLikelyCompleted(snapshot)) {
                        FileDownloadLog.d(this, "updateMoreLikelyCompleted");
                        return true;
                    }
                }
            }
        }

        for (BaseDownloadTask.IRunningTask task : taskList) {
            synchronized (task.getPauseLock()) {
                if (task.getMessageHandler().updateKeepFlow(snapshot)) {
                    FileDownloadLog.d(this, "updateKeepFlow");
                    return true;
                }
            }
        }

        if (FileDownloadStatus.warn == snapshot.getStatus()) {
            for (BaseDownloadTask.IRunningTask task : taskList) {
                synchronized (task.getPauseLock()) {
                    if (task.getMessageHandler().updateSameFilePathTaskRunning(snapshot)) {
                        FileDownloadLog.d(this, "updateSampleFilePathTaskRunning");
                        return true;
                    }
                }
            }
        }

        //noinspection SimplifiableIfStatement
        if (taskList.size() == 1) {
            // Cover the most case for restarting from the low memory status.
            final BaseDownloadTask.IRunningTask onlyTask = taskList.get(0);
            synchronized (onlyTask.getPauseLock()) {
                FileDownloadLog.d(this, "updateKeepAhead");
                return onlyTask.getMessageHandler().updateKeepAhead(snapshot);
            }
        }

        return false;
    }

    @Override
    public void receive(MessageSnapshot snapshot) {

        final String updateSyncLock = Integer.toString(snapshot.getId());
        synchronized (updateSyncLock.intern()) {
            final List<BaseDownloadTask.IRunningTask> taskList = FileDownloadList.getImpl().
                    getReceiveServiceTaskList(snapshot.getId());

            if (taskList.size() > 0) {
                final BaseDownloadTask topOriginTask = taskList.get(0).getOrigin();

                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "~~~callback %s old[%s] new[%s] %d",
                            snapshot.getId(), topOriginTask.getStatus(), snapshot.getStatus(),
                            taskList.size());
                }

                if (!transmitMessage(taskList, snapshot)) {

                    StringBuilder log = new StringBuilder("The event isn't consumed, id:"
                            + snapshot.getId() + " status:"
                            + snapshot.getStatus() + " task-count:" + taskList.size());
                    for (BaseDownloadTask.IRunningTask task : taskList) {
                        log.append(" | ").append(task.getOrigin().getStatus());
                    }
                    FileDownloadLog.i(this, log.toString());
                }


            } else {
                FileDownloadLog.i(this, "Receive the event %d, but there isn't any running"
                        + " task in the upper layer", snapshot.getStatus());
            }

        }
    }
}
