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

import com.liulishuo.filedownloader.download.CustomComponentHolder;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.robolectric.RuntimeEnvironment.application;

@RunWith(RobolectricTestRunner.class)
public class FileDownloaderTest {

    @Test
    public void setup_withContext_hold() {
        FileDownloader.setup(application);

        Assert.assertEquals(application.getApplicationContext(), FileDownloadHelper.getAppContext());
    }

    @Test
    public void setupOnApplicationOnCreate_withContext_hold() {
        FileDownloader.setupOnApplicationOnCreate(application);

        Assert.assertEquals(application.getApplicationContext(), FileDownloadHelper.getAppContext());
    }

    @Test
    public void setupOnApplicationOnCreate_InitCustomMaker_valid() {
        FileDownloader.setupOnApplicationOnCreate(application)
                .maxNetworkThreadCount(6)
                .commit();

        Assert.assertEquals(CustomComponentHolder.getImpl().getMaxNetworkThreadCount(), 6);
    }
}