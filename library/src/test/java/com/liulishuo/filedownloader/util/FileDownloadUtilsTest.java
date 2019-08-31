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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
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
        filename = FileDownloadUtils
                .parseContentDisposition(
                        "attachment; filename*=\"gb2312''%d4%b4%ce%c4%bc%fe.mp3\"");
        assertThat(filename).isEqualTo("源文件.mp3");
        filename = FileDownloadUtils
                .parseContentDisposition("attachment; filename*=gb2312''%d4%b4%ce%c4%bc%fe.mp3");
        assertThat(filename).isEqualTo("源文件.mp3");
        filename = FileDownloadUtils
                .parseContentDisposition(
                        "attachment; filename*=\"UTF-8''%e6%ba%90%e6%96%87%e4%bb%b6.mp3\"");
        assertThat(filename).isEqualTo("源文件.mp3");
        filename = FileDownloadUtils
                .parseContentDisposition("attachment;filename*=\"UTF8''1.mp3\"");
        assertThat(filename).isEqualTo("1.mp3");
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void findFilename_securityIssue() throws FileDownloadSecurityException {
        final FileDownloadConnection connection = mock(FileDownloadConnection.class);
        when(connection.getResponseHeaderField("Content-Disposition"))
                .thenReturn("attachment; filename=\"../abc\"");

        thrown.expect(FileDownloadSecurityException.class);
        FileDownloadUtils.findFilename(connection, "url");

        thrown.expect(FileDownloadSecurityException.class);
        when(connection.getResponseHeaderField("Content-Disposition"))
                .thenReturn("attachment; filename=\"a/b/../abc\"");
        FileDownloadUtils.findFilename(connection, "url");

        when(connection.getResponseHeaderField("Content-Disposition"))
                .thenReturn("attachment; filename=\"/abc/adb\"");
        assertThat(FileDownloadUtils.findFilename(connection, "url")).isEqualTo("/abc/adb");
    }

    @Test
    public void getFileNameFromUrl() {
        String url = "http://mirror.internode.on.net/pub/test/5meg.test5";
        assertThat(FileDownloadUtils.findFileNameFromUrl(url)).isEqualTo("5meg.test5");

        url = "http://cdn-l.llsapp.com/connett/25183b40-22f2-0133-6e99-029df5130f9e";
        assertThat(FileDownloadUtils.findFileNameFromUrl(url))
                .isEqualTo("25183b40-22f2-0133-6e99-029df5130f9e");

        url = "http://www.httpwatch.com/httpgallery/chunked/chunkedimage.aspx?0.04400023248109086";
        assertThat(FileDownloadUtils.findFileNameFromUrl(url)).isEqualTo("chunkedimage.aspx");

        url = "http://113.207.16.84/dd.myapp.com/16891/2E53C25B6BC55D3330AB85A1B7B57485.apk?mkey="
                + "5630b43973f537cf&f=cf87&fsname=com.htshuo.htsg_3.0.1_49.apk&asr=02f1&p=.apk";
        assertThat(FileDownloadUtils.findFileNameFromUrl(url))
                .isEqualTo("2E53C25B6BC55D3330AB85A1B7B57485.apk");

        url = "";
        assertThat(FileDownloadUtils.findFileNameFromUrl(url)).isNull();

        assertThat(FileDownloadUtils.findFileNameFromUrl(null)).isNull();

        assertThat(FileDownloadUtils.findFileNameFromUrl("abc")).isNull();
    }
}