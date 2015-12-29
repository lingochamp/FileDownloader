# FileDownloader
Android 文件下载引擎，稳定、高效、简单易用

[![Download][bintray_svg]][bintray_url]
![][file_downloader_svg]
![][license_2_svg]

> [README DOC](https://github.com/lingochamp/FileDownloader/blob/master/README.md)

> 本引擎依赖okhttp

---
#### 版本迭代日志: [Change Log](https://github.com/lingochamp/FileDownloader/blob/master/CHANGELOG.md)
---

### 特点

- 简单易用
- 高并发
- 独立进程
- 自动断点续传

#### 需要注意

- 为了绝大多数使用性能考虑，目前下载引擎目前受限于int可表示的范围，而我们的回调`total`与`so far`以byte为单位回调，因此最大只能表示到`2^31-1`=2_147_483_647 = 1.99GB(ps: 如果有更大的文件下载需求，提issue，我们会进行一些巧妙的优化，利用负值区间？根据大小走特殊通道传输?)
- 暂停: paused, 恢复: 直接调用start，默认就是断点续传

#### 使用okHttp并使用其中的一些默认属性

- retryOnConnectionFailure: Unreachable IP addresses/Stale pooled connections/Unreachable proxy servers
- connection/read/write time out 10s

## I. 效果

![][serial_tasks_demo_gif]
![][parallel_tasks_demo_gif]
![][single_task_demo_gif]
![][mix_tasks_demo_gif]


## II. 使用

在项目中引用:

```
compile 'com.liulishuo.filedownloader:library:0.1.2'
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
| unBindService(void) | 主动停止下载进程(如果不调用该方法，进程闲置一段时间以后，系统调度会自动将其回收)

#### Task接口说明

| 方法名 | 备注
| --- | ---
| setPath(path:String) | 下载文件的存储绝对路径
| setListener(listener:FileDownloadListener) | 设置监听，可以以相同监听组成队列
| setCallbackProgressTimes(times:int) | 设置progress最大回调次数
| setTag(tag:Object) | 内部不会使用，在回调的时候用户自己使用
| setForceReDownload(isForceReDownload:boolean) | 强制重新下载，将会忽略检测文件是否健在
| setFinishListener(listener:FinishListener) | 结束监听，仅包含结束(over(void))的监听
| setAutoRetryTimes(autoRetryTimes:int) | 当请求或下载或写文件过程中存在错误时，自动重试次数，默认为0次
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

## III. LICENSE

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
[single_task_demo_gif]: https://github.com/lingochamp/FileDownloader/raw/master/art/single_task_demo.gif
[bintray_svg]: https://api.bintray.com/packages/jacksgong/maven/FileDownloader/images/download.svg
[bintray_url]: https://bintray.com/jacksgong/maven/FileDownloader/_latestVersion
[file_download_listener_callback_flow_png]: https://github.com/lingochamp/FileDownloader/raw/master/art/filedownloadlistener_callback_flow.png

