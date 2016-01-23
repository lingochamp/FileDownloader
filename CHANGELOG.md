# Change log

> [中文迭代日志](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG-ZH.md)

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
