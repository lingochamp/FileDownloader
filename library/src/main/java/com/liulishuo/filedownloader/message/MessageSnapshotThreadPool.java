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
package com.liulishuo.filedownloader.message;

import com.liulishuo.filedownloader.util.FileDownloadExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * For guaranteeing only one-thread-pool for one-task, the task will be identified by its ID, make
 * sure the same task will be invoked in FIFO.
 */
public class MessageSnapshotThreadPool {

    private final List<FlowSingleExecutor> executorList;

    private final MessageSnapshotFlow.MessageReceiver receiver;

    MessageSnapshotThreadPool(@SuppressWarnings("SameParameterValue") final int poolCount,
                              MessageSnapshotFlow.MessageReceiver receiver) {
        this.receiver = receiver;
        executorList = new ArrayList<>();
        for (int i = 0; i < poolCount; i++) {
            executorList.add(new FlowSingleExecutor(i));
        }
    }

    public void execute(final MessageSnapshot snapshot) {
        FlowSingleExecutor targetPool = null;
        try {
            synchronized (executorList) {
                final int id = snapshot.getId();
                // Case 1. already had same task in executorList, so execute this event after
                // before-one.
                for (FlowSingleExecutor executor : executorList) {
                    if (executor.enQueueTaskIdList.contains(id)) {
                        targetPool = executor;
                        break;
                    }
                }

                // Case 2. no same task in executorList, so execute in executor which has the count
                // of active task is least.
                if (targetPool == null) {
                    int leastTaskCount = 0;
                    for (FlowSingleExecutor executor : executorList) {
                        if (executor.enQueueTaskIdList.size() <= 0) {
                            targetPool = executor;
                            break;
                        }

                        if (leastTaskCount == 0 ||
                                executor.enQueueTaskIdList.size() < leastTaskCount) {
                            leastTaskCount = executor.enQueueTaskIdList.size();
                            targetPool = executor;
                        }
                    }
                }

                //noinspection ConstantConditions
                targetPool.enqueue(id);
            }
        } finally {
            //noinspection ConstantConditions
            targetPool.execute(snapshot);
        }
    }

    public class FlowSingleExecutor extends FileDownloadExecutors.FileDownloadExecutor {

        private final List<Integer> enQueueTaskIdList = new ArrayList<>();

        public FlowSingleExecutor(int index) {
            super(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    "FowSingleExecutor" + index);
        }

        public void enqueue(final int id) {
            enQueueTaskIdList.add(id);
        }

        public void execute(final MessageSnapshot snapshot) {
            execute(new Runnable() {
                @Override
                public void run() {
                    receiver.receive(snapshot);
                    enQueueTaskIdList.remove((Integer) snapshot.getId());
                }
            });
        }

    }
}
