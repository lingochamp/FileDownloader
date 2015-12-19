package com.liulishuo.filedownloader.event;

import android.os.Handler;
import android.os.Looper;

import com.liulishuo.filedownloader.util.FileDownloadLog;

import junit.framework.Assert;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jacksgong on 15/6/23.
 */
final public class FileEventPoolImpl implements IFileEventPool {

    private ExecutorService threadPool = Executors.newFixedThreadPool(8);

    private final HashMap<String, LinkedList<IFileListener>> listenersMap = new HashMap<>();

    private Handler handler;

    public FileEventPoolImpl() {
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean addListener(final String eventId, final IFileListener listener) {
        FileDownloadLog.v(this, "addListener %s", eventId);
        Assert.assertNotNull("EventPoolImpl.add", listener);
        LinkedList<IFileListener> container = listenersMap.get(eventId);
        if (container == null) {
            listenersMap.put(eventId, container = new LinkedList<>());
        }
        synchronized (container) {
            return container.add(listener);
        }
    }

    @Override
    public boolean removeListener(final String eventId, final IFileListener listener) {
        FileDownloadLog.v(this, "removeListener %s", eventId);
//        Assert.assertNotNull("EventPoolImpl.remove", listener);
        LinkedList<IFileListener> container = listenersMap.get(eventId);
        if (container == null || listener == null) {
            return false;
        }

        synchronized (container) {
            return container.remove(listener);
        }
    }

    @Override
    public boolean publish(final IFileEvent event) {
        FileDownloadLog.v(this, "publish %s", event.getId());
        Assert.assertNotNull("EventPoolImpl.publish", event);
        String eventId = event.getId();
        LinkedList<IFileListener> listeners = listenersMap.get(eventId);
        if (listeners == null) {
            FileDownloadLog.w(this, "No listener for this event %s", eventId);
            return false;
        }
        trigger(listeners, event);
        return true;
    }

    @Override
    public void asyncPublish(final IFileEvent event, final Looper looper) {
        FileDownloadLog.v(this, "asyncPublish %s", event.getId());
        Assert.assertNotNull("EventPoolImpl.asyncPublish event", event);
        Assert.assertNotNull("EventPoolImpl.asyncPublish looper", looper);
        Handler handler = new Handler(looper);
        handler.post(new Runnable() {
            @Override
            public void run() {
                FileEventPoolImpl.this.publish(event);
            }
        });
    }


    @Override
    public void asyncPublishInNewThread(final IFileEvent event) {
        FileDownloadLog.v(this, "asyncPublishInNewThread %s", event.getId());
        Assert.assertNotNull("EventPoolImpl.asyncPublish event", event);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                FileEventPoolImpl.this.publish(event);
            }
        });
    }

    @Override
    public void asyncPublishInMain(final IFileEvent event) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                FileEventPoolImpl.this.publish(event);
            }
        });
    }

    private void trigger(final LinkedList<IFileListener> listeners, final IFileEvent event) {
        synchronized (listeners) {
            try {
                if (event.getOrder()) {
                    Collections.sort(listeners, new Comparator<IFileListener>() {
                        @Override
                        public int compare(IFileListener lhs, IFileListener rhs) {
                            return rhs.getPriority() - lhs.getPriority();
                        }
                    });
                }

            } catch (Exception e) {
                FileDownloadLog.e(this, e, "trigger error, %s", event != null ? event.getId() : null);
            }

            for (Object o : listeners.toArray()) {
                if (((IFileListener) o).callback(event)) {
                    break;
                }
            }

        }


        if (event.callback != null) {
            event.callback.run();
        }
    }

    @Override
    public boolean hasListener(final IFileEvent event) {
        FileDownloadLog.v(this, "hasListener %s", event.getId());
        Assert.assertNotNull("EventPoolImpl.hasListener", event);
        String eventId = event.getId();
        LinkedList<IFileListener> listeners = listenersMap.get(eventId);
        return listeners != null && listeners.size() > 0;
    }
}
