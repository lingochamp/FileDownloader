package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 */
public abstract class IFileListener {

    private final int priority;

    public IFileListener(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }

    public abstract boolean callback(IFileEvent event);

}
