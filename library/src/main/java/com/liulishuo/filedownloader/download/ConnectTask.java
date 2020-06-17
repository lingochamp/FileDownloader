/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.download;

import android.text.TextUtils;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.connection.RedirectHandler;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * The connect task which used for connect to the backend.
 */
public class ConnectTask {

    final int downloadId;
    final String url;
    final FileDownloadHeader header;

    private ConnectionProfile profile;
    private String etag;

    private Map<String, List<String>> requestHeader;
    private List<String> redirectedUrlList;


    private ConnectTask(ConnectionProfile profile,
                        int downloadId, String url, String etag, FileDownloadHeader header) {
        this.downloadId = downloadId;
        this.url = url;
        this.etag = etag;
        this.header = header;
        this.profile = profile;
    }

    void updateConnectionProfile(long downloadedOffset) {
        if (downloadedOffset == profile.currentOffset) {
            FileDownloadLog.w(this, "no data download, no need to update");
            return;
        }
        final long newContentLength =
                profile.contentLength - (downloadedOffset - profile.currentOffset);
        profile = ConnectionProfile.ConnectionProfileBuild.buildConnectionProfile(
                profile.startOffset,
                downloadedOffset,
                profile.endOffset,
                newContentLength);
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.i(this, "after update profile:%s", profile);
        }
    }

    FileDownloadConnection connect() throws IOException, IllegalAccessException {
        FileDownloadConnection connection = CustomComponentHolder.getImpl().createConnection(url);

        addUserRequiredHeader(connection);
        addRangeHeader(connection);
        fixNeededHeader(connection);

        // init request
        // get the request header in here, because of there are many connection
        // component(such as HttpsURLConnectionImpl, HttpURLConnectionImpl in okhttp3) don't
        // allow access to the request header after it connected.
        requestHeader = connection.getRequestHeaderFields();
        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "<---- %s request header %s", downloadId, requestHeader);
        }

        connection.execute();
        redirectedUrlList = new ArrayList<>();
        connection = RedirectHandler.process(requestHeader, connection, redirectedUrlList);

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(this, "----> %s response header %s", downloadId,
                    connection.getResponseHeaderFields());
        }

        return connection;
    }

    private void addUserRequiredHeader(FileDownloadConnection connection) {
        final HashMap<String, List<String>> additionHeaders;
        if (header != null) {
            additionHeaders = header.getHeaders();

            if (additionHeaders != null) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.v(this, "%d add outside header: %s",
                            downloadId, additionHeaders);
                }

                String name;
                List<String> list;

                Stack<String> names = new Stack<>();
                Stack<List<String>> lists = new Stack<>();

                // add addition headers which is provided by the user
                Set<Map.Entry<String, List<String>>> entries = additionHeaders.entrySet();
                for (Map.Entry<String, List<String>> e : entries) {
                    name = e.getKey();
                    list = e.getValue();
                    names.push(name);
                    lists.push(list);
                    /*if (list != null) {
                        for (String value : list) {
                            connection.addHeader(name, value);
                        }
                    }*/
                }

                while (lists.size() > 0) {
                    list = lists.pop();
                    name = names.pop();
                    if (list != null) {
                        for (String value : list) {
                            connection.addHeader(name, value);
                        }
                    }
                }
            }
        }
    }

    private void addRangeHeader(FileDownloadConnection connection) throws ProtocolException {
        if (connection.dispatchAddResumeOffset(etag, profile.startOffset)) {
            return;
        }

        if (!TextUtils.isEmpty(etag)) {
            connection.addHeader("If-Match", etag);
        }
        profile.processProfile(connection);
    }

    private void fixNeededHeader(FileDownloadConnection connection) {
        if (header == null || header.getHeaders().get("User-Agent") == null) {
            connection.addHeader("User-Agent", FileDownloadUtils.defaultUserAgent());
        }
    }

    boolean isRangeNotFromBeginning() {
        return profile.currentOffset > 0;
    }

    String getFinalRedirectedUrl() {
        if (redirectedUrlList != null && !redirectedUrlList.isEmpty()) {
            return redirectedUrlList.get(redirectedUrlList.size() - 1);
        }

        return null;
    }

    public Map<String, List<String>> getRequestHeader() {
        return requestHeader;
    }

    public ConnectionProfile getProfile() {
        return profile;
    }

    public void retryOnConnectedWithNewParam(ConnectionProfile profile, String etag)
            throws Reconnect {
        if (profile == null) throw new IllegalArgumentException();
        this.profile = profile;
        this.etag = etag;
        throw new Reconnect();
    }

    class Reconnect extends Throwable {
    }

// -----------------

    static class Builder {

        private Integer downloadId;
        private String url;
        private String etag;
        private FileDownloadHeader header;
        private ConnectionProfile connectionProfile;

        public Builder setDownloadId(int downloadId) {
            this.downloadId = downloadId;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setEtag(String etag) {
            this.etag = etag;
            return this;
        }

        public Builder setHeader(FileDownloadHeader header) {
            this.header = header;
            return this;
        }

        public Builder setConnectionProfile(ConnectionProfile model) {
            this.connectionProfile = model;
            return this;
        }

        ConnectTask build() {
            if (downloadId == null || connectionProfile == null || url == null) {
                throw new IllegalArgumentException();
            }

            return new ConnectTask(connectionProfile, downloadId, url, etag, header);
        }
    }
}
