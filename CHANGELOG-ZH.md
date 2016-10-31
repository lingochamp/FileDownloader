# Change log

> [ Change log in english](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG.md)

## Version 1.3.0

_2016-10-31_

#### 新接口

- 新增 `FileDownloadSerialQueue`: 便于动态管理串行执行的队列。 Closes #345.
- 移除 `FileDownloadListener`类中的`callback`方法, 并且新增`FileDownloadListener#isInvalid`方法，用于告知FileDownloader该监听器是否已经无效，不再接收任何消息。
- 新增 `FileDownloader#clearAllTaskData`: 清空`filedownloader`数据库中的所有数据。 Closes #361.

#### 性能与提高

- 提高实用性(`FileDownloadListener#blackCompleted`): 确保`blockCompleted`回调可以接收任何的`Exception`。 Closes #369.
- 提高实用性(service-not-connected): 在service-not-connected-helper中输出提示与原因, 这样当你调用有些需要确保下载服务已经连接上的方式，但下载服务没有连接上时，不但在`Logcat`中可以收到原因，还能收到提示。

#### 修复

- 修复(reuse): 修复调用`BaseDownloadTask#pause`之后短时间内调用`BaseDownloadTask#reuse`方法，可能会抛出异常的问题。 Closes #329.

## Version 1.2.2

_2016-10-15_

#### 修复

- 修复(fatal-crash): 修复当任务没有`FileDownloadListener`时，也不能收到该任务`FileDownloadMonitor.IMonitor#onTaskOver`的回调的问题。 Closes #348.

## Version 1.2.1

_2016-10-09_

#### 修复

- <s>修复(fatal-crash): 修复当任务没有`FileDownloadListener`时，也不能收到该任务`FileDownloadMonitor.IMonitor#onTaskOver`的回调的问题。 Closes #348.</s> 十分的抱歉这个问题在1.2.1版本中依然存在，最终在1.2.2中验证修复。

## Version 1.2.0

_2016-10-04_

#### 新接口

- 新增 `FileDownloader#insureServiceBind()`: 便于阻塞当前线程，并且启动下载服务，服务启动之后再执行需要服务的请求。 Refs #324.
- 新增 `FileDownloader#insureServiceBindAsync()`: 便于启动下载服务，并且在服务启动之后，执行需要下载服务的请求。 Refs #324.
- 新增 `FileDownloader#bindService(runnable:Runnable)`: 便于启动下载服务，并且在服务启动之后，执行 `runnable`。 Refs #324.
- 新增 `FileDownloader#init(Context,InitCustomMaker)`: 便于初始化下载引擎的时候可以传入更多的定制化组件。 Refs #157.

#### Enhancement

- 提高实用性(`InitCustomMaker#database`): 支持定制化数据库组件(`FileDownloadDatabase`)，并且实现默认的数据库组件： `DefaultDatabaseImpl`。 Closes #157.
- 提高实用性(`InitCustomMaker#outputStreamCreator`): 支持定制化输出流组件(`FileDownloadOutputStream`)，并且实现默认的输出流组件： `FileDownloadRandomAccessFile`，与一些可替代的组件： `FileDownloadBufferedOutputStream`、`FileDownloadOkio`。Closes #301.

## Version 1.1.5

_2016-09-29_

#### 新接口

- 支持在`filedownloader.properties`中配置`file.non-pre-allocation`: 是否不需要在开始下载的时候，预申请整个文件的大小(`content-length`), 默认值是`false`。Closes #313 .

#### 修复

- 修复(fatal-crash): 修复由于`ThreadPoolExecutor#getActiveCount()`是一个大概的值，导致在其反回的不是正确值时，thread-pool库中存在`StackOverflowError`Crash的问题。Closes #321 .
- 修复(minor-crash): 修复在一些小概率情况下出现Crash Message是'No reused downloaded file in this message'的IllegalStateException的问题。 Closes #316 .
- 修复(minor-crash): 修复当在下载服务还没有连接上时，同时有几个串行队列任务需要执行，在一些小概率的情况下，一些相同的任务会被启动两次导致crash的问题。 Refs #282 .

#### 其他

