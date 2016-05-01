package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.message.MessageSnapshot;

interface IFileDownloadIPCCallback {
    oneway void callback(in MessageSnapshot snapshot);
}
