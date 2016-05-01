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
import android.content.Intent;

import com.liulishuo.filedownloader.event.DownloadServiceConnectChangedEvent;
import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadTaskAtom;
import com.liulishuo.filedownloader.services.FDServiceSharedHandler;
import com.liulishuo.filedownloader.services.FDServiceSharedHandler.FileDownloadServiceSharedConnection;
import com.liulishuo.filedownloader.services.FileDownloadService.SharedMainProcessService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 4/17/16.
 * <p/>
 * This transmit layer is used for the FileDownloader-Process is shared the main process.
 * <p/>
 * If you want use this transmit and want the FileDownloadService share the main process, not in the
 * separate process, just add a command `process.non-separate=true` in `/filedownloader.properties`.
 *
 * @see FileDownloadServiceUIGuard
 */
public class FileDownloadServiceSharedTransmit implements IFileDownloadServiceProxy,
        FileDownloadServiceSharedConnection {

    private final static Class<?> SERVICE_CLASS = SharedMainProcessService.class;

    @Override
    public boolean startDownloader(String url, String path, int callbackProgressTimes, int autoRetryTimes, FileDownloadHeader header) {
        if (handler == null) {
            return false;
        }

        handler.start(url, path, callbackProgressTimes, autoRetryTimes, header);
        return true;
    }

    @Override
    public boolean pauseDownloader(int downloadId) {
        return handler.pause(downloadId);
    }

    @Override
    public MessageSnapshot checkReuse(String url, String path) {
        return handler.checkReuse(url, path);
    }

    @Override
    public MessageSnapshot checkReuse(int id) {
        return handler.checkReuse2(id);
    }

    @Override
    public boolean checkIsDownloading(String url, String path) {
        return handler.checkDownloading(url, path);
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
        return handler != null;
    }

    @Override
    public void bindStartByContext(Context context) {
        bindStartByContext(context, null);
    }

    private final ArrayList<Runnable> connectedRunnableList = new ArrayList<>();

    @Override
    public void bindStartByContext(Context context, Runnable connectedRunnable) {
        if (connectedRunnable != null) {
            if (!connectedRunnableList.contains(connectedRunnable)) {
                connectedRunnableList.add(connectedRunnable);
            }
        }
        Intent i = new Intent(context, SERVICE_CLASS);
        context.startService(i);
    }

    @Override
    public void unbindByContext(Context context) {
        Intent i = new Intent(context, SERVICE_CLASS);
        context.stopService(i);
        handler = null;
    }

    @Override
    public void startForeground(int id, Notification notification) {
        handler.startForeground(id, notification);
    }

    @Override
    public void stopForeground(boolean removeNotification) {
        handler.stopForeground(removeNotification);
    }

    @Override
    public boolean setTaskCompleted(String url, String path, long totalBytes) {
        return handler.setTaskCompleted(url, path, totalBytes);
    }

    @Override
    public boolean setTaskCompleted(List<FileDownloadTaskAtom> taskAtomList) {
        return handler.setTaskCompleted1(taskAtomList);
    }

    private FDServiceSharedHandler handler;

    @Override
    public void onConnected(final FDServiceSharedHandler handler) {
        this.handler = handler;
        @SuppressWarnings("unchecked") final List<Runnable> runnableList =
                (List<Runnable>) connectedRunnableList.clone();
        connectedRunnableList.clear();
        for (Runnable runnable : runnableList) {
            runnable.run();
        }

        FileDownloadEventPool.getImpl().
                asyncPublishInNewThread(new DownloadServiceConnectChangedEvent(
                        DownloadServiceConnectChangedEvent.ConnectStatus.connected,
                        SERVICE_CLASS));
    }

    @Override
    public void onDisconnected() {
        this.handler = null;
        FileDownloadEventPool.getImpl().
                asyncPublishInNewThread(new DownloadServiceConnectChangedEvent(
                        DownloadServiceConnectChangedEvent.ConnectStatus.disconnected,
                        SERVICE_CLASS));
    }


}
