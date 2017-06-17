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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * The connection used for connecting to the network.
 */

@SuppressWarnings("EmptyMethod")
public interface FileDownloadConnection {
    int NO_RESPONSE_CODE = 0;
    int RESPONSE_CODE_FROM_OFFSET = 1;

    /**
     * Sets the header named {@code name} to {@code value}.
     * <p>
     * The capacity of this method is similar to the {@link URLConnection#addRequestProperty(String, String)}
     */
    void addHeader(String name, String value);

    /**
     * If we find the file has been downloaded several bytes, we will try to resume from the
     * breakpoint from {@code offset} length.
     *
     * @param etag   the etag is stored by the past downloaded.
     * @param offset the offset length has already been downloaded.
     * @return {@code true} if adding resume offset was dispatched, so we can't handle that by internal.
     */
    @SuppressWarnings("UnusedParameters")
    boolean dispatchAddResumeOffset(String etag, long offset);

    /**
     * Returns an input stream that reads from this open connection.
     * <p>
     * The capacity of this method is similar to the {@link URLConnection#getInputStream()}
     *
     * @return an input stream that reads from this open connection.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns an unmodifiable Map of general request header fields for this connection. The Map
     * keys are Strings that represent the request-header field names. Each Map value is a
     * unmodifiable List of Strings that represents the corresponding field values.
     * <p>
     * The capacity of this method is similar to the {@link URLConnection#getRequestProperties()}
     *
     * @return a Map of the general request properties for this connection.
     */
    Map<String, List<String>> getRequestHeaderFields();

    /**
     * Returns an unmodifiable Map of the header fields. The Map keys are Strings that represent
     * the response-header field names. Each Map value is an unmodifiable List of Strings that
     * represents the corresponding field values.
     * <p>
     * The capacity of this method is similar to the {@link URLConnection#getHeaderFields()}
     *
     * @return a Map of header fields
     */
    Map<String, List<String>> getResponseHeaderFields();

    /**
     * Returns the value of the named header field, which would be the response-header field.
     * <p>
     * If called on a connection that sets the same header multiple times
     * with possibly different values, only the last value is returned.
     *
     * @param name the name of a header field.
     * @return the value of the named header field, or <code>null</code>
     * if there is no such field in the header.
     */
    String getResponseHeaderField(String name);

    /**
     * Invokes the request immediately, and blocks until the response can be processed or is in
     * error.
     */
    void execute() throws IOException;

    /**
     * Gets the status code from an HTTP response message.
     * <p>
     * <strong>If this is not http/https protocol connection</strong>:
     * 1. If you make sure this connection is resume from the offset breakpoint(which you can check
     * out this through {@link #dispatchAddResumeOffset(String, long)}), please return
     * {@link #RESPONSE_CODE_FROM_OFFSET}
     * 2. otherwise, return {@link #NO_RESPONSE_CODE}.
     *
     * @return the HTTP Status-Code, or -1
     */
    int getResponseCode() throws IOException;

    /**
     * To Be Reused or Close this connection, since this connection is ending in this session.
     */
    void ending();
}
