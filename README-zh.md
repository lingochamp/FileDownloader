# FileDownloader
Android 文件下载引擎，稳定、高效、简单易用

[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
![][license_2_svg]

> [README DOC](https://github.com/lingochamp/FileDownloader/blob/master/README.md)

> 本引擎依赖okhttp 3.0.1

---
#### 版本迭代日志: [Change Log](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG.md)
---

### 特点

- 简单易用
- 高并发
- 独立进程
- 自动断点续传

#### 需要注意

- 当下载的文件大小可能大于1.99GB(2^31-1`=2_147_483_647 = 1.99GB`)的时候, 请使用`FileDownloadLargeFileListener`而不是`FileDownloadListener`(同理使用`getLargeFileSofarBytes()`与`getLargeFileTotalBytes()`)
- 暂停: paused, 恢复: 直接调用start，默认就是断点续传

#### 使用okHttp并使用其中的一些默认属性

- retryOnConnectionFailure: Unreachable IP addresses/Stale pooled connections/Unreachable proxy servers
- connection/read/write time out 10s

## I. 效果

![][serial_tasks_demo_gif]
![][parallel_tasks_demo_gif]
![][tasks_manager_demo_gif]
![][mix_tasks_demo_gif]


## II. 使用

在项目中引用:

```
compile 'com.liulishuo.filedownloader:library:0.1.5'
```

#### 全局初始化在`Application.onCreate`中

```
public XXApplication extends Application{

    ...
    @Override
    public void onCreate() {
        // 不耗时，做一些简单初始化准备工作，不会启动下载进程
        FileDownloader.init(this);
    }

    ...
}
```

#### 启动单任务下载

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

#### 启动多任务下载

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

for (String url : URLS) {
    FileDownloader.getImpl().create(url)
            .setListener(queueTarget)
            .setCallbackProgressTimes(0) // 由于是队列任务, 这里是我们假设了现在不需要每个任务都回调`FileDownloadListener#progress`, 所以这里这样设置可以很有效的减少ipc.
            .ready();
}

if(serial){
    // 串行执行该队列
    FileDownloader.getImpl().start(queueTarget, true);
}

if(parallel){
    // 并行执行该队列
    FileDownloader.getImpl().start(queueTarget, false);
}

```

#### 全局接口说明(`FileDownloader.`)

> 所有的暂停，就是停止，会释放所有资源并且停到所有相关线程，下次启动的时候默认会断点续传

| 方法名 | 备注
| --- | ---
| init(Application) |  简单初始化，不会启动下载进程
| create(url:String) | 创建一个下载任务
| start(listener:FileDownloadListener, isSerial:boolean) | 启动是相同监听器的任务，串行/并行启动
| pause(listener:FileDownloadListener) | 暂停启动相同监听器的任务
| pauseAll(void) | 暂停所有任务
| pause(downloadId) | 启动downloadId的任务
| getSoFar(downloadId) | 获得下载Id为downloadId的soFarBytes
| getTotal(downloadId) | 获得下载Id为downloadId的totalBytes
| bindService(void) | 主动启动下载进程(可事先调用该方法(可以不调用)，保证第一次下载的时候没有启动进程的速度消耗)
| unBindService(void) | 主动关停下载进程
| unBindServiceIfIdle(void) | 如果目前下载进程没有任务正在执行，则关停下载进程
| isServiceConnected(void) | 是否已经启动并且连接上下载进程(可参考任务管理demo中的使用)
| getStatus(downloadId) | 获取下载Id为downloadId的状态(可参考任务管理demo中的使用)

#### Task接口说明

| 方法名 | 备注
| --- | ---
| setPath(path:String) | 下载文件的存储绝对路径
| setListener(listener:FileDownloadListener) | 设置监听，可以以相同监听组成队列
| setCallbackProgressTimes(times:int) | 设置FileDownloadListener#progress最大回调次数
| setTag(tag:Object) | 内部不会使用，在回调的时候用户自己使用
| setTag(key:int, tag:Object) | 用于存储任意的变量方便回调中使用，以key作为索引
| setForceReDownload(isForceReDownload:boolean) | 强制重新下载，将会忽略检测文件是否健在
| setFinishListener(listener:FinishListener) | 结束监听，仅包含结束(over(void))的监听
| setAutoRetryTimes(autoRetryTimes:int) | 当请求或下载或写文件过程中存在错误时，自动重试次数，默认为0次
| addHeader(name:String, value:String) | 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误
| addHeader(line:String) | 添加自定义的请求头参数，需要注意的是内部为了断点续传，在判断断点续传有效时会自动添加上(`If-Match`与`Range`参数)，请勿重复添加导致400或其他错误
| removeAllHeaders(name:String) | 删除由自定义添加上去请求参数为`{name}`的所有键对
| ready(void) | 用于队列下载的单任务的结束符(见上面:启动多任务下载的案例)
| start(void) | 启动下载任务
| pause(void) | 暂停下载任务(也可以理解为停止下载，但是在start的时候默认会断点续传)
| getDownloadId(void):int | 获取唯一Id(内部通过url与path生成)
| getUrl(void):String | 获取下载连接
| getCallbackProgressTimes(void):int | 获得progress最大回调次数
| getPath(void):String | 获取下载文件存储路径
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

#### 监听器(`FileDownloadListener`)说明

##### 一般的下载回调流程:

```
pending -> connected -> (progress <->progress) -> [retry] -> blockComplete -> completed
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
| connected | 已经连接上 | ETag, 是否断点续传, soFarBytes, totalBytes
| progress | 下载进度回调 | soFarBytes
| blockComplete | 在完成前同步调用该方法，此时已经下载完成 | -
| retry | 重试之前把将要重试是第几次回调回来 | 之所以重试遇到Throwable, 将要重试是第几次, soFarBytes
| completed | 完成整个下载过程 | -
| paused | 暂停下载 | soFarBytes
| error | 下载出现错误 | 抛出的Throwable
| warn | 在下载队列中(正在等待/正在下载)已经存在相同下载连接与相同存储路径的任务 | -


![][file_download_listener_callback_flow_png]

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

[license_2_svg]: https://img.shields.io/hexpm/l/plug.svg
[android_platform_svg]: https://img.shields.io/badge/Platform-Android-brightgreen.svg
[file_downloader_svg]: https://img.shields.io/badge/Android-FileDownloader-orange.svg
[mix_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/mix_tasks_demo.gif
[parallel_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/parallel_tasks_demo.gif
[serial_tasks_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/serial_tasks_demo.gif
[tasks_manager_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/tasks_manager_demo.gif
[bintray_svg]: https://api.bintray.com/packages/jacksgong/maven/FileDownloader/images/download.svg
[bintray_url]: https://bintray.com/jacksgong/maven/FileDownloader/_latestVersion
[file_download_listener_callback_flow_png]: https://github.com/lingochamp/FileDownloader/raw/master/art/filedownloadlistener_callback_flow.png
