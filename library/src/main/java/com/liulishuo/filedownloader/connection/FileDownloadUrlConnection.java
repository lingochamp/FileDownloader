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

package com.liulishuo.filedownloader.connection;

import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * The FiledownloadConnection implmented using {@link URLConnection}.
 */

public class FileDownloadUrlConnection implements FileDownloadConnection {
    private URLConnection mConnection;

    public FileDownloadUrlConnection(String originUrl) throws IOException {
        mConnection = new URL(originUrl).openConnection();
    }

    @Override
    public void addHeader(String name, String value) {
        mConnection.addRequestProperty(name, value);
    }

    @Override
    public boolean dispatchAddResumeOffset(String etag, long offset) {
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return mConnection.getInputStream();
    }

    @Override
    public Map<String, List<String>> getRequestHeaderFields() {
        return mConnection.getRequestProperties();
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        return mConnection.getHeaderFields();
    }

    @Override
    public String getResponseHeaderField(String name) {
        return mConnection.getHeaderField(name);
    }

    @Override
    public void execute() throws IOException {
        mConnection.connect();
    }

    @Override
    public int getResponseCode() throws IOException {
        if (mConnection instanceof HttpURLConnection) {
            return ((HttpURLConnection) mConnection).getResponseCode();
        }

        return FileDownloadConnection.NO_RESPONSE_CODE;
    }

    @Override
    public void ending() {
        // for reuse,so do nothing.
    }

    public static class Creator implements FileDownloadHelper.ConnectionCreator {

        @Override
        public FileDownloadConnection create(String url) throws IOException {
            return new FileDownloadUrlConnection(url);
        }
    }
}
