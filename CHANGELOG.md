# Change log

> [中文迭代日志](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG-ZH.md)

### Version 0.1.4

#### New Interfaces

- `FileDownloader#unBindServiceIfIdle(void)`: If there is no active task in the `:filedownloader` progress currently , then unbind & stop `:filedownloader` process
- `FileDownloader#getStatus(downloadId)`: Get download status by the downloadId(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java)
- `FileDownloader#isServiceConnected(void)`: Whether started and connected to the `:filedownloader` progress(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))

#### Enhancement

- Supported [Chunked transfer encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) data download(Recommend to glance at demo on [Single Task Test](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/SingleTaskTestActivity.java)).
- Improve Performance: Reduce IPC.
- Improve Performance: Reduce lock.
- Improve Performance: Delete invalid datas in db with the `:filedownloader` progress start.
- Improve Performance: Ignore the `callbackProgressTimes` column in db.

#### Fix

- Fix `FileDownloader#pauseAll` not effect in case of low memory and ui progress is Background Progress situation and the `:filedownloader` progress(Service Progress) alive and still have running tasks in the `filedownloader` progress but ui progress has died and relived.
- Fix not release connect resources when invoke `FileDownloader#unBinderService` manually.
- Handle case of ui progress be killed by sys and download progress not be killed, and ui progress relives and reexecutes same tasks queue.


### Version 0.1.3

- Enhancement: no longer subject to the upper bound of 1.99G, add `FileDownloadLargeFileListener`, `getLargeFileSoFarBytes()`,`getLargeFileTotalBytes()`.
- Performance optimization: some ipc transaction just need one-way call(async), not block(sync).
- Upgrade dependency okhttp from 2.7.0 to 2.7.1.

### Version 0.1.2

- Optimize thread digestion([map](https://github.com/lingochamp/FileDownloader/raw/master/art/filedownload_sample_description.png).
- Fix: may `pause()` invalid in large queue task.
- Fix: large queue task parallel download, may download has been completed but the callback

### Version 0.1.1

- Optimization of internal performance, according to the time split thread pool.
- Add auto retry feature.

### Version 0.1.0

- The `FileDownloadStatus` parameter type is changed from `int` to `byte`, which is frequently copied in IPC.
- Optimization of multi task queue filtering time.
- Optimizing serial task execution mechanism.

### Version 0.0.9

- The start operation into independent thread processing, sharing thread pool in EventPool.

### Version 0.0.8

- initial release
