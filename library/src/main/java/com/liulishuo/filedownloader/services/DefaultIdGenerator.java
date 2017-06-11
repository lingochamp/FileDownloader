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

package com.liulishuo.filedownloader.services;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import static com.liulishuo.filedownloader.util.FileDownloadUtils.formatString;

/**
 * The default id generator.
 */

public class DefaultIdGenerator implements FileDownloadHelper.IdGenerator {

    @Override
    public int transOldId(int oldId, String url, String path, boolean pathAsDirectory) {
        return generateId(url, path, pathAsDirectory);
    }

    @Override
    public int generateId(String url, String path, boolean pathAsDirectory) {
        if (pathAsDirectory) {
            return FileDownloadUtils.md5(formatString("%sp%s@dir", url, path)).hashCode();
        } else {
            return FileDownloadUtils.md5(formatString("%sp%s", url, path)).hashCode();
        }
    }
}
