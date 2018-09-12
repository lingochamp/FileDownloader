package com.liulishuo.filedownloader.i;

import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.model.ServiceStatusModel;

interface IFileDownloadIPCCallback {
    oneway void callback(in MessageSnapshot snapshot);
    void checkRunServiceForeground(inout ServiceStatusModel serviceStatusModel);
}
