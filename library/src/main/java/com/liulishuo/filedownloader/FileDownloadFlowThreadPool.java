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

import com.liulishuo.filedownloader.event.DownloadTransferEvent;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jacksgong on 4/8/16.
 * <p/>
 * For guarantee only one-flow for one-task.
 */
public class FileDownloadFlowThreadPool {

    private List<FlowSingleExecutor> executorList;

    public FileDownloadFlowThreadPool(final int poolCount) {
        executorList = new ArrayList<>();
        for (int i = 0; i < poolCount; i++) {
            executorList.add(new FlowSingleExecutor());
        }
    }

    public void execute(final DownloadTransferEvent event) {
        FlowSingleExecutor targetPool = null;
        try {
            synchronized (executorList) {
                final int id = event.getTransfer().getDownloadId();
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

                targetPool.enqueue(id);
            }
        } finally {
            targetPool.execute(event);
        }
    }

    public static class FlowSingleExecutor extends ThreadPoolExecutor {

        private List<Integer> enQueueTaskIdList = new ArrayList<>();

        public FlowSingleExecutor() {
            super(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }

        public void enqueue(final int id) {
            enQueueTaskIdList.add(id);
        }

        public void execute(final DownloadTransferEvent event) {
            final FileDownloadTransferModel model = event.getTransfer();


            execute(new Runnable() {
                @Override
                public void run() {
                    FileDownloadEventPool.getImpl().publish(event);
                    enQueueTaskIdList.remove((Integer) model.getDownloadId());
                }
            });
        }

    }
}
