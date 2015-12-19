package com.liulishuo.filedownloader.event;

import android.os.Looper;

/**
 * Created by Jacksgong on 15/6/23.
 */
public interface IFileEventPool {

    boolean addListener(final String eventId, final IFileListener listener);

    boolean removeListener(final String eventId, final IFileListener listener);

    boolean hasListener(final IFileEvent event);

    boolean publish(final IFileEvent event);

    void asyncPublish(final IFileEvent event, Looper looper);

    void asyncPublishInNewThread(final IFileEvent event);

    void asyncPublishInMain(final IFileEvent event);

}
