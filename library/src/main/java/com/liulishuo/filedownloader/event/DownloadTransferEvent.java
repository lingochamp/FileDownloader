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

package com.liulishuo.filedownloader.event;


import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

/**
 * Created by Jacksgong on 9/24/15.
 * <p/>
 * 内部 通信 事件
 */
public class DownloadTransferEvent extends IDownloadEvent {

    public final static String ID = "event.download.transfer";

    public DownloadTransferEvent(final FileDownloadTransferModel transfer) {
        super(ID);
        this.transfer = transfer;
    }

    private FileDownloadTransferModel transfer;

    public DownloadTransferEvent setTransfer(final FileDownloadTransferModel transfer) {
        this.transfer = transfer;
        return this;
    }

    public FileDownloadTransferModel getTransfer() {
        return transfer;
    }
}
