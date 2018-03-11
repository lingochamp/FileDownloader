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

import com.liulishuo.filedownloader.connection.FileDownloadConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("ALL")
@RunWith(RobolectricTestRunner.class)
public class DownloadRunnableTest {

    @Mock
    private ConnectTask mockConnectTask;

    private ProcessCallback mockCallback;

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private Exception mockIOException = new IOException("test");

    private DownloadRunnable downloadRunnable;

    @Before
    public void setUp() {
        initMocks(this);

        mockCallback = spy(new MockProcessCallback());
        downloadRunnable = new DownloadRunnable.Builder()
                .setCallback(mockCallback)
                .buildForTest(mockConnectTask);

        when(mockConnectTask.getProfile()).thenReturn(mock(ConnectionProfile.class));
    }

    @Test
    public void run_withConnectFailed_retry() throws IOException, IllegalAccessException {
        when(mockConnectTask.connect()).thenThrow(mockIOException);

        downloadRunnable.run();

        verify(mockCallback).onRetry(mockIOException);
    }

    @Test
    public void run_responseCodeNotMet_error() throws IOException, IllegalAccessException {
        final FileDownloadConnection connection = mock(FileDownloadConnection.class);
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PRECON_FAILED);
        when(mockConnectTask.connect()).thenReturn(connection);

        downloadRunnable.run();

        // retry first.
        verify(mockCallback).onRetry(any(Exception.class));

        // then callback error.
        verify(mockCallback).onError(any(Exception.class));
    }

    private static class MockProcessCallback implements ProcessCallback {

        @Override
        public void onProgress(long increaseBytes) {
        }

        @Override
        public void onCompleted(DownloadRunnable doneRunnable, long startOffset, long endOffset) {
        }

        boolean isFirstTime = true;

        @Override
        public boolean isRetry(Exception exception) {
            if (isFirstTime) {
                isFirstTime = false;
                return true;
            }

            return false;
        }

        @Override
        public void onError(Exception exception) {
        }


        @Override
        public void onRetry(Exception exception) {
        }

        @Override
        public void syncProgressFromCache() {
        }
    }
}