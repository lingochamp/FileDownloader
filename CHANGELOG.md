# Change log

> [中文迭代日志](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG-ZH.md)

## Version 0.3.2

_2016-06-12_

#### New Interfaces

- Add `BaseDownloadTask#setCallbackProgressMinInterval`: Set the minimum time interval between each callback of 'progress'. Closes #167.
- Add `FileDownloader#setMaxNetworkThreadCount`: Change the number of simultaneous downloads(the number of the simultaneously running network threads) at the code side. Closes #168.
- Add `FileDownloader#init(Context,OkHttpClientCustomMaker,int)`: Accept initializing the number of simultaneous downloads(the number of the simultaneously running network threads) with the FileDownloadService initializes. Closes #168.

#### Enhancement

- Improve Robust: Ensure the minimum time interval between each callback of 'progress' is 5ms, To prevent internal callback of 'progress' too frequent happening. Closes #167.
- Improve Practicability: Print the 'warn' priority log when a request does something in the FileDownloadService but it isn't connected yet.
- Improve Performance: Using the `SparseArray` instead of `HashMap` for mapping all `FileDownloadModel`.

#### Fix

- Fix(crash): Fix provided wrong params in formatting character string when to starting download runnable occur the unexpected downloading status.
- Fix(force-re-download): Fix the wrong logic: In the case of `BaseDownloadTask#setForceReDownload(true)` and the task has already downloaded will trigger 'warn' callback. Closes #169 .
- Fix(class-type): Keep the class type of `SocketTimeOutException`, and no longer care about whether the message of Throwable is empty, this is very redundant.

#### Others

- Upgrade dependency okhttp from `3.2.0` to `3.3.1`.

## Version 0.3.1

_2016-05-19_

#### Enhancement

- Improve Robust: Ensuring buffer is written out to the device when at the end of fetching data.

## Version 0.3.0

_2016-05-13_

#### Fix

> Why FileDownload can run in UI process? Ref [filedownloader.properties](https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties).

- Fix(shared-UI-process): fix the addition header does not attach to Http-request when the FileDownload service isn't running in the separate process to UI process. Closes #149.


## Version 0.2.9

_2016-05-10_

#### New Interfaces

- Add `BaseDownloadTask#isUsing():boolean`: Whether this task object has already started and used in FileDownload Engine. Closes #137 .

#### Fix

- Fix(high-concurrency-npe): Providing the default snapshot when a task's status is unexpected, preventing the npe is occurred in this case.
- Fix(response-416): Covering the response status code is 416 or still resume from breakpoint when its so far bytes more than or equal to total bytes.

## Version 0.2.8

_2016-05-02_

#### New Interfaces

- Add `BaseDownloadTask#getId():int`: deprecate `getDownloadId()`, and using the `getId()` instead, for `BaseDownloadTask`.

#### Enhancement

- Improve Robust: Refactor the launcher for launching tasks more make sense, and expire tasks with listener or expire all waiting-tasks more stable.
- Improve Robust: Refactor the architecture which is used to handle the event send to `FileDownloadListener`, the new architecture just like a messenger and message-station, each tasks would write snapshot messages to message-station.
- Improve Robust: Cover all high concurrent situations about pausing a task, remove some expected warn logs about it.
- Improve Performance: Reduce the FileDownloader database I/O.
- Improve Performance: Reduce creating object(less allocating memory request, friendly to GC) for each call-back, Taking a message snapshot for a status updating, and through whole communication architecture just use it.

#### Fix

- Fix: Provide the definite locale for formatting strings, prevent unexpected-locale as Default happening. Closes #127

## Version 0.2.7

_2016-04-22_

#### New Interfaces

- Add `FileDownloader#setTaskCompleted(taskAtomList:List<FileDownloadTaskAtom>)`: Used to telling the FileDownloader Engine that a bulk of tasks have already downloaded by other ways.

#### Enhancement

- Improve Robust: Throw the Fatal-Exception directly when request to bind the FileDownloadService in the `:filedownloader` process. Closes #119 .

## Version 0.2.6

_2016-04-20_

#### New Interfaces

- Adjust: Change the location of the `filedownloader.properties` ，no more in the root directory of project, instead below the `assets` of a module, for example `/demo/src/main/assets/filedownloader.properties`.

