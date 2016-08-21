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

import java.io.Serializable;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Throw this exception, when the HTTP status code is not {@link java.net.HttpURLConnection#HTTP_OK},
 * and not {@link java.net.HttpURLConnection#HTTP_PARTIAL} either.
 */
public class FileDownloadHttpException extends RuntimeException {
    private final int code;
    private final HeaderWrap requestHeaderWrap;
    private final HeaderWrap responseHeaderWrap;

    public FileDownloadHttpException(final Request request, final Response response) {
        super(FileDownloadUtils.formatString("response code error: %d, \n request headers: %s \n " +
                "response headers: %s", response.code(), request.headers(), response.headers()));

        this.code = response.code();
        this.requestHeaderWrap = new HeaderWrap(request.headers());
        this.responseHeaderWrap = new HeaderWrap(response.headers());
    }

    /**
     * @return the header of the current response.
     */
    public Headers getRequestHeader() {
        return this.requestHeaderWrap.getHeader();
    }

    /**
     * @return the header of the current request.
     */
    public Headers getResponseHeader() {
        return this.responseHeaderWrap.getHeader();
    }

    /**
     * @return the HTTP status code.
     */
    public int getCode() {
        return this.code;
    }

    static class HeaderWrap implements Serializable {
        private final String nameAndValuesString;
        private String[] namesAndValues;

        public HeaderWrap(final Headers headers) {
            nameAndValuesString = headers.toString();
        }

        public Headers getHeader() {
            do {
                if (namesAndValues != null) {
                    break;
                }

                if (nameAndValuesString == null) {
                    break;
                }

                synchronized (this) {
                    if (namesAndValues != null) {
                        break;
                    }

                    namesAndValues = FileDownloadUtils.convertHeaderString(nameAndValuesString);
                }
            } while (false);

            assert namesAndValues != null : "the header is empty!";
            return Headers.of(namesAndValues);
        }
    }
}