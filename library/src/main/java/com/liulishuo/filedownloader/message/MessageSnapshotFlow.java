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

import com.liulishuo.filedownloader.model.FileDownloadStatus;

/**
 * Created by Jacksgong on 4/30/16.
 * <p/>
 * The internal message snapshot station.
 * Making message snapshots keep flowing in order.
 */
public class MessageSnapshotFlow {

    private volatile MessageSnapshotThreadPool flowThreadPool;
    private volatile MessageReceiver receiver;

    public final static class HolderClass {
        private final static MessageSnapshotFlow INSTANCE = new MessageSnapshotFlow();
    }

    public static MessageSnapshotFlow getImpl() {
        return HolderClass.INSTANCE;
    }

    public void setReceiver(MessageReceiver receiver) {
        this.receiver = receiver;
        if (receiver == null) {
            this.flowThreadPool = null;
        } else {
            this.flowThreadPool = new MessageSnapshotThreadPool(5, receiver);
        }
    }

    public void inflow(final MessageSnapshot snapshot) {
        boolean isReceivedImmediately = false;
        switch (snapshot.getStatus()) {
            case FileDownloadStatus.warn:
                isReceivedImmediately = true;
                break;
            case FileDownloadStatus.completed:
                if (snapshot.isReusedDownloadedFile()) {
                    isReceivedImmediately = true;
                    break;
                }
        }

        if (isReceivedImmediately) {
            if (receiver != null) {
                receiver.receive(snapshot);
            }
        } else {
            if (flowThreadPool != null) {
                flowThreadPool.execute(snapshot);
            }
        }

    }


    public interface MessageReceiver {
        void receive(MessageSnapshot snapshot);
    }
}
