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
 * The listener is used to listen the publish event from Event Pool.
 *
 * @see IDownloadEvent
 * @see IDownloadEventPool
 */
public abstract class IDownloadListener {

//    private final int priority;

//    public IDownloadListener(int priority) {
//        this.priority = priority;
//    }

//    public int getPriority() {
//        return this.priority;
//    }

    public abstract boolean callback(IDownloadEvent event);

}
