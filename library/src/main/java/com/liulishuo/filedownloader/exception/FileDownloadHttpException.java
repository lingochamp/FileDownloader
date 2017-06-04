/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liulishuo.filedownloader.exception;

import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Throw this exception, when the HTTP status code is not {@link java.net.HttpURLConnection#HTTP_OK},
 * and not {@link java.net.HttpURLConnection#HTTP_PARTIAL} either.
 */
public class FileDownloadHttpException extends IOException {
    private final int mCode;
    private final Map<String, List<String>> mRequestHeaderMap;
    private final Map<String, List<String>> mResponseHeaderMap;

    public FileDownloadHttpException(final int code,
                                     final Map<String, List<String>> requestHeaderMap,
                                     final Map<String, List<String>> responseHeaderMap) {
        super(FileDownloadUtils.formatString("response code error: %d, \n request headers: %s \n " +
                "response headers: %s", code, requestHeaderMap, responseHeaderMap));

        this.mCode = code;
        this.mRequestHeaderMap = cloneSerializableMap(requestHeaderMap);
        this.mResponseHeaderMap = cloneSerializableMap(requestHeaderMap);
    }

    /**
     * @return the header of the current response.
     */
    public Map<String, List<String>> getRequestHeader() {
        return this.mRequestHeaderMap;
    }

    /**
     * @return the header of the current request.
     */
    public Map<String, List<String>> getResponseHeader() {
        return this.mResponseHeaderMap;
    }

    /**
     * @return the HTTP status code.
     */
    public int getCode() {
        return this.mCode;
    }

    private static Map<String, List<String>> cloneSerializableMap(final Map<String, List<String>> originMap) {
        final Map<String, List<String>> serialMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : originMap.entrySet()) {
            final String key = entry.getKey();
            final List<String> values = new ArrayList<>(entry.getValue());
            serialMap.put(key, values);
        }

        return serialMap;
    }
}