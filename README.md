<p align="center"><img src="logo/1024px.png" alt="FileDownloader" height="200px"></p>

# FileDownloader
Android multi-task file download engine.


[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
[![Build Status][build_status_svg]][build_status_link]

> [中文文档](https://github.com/lingochamp/FileDownloader/blob/master/README-zh.md)

## FileDownloader2

Now, [FileDownloader2-OkDownload](https://github.com/lingochamp/okdownload) is released, okdownload will contain all advantage on the FileDownloader and beyond.

Because of FileDownloader unit-test coverage is very low, so all farther features and enhances will be achieved on the okdownload instead of FileDownloader, and FileDownloader will only focuses on bug fixes.

## DEMO

![][single_demo_gif]
![][chunked_demo_gif]
![][serial_tasks_demo_gif]
![][parallel_tasks_demo_gif]
![][tasks_manager_demo_gif]
![][hybrid_test_demo_gif]
![][avoid_drop_frames_1_gif]
![][avoid_drop_frames_2_gif]

## Installation

FileDownloader is installed by adding the following dependency to your `build.gradle` file:

```groovy
dependencies {
    implementation 'com.liulishuo.filedownloader:library:1.7.4'
}
```

## Open customize component

From now on, FileDownloader support following components to be customized by yourself:

| Name | Interface | Default Impl
| --- | --- | ---
| Connection | [FileDownloadConnection][FileDownloadConnection-java-link] | [FileDownloadUrlConnection][FileDownloadUrlConnection-java-link]
| OutputStream | [FileDownloadOutputStream][FileDownloadOutputStream-java-link] | [FileDownloadRandomAccessFile][FileDownloadRandomAccessFile-java-link]
| Database | [FileDownloadDatabase][FileDownloadDatabase-java-link] | [RemitDatabase][RemitDatabase-java-link]
| ConnectionCountAdapter | [ConnectionCountAdapter][ConnectionCountAdapter-java-link] | [DefaultConnectionCountAdapter][DefaultConnectionCountAdapter-java-link]
| IdGenerator | [IdGenerator][IdGenerator-java-link] | [DefaultIdGenerator][DefaultIdGenerator-java-link]

> - If you want to use okhttp as your connection component, the simplest way is [this repo](https://github.com/Jacksgong/filedownloader-okhttp3-connection).
> - If you don't want to use any database on FileDownloader(the database on FileDownloader is used for persist tasks' breakpoint info) just using [NoDatabaseImpl.java](https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/services/NoDatabaseImpl.java)

### How to valid it?

Just create your own `DownloadMgrInitialParams.InitCustomMaker` and put those customized component to it, finally init the FileDownloader with it: [FileDownloader#init](https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/FileDownloader.java#L62)

## Welcome PR

> If you can improve the unit test for this project would be great.

- Comments as much as possible.
- Commit message format follow: [AngularJS's commit message convention](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#-git-commit-guidelines) .
- The change of each commit as small as possible.

![][structure-img]
![][message-system-img]

## Usage

By default, the FileDownloadService runs on the separate process, if you want to run it on the main process, just configure on the [filedownloader.properties](https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties), and you can use `FileDownloadUtils.isDownloaderProcess(Context)` to check whether the FileDownloadService can run on the current process.

For more readable, Moved to [Wiki](https://github.com/lingochamp/FileDownloader/wiki).

## LICENSE

```
Copyright (c) 2015 LingoChamp Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[license_2_svg]: https://img.shields.io/hexpm/l/plug.svg
[android_platform_svg]: https://img.shields.io/badge/Platform-Android-brightgreen.svg
[file_downloader_svg]: https://img.shields.io/badge/Android-FileDownloader-orange.svg
[structure-img]: https://github.com/lingochamp/FileDownloader/raw/master/art/structure.png
[message-system-img]: https://github.com/lingochamp/FileDownloader/raw/master/art/message-system.png
[hybrid_test_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/hybrid_test_demo.gif
[parallel_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/parallel_tasks_demo.gif
[serial_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/serial_tasks_demo.gif
[tasks_manager_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/tasks_manager_demo.gif
[avoid_drop_frames_1_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/avoid_drop_frames1.gif
[avoid_drop_frames_2_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/avoid_drop_frames2.gif
[single_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/single_demo.gif
[chunked_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/chunked_demo.gif
[bintray_svg]: https://api.bintray.com/packages/jacksgong/maven/FileDownloader/images/download.svg
[bintray_url]: https://bintray.com/jacksgong/maven/FileDownloader/_latestVersion
[file_download_listener_callback_flow_png]: https://github.com/lingochamp/FileDownloader/raw/master/art/filedownloadlistener_callback_flow.png
[build_status_svg]: https://travis-ci.org/lingochamp/FileDownloader.svg?branch=master
[build_status_link]: https://travis-ci.org/lingochamp/FileDownloader
[FileDownloadConnection-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/connection/FileDownloadConnection.java
[FileDownloadUrlConnection-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/connection/FileDownloadUrlConnection.java
[FileDownloadDatabase-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/database/RemitDatabase.java
[RemitDatabase-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/database/RemitDatabase.java
[FileDownloadOutputStream-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/stream/FileDownloadOutputStream.java
[FileDownloadRandomAccessFile-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/stream/FileDownloadRandomAccessFile.java
[ConnectionCountAdapter-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/util/FileDownloadHelper.java#L100
[DefaultConnectionCountAdapter-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/connection/DefaultConnectionCountAdapter.java
[IdGenerator-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/util/FileDownloadHelper.java#L55
[DefaultIdGenerator-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/services/DefaultIdGenerator.java
