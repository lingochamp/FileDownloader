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
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * The FileDownloadConnection implemented using {@link URLConnection}.
 */

public class FileDownloadUrlConnection implements FileDownloadConnection {
    protected URLConnection mConnection;

    public FileDownloadUrlConnection(String originUrl, Configuration configuration) throws IOException {
        this(new URL(originUrl), configuration);
    }

    public FileDownloadUrlConnection(URL url, Configuration configuration) throws IOException {
        if (configuration != null && configuration.proxy != null) {
            mConnection = url.openConnection(configuration.proxy);
        } else {
            mConnection = url.openConnection();
        }

        if (configuration != null) {
            if (configuration.readTimeout != null) {
                mConnection.setReadTimeout(configuration.readTimeout);
            }

            if (configuration.connectTimeout != null) {
                mConnection.setConnectTimeout(configuration.connectTimeout);
            }
        }
    }

    public FileDownloadUrlConnection(String originUrl) throws IOException {
        this(originUrl, null);
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
        private final Configuration mConfiguration;

        public Creator() {
            this(null);
        }

        public Creator(Configuration configuration) {
            this.mConfiguration = configuration;
        }

        FileDownloadConnection create(URL url) throws IOException {
            return new FileDownloadUrlConnection(url, mConfiguration);
        }

        @Override
        public FileDownloadConnection create(String originUrl) throws IOException {
            return new FileDownloadUrlConnection(originUrl, mConfiguration);
        }
    }

    /**
     * The sample configuration for the {@link FileDownloadUrlConnection}
     */
    public static class Configuration {
        private Proxy proxy;
        private Integer readTimeout;
        private Integer connectTimeout;

        /**
         * The connection will be made through the specified proxy.
         * <p>
         * This {@code proxy} will be used when invoke {@link URL#openConnection(Proxy)}
         *
         * @param proxy the proxy will be applied to the {@link FileDownloadUrlConnection}
         */
        public Configuration proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies
         * the timeout when reading from Input stream when a connection is established to a resource.
         * If the timeout expires before there is data available for read, a
         * java.net.SocketTimeoutException is raised. A timeout of zero is interpreted as an
         * infinite timeout.
         * <p>
         * This {@code readTimeout} will be applied through {@link URLConnection#setReadTimeout(int)}
         *
         * @param readTimeout an <code>int</code> that specifies the timeout value to be used in
         *                    milliseconds
         */
        public Configuration readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * Sets a specified timeout value, in milliseconds, to be used when opening a communications
         * link to the resource referenced by this URLConnection.  If the timeout expires before the
         * connection can be established, a java.net.SocketTimeoutException is raised. A timeout of
         * zero is interpreted as an infinite timeout.
         * <p>
         * This {@code connectionTimeout} will be applied through {@link URLConnection#setConnectTimeout(int)}
         *
         * @param connectTimeout an <code>int</code> that specifies the connect timeout value in
         *                       milliseconds
         */
        public Configuration connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }


    }
}
