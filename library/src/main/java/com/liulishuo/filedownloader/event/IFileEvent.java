package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 */
public abstract class IFileEvent {
    public Runnable callback = null;

    public IFileEvent(final String id) {
        this.id = id;
    }

    public IFileEvent(final String id, boolean order) {
        this.id = id;
        this.order = order;
    }

    protected String id;
    protected boolean order;

    public final String getId() {
        return this.id;
    }

    public boolean getOrder() {
        return this.order;
    }
}
