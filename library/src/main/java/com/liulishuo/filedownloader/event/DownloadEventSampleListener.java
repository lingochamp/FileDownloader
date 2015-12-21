package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 */
public class DownloadEventSampleListener extends IDownloadListener {
    public IEventListener i;

    public DownloadEventSampleListener(IEventListener i) {
        this(0, i);
    }

    public DownloadEventSampleListener(int priority, IEventListener i) {
        super(priority);
        this.i = i;
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        return i != null ? i.callback(event) : false;
    }

    public interface IEventListener {
        public boolean callback(IDownloadEvent event);
    }

}
