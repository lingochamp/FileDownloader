# FileDownloader
Android multi-task file download engine.


[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
[![Build Status][build_status_svg]][build_status_link]

> [中文文档](https://github.com/lingochamp/FileDownloader/blob/master/README-zh.md)

> This project dependency on [square/okhttp 3.4.1](https://github.com/square/okhttp)

## DEMO

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
    compile 'com.liulishuo.filedownloader:library:1.3.0'
}
```

## Welcome PR

> If you can improve the unit test for this project would be great.

- Comments as much as possible.
- Commit message format follow: [AngularJS's commit message convention](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#-git-commit-guidelines) .
- The change of each commit as small as possible.
- As far as possible follow the advice from Inspection by IDE(Such as 'Inspect Code' in Android Studio).

## Usage

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
[hybrid_test_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/hybrid_test_demo.gif
[parallel_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/parallel_tasks_demo.gif
[serial_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/serial_tasks_demo.gif
[tasks_manager_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/tasks_manager_demo.gif
[avoid_drop_frames_1_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/avoid_drop_frames1.gif
[avoid_drop_frames_2_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/avoid_drop_frames2.gif
[bintray_svg]: https://api.bintray.com/packages/jacksgong/maven/FileDownloader/images/download.svg
[bintray_url]: https://bintray.com/jacksgong/maven/FileDownloader/_latestVersion
[file_download_listener_callback_flow_png]: https://github.com/lingochamp/FileDownloader/raw/master/art/filedownloadlistener_callback_flow.png
[build_status_svg]: https://travis-ci.org/lingochamp/FileDownloader.svg?branch=master
[build_status_link]: https://travis-ci.org/lingochamp/FileDownloader
