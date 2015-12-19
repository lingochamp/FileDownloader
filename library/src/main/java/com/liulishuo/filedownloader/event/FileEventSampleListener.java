package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 */
public class FileEventSampleListener extends IFileListener {
    public IEventListener i;

    public FileEventSampleListener(IEventListener i) {
        this(0, i);
    }

    public FileEventSampleListener(int priority, IEventListener i) {
        super(priority);
        this.i = i;
    }

    @Override
    public boolean callback(IFileEvent event) {
        return i != null ? i.callback(event) : false;
    }

    public interface IEventListener {
        public boolean callback(IFileEvent event);
    }

}
