package com.liulishuo.filedownloader.event;


/**
 * Created by Jacksgong on 9/24/15.
 */
public class FileServiceConnectChangedEvent extends IFileEvent {
    public final static String ID = "event.service.connect.changed";

    public FileServiceConnectChangedEvent(final ConnectStatus status, final Class<?> serviceClass) {
        super(ID);

        this.status = status;
        this.serviceClass = serviceClass;
    }

    private ConnectStatus status;

    public enum ConnectStatus {
        connected, disconnected
    }

    public ConnectStatus getStatus() {
        return status;
    }


    private Class<?> serviceClass;

    public boolean isSuchService(final Class<?> serviceClass) {
        if (serviceClass == null) {
            return false;
        }

        return this.serviceClass.getName().equals(serviceClass.getName());

    }
}