- 依赖: 取消对thread-pool库的依赖。 Refs #321 .
- MinSDKVersion: 升级`minSdkVersion` : 8->9。 Refs #321 .

## Version 1.1.0

_2016-09-13_

#### 新接口

- 新增 `BaseDownloadTask#setWifiRequired`: 设置任务是否只允许在Wifi网络环境下进行下载。 默认值 `false`。 Closes #281 .

#### 性能与提高

- 提高性能: 替换所有的线程池为exceed-wait-pool(更多详情参见: `FileDownloadExecutors`) 并且所有线程池中的线程将会在闲置五秒后自动结束。 Refs #303 .
- 提高实用性: 当有异常从`FileDownloadListener#blockComplete`抛出时，将会被`catch`并且回调到`FileDownloadListener#error`中而非回调`FileDownloadListener#completed`。 Closes #305 .

#### 修复

- 修复(lost-connect): 避免等待服务连接的列表中在一些小概率情况下存在重复任务的问题。

## Version 1.0.2

_2016-09-06_

#### 修复

- 修复: 服务还没有连接上时，启动的'队列任务'被放在等待队列，当服务连接上以后，FileDownloader尝试重启这些等待队列中的任务，但是抛了`IllegalStateException`的Bug。 Closes #307 .

## Version 1.0.1

_2016-09-05_

#### 新接口

> 如果你之前有使用现在已经被申明弃用的方法`BaseDownloadTask#ready()`, 只需要简单的将它迁移为:`BaseDownloadTask#asInQueueTask():InQueueTask`并且调用`InQueueTask#enqueue()`。

- 添加`BaseDownloadTask#asInQueueTask():InQueueTask`并申明弃用`BaseDownloadTask#ready()`: 申明当前任务是队列任务，并且可以通过`InQueueTask#enqueue()`将当前任务放入全局队列以便于启动队列任务的时候，能被队列收编执行。`InQueueTask#enqueue()`中的操作与`BaseDownloadTask#ready()`相同, 我们通过这个方式封装`ready()`是为了让你更加清晰的了解: 只有当前任务是队列任务，才需要调用该方法；如果当前任务不是队列任务，而却调用了这个方法，你将会收到一个异常(具体异常的原因可以移步到`DownloadTask#start`报的异常信息进行了解)。

#### 修复

- 修复: 当有使用相同`listener`对象的多个孤立任务与队列任务在不同的线程中同时被启动时(后)，有可能会遇到IllegalStateException异常的问题。 Closes #282 .

## Version 1.0.0

_2016-08-21_

#### 新接口

- 添加 `BaseDownloadTask#cancel`: 这个方法是为了说明为什么`pause`的操作也可以达到`cancel`的作用。

#### 性能与提高

- 提高性能: 持有`isDownloaderProcess`的结果，防止多次判断。
- 提高实用性: 重构代码的可见层。Closes #283
- 提高实用性: 完善Java Doc。Closes #284
- 提高实用性: 提供Java Doc 站点: http://fd.dreamtobe.cn 。Closes #284

## Version 0.3.5

_2016-08-16_

#### 性能与提高

- 提高实用性: 为FileDownloader中的所有线程添加线程名。
- 提高性能: 调整`block-completed-thread-pool`中的核心线程数: 5->2，减少资源的浪费。

#### 修复

- 修复(SQLiteFullException): 覆盖了在整个下载过程中可能遇到`SQLiteFullException`的错误，就捕获相关错误并回调回 `FileDownloadListener#error` 。 Closes #243
- 修复(提供目录的情况): 修复若是提供的是文件夹，并且对应的任务已经下载完成，再次启动的时候，在直接回调`FileDownloadListener#completed`时，获取的`targetFilePath`可能为null的问题。 Closes #237

## Version 0.3.4

_2016-07-31_

#### 新接口

- 添加 `FileDownloader#clear`: 用于强制根据任务ID清理其在filedownloader中的数据。Closes #218

#### 性能与提高

- 提高实用性: 为 `FileDownloader#start(FileDownloader, boolean)` 添加返回值: 是否成功启动任务下载。Closes #215
- 提高实用性: `FileDownloader#pause` 暂停任务时，不再仅仅是暂停一个任务，而是暂停掉所有ID为指定ID的运行中的任务。

