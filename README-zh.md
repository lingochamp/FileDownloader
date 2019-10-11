# FileDownloader
Android 文件下载引擎，稳定、高效、灵活、简单易用

[![Gitter][gitter_svg]][gitter_url]
[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
[![Build Status][build_status_svg]][build_status_link]
[![][filedownloader_snapshot_svg]](https://oss.sonatype.org/content/repositories/snapshots/com/liulishuo/filedownloader/)


> [README DOC](https://github.com/lingochamp/FileDownloader/blob/master/README.md)

---

#### 版本迭代日志: [Change Log](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG.md)

#### 英文文档: [Wiki](https://github.com/lingochamp/FileDownloader/wiki)、[优化建议](https://github.com/lingochamp/FileDownloader/wiki/Optimize-Tutorial)

---

### FileDownloader2 

现在, [FileDownloader2-OkDownload](https://github.com/lingochamp/okdownload) 已经正式发布, okdownload继承了所有FileDownloader的优点，甚至做了更多的优化以及更多的特性。

由于FileDownloader的单元测试覆盖太低，因此所有的进一步的需求以及提高都将会在okdownload上进行实现而非FileDownloader，而FileDownloader本身将只会关注于修复Bug。

---

### 特点

- 简单易用
- 单任务多线程/多连接/分块下载(并支持通过`ConnectionCountAdapter`定制)
- 高并发
- 灵活
- 可选择性支持: 独立/非独立进程
- 自动断点续传

#### 需要注意

- 当下载的文件大小可能大于1.99GB(2^31-1`=2_147_483_647 = 1.99GB`)的时候, 请使用`FileDownloadLargeFileListener`而不是`FileDownloadListener`(同理使用`getLargeFileSofarBytes()`与`getLargeFileTotalBytes()`)
- 暂停: paused, 恢复: 直接调用start，默认就是断点续传
- 引擎默认会打开避免掉帧的处理(使得在有些情况下回调(FileDownloadListener)不至于太频繁导致ui线程被ddos), 如果你希望关闭这个功能（关闭以后，所有回调会与0.1.9之前的版本一样，所有的回调会立马抛一个消息ui线程(Handler)）
- 如果没有特殊需要，直接通过配置`filedownloader.properties`将`process.non-separate`置为`true`，可以有效减少每次回调IPC带来的I/O。

---

## Android 系统适配

### 适配 Android 8.0

从 Android 8.0 开发，后台服务的限制增强了，可以参考[这里](https://developer.android.com/about/versions/oreo/background)了解更多信息。
因此，自 FileDownloader 1.7.6 版本开始， Android 8.0 及之后的系统上，如果在后台启动下载服务，这个服务将会是一个前台服务，同时你会看到一个标题为 "FileDownloader" 的通知。
你可以参考[这里](https://github.com/lingochamp/FileDownloader/wiki/Compatibility-of-Android-O-Servic)去自定义通知的内容。

### 适配 Android 9.0

从 Android 9.0 (API level 28) 开始，明文请求默认被禁止，你可以在[这里](https://stackoverflow.com/questions/45940861/android-8-cleartext-http-traffic-not-permitted)了解详细信息。
FileDownloader 1.7.6 已经在 demo 中处理了此问题。

根据[迁移笔记](https://developer.android.com/about/versions/pie/android-9.0-migration#tya)，`FOREGROUND_SERVICE` 这个权限已经在 1.7.6 版本添加到 library 的 manifest 里面了。

---

## 欢迎提交 Pull requests

- 尽量多的英文注解。
- 每个提交尽量的细而精准。
- Commit message 遵循: [AngularJS's commit message convention](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#-git-commit-guidelines)。

---

## I. 效果

![][single_demo_gif]
![][chunked_demo_gif]
![][serial_tasks_demo_gif]
![][parallel_tasks_demo_gif]
![][tasks_manager_demo_gif]
![][mix_tasks_demo_gif]
![][avoid_drop_frames_1_gif]
![][avoid_drop_frames_2_gif]


## II. 使用

在项目中引用:

```groovy
implementation 'com.liulishuo.filedownloader:library:1.7.7'
```

> 如果是eclipse引入jar包参考: [这里](https://github.com/lingochamp/FileDownloader/issues/212#issuecomment-232240415)

如果需要引入snapshot版本，请添加sonatype的仓库:

```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

#### 全局初始化

如果你需要注册你的定制组件，你需要在`Application#onCreate`中调用`FileDownloader.setupOnApplicationOnCreate(application):InitCustomMaker`, 否则你只需要在使用FileDownloader之前的任意时候调用`FileDownloader.setup(Context)`即可。

这些初始化方法都十分的简单，不会启动下载服务，一般都是在10ms内完成。

#### 启动单任务下载

```java
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

#### 启动多任务下载

```java
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

// 第一种方式 :

//for (String url : URLS) {
//    FileDownloader.getImpl().create(url)
//            .setCallbackProgressTimes(0) // 由于是队列任务, 这里是我们假设了现在不需要每个任务都回调`FileDownloadListener#progress`, 我们只关系每个任务是否完成, 所以这里这样设置可以很有效的减少ipc.
//            .setListener(queueTarget)
//            .asInQueueTask()
//            .enqueue();
//}

//if(serial){
    // 串行执行该队列
//    FileDownloader.getImpl().start(queueTarget, true);
// }

// if(parallel){
    // 并行执行该队列
//    FileDownloader.getImpl().start(queueTarget, false);
//}

// 第二种方式:

final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(downloadListener);

final List<BaseDownloadTask> tasks = new ArrayList<>();
for (int i = 0; i < count; i++) {
     tasks.add(FileDownloader.getImpl().create(Constant.URLS[i]).setTag(i + 1));
}

queueSet.disableCallbackProgressTimes(); // 由于是队列任务, 这里是我们假设了现在不需要每个任务都回调`FileDownloadListener#progress`, 我们只关系每个任务是否完成, 所以这里这样设置可以很有效的减少ipc.

// 所有任务在下载失败的时候都自动重试一次
queueSet.setAutoRetryTimes(1);

if (serial) {
  // 串行执行该任务队列
     queueSet.downloadSequentially(tasks);
     // 如果你的任务不是一个List，可以考虑使用下面的方式，可读性更强
//      queueSet.downloadSequentially(
//              FileDownloader.getImpl().create(url).setPath(...),
//              FileDownloader.getImpl().create(url).addHeader(...,...),
//              FileDownloader.getImpl().create(url).setPath(...)
//      );
}

if (parallel) {
  // 并行执行该任务队列
   queueSet.downloadTogether(tasks);
   // 如果你的任务不是一个List，可以考虑使用下面的方式，可读性更强
//    queueSet.downloadTogether(
//            FileDownloader.getImpl().create(url).setPath(...),
//            FileDownloader.getImpl().create(url).setPath(...),
//            FileDownloader.getImpl().create(url).setSyncCallback(true)
//    );
}

// 最后你需要主动调用start方法来启动该Queue
queueSet.start()

// 串行任务动态管理也可以使用FileDownloadSerialQueue。
```

#### 全局接口说明(`FileDownloader`)

> 所有的暂停，就是停止，会释放所有资源并且停到所有相关线程，下次启动的时候默认会断点续传

| 方法名 | 备注
| --- | ---
| setup(Context) | 如果不需要注册定制组件，就使用该方法在使用下载引擎前调用，该方法只会缓存Context
| setupOnApplicationOnCreate(application):InitCustomMaker | 如果需要注册定制组件，就在Application#onCreate中调用该方法来注册定制组件以及初始化下载引擎，该方法不会启动下载服务
| create(url:String) | 创建一个下载任务
| start(listener:FileDownloadListener, isSerial:boolean) | 启动是相同监听器的任务，串行/并行启动
| pause(listener:FileDownloadListener) | 暂停启动相同监听器的任务
| pauseAll(void) | 暂停所有任务
| pause(downloadId) | 暂停downloadId的任务
| clear(downloadId, targetFilePath) | 强制清理ID为downloadId的任务在filedownloader中的数据
| getSoFar(downloadId) | 获得下载Id为downloadId的soFarBytes
| getTotal(downloadId) | 获得下载Id为downloadId的totalBytes
| bindService(void) | 主动启动下载进程(可事先调用该方法(可以不调用)，保证第一次下载的时候没有启动进程的速度消耗)
| unBindService(void) | 主动关停下载进程
| unBindServiceIfIdle(void) | 如果目前下载进程没有任务正在执行，则关停下载进程
| isServiceConnected(void) | 是否已经启动并且连接上下载进程(可参考任务管理demo中的使用)
| getStatusIgnoreCompleted(downloadId) | 获取不包含已完成状态的下载状态(如果任务已经下载完成，将收到`INVALID`)
| getStatus(id:int, path:String) | 获取下载状态
| getStatus(url:String, path:String) | 获取下载状态
| setGlobalPost2UIInterval(intervalMillisecond:int) | 为了避免掉帧，这里是设置了最多每interval毫秒抛一个消息到ui线程(使用Handler)，防止由于回调的过于频繁导致ui线程被ddos导致掉帧。 默认值: 10ms. 如果设置小于0，将会失效，也就是说每个回调都直接抛一个消息到ui线程
| setGlobalHandleSubPackageSize(packageSize:int) | 为了避免掉帧, 如果上面的方法设置的间隔是一个小于0的数，这个packageSize将不会生效。packageSize这个值是为了避免在ui线程中一次处理过多回调，结合上面的间隔，就是每个interval毫秒间隔抛一个消息到ui线程，而每个消息在ui线程中处理packageSize个回调。默认值: 5
| enableAvoidDropFrame(void) | 开启 避免掉帧处理。就是将抛消息到ui线程的间隔设为默认值10ms, 很明显会影响的是回调不会立马通知到监听器(FileDownloadListener)中，默认值是: 最多10ms处理5个回调到监听器中
| disableAvoidDropFrame(void) | 关闭 避免掉帧处理。就是将抛消息到ui线程的间隔设置-1(无效值)，这个就是让每个回调都会抛一个消息ui线程中，可能引起掉帧
| isEnabledAvoidDropFrame(void) | 是否开启了 避免掉帧处理。默认是开启的
| startForeground(id:int, notification:Notification) | 设置FileDownloadService为前台模式，保证用户从最近应用列表移除应用以后下载服务不会被杀
| stopForeground(removeNotification:boolean) | 取消FileDownloadService的前台模式
| setTaskCompleted(url:String, path:String, totalBytes:long) | 用于告诉FileDownloader引擎，以指定Url与Path的任务已经通过其他方式(非FileDownloader)下载完成
| setTaskCompleted(taskAtomList:List<FileDownloadTaskAtom>) | 用于告诉FileDownloader引擎，指定的一系列的任务都已经通过其他方式(非FileDownloader)下载完成
| setMaxNetworkThreadCount(int) | 设置最大并行下载的数目(网络下载线程数), [1,12]
| clearAllTaskData() | 清空`filedownloader`数据库中的所有数据

#### 定制化组件接口说明(`InitCustomMaker`)

| 方法名 | 需实现接口 | 已有组件 | 默认组件 | 说明
| --- | --- | --- | --- | ---
| database | FileDownloadDatabase | RemitDatabase、SqliteDatabaseImpl、NoDatabaseImpl | RemitDatabase | 传入定制化数据库组件，用于存储用于断点续传的数据
| connection | FileDownloadConnection | FileDownloadUrlConnection | FileDownloadUrlConnection | 传入定制化的网络连接组件，用于下载时建立网络连接
| outputStreamCreator | FileDownloadOutputStream | FileDownloadRandomAccessFile | FileDownloadRandomAccessFile | 传入输出流组件，用于下载时写文件使用
| maxNetworkThreadCount | - | - | 3 | 传入创建下载引擎时，指定可用的下载线程个数
| ConnectionCountAdapter | ConnectionCountAdapter | DefaultConnectionCountAdapter | DefaultConnectionCountAdapter | 根据任务指定其线程数
| IdGenerator | IdGenerator | DefaultIdGenerator | DefaultIdGenerator | 自定义任务Id生成器

> - 如果你希望Okhttp作为你的网络连接组件，可以使用[这个库](https://github.com/Jacksgong/filedownloader-okhttp3-connection)。
> - 如果你不希望FileDownloader用到任何的数据库(是用于存储任务的断点续成信息的)，只需要使用[NoDatabaseImpl.java](https://github.com/lingochamp/FileDownloader/blob/master/library/src/main/java/com/liulishuo/filedownloader/services/NoDatabaseImpl.java)即可。

#### Task接口说明

| 方法名 | 备注
| --- | ---
| setPath(path:String) | 下载文件的存储绝对路径
| setPath(path:String, pathAsDirectory:boolean) | 如果`pathAsDirectory`是`true`,`path`就是存储下载文件的文件目录(而不是路径)，此时默认情况下文件名`filename`将会默认从`response#header`中的`contentDisposition`中获得
| setListener(listener:FileDownloadListener) | 设置监听，可以以相同监听组成队列
| setCallbackProgressTimes(times:int) | 设置整个下载过程中`FileDownloadListener#progress`最大回调次数
| setCallbackProgressIgnored() | 忽略所有的`FileDownloadListener#progress`的回调
| setCallbackProgressMinInterval(minIntervalMillis:int) | 设置每个`FileDownloadListener#progress`之间回调间隔(ms)
| setTag(tag:Object) | 内部不会使用，在回调的时候用户自己使用
| setTag(key:int, tag:Object) | 用于存储任意的变量方便回调中使用，以key作为索引
| setForceReDownload(isForceReDownload:boolean) | 强制重新下载，将会忽略检测文件是否健在
| setFinishListener(listener:FinishListener) | 结束监听，仅包含结束(over(void))的监听
| setAutoRetryTimes(autoRetryTimes:int) | 当请求或下载或写文件过程中存在错误时，自动重试次数，默认为0次
| setSyncCallback(syncCallback:boolean)  | 如果设为true, 所有FileDownloadListener中的回调都会直接在下载线程中回调而不抛到ui线程, 默认为false
| addHeader(name:String, value:String) | 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误
| addHeader(line:String) | 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误
| setMinIntervalUpdateSpeed(minIntervalUpdateSpeedMs:int) | 设置下载中刷新下载速度的最小间隔
| removeAllHeaders(name:String) | 删除由自定义添加上去请求参数为`{name}`的所有键对
| setWifiRequired(isWifiRequired:boolean) | 设置任务是否只允许在Wifi网络环境下进行下载。 默认值 `false`
| asInQueueTask(void):InQueueTask | 申明该任务将会是队列任务中的一个任务，并且转化为`InQueueTask`，之后可以调用`InQueueTask#enqueue`将该任务入队以便于接下来启动队列任务时，可以将该任务收编到队列中
| start(void) | 启动孤立的下载任务
| pause(void) | 暂停下载任务(也可以理解为停止下载，但是在start的时候默认会断点续传)
| getId(void):int | 获取唯一Id(内部通过url与path生成)
| getUrl(void):String | 获取下载连接
| getCallbackProgressTimes(void):int | 获得progress最大回调次数
| getCallbackProgressMinInterval(void):int | 获得每个progress之间的回调间隔(ms)
| getPath(void):String | 获取文件路径 或 文件目录
| isPathAsDirectory | 判断`getPath()`返回的路径是文件存储目录(`directory`)，还是文件存储路径(`directory/filename`)
| getTargetFilePath | 获取目标文件的存储路径
| getListener(void):FileDownloadListener | 获取监听器
| getSoFarBytes(void):int | 获取已经下载的字节数
| getTotalBytes(void):int | 获取下载文件总大小
| getStatus(void):int | 获取当前的状态
| isForceReDownload(void):boolean | 是否强制重新下载
| getEx(void):Throwable | 获取下载过程抛出的Throwable
| isReusedOldFile(void):boolean | 判断是否是直接使用了旧文件(检测是有效文件)，没有启动下载
| getTag(void):Object | 获取用户setTag进来的Object
| getTag(key:int):Object | 根据key获取存储在task中的变量
| isContinue(void):boolean | 是否成功断点续传
| getEtag(void):String | 获取当前下载获取到的ETag
| getAutoRetryTimes(void):int | 自动重试次数
| getRetryingTimes(void):int | 当前重试次数。将要开始重试的时候，会将接下来是第几次
| isSyncCallback(void):boolean | 是否是设置了所有FileDownloadListener中的回调都直接在下载线程直接回调而不抛到ui线程
| getSpeed():int | 获取任务的下载速度, 下载过程中为实时速度，下载结束状态为平均速度
| isUsing():boolean | 判断当前的Task对象是否在引擎中启动过
| isWifiRequired():boolean | 获取当前任务是否被设置过只允许在Wifi网络环境下下载

#### 监听器(`FileDownloadListener`)说明

##### 一般的下载回调流程:

```
pending -> started -> connected -> (progress <->progress) -> blockComplete -> completed
```

##### 可能会遇到以下回调而直接终止整个下载过程:

```
paused / completed / error / warn
```

##### 如果检测存在已经下载完成的文件(可以通过`isReusedOldFile`进行决策是否是该情况)(也可以通过`setForceReDownload(true)`来避免该情况):

```
blockComplete -> completed
```

##### 方法说明

| 回调方法 | 备注 | 带回数据
| --- | --- | ---
| pending | 等待，已经进入下载队列 | 数据库中的soFarBytes与totalBytes
| started | 结束了pending，并且开始当前任务的Runnable | -
| connected | 已经连接上 | ETag, 是否断点续传, soFarBytes, totalBytes
| progress | 下载进度回调 | soFarBytes
| blockComplete | 在完成前同步调用该方法，此时已经下载完成 | -
| retry | 重试之前把将要重试是第几次回调回来 | 之所以重试遇到Throwable, 将要重试是第几次, soFarBytes
| completed | 完成整个下载过程 | -
| paused | 暂停下载 | soFarBytes
| error | 下载出现错误 | 抛出的Throwable
| warn | 在下载队列中(正在等待/正在下载)已经存在相同下载连接与相同存储路径的任务 | -


![][file_download_listener_callback_flow_png]

##### 由于`FileDownloadListener`中的方法回调过快，导致掉帧?

> 你有两种方法可以解决这个问题

1. `FileDownloader#enableAvoidDropFrame`, 默认 就是开启的
2. `BaseDownloadTask#setSyncCallback`, 默认是false, 如果设置为true，所有的回调都会在下载线程直接同步调用而不会抛到ui线程。

#### `FileDownloadMonitor`

> 你可以添加一个全局监听器来进行打点或者是调试

| 方法名 | 备注
| --- | ---
| setGlobalMonitor(monitor:IMonitor) | 设置与替换一个全局监听器到下载引擎中
| releaseGlobalMonitor(void) | 释放已经设置到下载引擎中的全局监听器
| getMonitor(void) | 获取已经设置到下载引擎中的全局监听器


##### `FileDownloadMonitor.IMonitor`

> 监听器接口类

|  接口 | 备注
| --- | ---
| onRequestStart(count:int, serial:boolean, lis:FileDownloadListener) | 将会在启动队列任务是回调这个方法
| onRequestStart(task:BaseDownloadTask) | 将会在启动单一任务时回调这个方法
| onTaskBegin(task:BaseDownloadTask) | 将会在内部接收并开始task的时候回调这个方法(会在`pending`回调之前)
| onTaskStarted(task:BaseDownloadTask) | 将会在task结束pending开始task的runnable的时候回调该方法
| onTaskOver(task:BaseDownloadTask) | 将会在task走完所有生命周期是回调这个方法

#### `FileDownloadUtils`

| 方法名 | 备注
| --- | ---
| setDefaultSaveRootPath(path:String) | 在整个引擎中没有设置路径时`BaseDownloadTask#setPath`这个路径将会作为它的Root path
| getTempPath | 获取用于存储还未下载完成文件的临时存储路径: `filename.temp`
| isFilenameConverted(context:Context) | 判断是否所有数据库中下载中的任务的文件名都已经从`filename`(在旧架构中)转为`filename.temp`

#### `FileDownloadNotificationHelper`

> 如何快速集成Notification呢? 建议参考[NotificationMinSetActivity](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/NotificationMinSetActivity.java)、[NotificationSampleActivity](https://github.com/lingochamp/FileDownloader/blob/master/demo/src/main/java/com/liulishuo/filedownloader/demo/NotificationSampleActivity.java)。

#### `filedownloader.properties`

> 如果你需要定制化FileDownloader，可以在你的项目模块的`assets` 目录下添加 'filedownloader.properties' 文件(如 `/demo/src/main/assets/filedownloader.properties`)，然后添加以下可选相关配置。

> 格式: `keyword=value`

| 关键字 | 描述 | 默认值
| --- | --- | ---
| http.lenient | 如果你遇到了: 'can't know the size of the download file, and its Transfer-Encoding is not Chunked either', 但是你想要忽略类似的返回头不规范的错误，直接将该关键字参数设置为`true`即可，我们将会将其作为`chunck`进行处理 | false
| process.non-separate | FileDownloadService 默认是运行在独立进程':filedownloader'上的, 如果你想要FileDownloadService共享并运行在主进程上, 将该关键字参数设置为`true`，可以有效减少IPC产生的I/O | false
| download.min-progress-step | 最小缓冲大小，用于判定是否是时候将缓冲区中进度同步到数据库，以及是否是时候要确保下缓存区的数据都已经写文件。值越小，更新会越频繁，下载速度会越慢，但是应对进程被无法预料的情况杀死时会更加安全 | 65536
| download.min-progress-time | 最小缓冲时间，用于判定是否是时候将缓冲区中进度同步到数据库，以及是否是时候要确保下缓存区的数据都已经写文件。值越小，更新会越频繁，下载速度会越慢，但是应对进程被无法预料的情况杀死时会更加安全 | 2000
| download.max-network-thread-count | 用于同时下载的最大网络线程数, 区间[1, 12] | 3
| file.non-pre-allocation | 是否不需要在开始下载的时候，预申请整个文件的大小(`content-length`) | false
| broadcast.completed | 是否需要在任务下载完成后发送一个完成的广播 | false

> 如果你使用`broadcast.completed`并且接收任务完成的广播,你需要注册Action为`filedownloader.intent.action.completed`的广播并且使用`FileDownloadBroadcastHandler`来处理接收到的`Intent`。

III. 异常处理

> 所有的异常，都将在 `FileDownloadListener#error(BaseDownloadTask, Throwable)` 中获知。

| Exception | 原因
| --- | ---
| `FileDownloadHttpException`| 在发出请求以后，response-code不是200(HTTP_OK)，也不是206(HTTP_PARTIAL)的情况下会抛出该异常; 在这个异常对象会带上 response-code、response-header、request-header。
| `FileDownloadGiveUpRetryException` | 在请求返回的 response-header 中没有带有文件大小(content-length)，并且不是流媒体(transfer-encoding)的情况下会抛出该异常；出现这个异常，将会忽略所有重试的机会(`BaseDownloadTask#setAutoRetryTimes`). 你可以通过在 `filedownloader.properties`中添加 `http.lenient=true` 来忽略这个异常，并且在该情况下，直接作为流媒体进行下载。
| `FileDownloadOutOfSpaceException` | 当将要下载的文件大小大于剩余磁盘大小时，会抛出这个异常。
| 其他 | 程序错误。
| `FileDownloadNetworkPolicyException` | 设置了`BaseDownloadTask#setWifiRequired(true)`，在下载过程中，一旦发现网络情况转为非Wifi环境，便会抛回这个异常
| `PathConflictException` | 当有一个正在下载的任务，它的存储路径与当前任务的存储路径完全一致，为了避免多个任务对同一个文件进行写入，当前任务便会抛回这个异常



## III. 低内存情况

### 非下载进程(一般是UI进程):

> 这边的数据并不多，只是一些队列数据，用不了多少内存。

#### [前台进程](http://developer.android.com/intl/zh-cn/guide/components/processes-and-threads.html)数据被回收:

如果在前台的时候这个数据都被回收了, 你的应用应该也挂了。极低概率事件。

#### [后台进程](http://developer.android.com/intl/zh-cn/guide/components/processes-and-threads.html)数据被回收:

一般事件, 如果是你的下载是UI进程启动的，如果你的UI进程处于`后台进程`(可以理解为应用被退到后台)状态，在内存不足的情况下会被回收(回收优先级高于`服务进程`)，此时分两种情况:

1. 是串行队列任务，在回收掉UI进程内存以后，下载进程会继续下载完已经pending到下载进程的那个任务，而还未pending到下载进程的任务会中断下载(由于任务驱动线性执行的是在UI进程); 有损体验: 下次进入应用重启启动整个队列，会继续上次的下载。

2. 是并行队列任务，在回收掉UI进程内存以后，下载进程会继续下载所有任务(所有已经pending到下载进程的任务，由于这里的pending速度是很快的，因此几乎是点击并行下载，所有任务在很短的时间内都已经pending到下载进程了)，而UI进程由于被回收，将不会收到所有的监听; 有损体验: 下次进入应用重新启动整个队列，就会和正常的下载启动一致，收到所有情况的监听。

### 下载进程:

> 对内存有一定的占用，但是并不多，每次启动进程会根据数据的有效性进行清理冗余数据，被回收是低概率事件

由于下载不断有不同的buffer占用内存，但是由于在下载时，是活跃的`服务进程`，因此被回收是低概率事件(会先回收完所有`空进程`、`后台进程`(后台应用)以后，如果内存还不够，才会回收该进程)。

即使被回收，也不会有任何问题。由于我们使用的是`START_STICKY`(如果不希望被重启可主动调用`FileDownloader#unBindService`/`FileDownloader#unBindServiceIfIdle`)，因此在内存足够的时候，下载进程会尝试重启(系统调度)，非下载进程(一般是UI进程) 接收到下载进程的连接，会继续下载与继续接收回调，下载进程也会断点续传没有下载完的所有任务(无论并行与串行)，不会影响体验。

## IV. LICENSE

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

[gitter_url]: https://gitter.im/lingochamp/FileDownloader?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge
[gitter_svg]: https://badges.gitter.im/lingochamp/FileDownloader.svg
[license_2_svg]: https://img.shields.io/hexpm/l/plug.svg
[android_platform_svg]: https://img.shields.io/badge/Platform-Android-brightgreen.svg
[file_downloader_svg]: https://img.shields.io/badge/Android-FileDownloader-orange.svg
[mix_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/mix_tasks_demo.gif
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
[filedownloader_snapshot_svg]: https://img.shields.io/badge/SnapShot-1.7.8-yellow.svg
[build_status_link]: https://travis-ci.org/lingochamp/FileDownloader
