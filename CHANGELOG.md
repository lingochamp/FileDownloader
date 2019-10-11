# Change log

> [中文迭代日志](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG-ZH.md)

## Version 1.7.7

- Fix: FileDownloadThreadPool may throw ArrayIndexOutOfBoundsException & ClassCastException. closes #1258
- Fix: Resume a task may occur 416 problem after upgrading from 1.6.x to 1.7.x.
- Fix: Cannot show notification in demo. closes #1224
- Fix: The callback blockComplete may be invoked in main thread.closes #1069
- Fix: The thread unsafe problem of SparseArray in NoDatabaseImpl. closes #1225 

## Version 1.7.6

_2019-02-20_

#### Fix

- Fix: stop foreground service after all tasks finished in Android O. closes #1096
- Fix: fix 'Context.startForegroundService() did not then call Service.startForeground()' issue. closes #1104
- Fix: insure all foreground service running notification is canceled when pause download. closes #1136
- Fix: fix tiny possibility npe during retry. closes #1100

## Version 1.7.5

_2018-08-03_

#### Fix

- Fix: fix raise "Not allowed to start service Intent..." issue when starting DownloadService on Android O and the application isn't on the foreground and also not on the whitelist, because we can't use `JobScheduler` to handle the download affair. closes #1078

#### Enhance

- Improve Practicability: support character set and the launguage encoding for `Content-Disposition`. closes #1057
- Improve Practicability: cover the error response code 416 from aliyun repo. closes #1050

## Version 1.7.4

_2018-05-19_

#### Fix

- Fix: fix raise 'IllegalStateException' on Android 8+ when FileDownloader try to re-bind service after the connection with the service is lost on downloading state and the app is on the background. closes #1017
- Fix: fix directory traversal vulnerability security issue. closes #1028

## Version 1.7.3

_2018-04-20_

#### Fix

Fix: fix `fd` isn't released manually when download finished which may raise oom when there are a large number of tasks that are continuously initiated.

## Version 1.7.2

_2018-03-14_

#### Fix

- Fix: do not download callback error when the instance length of the task is zero, callback complete directly instead. closes #789
- Fix: fix the temp duplicate data in the database isn't removed when there is another running task with the same temp file path. closes #953
- Fix: the data lost when retry. closes #949
- Fix: fix the instance-length is always 1 when the Content-Range isn't provided but the Content-Length is provided on the trial connection.

#### Enhancement

- Improve Practicability: using the content length value on the Content-Range field when there isn't Content-Length field found in the response header. closes #967

## Version 1.7.1

_2018-02-05_

#### Fix

- Fix: fix download failed with 405 response code when backend can't support `HEAD` method. close #942

## Version 1.7.0

_2018-02-01_

#### Fix

- Fix: fix update status can't keep flow through making updating status synchronized with pause action. close #889
- Fix: fix the sofar-bytes carry back through pending state callback has already discarded. close #884
- Fix: fix can't find filename if filename value on content-disposition without around with ". close #908
- Fix: correct `setCallbackProgressTimes` method make `setCallbackProgressTimes` work correctly. close #901
- Fix: fix download useless data on tcp-window because of the first trial connection use `0-infinite` range. close #933
- Fix: close intput stream when connection ending avoid input-stream leak especially for the trial connection.

#### Enhancement

- Improve Practicability: do not remove the temp-file if rename it to the target path success to prevent raise some file-system problem. close #912
- Improve Practicability: discard range totally if range is right but backend response 416. close #921
- Improve Performance: using HEAD request method instead of GET method for trial connect. ref #933

#### Other

If you are using filedownloader-okhttp3-connection, please upgrade it to the `1.1.0` to adapter FileDownloader 1.7.0.

## Version 1.6.9

_2017-12-16_

#### Fix

- Fix(serial-queue): fix deadlock on `FileDownloadSerialQueue`. closes #858
- Fix: do not use j-unit on library non-unit-test env to fix the `no-static-method-found` issue on some mi-phones. closes #867
- Fix: fix decrease two times of retry-chance each time of retry. closes #838
- Fix: fix get status is pending when a task has been paused on pending status. closes #855

