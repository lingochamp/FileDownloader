# FileDownloader
Android multi-task file download engine.


[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
[![Build Status][build_status_svg]][build_status_link]

> [中文文档](https://github.com/lingochamp/FileDownloader/blob/master/README-zh.md)

> This project dependency on [square/okhttp 3.2.0](https://github.com/square/okhttp)

## DEMO

![][serial_tasks_demo_gif]
![][parallel_tasks_demo_gif]
![][tasks_manager_demo_gif]
![][hybrid_test_demo_gif]
![][avoid_drop_frames_1_gif]
![][avoid_drop_frames_2_gif]


## Installation

FileDownloader is installed by adding the following dependency to your build.gradle file:

```
dependencies {
    compile 'com.liulishuo.filedownloader:library:0.2.3'
}
```

## Welcome PR

- Comments as much as possible.
- Commit message format follow: [AngularJS's commit message convention](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#-git-commit-guidelines) .
- The change of each commit as small as possible.
- As far as possible follow the advice from Inspection by IDE(Such as 'Inspect Code' in Android Studio).

## Usage

#### Basic

To begin using FileDownloader, have your`Application#onCreate` invoke the `FileDownloader.init(this)` such as:

> If your want to customize `OkHttpClient` for downloading files, @see [DemoApplication](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/DemoApplication.java)

```
public XXApplication extends Application{

    ...
    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * Just for cache Application's Context, and ':filedownloader' progress will NOT be launched
         * by below code, so please do not worry about performance.
         */
        FileDownloader.init(getApplicationContext());
    }

    ...
}
```

#### Start Single download task

```
FileDownloader.getImpl().create(url)
        .setPath(path)
        .setListener(new FileDownloadListener() {
            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
            }

            @Override
            protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
            }

            @Override
            protected void completed(BaseDownloadTask task) {
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
            }

            @Override
            protected void warn(BaseDownloadTask task) {
            }
        }).start();
```

#### Start multitask download tasks

To start tasks queue serial or parallel(download simultaneously)

```
final FileDownloadListener queueTarget = new FileDownloadListener() {
            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
            }

            @Override
            protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
            }

            @Override
            protected void completed(BaseDownloadTask task) {
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
            }

            @Override
            protected void warn(BaseDownloadTask task) {
            }
        };

// The first way :

//for (String url : URLS) {
//    FileDownloader.getImpl().create(url)
//            .setCallbackProgressTimes(0) // why do this? in here i assume do not need for each task callback `FileDownloadListener#progress`,
// we just consider which task will complete. so in this way reduce ipc will be effective optimization
//            .setListener(queueTarget)
//            .ready();
//}

//if(serial){
    // To form a queue with the same queueTarget and execute them linearly
//    FileDownloader.getImpl().start(queueTarget, true);
// }

// if(parallel){
    // To form a queue with the same queueTarget and execute them in parallel
//    FileDownloader.getImpl().start(queueTarget, false);
//}

// the second way:

final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(downloadListener);

final List<BaseDownloadTask> tasks = new ArrayList<>();
for (int i = 0; i < count; i++) {
     tasks.add(FileDownloader.getImpl().create(Constant.URLS[i]).setTag(i + 1));
}

queueSet.disableCallbackProgressTimes(); // do not need for each task callback `FileDownloadListener#progress`,
// we just consider which task will complete. so in this way reduce ipc will be effective optimization

// each task will auto retry 1 time if download fail
queueSet.setAutoRetryTimes(1);

if (serial) {
     // start download in serial order
     queueSet.downloadSequentially(tasks);
     // if your tasks are not a list, invoke such following will more readable:
//      queueSet.downloadSequentially(
//              FileDownloader.getImpl().create(url).setPath(...),
//              FileDownloader.getImpl().create(url).addHeader(...,...),
//              FileDownloader.getImpl().create(url).setPath(...)
//      );
}

if (parallel) {
   // start parallel download
   queueSet.downloadTogether(tasks);
   // if your tasks are not a list, invoke such following will more readable:
//    queueSet.downloadTogether(
//            FileDownloader.getImpl().create(url).setPath(...),
//            FileDownloader.getImpl().create(url).setPath(...),
//            FileDownloader.getImpl().create(url).setSyncCallback(true)
//    );
}


queueSet.start();

```

## How?

#### `FileDownloader`

> Automatically resume breakpoint by last downloaded file if possible default.

> FileDownloader will stop task but retain download status in db by invoke `Filedownloader#pause`

| function | description
| --- | ---
| init(Context) |  Just cache `Context` in Main-Process and FileDownloader-Process.
| init(Context, OkHttpClientCustomMaker) |  Cache `Context` in Main-Process and FileDownloader-Process, and init the OkHttpClient in FileDownloader-Process when FileDownloader-Process is launching.
| create(url:String) | Create a download task
| start(listener:FileDownloadListener, isSerial:boolean) | Start the download queue by the same listener(maybe do not need callback each task's `FileDownloadListener#progress` in this case, then set `setCallbackProgressTimes(0)` is effective optimization)
| pause(listener:FileDownloadListener) | Pause the download queue by the same listener
| pauseAll(void) | Pause all task
| pause(downloadId) | Pause the download task by the downloadId
| getSoFar(downloadId) | Get downloaded so far bytes by the downloadId
| getTotal(downloadId) | Get file total bytes by the downloadId
| bindService(void) | Bind & start `:filedownloader` process manually(Do not need, will bind & start automatically by Download Engine if real need)
| unBindService(void) | Unbind & stop `:filedownloader` process manually
| unBindServiceIfIdle(void) | If there is no active task in the `:filedownloader` progress currently , then unbind & stop `:filedownloader` process
| isServiceConnected(void) | Whether started and connected to the `:filedownloader` progress(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))
| getStatus(downloadId) | Get download status by the downloadId(ps: Please refer to [Tasks Manager demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))
| setGlobalPost2UIInterval(intervalMillisecond:int) | For Avoid Missing Screen Frames. Each intervalMillisecond post 1 message to ui thread at most. if the value is less than 0, each callback will always post a message to ui thread immediately, may will cause missing screen frames and produce great pressure on the ui thread Looper. Default: 10ms
| setGlobalHandleSubPackageSize(packageSize:int) | For Avoid Missing Screen Frames. {packageSize}: The number of FileDownloadListener's callback contained in each message. value completely dependent on the intervalMillisecond of setGlobalPost2UIInterval, describe will handle up to {packageSize} callbacks on the each message posted to ui thread. Default: 5
| enableAvoidDropFrame(void) | Avoid missing screen frames, but this leads to all callbacks of FileDownloadListener do not be invoked at once when it has already achieved.
| disableAvoidDropFrame(void) | Disable avoid missing screen frames, let all callbacks of FileDownloadListener be invoked at once when it achieve.
| isEnabledAvoidDropFrame(void) | Has already enabled Avoid Missing Screen Frames. Default: true

#### `FileDownloadTask`

> For example: `FileDownloader.create(URL).setPath(xxx).setListener(xxx).start()`

| function | description
| --- | ---
| setPath(path:String) | Absolute path for save the download file
| setListener(listener:FileDownloadListener) | For callback download status(pending,connected,progress,blockComplete,retry,error,paused,completed,warn)
| setCallbackProgressTimes(times:int) | Set maximal callback times on callback `FileDownloadListener#progress`
| setTag(tag:Object) | Sets the tag associated with this task, not be used by internal
| setTag(key:int, tag:Object) | Sets a tag associated with this task. If the key already existed, the old tag will be replaced
| setForceReDownload(isForceReDownload:boolean) | If set to true, will not check whether the file is downloaded by past, default false
| setFinishListener(listener:FinishListener) | -
| setAutoRetryTimes(autoRetryTimes:int) | Set the number of times to automatically retry when encounter any error, default 0
| setSyncCallback(syncCallback:boolean) | if true will invoke callbacks of FileDownloadListener directly on the download thread(do not post the message to the ui thread), default false
| addHeader(name:String, values:String) | Add custom request header to the task. Attention: We have already handled ETag, and will add `If-Match` & `Range` value if it works
| addHeader(line:String) | Add custom request header to the task. Attention: We have already handled ETag, and will add `If-Match` & `Range` value if it works
| removeAllHeaders(name:String) | Remove all custom request header bind with the `{name}`
| ready(void) | Ready task( For queue task )
| start(void) | Start task
| pause(void) | Pause task
| getDownloadId(void):int | Get download id (generate by url & path)
| getUrl(void):String | Get download url
| getCallbackProgressTimes(void):int | Get maximal callback times on callback `FileDownloadListener#progress`
| getPath(void):String | Get absolute path for save the download file
| getListener(void):FileDownloadListener | Get current listener
| getSoFarBytes(void):int | Get already downloaded bytes
| getTotalBytes(void):int | Get file total bytes
| getStatus(void):int | Get current status
| isForceReDownload(void):boolean | Force re-download,do not care about whether already downloaded or not
| getEx(void):Throwable | Get throwable
| isReusedOldFile(void):boolean | Is reused downloaded old file
| getTag(void):Object | Get the task's tag
| getTag(key:int):Object | Get the object stored in the task as a tag, or null if not set.
| isContinue(void):boolean | Is resume by breakpoint
| getEtag(void):String | Get current ETag on header
| getAutoRetryTimes(void):int | Get the number of times to automatically retry
| getRetryingTimes(void):int | Get the current number of retry
| isSyncCallback(void):boolean | Whether sync invoke callbacks of FileDownloadListener directly on the download thread

#### `FileDownloadListener`

##### Natural callback flow :

```
pending -> started -> connected -> (progress <->progress) -> blockComplete -> completed
```

##### Maybe get follow callback and finish the download:

```
paused / completed / error / warn
```

##### Maybe reuse downloaded file and just get callback flow:

```
blockComplete -> completed
```

| function | description | update
| --- | --- | ---
| pending | Pending for download | soFarBytes、totalBytes
| started | Finish pending, and start the download runnable for the task | -
| connected | Connected to the remote file server | ETag、IsResumeBreakpoint、 soFarBytes、totalBytes
| progress | Download progress | soFarBytes
| blockComplete | Callback before the 'completed callback' sync in the new thread| -
| retry | Callback before automatically retry download | Throwable、RetryingTimes、soFarBytes
| completed | Succeed download and completed | -
| paused | Paused download and over | soFarBytes
| error | Occur error and over | Throwable
| warn | There is already an identical task(same url & same path) being downloaded(pending/connected/process/retry) | -


![][file_download_listener_callback_flow_png]

##### Calling methods in the FileDownloadListener too fast leads to Missing Screen Frames?

> Your have 2 ways to handle this problem:

1. `FileDownloader#enableAvoidDropFrame`, The default is enabled.
2. `BaseDownloadTask#setSyncCallback`, The default is false, if true will invoke callbacks of FileDownloadListener directly on the download thread(do not post the message to the ui thread).

#### `FileDownloadMonitor`

> You can add this global monitor for Statistic/Debugging.

| function | description
| --- | ---
| setGlobalMonitor(monitor:IMonitor) | set and replace global monitor into the Download Engine
| releaseGlobalMonitor(void) | release the monitor has already set into the Download Engine
| getMonitor(void) | get the monitor has already set into the Download Engine


##### `FileDownloadMonitor.IMonitor`

> monitor interface.

| interface | description
| --- | ---
| onRequestStart(count:int, serial:boolean, lis:FileDownloadListener) | Will be invoked when request to start multi-tasks manually
| onRequestStart(task:BaseDownloadTask) | Will be invoked when request to start the task manually
| onTaskBegin(task:BaseDownloadTask) | Will be invoked when the task in the internal is begin(before pending)
| onTaskStarted(task:BaseDownloadTask) | Will be invoked when the task finish pending and start download runnable
| onTaskOver(task:BaseDownloadTask) | Will be invoked when the task in the internal is over(finish all lifecycle of the task)

#### `FileDownloadUtils`

| function | description
| --- | ---
| setDefaultSaveRootPath(path:String) | The path is used as Root Path in the case of task without setting path in the entire Download Engine

#### `FileDownloadNotificationHelper`

> How to integrate with Notification quickly? Recommend to refer to [NotificationDemoActivity](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/NotificationDemoActivity.java)

#### `filedownloader.properties`

> If you want to use this property and don't find 'filedownloader.properties' on the root path of your project, please create a new empty file with naming 'filedownloader.properties' on there feel free.

> Format: `keyword=value`.

| keyword | description | default
| --- | --- | ---
| http.lenient | if you occur exception: 'can't know the size of the download file, and its Transfer-Encoding is not Chunked either', but you want to ignore such exception, set true, will deal with it as the case of transfer encoding chunk. | false

## Exception

> If occur a exception, you will get it in `FileDownloadListener#error(BaseDownloadTask, Throwable)`

| Exception | reason
| --- | ---
| `FileDownloadHttpException`| Throw this exception, when the HTTP status code is not 200(HTTP_OK),  and not 206(HTTP_PARTIAL) either. You can find the request-header, the response-header and response-code in this exception.
| `FileDownloadGiveUpRetryException` | Throw this exception, when can't know the size of the download file, and its Transfer-Encoding is not Chunked either; And With this exception, will ignore all retry-chances(`BaseDownloadTask#setAutoRetryTimes`). You can ignore such exception by add `http.lenient=true` to the `filedownloader.properties`, and will download directly as a Chunked-Resource.
| `FileDownloadOutOfSpaceException` | Throw this exception, when the file will be downloaded is too large to store.
| Others | some program Exceptions.

## Attention

- Using `FileDownloadLargeFileListener` instance instead of `FileDownloadListener`, when file size maybe greater than 1.99G(`2^31-1=2_147_483_647`)(The same use: `getLargeFileSoFarBytes()` and `getLargeFileTotalBytes()`).
- Default by okhttp: retryOnConnectionFailure: Unreachable IP addresses/Stale pooled connections/Unreachable proxy servers
- Default by okhttp: connection/read/write time out 10s
- FileDownloader is enable Avoid Missing Screen Frames as default, if you want to disable it, please invoke `FileDownloader.getImpl().disableAvoidDropFrame()`.

#### Low Memory?

We covered all low memory cases follow [Processes and Threads](http://developer.android.com/guide/components/processes-and-threads.html), just feel free to use the FileDownloader, it will be follow your expect.


#### Chunked transfer encoding data?

Has supported, just use as normal task, but recommend to glance at demo on [Single Task Test](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/SingleTaskTestActivity.java).

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