#### 修复

- 修复(初始化-CRASH): 修复初始化FileDownloader时，从`ActivityManager`获取到运行中进程信息为空时发生CRASH。Closes #210
- 修复(小概率-CRASH): 修复当FileDownloadService已经`onDestroy`后，还接收到`snapshot-message`时发生CRASH的情况。 Closes #213
- 修复(消息流准确性): 在真正启动下载时删除目标文件，以此保证当有相同任务正在下载时，获取下载状态，不会获取到已经下载完成的错误的状态。Closes #220
- 修复(启动线性下载): 收集未绑定的任务进行启动而非只是根据FileDownloadListener去收集任务，修复无法启动两个相同`FileDownloadListener`的队列。Closes #233
- 修复(清理Messenger): 在回调 结束的消息 的回调之前进行清理任务的Messenger，而非在回调之后清理，以此确保在回调方法中可以调用`BaseDownloadTask#reuse`。Closes #229

#### 其他

- 所依赖的okhttp从`3.3.1`升到`3.4.1`。

## Version 0.3.3

_2016-07-10_

#### 新接口

- 添加 `FileDownloadUtils#getTempPath`: 获取用于存储还未下载完成文件的临时存储路径: `filename.temp`。 Refs #172.
- 添加 `FileDownloadUtils#isFilenameConverted(context:Context)`: 判断是否所有数据库中下载中的任务的文件名都已经从`filename`(在旧架构中)转为`filename.temp`。
- 添加 `FileDownloader#getStatusIgnoreCompleted(id:int)`:  获取不包含已完成状态的下载状态(如果任务已经下载完成，将收到`INVALID`)。
- 添加 `FileDownloader#getStatus(id:int, path:String)`:  获取下载状态。
- 添加 `FileDownloader#getStatus(url:String, path:String)`:  获取下载状态
- 添加 `FileDownloadUtils#generateId(url:String, path:String, pathAsDirectory:boolean)`: 生成可以被FileDownloader识别的`Download Id`。
- 添加 `BaseDownloadTask#setPath(path:String, pathAsDirectory:boolean)`: 如果`pathAsDirectory`是`true`,`path`就是存储下载文件的文件目录(而不是路径)，此时默认情况下文件名`filename`将会默认从`response#header`中的`contentDisposition`中获得。
- 添加 `BaseDownloadTask#isPathAsDirectory`: 判断`BaseDownloadTask#getPath()`返回的路径是文件存储目录(`directory`)，还是文件存储路径(`directory/filename`)。
- 添加 `BaseDownloadTask#getTargetFilePath`: 获取目标文件的存储路径。
- 添加 `FileDownloadQueueSet#setDirectory`: 设置队列中所有任务文件存储的目录。

#### 性能与提高

- 提高实用性: 支持将`path`作为目录来存储文件，在这个情况下，文件名默认将从`response#header`中的`contentDisposition`中获得。 Refs #200.
- 提高实用性: 将还未下载完成的文件存储在临时文件中(`filename.temp`)。 Refs #172.
- 提高性能: FileDownloader不再将已经完成下载的任务存储在数据库中，判定任务是否已经下载完成，直接通过判断目标文件是否存在。 Refs #176, #172.
- 提高稳定性: 选用状态是`INVALID`或`progress`优先接收`completed`消息, 以此确保`connected`状态的任务能够留下来接收`progress`状态的消息。 Refs #123
- 提高稳定性: 扩张 __任务同步锁__ 到 __获取相同ID任务队列__ 的外面，以此修复由于有些状态在 __获取相同ID任务队列__ 与 __等待任务同步锁__ 的过程中已经被改变导致有些消息不能被消耗的问题。

#### 修复

- 修复(DB-维护): 保留状态是`pending`并且已经下载的字节数大于0的Model，因为这些Model可以用于恢复断点续传。 Closes #176.
- 修复(crash-NPE): FileDownloader 可能遇到NPE当下载监听器被移除，但是对应任务还在FileDownloader中运行。 Closes #171.

## Version 0.3.2

_2016-06-12_

#### 新接口

