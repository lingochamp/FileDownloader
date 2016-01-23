# Change log

## Version 0.1.9

_2016-01-23_

> 引擎默认会打开 避免掉帧的处理(使得一次回调(FileDownloadListener)不至于太频繁导致手机显示掉帧), 如果你希望关闭这个功能（关闭以后，所有回调会与之前版本一样，所有的回调会立马抛一个消息ui线程(Handler)）: `FileDownloader.getImpl().disableAvoidDropFrame()`.

#### 新接口


- `FileDownloadMonitor`: 现在你可以通过这个来添加一个全局的监听器，方便调试或打点
- `FileDownloader#enableAvoidDropFrame(void)`: 开启 避免掉帧, 原理最多10ms抛一个消息到ui线程，每次在ui线程每次处理5个回调(FileDownloadListener), 默认: 开启。
- `FileDownloader#disableAvoidDropFrame(void)`: 关闭 避免掉帧，会和之前的版本一样，每个回调(FileDownloadListener)都抛一个消息到ui线程，如果频率非常高（如高并发的文件检测）可以导致ui线程被ddos。
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

- 修复: 修复`EventPool`中的listener存储器无限制的bug.

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

- 修复在一些高并发的情况下，有可能内部队列存在残留任务的bug，此bug可能可能引发回调被旧的任务吞掉的问题。
- 修复了出现网络错误，或者其他错误，重新下载无法自动断点续传的bug。

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
