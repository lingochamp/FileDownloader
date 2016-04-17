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

import android.app.Notification;
import android.content.Context;

import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadTransferModel;
import com.liulishuo.filedownloader.services.FDServiceSharedHandler;

/**
 * Created by Jacksgong on 4/17/16.
 * <p/>
 * The proxy used for executing the action from FileDownloader to FileDownloadService.
 *
 * @see FileDownloadServiceSharedTransmit In case of FileDownloadService runs in the main process.
 * @see FileDownloadServiceUIGuard In case of FileDownloadService runs in the separate `:filedownloader` process.
 * <p/>
 * You can add a command `process.non-separate=true` to `/filedownloader.properties` to make the
 * FileDownloadService runs in the main process, and by default the FileDownloadService runs in the
 * separate `:filedownloader` process.
 */
public class FileDownloadServiceProxy implements IFileDownloadServiceProxy {
    private final static class HolderClass {
        private final static FileDownloadServiceProxy INSTANCE = new FileDownloadServiceProxy();
    }

    public static FileDownloadServiceProxy getImpl() {
        return HolderClass.INSTANCE;
    }

    public static FDServiceSharedHandler.FileDownloadServiceSharedConnection getConnectionListener() {
        if (getImpl().handler instanceof FileDownloadServiceSharedTransmit) {
            return (FDServiceSharedHandler.FileDownloadServiceSharedConnection) getImpl().handler;
        }
        return null;
    }

    private final IFileDownloadServiceProxy handler;

    private FileDownloadServiceProxy() {
        handler = BuildConfig.PROCESS_NON_SEPARATE ?
                new FileDownloadServiceSharedTransmit() :
                new FileDownloadServiceUIGuard();
    }

    @Override
    public boolean startDownloader(String url, String path, int callbackProgressTimes, int autoRetryTimes, FileDownloadHeader header) {
        return handler.startDownloader(url, path, callbackProgressTimes, autoRetryTimes, header);
    }

    @Override
    public boolean pauseDownloader(int downloadId) {
        return handler.pauseDownloader(downloadId);
    }

    @Override
    public FileDownloadTransferModel checkReuse(String url, String path) {
        return handler.checkReuse(url, path);
    }

    @Override
    public FileDownloadTransferModel checkReuse(int id) {
        return handler.checkReuse(id);
    }

    @Override
    public boolean checkIsDownloading(String url, String path) {
        return handler.checkIsDownloading(url, path);
    }

    @Override
    public long getSofar(int downloadId) {
        return handler.getSofar(downloadId);
    }

    @Override
    public long getTotal(int downloadId) {
        return handler.getTotal(downloadId);
    }

    @Override
    public int getStatus(int downloadId) {
        return handler.getStatus(downloadId);
    }

    @Override
    public void pauseAllTasks() {
        handler.pauseAllTasks();
    }

    @Override
    public boolean isIdle() {
        return handler.isIdle();
    }

    @Override
    public boolean isConnected() {
        return handler.isConnected();
    }

    @Override
    public void bindStartByContext(Context context) {
        handler.bindStartByContext(context);
    }

    @Override
    public void bindStartByContext(Context context, Runnable connectedRunnable) {
        handler.bindStartByContext(context, connectedRunnable);
    }

    @Override
    public void unbindByContext(Context context) {
        handler.unbindByContext(context);
    }

    @Override
    public void startForeground(int id, Notification notification) {
        handler.startForeground(id, notification);
    }

    @Override
    public void stopForeground(boolean removeNotification) {
        handler.stopForeground(removeNotification);
    }
}