- 添加 `BaseDownloadTask#setCallbackProgressMinInterval`: 用于设置每个'progress'方法回调的间隔。 Closes #167 .
- 添加 `FileDownloader#setMaxNetworkThreadCount`: 用于设置最大同时下载的数目（最大同时运行的网络线程）。 Closes #168.
- 添加 `FileDownloader#init(Context,OkHttpClientCustomMaker,int)`: 在下载服务初始化的时候接受设置最大同时下载数目（最大同时运行的网络线程）。 Closes #168.

#### 性能与提高

- 提高稳定性: 确保每个'progress'回调方法之间的最小间隔是5ms，防止对于一个任务而言'progress'回调太频繁导致'防掉帧队列'极速膨胀导致各类Action响应都延时。 Closes #167.
- 提高实用性: 在请求的操作需要在下载服务中完成，但是还未连接上下载服务时，输出对应的'warn'级别的日志。
- 提高性能: 使用`SparseArray`代替`HashMap`用于索引所有的`FileDownloadModel`。

#### 修复

- 修复(crash): 修复在某个下载任务开始下载时，发现任务的状态不正确的情况下，输出日志中提供了错误的参数类型导致的Crash。
- 修复(强制重新下载): 修复错误逻辑导致设置`BaseDownloadTask#setForceReDownload(true)`并且任务已经下载完成会促发'warn'的回调，却没有进行强制重新下载的Bug。
- 修复(class-type): 保持`SocketTimeOutException`的Class类型，不再关心`Throwable`的`message`是否为空。

#### 其他

- 所依赖的okhttp从`3.2.0`升到`3.3.1`。

## Version 0.3.1

_2016-05-19_

#### 性能与提高

- 提高稳定性: 在结束下载时确保缓存中的数据都写入文件。

## Version 0.3.0

_2016-05-13_

#### 修复

> 为什么FileDownload服务可以运行在UI进程? 参考 [filedownloader.properties](https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties).

- 修复(下载服务共享UI进程时): 修复在下载服务不是运行在独立进程的情况下（非默认情况），附加的header没有带上请求的bug。Closes #149.

## Version 0.2.9

_2016-05-10_

#### 新接口

- 添加 `BaseDownloadTask#isUsing():boolean`: 用于判断当前的Task对象是否在引擎中启动过. Closes #137 。

#### 修复

- 修复(高并发情况下的npe): 当任务的状态是一个未预期的状态是，提供一个默认的错误快照，避免出现npe 。
- 修复(返回错误码-416): 覆盖返回错误码是416或者当出现已下载大小大于等于文件总大小的时候依然断点续传的bug。

## Version 0.2.8

_2016-05-02_

#### 新接口

- 添加 `BaseDownloadTask#getId():int`: 弃用(没有删除该接口) `getDownloadId()`, 建议使用 `getId()` 代替 。

#### 性能与提高

- 提高稳定性: 重构任务启动器，使得启动任务更加可维护，以及标记任务过期更加可靠。
- 提高稳定性: 重构将事件派发给`FileDownloadListener`的体系，新的体系就如同，派件员与快递驿站的关系，每次都会对事件进行快照，打包为一个消息快件，派发到驿站，转包给 `FileDownloadListener`。
- 提高稳定性: 覆盖所有的有关暂停的高并发情况，删掉一些符合预期的警告性日志。
- 提高性能: 减少FileDownloader database I/O 。
- 提高性能: 减少创建对象(更少的内存分配请求，对于GC友好)对于每次回调, 对于一个下载状态的更新，只创建一个快照，整个通讯架构使用。

#### 修复

- 修复: 提供明确的locale用于格式化字符串，避免一些默认locale是非预期的情况发生。Closes #127

## Version 0.2.7

_2016-04-22_

#### 新接口

- 添加 `FileDownloader#setTaskCompleted(taskAtomList:List<FileDownloadTaskAtom>)`: 用于告诉FileDownloader引擎，指定的一系列的任务都已经通过其他方式(非FileDownloader)下载完成。

#### 性能与提高

- 提高稳定性: 假如在下载进程调用 `bindService` 直接抛异常，防止用户在使用过程中，错误的在下载进程绑定服务，而没有暴露这个根本问题，引发其他一系列的异常。Closes #119。

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
