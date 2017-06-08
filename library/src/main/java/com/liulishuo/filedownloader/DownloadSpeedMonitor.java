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

package com.liulishuo.filedownloader;

import android.os.SystemClock;

/**
 * The downloading speed monitor.
 */

public class DownloadSpeedMonitor implements IDownloadSpeed.Monitor, IDownloadSpeed.Lookup {

    private long mLastRefreshTime;
    private long mLastRefreshSofarBytes;
    private long mStartSofarBytes;
    private long mStartTime;
    // KB/s
    private int mSpeed;

    private long mTotalBytes;

    // The min interval millisecond for updating the download mSpeed.
    private int mMinIntervalUpdateSpeed = 1000;

    @Override
    public void start(long startBytes) {
        this.mStartTime = SystemClock.uptimeMillis();
        this.mStartSofarBytes = startBytes;
    }

    @Override
    public void end(long sofarBytes) {
        if (mStartTime <= 0) {
            return;
        }

        long downloadSize = sofarBytes - mStartSofarBytes;
        this.mLastRefreshTime = 0;
        long interval = SystemClock.uptimeMillis() - mStartTime;
        if (interval <= 0) {
            mSpeed = (int) downloadSize;
        } else {
            mSpeed = (int) (downloadSize / interval);
        }
    }

    @Override
    public void update(long sofarBytes) {
        if (mMinIntervalUpdateSpeed <= 0) {
            return;
        }

        boolean isUpdateData = false;
        do {
            if (mLastRefreshTime == 0) {
                isUpdateData = true;
                break;
            }

            long interval = SystemClock.uptimeMillis() - mLastRefreshTime;
            if (interval >= mMinIntervalUpdateSpeed || (mSpeed == 0 && interval > 0)) {
                mSpeed = (int) ((sofarBytes - mLastRefreshSofarBytes) / interval);
                mSpeed = Math.max(0, mSpeed);
                isUpdateData = true;
                break;
            }
        } while (false);

        if (isUpdateData) {
            mLastRefreshSofarBytes = sofarBytes;
            mLastRefreshTime = SystemClock.uptimeMillis();
        }

    }

    @Override
    public void reset() {
        this.mSpeed = 0;
        this.mLastRefreshTime = 0;
    }

    @Override
    public int getSpeed() {
        return this.mSpeed;
    }

    @Override
    public void setMinIntervalUpdateSpeed(int minIntervalUpdateSpeed) {
        this.mMinIntervalUpdateSpeed = minIntervalUpdateSpeed;
    }
}
