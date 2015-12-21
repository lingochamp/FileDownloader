package com.liulishuo.filedownloader.event;

import android.os.Looper;

/**
 * Created by Jacksgong on 15/6/23.
 */
public interface IDownloadEventPool {

    boolean addListener(final String eventId, final IDownloadListener listener);

    boolean removeListener(final String eventId, final IDownloadListener listener);

    boolean hasListener(final IDownloadEvent event);

    boolean publish(final IDownloadEvent event);

    void asyncPublish(final IDownloadEvent event, Looper looper);

    void asyncPublishInNewThread(final IDownloadEvent event);

    void asyncPublishInMain(final IDownloadEvent event);

}
