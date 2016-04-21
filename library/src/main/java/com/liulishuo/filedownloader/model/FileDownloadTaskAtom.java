package com.liulishuo.filedownloader.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.List;

/**
 * Created by Jacksgong on 4/21/16.
 * <p/>
 * The Minimal unit for a task.
 * <p/>
 * Used for telling the FileDownloader Engine that a task was downloaded by the other ways.
 *
 * @see com.liulishuo.filedownloader.FileDownloader#setTaskCompleted(List)
 */
public class FileDownloadTaskAtom implements Parcelable {
    private String url;
    private String path;
    private long totalBytes;

    public FileDownloadTaskAtom(String url, String path, long totalBytes) {
        setUrl(url);
        setPath(path);
        setTotalBytes(totalBytes);
    }

    private int id;

    public int getId() {
        if (id != 0) {
            return id;
        }

        return id = FileDownloadUtils.generateId(getUrl(), getPath());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.path);
        dest.writeLong(this.totalBytes);
    }

    protected FileDownloadTaskAtom(Parcel in) {
        this.url = in.readString();
        this.path = in.readString();
        this.totalBytes = in.readLong();
    }

    public static final Parcelable.Creator<FileDownloadTaskAtom> CREATOR = new Parcelable.Creator<FileDownloadTaskAtom>() {
        @Override
        public FileDownloadTaskAtom createFromParcel(Parcel source) {
            return new FileDownloadTaskAtom(source);
        }

        @Override
        public FileDownloadTaskAtom[] newArray(int size) {
            return new FileDownloadTaskAtom[size];
        }
    };
}