#### Fix

- Fix: `filedownloader.properties` not work. Closes #117.

## Version 0.2.5

_2016-04-19_

#### New Interfaces

- Add `FileDownloader#setTaskCompleted`: Used to telling the FileDownloader Engine that the task with the url and the path has already completed downloading by other ways(not by FileDownloader Engine).
- Support the configuration `download.max-network-thread-count` in `filedownloader.properties`: The maximum network thread count for downloading simultaneously, default is 3. Closes #116.

## Version 0.2.4

_2016-04-18_

#### New Interfaces

- Add `BaseDownloadTask#getSpeed` and `BaseDownloadTask#setMinIntervalUpdateSpeed`: Get the download speed for a task. If it is in processing, the speed would be real-time speed; If finished, the speed would be average speed. Closes #95
- Add the `FileDownloader#startForeground` and `FileDownloader#stopForeground` for supporting the Foreground mode([Service#startForeground](https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties)); For ensure the FileDownloadService would keep alive when user removed the App from the recent apps. Closes #110 .
- Support configurations `download.min-progress-step` and `download.min-progress-time`: The min buffered so far bytes and millisecond, used for adjudging whether is time to sync the download so far bytes to database and make sure sync the downloaded buffers to the local file. More small more frequent, then download more slowly, but will safer in the scene of the process is killed unexpectedly. Default 65536(MinProgressStep) and 2000(MinProgressTime), which follow the value in `com.android.providers.downloads.Constants`.
- Support the configuration `process.non-separate` in `filedownloader.properties`: The FileDownloadService runs in the separate process ':filedownloader' as default, if you want to run the FileDownloadService in the main process, set this configuration as `true`. Closes #106 .

#### Enhancement

- Improve Performance: Download more quickly, Optimize the strategy about sync the buffered datum to database and local file when processing. Closes #112 .

#### Fix

- Fix: Can't restart the task which in paused but is still settling in the download-pool. Closes #111

## Version 0.2.3

_2016-04-11_

#### New Interfaces

- Add `FileDownloadOutOfSpaceException`, Throw this exception, when the file will be downloaded is too large to store.
- Add new call-back method in `FileDownloadListener`: `started` which will be invoked when finish pending, and start the download runnable.
- Add new call-back method in `FileDownloadMonitor.IMonitor`: `onTaskStarted` which will be invoked when finish pending, and start the download runnable.

#### Enhancement

- Improve Practicability: Provide the current task to the method `over` in `FinishListener`, for recognizing target task in case of one-FinishListener for more than one task. Closes #69 .
- Improve Robust: Throw the exception directly when invoke `BaseDownloadTask#start` for a running-task object, add provide 'reuse' method to reuse a used and already finished task object. Closes #91 .
- Improve Performance: Intercept the enqueue operate for the otiose event which is no listener for handling it.

#### Fix

- Fix: In handful cases the task-call-back flow not follow the expect.
- Fix: `progress` call-back included the ending frame ( `sofarBytes == totalBytes` ).
- Fix: Carry back the total bytes in the status of warn, for covering the case of UI-process had killed but has restarted App with restarting the task and download-process is alive still, the total bytes is 0 in UI-process. Closes #90 .
- Fix: Can't call-back 'retry' in expect, the case of the call-back method 'retry' one-by-one. Refs: #91 .
- Fix: The wrong sofar bytes will cover the right one, when occur error in no-network and has chance to retry. Closes #92 .
- Fix: Handle the case of the downloading is finished during the 'check-reuse' to 'check-downloading' in filedownloader-process.
- Fix: The serial-queue converts to The parallel-queue in restoring from filedownloader-process has killed and restarting.

## Version 0.2.2

_2016-04-06_

#### New Interfaces

- Add `FileDownloadHttpException` and `FileDownloadGiveUpRetryException`, and optimize the mechanism of exception. Closes #67 .
- Init the `FileDownloader` use `Context` instead of `Application` ( `FileDownloader#init(Context)` ) , for more make sense and unit-test. Closes #54 .

#### Enhancement

- Improve Robust: Check whether free space is enough, and throw IOException directly when not enough; And pre-allocate need-available-space before fetching datum when the free space more than need-available-space. Closes #46 .
- Improve Practicability: Support resume from breakpoint without ETag. Just need the server support the request-header param 'Range'. Close #35 , #66 .


#### Fix

- Fix: The `IllegalFormatConversionException` on `EventPool` when publishing the event which does not in effect and `FileDownloadLog.NEED_LOG` is `true`. Closes #30 .
- Fix: The non-fatal-crash in `IFileDownloadIPCService.java` , when lost connection from filedownloader process. because the IBinder's hosting process(filedownloader process) has been killed/cancelled. Closes #38 .
- Fix: The leak of response-body: 'WARNING: A connection to https://... was leaked. Did you forget to close a response body?' Closes #68 .
- Fix: Using the internal-string as synchronized lock-object instead of string-original.
- Fix: The number of the Ing-call-back is not correct in some cases.

#### Others

- Upgrade dependency okhttp from `3.1.2` to `3.2.0`.

## Version 0.2.0

_2016-02-15_

#### New Interfaces

- `filedownloader.properties-http.lenient`: Add 'filedownloader.properties' for some special global configs, and add 'http.lenient' keyword to 'filedownloader.properties' to handle the case of want to ignore HTTP response header from download file server isn't legal.
- `FileDownloadNotificationHelper`: Refashioning NotificationHelper, let handle notifications with FileDownloader more make sense. #25
- `FileDownloader#init(Application,OkHttpClientCustomMaker)`: Support customize OkHttpClient which will be used for downloading files.

#### Fix

- Fix: Occur 'Concurrent Modification Exception' when Downloader service is unbound or lost connection to service and NeedRestart list not empty. #23
- Fix: The case of re-connect from lost connection to service but all auto restart tasks' call-back do not effect.
- Fix: In some cases of high concurrency, the Pause on some tasks is no effect.

## Version 0.1.9

_2016-01-23_

> FileDownloader is enable Avoid Missing Screen Frames as default, if you want to disable it, please invoke `FileDownloader.getImpl().disableAvoidDropFrame()`.

#### New Interfaces

> We default open Avoid Missing Screen Frames, if you want to disable it(will post to ui thread for each FileDownloadListener event achieved as pre version), please invoke: `FileDownloader.getImpl().disableAvoidDropFrame()`.

- `FileDownloadMonitor`: You can add the global monitor for Statistic/Debugging now.
- `FileDownloader#enableAvoidDropFrame(void)`: Avoid missing screen frames, but this leads to all callbacks of FileDownloadListener do not be invoked at once when it has already achieved.
- `FileDownloader#disableAvoidDropFrame(void)`: Disable avoid missing screen frames, let all callbacks of FileDownloadListener be invoked at once when it achieve.
- `FileDownloader#isEnabledAvoidDropFrame(void)`: Has already enabled Avoid Missing Screen Frames. Default: true
- `FileDownloader#setGlobalPost2UIInterval(intervalMillisecond:int)`: For Avoid Missing Screen Frames. Each intervalMillisecond post 1 message to ui thread at most. if the value is less than 0, each callback will always post a message to ui thread immediately, may will cause missing screen frames and produce great pressure on the ui thread Looper. Default: 10ms.
- `FileDownloader#setGlobalHandleSubPackageSize(packageSize:int)`: For Avoid Missing Screen Frames. {packageSize}: The number of FileDownloadListener's callback contained in each message. value completely dependent on the intervalMillisecond of setGlobalPost2UIInterval, describe will handle up to {packageSize} callbacks on the each message posted to ui thread. Default: 5.
- `BaseDownloadTask#setSyncCallback(syncCallback:boolean)`: if true will invoke callbacks of FileDownloadListener directly on the download thread(do not post the message to the ui thread), default false.
- `BaseDownloadTask#isSyncCallback(void):boolean`: Whether sync invoke callbacks of FileDownloadListener directly on the download thread.
- `FileDownloadUtils#setDefaultSaveRootPath`: The path is used as Root Path in the case of task without setting path in the entire Download Engine.
- `FileDownloadQueueSet`: In order to be more convenient to bind multiple tasks to a queue, and to the overall set.

#### Enhancement

- Improve Debugging: Provide the `FileDownloadMonitor` to monitor entire Download Engine.
- Improve Performance: Optimize EventPool lock & do not handle listener priority any more(no use internal).
- Improve Performance: Call `FileDownloadListener` methods do not through EventPool, instead, invoke directly.

#### Fix

- Fix: EventPool listener unlimited increased bug.

## Version 0.1.5

_2016-01-17_

#### New Interfaces

- `BaseDownloadTask#setTag(key:int, tag:Object)`: Set a tag associated with this task. If the key already existed, the old tag will be replaced.
- `BaseDownloadTask#getTag(key:int)`: Get the object stored in the task as a tag, or null if not set.
- `BaseDownloadTask#addHeader(name:String, values:String)`: Add custom request header to the task. Attention: We have already handled ETag, and will add `If-Match` & `Range` value if it works.
- `BaseDownloadTask#addHeader(line:String)`: Add custom request header to the task. Attention: We have already handled ETag, and will add `If-Match` & `Range` value if it works.
- `BaseDownloadTask#removeAllHeaders(name:String)`: Remove all custom request header bind with the `{name}`.

#### Enhancement

- Improve Performance: Reduce the consumption of the generated log.
- Improve Debugging: To filter all the log level, reduce the high level of log output, and by default, will output `Warn`、`Error`、`Assert` level of log in order to debugging in the case of the value of `FileDownloadLog.NEED_LOG` is false(default).

#### Fix

- Fix can't resume from the break point naturally in case of the download status of the task is Error.
- Fix the size of the queue may not match the number of actual active tasks in case of high concurrency. This bug may would caused some callbacks to be consumed by the old tasks.

#### Others

- Upgrade dependency okhttp from `2.7.1` to `3.0.1`.

## Version 0.1.4

_2016-01-13_

#### New Interfaces

- `FileDownloader#unBindServiceIfIdle(void)`: If there is no active task in the `:filedownloader` progress currently , then unbind & stop `:filedownloader` process
- `FileDownloader#getStatus(downloadId)`: Get download status by the downloadId(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java)
- `FileDownloader#isServiceConnected(void)`: Whether started and connected to the `:filedownloader` progress(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))

#### Enhancement

- Supported [Chunked transfer encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) data download(Recommend to glance at demo on [Single Task Test](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/SingleTaskTestActivity.java)).
- Improve Performance: Reduce IPC.
- Improve Performance: Reduce lock.
- Improve Performance: Delete invalid datum in db with the `:filedownloader` progress start.
- Improve Performance: Ignore the `callbackProgressTimes` column in db.

#### Fix

- Fix `FileDownloader#pauseAll` not effect in case of low memory and ui progress is Background Progress situation and the `:filedownloader` progress(Service Progress) alive and still have running tasks in the `filedownloader` progress but ui progress has died and relived.
- Fix not release connect resources when invoke `FileDownloader#unBinderService` manually.
- Handle case of ui progress be killed by sys and download progress not be killed, and ui progress relives and re-executes same tasks queue.


## Version 0.1.3

_2016-01-04_

- Enhancement: no longer subject to the upper bound of 1.99G, add `FileDownloadLargeFileListener`, `getLargeFileSoFarBytes()`,`getLargeFileTotalBytes()`.
- Performance optimization: some ipc transaction just need one-way call(async), not block(sync).
- Upgrade dependency okhttp from `2.7.0` to `2.7.1`.

## Version 0.1.2

_2015-12-27_

- Optimize thread digestion([map](https://github.com/lingochamp/FileDownloader/raw/master/art/filedownload_sample_description.png).
- Fix: may `pause()` invalid in large queue task.
- Fix: large queue task parallel download, may download has been completed but the callback

## Version 0.1.1

_2015-12-25_

- Optimization of internal performance, according to the time split thread pool.
- Add auto retry feature.

## Version 0.1.0

_2015-12-24_

- The `FileDownloadStatus` parameter type is changed from `int` to `byte`, which is frequently copied in IPC.
- Optimization of multi task queue filtering time.
- Optimizing serial task execution mechanism.

## Version 0.0.9

_2015-12-23_

- The start operation into independent thread processing, sharing thread pool in EventPool.

## Version 0.0.8

_2015-12-22_

- initial release
