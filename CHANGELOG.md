# Change log

> [中文迭代日志](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG-ZH.md)

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