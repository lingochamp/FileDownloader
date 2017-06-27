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

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.IThreadPoolMonitor;
import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.FileDownloadDatabase;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Iterator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadLaunchRunnableTest {

    @Test
    public void run_noWifiButRequired_callbackNetworkError() {
        // init context
        final Application spyApplication = spy(application);
        when(spyApplication.getApplicationContext()).thenReturn(spyApplication);
        FileDownloader.setupOnApplicationOnCreate(spyApplication)
                .database(getMockNonOptDatabaseMaker())
                .commit();

        // no wifi state
        mockContextNoWifiState(spyApplication);

        // pending model
        final FileDownloadModel model = mock(FileDownloadModel.class);
        when(model.getId()).thenReturn(1);
        when(model.getStatus()).thenReturn(FileDownloadStatus.pending);

        // mock launch runnable.
        final DownloadStatusCallback callback = mock(DownloadStatusCallback.class);
        final DownloadLaunchRunnable launchRunnable = DownloadLaunchRunnable.createForTest(callback,
                model, mock(FileDownloadHeader.class), mock(IThreadPoolMonitor.class),
                1000, 100, false,
                true, 0);


        launchRunnable.run();

        verify(callback).onError(any(FileDownloadNetworkPolicyException.class));
    }


    private static void mockContextNoWifiState(Context context) {
        when(context.checkCallingOrSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        final ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);

        // not wifi.
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);
    }

    private static FileDownloadHelper.DatabaseCustomMaker getMockNonOptDatabaseMaker() {
        final FileDownloadDatabase database = mock(FileDownloadDatabase.class);
        when(database.maintainer()).thenReturn(new FileDownloadDatabase.Maintainer() {
            @Override
            public void onFinishMaintain() {
            }

            @Override
            public void onRemovedInvalidData(FileDownloadModel model) {
            }

            @Override
            public void onRefreshedValidData(FileDownloadModel model) {
            }

            @Override
            public void changeFileDownloadModelId(int oldId, FileDownloadModel modelWithNewId) {
            }

            @Override
            public Iterator<FileDownloadModel> iterator() {
                return new Iterator<FileDownloadModel>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public FileDownloadModel next() {
                        return null;
                    }
                };
            }
        });

        return new FileDownloadHelper.DatabaseCustomMaker() {
            @Override
            public FileDownloadDatabase customMake() {
                return database;
            }
        };
    }
}