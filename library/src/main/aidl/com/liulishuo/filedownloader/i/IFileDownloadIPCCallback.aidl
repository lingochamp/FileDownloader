package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.model.FileDownloadTransferModel;

interface IFileDownloadIPCCallback {
    oneway void callback(in FileDownloadTransferModel transfer);
}
