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
public abstract class IDownloadEvent {
    public Runnable callback = null;

    public IDownloadEvent(final String id) {
        this.id = id;
    }

    public IDownloadEvent(final String id, boolean order) {
        this.id = id;
        this.order = order;
    }

    protected final String id;
    protected boolean order;

    public final String getId() {
        return this.id;
    }

    public boolean getOrder() {
        return this.order;
    }
}
