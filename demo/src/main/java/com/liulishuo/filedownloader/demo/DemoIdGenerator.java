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

package com.liulishuo.filedownloader.demo;

import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jacksgong on 11/06/2017.
 */

public class DemoIdGenerator implements FileDownloadHelper.IdGenerator {
    private final HashMap<String, Integer> demoIdMap = new HashMap<>();
    private final AtomicInteger increaseId = new AtomicInteger(1);

    @Override
    public int transOldId(int oldId, String url, String path, boolean pathAsDirectory) {
        return generateId(url, path, pathAsDirectory);
    }

    @Override
    public int generateId(String url, String path, boolean pathAsDirectory) {
        final String key = generateKey(url, path, pathAsDirectory);
        Integer id = demoIdMap.get(key);
        if (id == null) {
            id = increaseId.getAndIncrement();
            demoIdMap.put(key, id);
        }
        return id;
    }

    private static String generateKey(String url, String path, boolean pathAsDirectory) {
        return String.format("%s%s%B", url, path, pathAsDirectory);
    }
}
