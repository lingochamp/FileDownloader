# Change log

### Version 0.1.4

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

### Version 0.1.3

- 不再受到1.99G限制;如果是大于1.99G的文件，请使用`FileDownloadLargeFileListener`作为监听器，使用对应的`getLargeFileSoFarBytes()`与`getLargeFileTotalBytes()`接口
- 性能优化: 部分接口跨进程通信不受binder thread 阻塞。
- 依赖okhttp，从2.7.0 升到2.7.1

### Version 0.1.2

- 优化线程消化能力
- 修复大队列任务暂停可能部分无效的问题
- 修复大队列并行下载时一定概率下载已经完成回调囤积延后回调的问题

### Version 0.1.1

- event线程区分敏捷线程池与其他线程池，减少资源冗余强制、内部稳定性以及消化能力与性能，
- 添加自动重试接口，新增用户指定如果失败自动重试的次数

### Version 0.1.0

- FileDownloadStatus 由`int`改为`byte`，该参数会频繁的在IPC时被拷贝
- 优化串行or并行任务时，筛选task在准备数据时就筛选好，减少冗余操作，更加安全
- 优化串行任务执行保证使用更可靠的方式

### Version 0.0.9

- 将调用start(启动任务)抛独立线程处理，其中的线程是通过共享非下载进程EventPool中的线程池(可并行8个线程)

### Version 0.0.8

- initial release
