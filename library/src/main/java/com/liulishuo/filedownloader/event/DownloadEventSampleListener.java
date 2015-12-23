/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.event;

/**
 * Created by Jacksgong on 15/6/23.
 */
public class DownloadEventSampleListener extends IDownloadListener {
    private final IEventListener i;

    public DownloadEventSampleListener(IEventListener i) {
        this(0, i);
    }

    public DownloadEventSampleListener(int priority, IEventListener i) {
        super(priority);
        this.i = i;
    }

    @Override
    public boolean callback(IDownloadEvent event) {
        return i != null && i.callback(event);
    }

    public interface IEventListener {
        boolean callback(IDownloadEvent event);
    }

}
