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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("CanBeFinal")
public class FileDownloadUrlConnectionTest {

    @Mock
    private Proxy mProxy;

    @Mock
    private URLConnection mConnection;

    @Mock
    private URL mURL;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        Mockito.when(mURL.openConnection()).thenReturn(mConnection);
        Mockito.when(mURL.openConnection(mProxy)).thenReturn(mConnection);
    }

    @Test
    public void construct_noConfiguration_noAssigned() throws IOException {
        FileDownloadUrlConnection.Creator creator = new FileDownloadUrlConnection.Creator();

        creator.create("http://blog.dreamtobe.cn");

        verify(mConnection, times(0)).setConnectTimeout(anyInt());
        verify(mConnection, times(0)).setReadTimeout(anyInt());
    }

    @Test
    public void construct_validConfiguration_Assigned() throws IOException {


        FileDownloadUrlConnection.Creator creator = new FileDownloadUrlConnection.Creator(
                new FileDownloadUrlConnection.Configuration()
                        .proxy(mProxy)
                        .connectTimeout(1001)
                        .readTimeout(1002)
        );

        creator.create(mURL);

        verify(mURL).openConnection(mProxy);
        verify(mConnection).setConnectTimeout(1001);
        verify(mConnection).setReadTimeout(1002);
    }


}