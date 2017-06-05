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

/**
 * The interface for the downloading speed.
 */

public interface IDownloadSpeed {

    /**
     * The downloading monitor, used for calculating downloading speed.
     */
    interface Monitor {
        /**
         * Start the monitor.
         */
        void start(long startBytes);

        /**
         * End the monitor, and calculate the average speed during the entire downloading processing.
         *
         * @param sofarBytes The so far downloaded bytes.
         */
        void end(final long sofarBytes);

        /**
         * Refresh the downloading speed.
         *
         * @param sofarBytes The so far downloaded bytes.
         */
        void update(long sofarBytes);

        /**
         * Reset the monitor.
         */
        void reset();

    }

    /**
     * For lookup the downloading speed data.
     */
    interface Lookup {
        /**
         * @return The currently downloading speed when the task is running.
         * The average speed when the task is finished.
         */
        int getSpeed();

        /**
         * @param minIntervalUpdateSpeed The minimum interval to update the speed, used to adjust the
         *                               refresh frequent.
         */
        void setMinIntervalUpdateSpeed(int minIntervalUpdateSpeed);
    }
}
