# Change log

> [ Change log in english](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG.md)

## Version 0.2.6

_2016-04-20_

#### 新接口

- 调整: 将原本需要在根目录创建的 `filedownloader.properties` ，改为到 模块的 `assets` 目录下， 如 `/demo/src/main/assets/filedownloader.properties`。

#### 修复

- 修复 `filedownloader.properties` 中的参数不起作用的bug。 Closes #117.

## Version 0.2.5

_2016-04-19_

#### 新接口

- 添加 `FileDownloader#setTaskCompleted`: 用于告诉FileDownloader引擎，以指定Url与Path的任务已经通过其他方式(非FileDownloader)下载完成。
- 支持 新的配置参数`download.max-network-thread-count` 在 `filedownloader.properties`: 同时下载的最大网络线程数，默认值是3。 Closes #116.

## Version 0.2.4

_2016-04-18_

#### 新接口

- 添加 `BaseDownloadTask#getSpeed` 以及 `BaseDownloadTask#setMinIntervalUpdateSpeed`: 获取任务的下载速度, 下载过程中为实时速度，下载结束状态为平均速度。 Closes #95 。
- 添加 `FileDownloader#startForeground` 以及 `FileDownloader#stopForeground` 用于支持 前台模式([Service#startForeground](http://developer.android.com/intl/zh-cn/reference/android/app/Service.html#startForeground(int, android.app.Notification)))，保证用户从最近应用列表移除应用以后下载服务被杀。 Closes #110 。
- 支持 新的配置参数 `download.min-progress-step` 以及 `download.min-progress-time`: 最小缓冲大小以及最小缓冲时间，用于判定是否是时候将缓冲区中进度同步到数据库，以及是否是时候要确保下缓存区的数据都已经写文件。这两个值越小，更新会越频繁，下载速度会越慢，但是应对进程被无法预料的情况杀死时会更加安全。默认值是与 `com.android.providers.downloads.Constants`中的一致 65536(最小缓冲大小) 以及 2000(最小缓冲时间)。
- 支持 新的配置参数 `process.non-separate` 在 `filedownloader.properties` 中 : FileDownloadService 默认是运行在独立进程 `:filedownloader` 上的, 如果你想要FileDownloadService共享并运行在主进程上，以减少不必要的消耗(如IPC的I/O，维护进程的CPU的消耗等), 添加将该配置参数值设置为 `true`。 Closes #106 。

#### 性能与提高

- 提高性能: 提高了下载速度, 优化了同步缓冲区的数据到本地文件以及数据库的架构，很大程度的提高了下载速度。 Closes #112 。

#### 修复

- 修复: 无法重新启动一个已经暂停但是依然存在下载线程池中在pending中的任务。 Closes #111 。

## Version 0.2.3

_2016-04-11_

#### 新接口

- 添加 `FileDownloadOutOfSpaceException`, 当将要下载的文件大小大于剩余磁盘大小时，会抛出这个异常。
- 在 `FileDownloadListener` 新增 `started` 回调方法: 在结束 `pending` 开始运行当前任务的线程时，回调该方法。
- 在 `FileDownloadMonitor.IMonitor` 新增 `onTaskStarted` 回调方法，用于监控在结束 `pending` 开始运行当前任务的线程时，回调该方法。这样就可以在监控中通过 `onTaskBegin`到`onTaskStarted`计算出pending的时间，在`onTaskStarted`到`onTaskOver`计算出真正消耗在下载的时间(Connection、Fetching)。

#### 性能与提高

- 提高实用性: 为 `FinishListener` 的 `over` 方法提供所指向的Task，为了有些时候我们为多个任务添加相同的 `FinishListener` 时，需要这个参数来判断当前是哪个任务的回调。 Closes #69 。
- 提高稳定性: 如果调用一个正在运行中的Task对象的 `start` 方法，直接抛异常；并且为已经结束的Task对象提供 `BaseDownloadTask#reuse` 进行复用。 Closes #91 。
- 提高性能: 在进入事件队列之前，拦截掉一些原本就没有监听器进行监听的事件。

#### 修复

- 修复: 在一些极端情况下，非结束的回调回调次数不符合预期的情况。
- 修复: `progress` 方法的回调中包含了对完成( `sofarBytes==totalBytes` )的回调，导致回调间隔不达预期的bug。
- 修复: 在 `warn` 回调带回 total-bytes，为了覆盖在 主进程被杀，下载进程存在的情况下，主进程重新重启并启动相同任务，total-bytes为0的bug。 Closes #90 。
- 修复: 如果连续出现失败，连续回调 `retry` 时，`retry` 只被回调一次，其他的次数的 `retry` 都不被回调的bug。 Refs: #91 。
- 修复: 在无网络状态下，启动下载，如果存在重试的机会，下载的进度被覆盖，导致下次无法断点续传的bug。 Closes #92 。
- 修复: 有可能在'检测是否可以复用'到'检测是否在下载队列'的这段时间内已经下载完成但是任务还在队列中的极端情况。
- 修复: 线性任务，在下载进程被杀重新启动被转为并行任务的bug。

## Version 0.2.2

_2016-04-06_

#### 新接口

- 添加 `FileDownloadHttpException` 与 `FileDownloadGiveUpRetryException`, 优化异常回调处理机制. Closes #67 。
- 初始化 `FileDownloader` 传入参数由原来需要 `Application` 改为 需要 `Context`( `FileDownloader#init(Context)` ), 优化接口，并且便于单元测试。 Closes #54 。

#### 性能与提高

- 提高稳定性: 在开始获取数据之前，先检查是否有足够的空间用于存储下载文件，如果不够直接抛异常，如果足够将锁定对应空间用于正常存储正在下载的文件。 Closes #46 。
- 提高实用性: 断点续传支持，不再强制要求Etag存在；支持不需要Etag，只要后台支持 `Range` 头部参数就可以支持断点续传。 Close #35 , #66 。

#### 修复

- 修复: 在 `FileDownloadLog.NEED_LOG` 为 `true` 时，并且事件无效的情况下，`EventPool` 出现 `IllegalFormatConversionException` 异常的问题。 Closes #30 。
- 修复: 在 Filedownloader进程被杀以后， 在 `IFileDownloadIPCService` 出现异常。Closes #38 。
- 修复: 修复 response-body 可能存在的泄漏: 'WARNING: A connection to https://... was leaked. Did you forget to close a response body?' Closes #68 。
- 修复: 使用 `internal-string` 作为同步的对象，而非直接用 String对象。
- 修复: 在一些情况下如果存在重复任务，在高并发下进行中的回调次数可能不对的bug。

#### 其他

- 所依赖的okhttp从`3.1.2`升到`3.2.0`。

## Version 0.2.0

_2016-02-15_

#### 新接口

- `filedownloader.properties-http.lenient`: 添加`http.lenient`用于配置下载引擎中是否需要忽略一些http规范性的错误(如: 忽略 `can't know the size of the download file, and its Transfer-Encoding is not Chunked`), 默认值`false`。
- `FileDownloadNotificationHelper`: 用于支持在通知栏中的通知对下载引擎中任务下载状态同步的快速集成。
- `FileDownloader#init(Application,OkHttpClientCustomMaker)`: 用于为下载引擎提供定制的OkHttpClient。

#### 修复

- 修复: 需要重新启动的列表(`FileDownloadTask.NEED_RESTART_LIST`)不为空并且下载服务断开时出现`Concurrent Modification Exception`的异常。
- 修复: 下载引擎连接丢失以后，重连任务的回调失效的bug。
- 修复: 在一些高并发下载情况下，对队列进行暂停，部分暂停不生效的bug。

## Version 0.1.9

_2016-01-23_

> 引擎默认会打开 避免掉帧的处理(使得一次回调(FileDownloadListener)不至于太频繁导致手机显示掉帧), 如果你希望关闭这个功能（关闭以后，所有回调会与之前版本一样，所有的回调会立马抛一个消息ui线程(Handler)）: `FileDownloader.getImpl().disableAvoidDropFrame()`.

#### 新接口


- `FileDownloadMonitor`: 现在你可以通过这个来添加一个全局的监听器，方便调试或打点
- `FileDownloader#enableAvoidDropFrame(void)`: 开启 避免掉帧, 原理最多10ms抛一个消息到ui线程，每次在ui线程每次处理5个回调(FileDownloadListener), 默认: 开启。
- `FileDownloader#disableAvoidDropFrame(void)`: 关闭 避免掉帧，会和之前的版本一样，每个回调(FileDownloadListener)都抛一个消息到ui线程，如果频率非常高（如高并发的文件检测）可以导致ui线程被DDOS。
- `FileDownloader#isEnabledAvoidDropFrame(void)`: 是否是 开启了避免掉帧，目前如果没有设置默认是开启的。
- `FileDownloader#setGlobalPost2UIInterval(intervalMillisecond:int)`: 设置最多intervalMillisecond毫秒抛一个消息到ui线程，是 避免掉帧的具体设置。默认: 10ms，如果设置为小于0的数值，会 关闭 避免掉帧。
- `FileDownloader#setGlobalHandleSubPackageSize(packageSize:int)`: 设置每次在ui线程每次处理packageSize个回调，如果已经关闭了 避免掉帧，那么这个值将没有任何意义，默认: 5个。
- `BaseDownloadTask#setSyncCallback(syncCallback:boolean)`: 是否同步回调该task中的所有的回调(FileDownloadListener), 如果设为true, 该task的所有回调会直接在下载线程直接回调，不会抛到ui线程, 默认: false。
- `BaseDownloadTask#isSyncCallback(void):boolean`: 该task是否设置了所有回调(FileDownloadListener)同步调用(直接在下载线程直接调用，而非抛到ui线程)。
- `FileDownloadUtils#setDefaultSaveRootPath`: 设置全局默认的存储路径(Root Path)，在task没有指定对应的存储路径的时候，会存储在该目录下。
- `FileDownloadQueueSet`: 用于更方便的指定几个task为一个队列，进行并行/串行下载，并且可以很方便的对整个队列中的所有任务进行统一设置。

#### 性能与提高

- 提高可调试性: 提供了一个全局监听器(`FileDownloadMonitor`)，更方便与调试或打点。
- 提高性能: 优化内部EventPool的锁机制，不再处理listener的priority。
- 提高性能: 所有`FileDownloadListener`中的回调将会直接调用，而不再过一层EventPool。

#### 修复

- 修复: `EventPool`中的listener存储器无限制的bug.

## Version 0.1.5

_2016-01-17_

#### 新接口

- `BaseDownloadTask#setTag(key:int, tag:Object)`: 用于存储任意的变量方便回调中使用，以key作为索引。
- `BaseDownloadTask#getTag(key:int)`: 根据key获取存储在task中的变量。
- `BaseDownloadTask#addHeader(name:String, value:String)`: 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误。
- `BaseDownloadTask#addHeader(line:String)`: 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误。
- `BaseDownloadTask#removeAllHeaders(name:String)`: 删除由自定义添加上去请求参数为`{name}`的所有键对。

#### 性能与提高

- 提高性能: 在未打开Log的情况下，屏蔽了所有Log生成的代码。
- 提高可调试性: 重新过滤所有的日志级别，减少高级别日志输出，并且默认将会打出`Warn`、`Error`、`Assert`级别的log以便于用户在未打开日志的情况下也可以定位到基本的组件异常。

#### 修复

- 修复: 在一些高并发的情况下，有可能内部队列存在残留任务的bug，此bug可能可能引发回调被旧的任务吞掉的问题。
- 修复: 出现网络错误，或者其他错误，重新下载无法自动断点续传的bug。

#### 其他

- 所依赖的okhttp从`2.7.1`升到`3.0.1`。

## Version 0.1.4

_2016-01-13_

#### 新接口

- `FileDownloader#unBindServiceIfIdle(void)`: 如果目前下载进程没有任务正在执行，则关停下载进程
- `FileDownloader#getStatus(downloadId)`: 获取下载Id为downloadId的状态(可参考[任务管理demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))
- `FileDownloader#isServiceConnected(void)`: 是否已经启动并且连接上下载进程(可参考[任务管理demo](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/TasksManagerDemoActivity.java))

#### 性能与提高

- 支持[Chunked transfer encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) 数据下载(建议看一眼[Single Task Test](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/SingleTaskTestActivity.java)).
- 提高性能: 减少 IPC。
- 提高性能: 减少线程锁。
- 提高性能: 在`:filedownloader`进程启动时，对数据库中的数据进行第一级别维护。
- 提高性能: 忽略数据库中的`callbackProgressTimes`字段。

#### 修复

- 修复: 在低内存情况下，ui进程处于后台进程的情况下被回收，而下载进程(服务进程)还在, 并且还存在在下载中的任务，此时重新启动ui进程`FileDownloader#pauseAll`无法暂停已经在下载进程启动的任务的bug。
- 修复: 主动调用`FileDownloader#unBinderService`，没有释放连接相关资源的bug。
- 修复: ui进程被干掉，下载进程健还有活跃的并行任务正在下载，ui进程启动以后启动相同的队列列表，无法收到进度只收到warn的bug。

## Version 0.1.3

_2016-01-04_

- 不再受到1.99G限制;如果是大于1.99G的文件，请使用`FileDownloadLargeFileListener`作为监听器，使用对应的`getLargeFileSoFarBytes()`与`getLargeFileTotalBytes()`接口
- 性能优化: 部分接口跨进程通信不受binder thread 阻塞。
- 依赖okhttp，从`2.7.0`升到`2.7.1`

## Version 0.1.2

_2015-12-27_

- 优化线程消化能力
- 修复大队列任务暂停可能部分无效的问题
- 修复大队列并行下载时一定概率下载已经完成回调囤积延后回调的问题

## Version 0.1.1

_2015-12-25_

- event线程区分敏捷线程池与其他线程池，减少资源冗余强制、内部稳定性以及消化能力与性能，
- 添加自动重试接口，新增用户指定如果失败自动重试的次数

## Version 0.1.0

_2015-12-24_

- FileDownloadStatus 由`int`改为`byte`，该参数会频繁的在IPC时被拷贝
- 优化串行or并行任务时，筛选task在准备数据时就筛选好，减少冗余操作，更加安全
- 优化串行任务执行保证使用更可靠的方式

## Version 0.0.9

_2015-12-23_

- 将调用start(启动任务)抛独立线程处理，其中的线程是通过共享非下载进程EventPool中的线程池(可并行8个线程)

## Version 0.0.8

_2015-12-22_

- initial release