#### Enhancement

- Improve Practicability: public `SqliteDatabaseImpl`、`RemitDatabase`、`NoDatabaseImpl`, so you can overwrite them
- Improve Practicability: support downgrade version from newer version
- Improve Practicability: add the default `User-Agent` if upper layer does not add. closes #848
- Improve Performance: change the keepalive second(5s to 15s) for each executor, since when downloading multiple tasks thread release and recreate too frequently
- Improve Performance: using `RemitDatabase` instead of `DefaultFiledownloadDatabase` to avoid some small task start and finished on the very short time but consume too much time on sync status to database what is useless

![][RemitDatabase-png]

## Version 1.6.8

_2017-10-13_

#### Fix

- Fix: fix resume from breakpoint failed because of `isAlive` not guarantee on Network-thread. this closes #793
- Fix: fix resume from breakpoint failed, because of multi-thread update status very frequently and Messenger can't guarantee order. this refs #793, #764, #721, #769, #763, #761, #716
- Fix: do not crash user when a task has finished but the messenger still has messages, because it's fine for the user. this closes #562
- Fix: fix the callback error of 'it can't take a snapshot for the task xxx' when a user invokes pause very frequently.
- Fix: fix the case of process on the model is wrong which raise 416 each time when restarting it.

## Version 1.6.7

_2017-10-12_

#### Fix

- Fix: Avoid error/pause status is covered by other processing-status which will cause resume-failed, task-never-end. this closes #769, closes #764, closes #761, closes #763, closes #721, closes #716
- Fix: Fix request range value turn to negative when resuming a task which has a process more than 1.99G on its one block. Thanks to @hongbiangoal closes #791

## Version 1.6.6

_2017-09-29_

#### Fix

- Fix(file-integrality): update the process to database only if all buffers on output-stream or system has been flush and sync to device successfully to avoid resume on the wrong point raise complete file not integrality. Closes #745
- Fix(clear): fix `FileDownloader#clearAllTaskData` not clear connection table. Closes #754

#### Enhancement

- Import Performance: optimize the default output-stream with buffered-output-stream, now the VM buffers length is 8192 bytes.

## Version 1.6.5

_2017-09-11_

#### Fix

- Fix: fix `IllegalFormatConversionException` because of format `AtomicLong` with `%d` on `FileDownloadModel.toString`. Closes #743

## Version 1.6.4

_2017-08-21_

#### New Interfaces

- Add `NoDatabaseImpl` for the case of some users need a no-database FileDownloader. Refs #727

#### Enhancement

- Import Performance: Using the `AtomicLong` instead of lock to make better efficiency on increase progressing.

#### Fix

- Fix: Fix response 416 http status because of the last connection range is wrong when resume downloading with the last connection has been downloaded. Closes #737
- Fix(npe): Fix the small probability NPE crash when publish event with it has been removed on other thread. Closes #723

## Version 1.6.3

_2017-07-29_

#### Fix

- Fix: Fix the small probability occur npe when the task is calling back over status with user invoke pause simultaneously. Closes #680
- Fix: Fix `MissingFormatArgumentException` when you pause or resume the FileDownloadserialQueue with it has already done it. Closes #699

## Version 1.6.2

_2017-07-16_

#### Fix

- Fix: Fix raise 'offset < 0' exception when FileDownloader downloading file with the one split connection range begin with larger than 1.99G. Closes #669

## Version 1.6.1

_2017-07-13_

#### Enhancement

- Import Practicability: Throw `GiveUpException` directly when the response `content-length` isn't equal to the expect `content-length` calculated from range. Closes #636

#### Fix

- Fix: Fix sync twice when downloading paused/error.
- Fix: fix file is destroyed when you download chunked file from breakpoint.

## Version 1.6.0

_2017-07-07_

#### Fix

