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

package com.liulishuo.filedownloader.util;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.exception.FileDownloadSecurityException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class FileDownloadUtilsTest {

    @Test
    public void parseContentDisposition() {
        String filename = FileDownloadUtils
                .parseContentDisposition("attachment; ...filename ii=\"hello world\"");
        assertThat(filename).isNull();
        filename = FileDownloadUtils
                .parseContentDisposition("attachment; filename=\"hello world\"");
        assertThat(filename).isEqualTo("hello world");
        filename = FileDownloadUtils
                .parseContentDisposition("attachment; filename=genome.jpeg\nabc");
        assertThat(filename).isEqualTo("genome.jpeg");
    }

    @Test
    public void parseContentLengthFromContentRange_withNullContentRange() {
        long length = FileDownloadUtils.parseContentLengthFromContentRange(null);
        assertThat(length).isEqualTo(-1);
    }

    @Test
    public void parseContentLengthFromContentRange_withEmptyContentRange() {
        long length = FileDownloadUtils.parseContentLengthFromContentRange("");
        assertThat(length).isEqualTo(-1);
    }

    @Test
    public void parseContentLengthFromContentRange_withStartToEndRange() {
        long length = FileDownloadUtils
                .parseContentLengthFromContentRange("bytes 25086300-37629450/37629451");
        assertThat(length).isEqualTo(12543151);
    }

    @Test
    public void parseContentLengthFromContentRange_withUnavailableContentRange() {
        long length = FileDownloadUtils.parseContentLengthFromContentRange("bytes 0-/37629451");
        assertThat(length).isEqualTo(-1);
    }

    @Test(expected = FileDownloadSecurityException.class)
    public void findFilename_securityIssue() throws FileDownloadSecurityException {
        final FileDownloadConnection connection = mock(FileDownloadConnection.class);
        when(connection.getResponseHeaderField("Content-Disposition")).thenReturn("attachment; filename=\"../abc\"");

        FileDownloadUtils.findFilename(connection, "url");
    }

}