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

package com.liulishuo.filedownloader;

import android.app.Application;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

/**
 * Created by Jacksgong on 28/06/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadTaskTest {

    @Before
    public void setup() {
        final Application spyApplication = spy(application);
        when(spyApplication.getApplicationContext()).thenReturn(spyApplication);

        FileDownloader.setup(spyApplication);

        when(spyApplication.checkCallingUriPermission(any(Uri.class), anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED);
    }

    @Test
    public void setPath_hasUri_assignUriToNull() throws Exception {
        DownloadTask task = new DownloadTask("mock url");
        task.setUri(mock(Uri.class));

        task.setPath("mock path");

        Assert.assertNull(task.getUri());
    }

    @Test
    public void setUri_hasPath_assignPathToNull() throws Exception {
        DownloadTask task = new DownloadTask("mock url");
        task.setPath("mock path", true);

        task.setUri(mock(Uri.class));


        Assert.assertNull(task.getPath());
        Assert.assertFalse(task.isPathAsDirectory());
    }

}