- Fix(no-response): Fix may occur no-respose when multiple connections complete fetch data simultaneously, more detail please move to [here](https://github.com/lingochamp/FileDownloader/issues/631#issuecomment-313451398). Closes #631

## Version 1.5.9

_2017-07-04_

#### Fix

- Fix(duplicate-permission): fix `INSTALL_FAILED_DUPLICATE_PERMISSION` when there are more than one application using FileDownloader lib 1.5.7 or more newer since Android 5.0. This problem is raised since v1.5.7, because of we declared permission for receiving completed status broadcast more secure, now we remove it to fix this problem. Closes #641

## Version 1.5.8

_2017-06-28_

#### Fix

- Fix(no-response): fix no-response when switch between pause and start for the same task very fast frequency. Closes #625

## Version 1.5.7

_2017-06-25_

#### New Interfaces

- Support the configuration `broadcast.completed` in `filedownloader.properties`: determine whether need post a broadcast when task is completed. Closes #605
- Support accepting 201 http status. Closes #545
- Support pause and resume for the `FileDownloadSerialQueue`. Closes #547
- Handle the case of redirect(300、301、302、303、307、308) on FileDownloader self. Closes #611
- Deprecated the `FileDownloader#init` and add the replacer `FileDownloader#setup` to let user invoke anytime before using `Filedownloader`. Closes #500

> - If you want to using `broadcast.completed` and receive completed broadcast, you also need to register receiver with `filedownloader.intent.action.completed` action name on `AndroidManifest.xml` and please using `FileDownloadBroadcastHandler` class to parse the received `Intent`.
> - Now, rather than using `FileDownloader#init`, if you want to register your own customize components for FileDownloader please invoking `FileDownloader.setupOnApplicationOnCreate(application):InitCustomMaker` on the `Application#onCreate`, otherwise you just need invoke `FileDownloader.setup(Context)` anytime before using `FileDownloader`.

#### Fix

- Fix: fix `FileDownloadQueueSet` can't handle the case of disable wifi-required. Thanks @qtstc
- Fix(output-stream): fix broken support for output-stream when it don't support seek. Closes #622

#### Enhancement

- Improve Practicability: Cover the case of reusing the downloaded processing with the different url( using with `idGenerator` ). Closes #617

## Version 1.5.6

_2017-06-18_

#### Fix

- Fix(crash): fix raise NPE crash when require paused a task and invoking `findRunningTaskIdBySameTempPath` at the same time. Closes #613
- Fix(crash): fix raise `IllegalArgumentException` when response code is 206 and its ETAG is changed. Closes #612
- Fix(crash): fix raise `FileDownloadNetworkPolicyException` unhandled exception, when user enable wifi-required but no wifi-state. Thanks @qtstc
- Fix(crash): fix raise `IllegalStateException` when user upgrades from `v1.4.3` or older version to `v1.5.2` or newer version directly and some more conditions, more detail please move to #610
- Fix(crash): fix some small probability case raise `IllegalStateException` when callback-flow has been final but occurring completed/error at the same time.
- Fix(no-response): fix no-response after start download and receive connected callback because the resource state has been changed during the connection of verification and connections of fetch data.

#### Enhancement

- Improve Practicability: callback `error` directly when create the parent directory failed. Closes #542
- Improve Practicability: handle the case of response code is `416`. Closes #612

## Version 1.5.5

_2017-06-12_

#### Fix

- Fix(max-network-thread-count): fix the `download.max-network-thread-count` not work and there are no restrictions on the number of tasks downloaded at the same time since v1.5.0 when tasks runs on the multi-connection Closes #607

## Version 1.5.4

_2017-06-11_

#### New Interfaces

- Support customizing the download task identify generator through `IdGenerator`. Closes #224

#### Enhancement

- Improve Practicability: Decoupling the filedownload-database with filedownload-model, let filedownload-database only care about database operation.
- Improve Practicability: Decoupling the database initial-maintain from the filedownload-database default implementation to let the customized database can be maintained.

## Version 1.5.3

_2017-06-08_

#### Fix

- Fix(crash): Fix divide by zero on calculating average speed when download completed and connected at the same time. Refs #601
- Fix(crash): Fix raise NPE crash when you require pause the task between executed the fetch-data-task and fetch-data-task has not yet started. Closes #601

## Version 1.5.2

_2017-06-07_

#### Fix

- Fix(crash): Fix raising NPE crash or ConcurrentModificationException when the Task is paused or error with the connection is completing at the same time. Closes #598
- Fix(crash): Fix raising NPE crash when pause the `FetchDataTask` and it still without any time to sync data to database or file-system. Refs #598
- Fix(crash): Fix raising NPE crash when using the multiple connections to download and connect failed or create `FetchDataTast` failed. Refs #598
- Fix(speed-calculate): Fix the speed result is `0` when ignore all processing callbacks and just using `FinishListener`.
- Fix(finish-listener): Fix there isn't `over` callback for the `FinishListener` when the file has already been downloaded in the past.

#### Enhancement

- Improve Performance: Enable the WAL for the default sqlite to speed up sql operation because the most of our case is concurrently accessed and modified by multiple threads at the same time.

## Version 1.5.1

_2017-06-05_

#### Fix

- Fix(crash): Fix the NPE crash when don't provided the `InitCustomMaker` on `FileDownloader#init`. Closes #592
- Fix(callback): Fix on the `pending` callback you can't get the right `sofarBytes` when there are several connections served for the task and the task is resuming from the breakpoint.
- Fix(speed-monitor): Correct the result of the total average speed when the task resume from a breakpoint on `IDownloadSpeed.Monitor`.

#### Enhancement

- Improve Robust: Sync all process on fetch task manually when it is paused to make the process can be persist.
- Improve Robust: Raise `IllegalArgumentException` when provide `context` is null on `FileDownloader.init` to expose the problem earlier.

## Version 1.5.0

_2017-06-05_

#### New Interfaces

- Support multiple-connection(multiple threads) for one downloading task.  Closes #102
- Support `ConnectionCountAdapter` to customize connection count for each task(you can set it through `FileDownloader#init`).

#### Enhancement

- Improve Performance: Refactor whole download logic and origin callback logic and remove 1000 line class `FileDownloadRunnable`.

The default connection count strategy for each task, you can customize it through `ConnectionCountAdapter`:

- one connection: file length [0, 1MB)
- two connections: file length [1MB, 5MB)
- three connections: file length [5MB, 50MB)
- four connections: file length [50MB, 100MB)
- five connections: file length [100MB, -]

## Version 1.4.3

_2017-05-07_

#### Fix

- Fix: Remove redundant deprecated method: `FileDownloader#init(Application)`, because `Application` is implement of `Context`.

## Version 1.4.2

_2017-03-15_

#### Fix

- Fix(Same File Path): Avoid two tasks writing to the same file simultaneously, Once there is an another running task with the same target path to the current task's, the current task will receive the `PathConflictException` to refused start downloading. Closes #471

#### New Interfaces

- Add `FileDownloadSerialQueue#getWaitingTaskCount`: Get the count of tasks which is waiting on the serial-queue instance. Refs #345

## Version 1.4.1

_2017-02-03_

#### Fix

- Fix(High concurrency): Fix occurring the NPE crash because of it still receiving message-snapshot in the messenger but the host task has been assigned to null since it has been received over-status message-snapshot. Closes #462
- Fix(`FileDownloadHttpException`): Fix occurring the `IllegalStateException` because of cannot access request header fields after connection is set when occurring http-exception. Closes #458

## Version 1.4.0

_2017-01-11_

#### Enhancement

- Improve Performance: Optimize the logic in `FileDownloader#init`, let it lighter(just do some action like assign `context` and `maker`)

#### Fix

- Fix(pause): fix can't stop the task when occurring the high concurrency event about pausing task after starting it in very close time. Closes #402
- Fix(init FileDownloader): fix the very low frequent crash when init FileDownloader on the process the `FileDownloadService` settled on. Closes #420  
- Fix(FileDownloadHttpException): fix params can't match the `formatter` when occur `FileDownloadHttpException` Closes #438

## Version 1.3.9

_2016-12-18_

### Important:

- Since this version you can customize you own [FileDownloadConnection][FileDownloadConnection-java-link] component, we use [this one][FileDownloadUrlConnection-java-link] as default.
- Since this version, FileDownloader don't dependency the okhttp as default. (If you still want to use the okhttp as your connection component, you can integrate [this repo](https://github.com/Jacksgong/filedownloader-okhttp3-connection) feel free)

> If you still need configure `timeout`、`proxy` for the connection component, but you don't want to implement your own one, don't worry, I implement it for the default connection component too, just move to : [DemoApplication](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/DemoApplication.java#L35), check the code if you want.

#### New Interfaces

- Add `FileDownloadQueueSet#reuseAndStart`: Add reuseAndStart function to the queue-set to reuse task instances before start them. Ref #383
- Add `FileDownloadConnection`: Support customize the connection component for FileDownloader and remove the dependency of the okhttp as default. Closes #158

## Version 1.3.0

_2016-10-31_

#### New Interfaces

- Add `FileDownloadSerialQueue`: Easy to dynamically manage tasks and tasks in the queue will automatically start download one by one. Closes #345.
- Remove the `callback` method in the `FileDownloadListener` class, besides adding the `FileDownloadListener#isInvalid` method to tell the FileDownloader whether the listener has already invalidated, which means it can't receive any messages.
- Add `FileDownloader#clearAllTaskData`: Clear all data in the `filedownloader` database. Closes #361

#### Enhancement

- Improve Practicability(`FileDownloadListener#blackCompleted`): Ensure the `blockCompleted` callback method can accept any `Exception`. Closes #369.
- Improve Practicability(service-not-connected): Print the tips with the cause in service-not-connected-helper, in this way, when you invoke some methods need the FileDownload service has already connected but not yet, FileDownloader will not only print causes in the `Logcat` but also print the tips.

#### Fix

- Fix(reuse): fix `BaseDownloadTask#reuse` is called shortly after the call to `BaseDownloadTask#pause` may raise an exception. Closes #329.

## Version 1.2.2

_2016-10-15_

#### Fix

- Fix(fatal-crash): fix when the task doesn't have `FileDownloadListener`, we can't receive the callback of `FileDownloadMonitor.IMonitor#onTaskOver` for it. Closes #348.

## Version 1.2.1

_2016-10-09_

#### Fix

- <s>Fix(fatal-crash): fix when the task doesn't have `FileDownloadListener`, we can't receive the callback of `FileDownloadMonitor.IMonitor#onTaskOver` for it. Closes #348. </s> Sorry for my mistake, this bug is still exist in 1.2.1 and finally fixed in 1.2.2.

## Version 1.2.0

_2016-10-04_

#### New Interfaces

- Add `FileDownloader#insureServiceBind()`: Easy to block the current thread, and start FileDownloader service, after the service started then executes the request which needs the service alive. Refs #324.
- Add `FileDownloader#insureServiceBindAsync()`: Easy to start FileDownloader service, and after the service started then executes the request which needs the service alive. Refs #324.
- Add `FileDownloader#bindService(runnable:Runnable)`: Easy to start FileDownloader service, and after the service started then executes the `runnable`. Refs #324.
- Add `FileDownloader#init(Context,InitCustomMaker)`: Easy to initialize the FileDownloader engine with various kinds of customized components.

#### Enhancement

- Improve Practicability(`InitCustomMaker#database`): Support customize the database component with the implementation of `FileDownloadDatabase`, and implements the default database component: `DefaultDatabaseImpl`.
- Improve Practicability(`InitCustomMaker#outputStreamCreator`): Support customize the output stream with the implementation of `FileDownloadOutputStream`, and implements the default output stream component `FileDownloadRandomAccessFile`, and some alternative components: `FileDownloadBufferedOutputStream`、`FileDownloadOkio`.

## Version 1.1.5

_2016-09-29_

#### New Interfaces

- Support the configuration `file.non-pre-allocation` in `filedownloader.properties`: Whether doesn't need to pre-allocates the 'content-length' space when to start downloading, default is `false`. Closes #313 .

#### Fix

- Fix(fatal-crash): fix occur the `StackOverflowError` when thread pool getActiveCount is not right because of it just an approximate number. Closes #321 .
- Fix(minor-crash): fix in some minor cases occur `IllegalStateException` which message is 'No reused downloaded file in this message'. Closes #316 .
- Fix(minor-crash): fix when there are several serial-queues started in case of the FileDownloader service doesn't connect yet and in minor cases that the same task in the queue will be started twice which lead to crash. Refs #282 .

#### Others

- Dependency: Cancel the dependence of thread-pool library. Refs #321 .
- MinSDKVersion: Upgrade `minSdkVersion` : 8->9. Refs #321 .

## Version 1.1.0

_2016-09-13_

#### New Interfaces

- Add `BaseDownloadTask#setWifiRequired`: Set whether the task only allows downloading on the wifi network type. Default `false`. Closes #281 .

#### Enhancement

- Improve Performance: Alternate all thread pools to exceed-wait-pool(more detail: docs in `FileDownloadExecutors`) and all threads in pools will be terminate after idle 5 second. Refs #303 .
- Improve Practicability: Handle any `Throwable`s thrown on `FileDownloadListener#blockComplete` method and callback to `FileDownloadListener#error` method instead of `FileDownloadListener#completed`. Closes #305 .

#### Fix

- Fix(lost-connect): Prevent the waiting-connect-list contains duplicate tasks in minor cases.

## Version 1.0.2

_2016-09-06_

#### Fix

- Fix: When the service didn't connected and now it is connected and FileDownloader try to restart the 'queue-task's which in the waiting-service-connect list but occur an `IllegalStateException`. Closes #307 .

## Version 1.0.1

_2016-09-05_

#### New Interfaces

> If you used `BaseDownloadTask#ready()` which is a deprecated method now, just migrate it to `BaseDownloadTask#asInQueueTask():InQueueTask` and `InQueueTask#enqueue()`.

- Add `BaseDownloadTask#asInQueueTask():InQueueTask` and Deprecated `BaseDownloadTask#ready()`: Declare the task is a queue task, what will be assembled by a queue which makes up of the same `listener` task and there is a method `InQueueTask#enqueue()` to enqueue this task to the global queue to ready for being assembled by the queue. The operation of method `InQueueTask#enqueue()` is the same to the Deprecated method `BaseDownloadTask#ready()`, we wrap the `ready()` method in this way just want you to know clearly: Only if the task belongs to a queue, you need to invoke this method otherwise if this task is an isolated task but you invoke this method, it's wrong and you will receive an exception(More detail reason please move to the exception thrown in `DownloadTask#start`).

#### Fix

- Fix: Maybe occur an IllegalStateException when there are several isolated tasks and queues with the same `listener` object, and they are started in the different thread simultaneously. Closes #282 .

## Version 1.0.0

_2016-08-21_

#### New Interfaces

- Add `BaseDownloadTask#cancel`: This method is used for explaining why the pause operation is the same as the cancel operation.

#### Enhancement

- Improve Performance: Hold the result of `isDownloaderProcess`.
- Improve Practicability: Refactor the visible layer of the code. Closes #283
- Improve Practicability: Perfect the java doc. Closes #284
- Improve Practicability: Add the java doc website: http://fd.dreamtobe.cn. Closes #285

## Version 0.3.5

_2016-08-16_

#### Enhancement

- Improve Practicability: Add thread name to all threads used in FileDownloader.
- Improve Performance: Change the count of core thread for block-completed-thread-pool: 5->2, reduce redundant resource waste.

#### Fix

- Fix(SQLiteFullException): Cover the case of SQLiteFullException during the entire downloading process, and ensure the exception can be carried back to `FileDownloadListener#error` . Closes #243
- Fix(directory-case): Fix in the case of the provided path is a directory, and the task already completed, if you start the task again you will receive `FileDownloadListener#completed` directly, but the `targetFilePath` may be null in the `FileDownloadListener#completed` callback method. Closes #237

## Version 0.3.4

_2016-07-31_

#### New Interfaces

- Add `FileDownloader#clear`: clear the data with the task id in the filedownloader database. Closes #218.

#### Enhancement

- Improve Practicability: Add return value to the method `FileDownloader#start(FileDownloadListener, boolean)` : Whether start tasks successfully. Closes #215.
- Improve Practicability: Pause tasks with the same download-id rather than just pause one task through there are more than one task in downloading.

#### Fix

- Fix(init-crash): Fix the crash about the list of running-app-process-info from `ActivityManager` is null when to init FileDownloader. Closes #210.
- Fix(minor-crash): Fix the NPE-crash when to execute receiving snapshot-message after FileDownloadService already onDestroy. Closes #213.
- Fix(message-keep-flow): Delete the target file before start downloading, ensure can't get the `completed` status when another same task is downloading. Closes #220
- Fix(start-serial): Assemble non-attached-tasks to start rather than assemble tasks just refer to FileDownloadListener, fix no possibility to start two queues with the same `FileDownloadListener`. Closes #223.
- Fix(free-messenger): Free the messenger of Task before call back 'over-message' to FileDownloadListener instead of after callback, ensure Task can be reused in FileDownloadListener callback method. Closes #229.

#### Others

- Upgrade dependency okhttp from `3.3.1` to `3.4.1`.


## Version 0.3.3

_2016-07-10_

#### New Interfaces

- Add `FileDownloadUtils#getTempPath`: Get the temp path is used for storing the temporary file not completed downloading yet(`filename.temp`). Refs #172.
- Add `FileDownloader#getStatusIgnoreCompleted(id:int)`:  Get the downloading status without cover the completed status(If completed you will receive `INVALID`).
- Add `FileDownloader#getStatus(id:int, path:String)`:  Get the downloading status.
- Add `FileDownloader#getStatus(url:String, path:String)`:  Get the downloading status.
- Add `FileDownloadUtils#isFilenameConverted(context:Context)`: Whether tasks from FileDownloader Database has converted all files' name from `filename`(in old architecture) to `filename.temp`, if it is not completed downloading yet.
- Add `FileDownloadUtils#generateId(url:String, path:String, pathAsDirectory:boolean)`: Generate a `Download Id` which can be recognized in FileDownloader.
- Add `BaseDownloadTask#setPath(path:String, pathAsDirectory:boolean)`: If `pathAsDirectory` is `true`, the `path` would be the absolute directory to store the downloading file, and the `filename` will be found in `contentDisposition` from the `response#header` as default.
- Add `BaseDownloadTask#isPathAsDirectory`: Whether the result of `BaseDownloadTask#getPath()` is a `directory` path or `directory/filename` path.
- Add `BaseDownloadTask#getTargetFilePath`: Get the target file path to store the downloading file.
- Add `FileDownloadQueueSet#setDirectory`: Set the `directory` to store files in this queue.

#### Enhancement

- Improve Practicability: Support the `path` of the task as the directory to store the file, and in this case, the `filename` will be found in `contentDisposition` from the `response#header` as default. Refs #200.
- Improve Practicability: Using the temp path to store the file not completed downloading yet(`filename.temp`). Refs #172.
- Improve Performance: FileDownloader doesn't store completed tasks in Database anymore, and check whether the task has completed downloading with `File#exists()` directly. Refs #176, #172.
- Improve Robust: Choosing the task which status is `INVALID` or `progress` to receive `completed` message preferentially, to ensure the callback of `progress` can be handled. Refs #123
- Improve Robust: Expanding task-sync-lock to the outside of getting-same-id-downloading-task, to fix some messages can't be consumed because status changed during getting-same-id-downloading-task and waiting for task-sync-lock.

#### Fix

- Fix(DB-maintain): Keeping models, whose status is `pending` and downloaded so far bytes is more than 0 because it can be used for resuming from the breakpoint. Closes #176.
- Fix(crash-NPE): FileDownloader might occur NPE when the download-listener was removed, but the task is still running in FileDownloader. Closes #171.

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

[RemitDatabase-png]: https://github.com/lingochamp/FileDownloader/raw/master/art/remit-database.png
[FileDownloadConnection-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/connection/FileDownloadConnection.java
[FileDownloadUrlConnection-java-link]: https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/connection/FileDownloadUrlConnection.java
