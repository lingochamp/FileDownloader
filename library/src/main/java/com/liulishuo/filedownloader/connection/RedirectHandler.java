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

import com.liulishuo.filedownloader.download.CustomComponentHolder;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handle redirect case.
 */
public class RedirectHandler {

    private static final int MAX_REDIRECT_TIMES = 10;

    /**
     * The target resource resides temporarily under a different URI and the user agent MUST NOT
     * change the request method if it performs an automatic redirection to that URI.
     */
    private static final int HTTP_TEMPORARY_REDIRECT = 307;
    /**
     * The target resource has been assigned a new permanent URI and any future references to this
     * resource ought to use one of the enclosed URIs.
     */
    private static final int HTTP_PERMANENT_REDIRECT = 308;


    public static FileDownloadConnection process(
            final Map<String, List<String>> requestHeaderFields,
            final FileDownloadConnection connection,
            List<String> redirectedUrlList)
            throws IOException, IllegalAccessException {

        int code = connection.getResponseCode();
        String location = connection.getResponseHeaderField("Location");

        List<String> redirectLocationList = new ArrayList<>();
        int redirectTimes = 0;
        FileDownloadConnection redirectConnection = connection;

        while (isRedirect(code)) {
            if (location == null) {
                throw new IllegalAccessException(FileDownloadUtils.
                        formatString(
                                "receive %d (redirect) but the location is null with response [%s]",
                                code, redirectConnection.getResponseHeaderFields()));
            }

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(RedirectHandler.class, "redirect to %s with %d, %s",
                        location, code, redirectLocationList);
            }

            redirectConnection.ending();
            redirectConnection =
                    buildRedirectConnection(requestHeaderFields, location);
            redirectLocationList.add(location);

            redirectConnection.execute();
            code = redirectConnection.getResponseCode();
            location = redirectConnection.getResponseHeaderField("Location");

            if (++redirectTimes >= MAX_REDIRECT_TIMES) {
                throw new IllegalAccessException(
                        FileDownloadUtils
                                .formatString("redirect too many times! %s", redirectLocationList));
            }
        }

        if (redirectedUrlList != null) {
            redirectedUrlList.addAll(redirectLocationList);
        }

        return redirectConnection;
    }

    private static boolean isRedirect(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == HttpURLConnection.HTTP_MULT_CHOICE
                || code == HTTP_TEMPORARY_REDIRECT
                || code == HTTP_PERMANENT_REDIRECT;
    }

    private static FileDownloadConnection buildRedirectConnection(
            Map<String, List<String>> requestHeaderFields,
            String newUrl) throws IOException {
        FileDownloadConnection redirectConnection = CustomComponentHolder.getImpl().
                createConnection(newUrl);

        String name;
        List<String> list;

        Set<Map.Entry<String, List<String>>> entries = requestHeaderFields.entrySet();
        for (Map.Entry<String, List<String>> e : entries) {
            name = e.getKey();
            list = e.getValue();
            if (list != null) {
                for (String value : list) {
                    redirectConnection.addHeader(name, value);
                }
            }
        }

        return redirectConnection;
    }
}
