# FileDownloader

> Android 文件下载引擎，稳定、高效、简单易用
> 本引擎目前基于口碑很好的okhttp

### 特点

> 总之，你负责大胆高并发高频率的往下载引擎里面以队列(请求需要并行处理/串行处理)/单任务的形式放任务，引擎负责给你达预期稳定、高效的结果输出

#### 1. 稳定: 

- 高并发: 兼容高频率不断入队 并行任务队列/串行任务队列/单一下载任务，甚至其中包含大量重复任务(url与path都相同)，不同队列独立运作，不同任务相对独立运作，互不干涉
- 独立进程: 在需要下载时，启动独立进程，减小对应用本身的影响，相比较`android.app.DownloadManager`，`FileDownloader`采用AIDL基于Binder更加高效，并且不会受到其他应用下载的影响。


#### 2. 冗余低: 

- **默认**通过__文件有效性检测__已经下载并且下载完成的任务不重复启动下载 
- 通过__实时监控已启动的任务或排队中的任务__，校对准备进入的任务是否是重复任务(url与path相同)，以`warn`抛出用户层，防止重复任务冗余下载
- 通过__本地数据库及自动断点续传__结合相关脏数据矫正，保证任务下载进度尽可能的被快照，只要后端带有`etag`(七牛默认会带)，无论是进程被杀，还是其他任何异常情况，下次启动自动从上次有效位置开始续传，不重复下载


#### 3. 需要注意

- 为了绝大多数使用性能考虑，目前下载引擎目前受限于int可表示的范围，而我们的回调`total`与`so far`以byte为单位回调，因此最大只能表示到`2^31-1`=2_147_483_647 = 1.99GB(ps: 如果有更大的文件下载需求，提issue，我们会进行一些巧妙的优化，利用负值区间？根据大小走特殊通道传输?)
- 暂停: paused, 恢复: 直接调用start，默认就是断点续传

#### 4. 使用okHttp并使用其中的一些默认属性

- retryOnConnectionFailure: Unreachable IP addresses/Stale pooled connections/Unreachable proxy servers
- connection/read/write time out 10s

## I. 效果

## II. 使用

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
         .savePath(path)
         .addListener(new FileDownloadListener() {
             @Override
             protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
             }

             @Override
             protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
             }

             @Override
             protected void blockComplete(BaseDownloadTask task) {
             }

             @Override
             protected void completed(BaseDownloadTask task) {
             }

             @Override
             protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
             }

             @Override
             protected void error(BaseDownloadTask task, Throwable e) {
             }

             @Override
             protected void warn(BaseDownloadTask task) {
             }
         }).start()
```

#### 启动多任务下载

```
final FileDownloadListener queueTarget = new FileDownloadListener() {
    @Override
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void blockComplete(BaseDownloadTask task) {

    }

    @Override
    protected void completed(BaseDownloadTask task) {

    }

    @Override
    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {

    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }
}

for (String url : URLS) {
    FileDownloader.getImpl().create(url)
            .addListener(queueTarget)
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




## III. 架构层简单说明

## TODO

- 对外开放自动重试次数封装
- 对外开放连接/读/写超时时间
- 线程池教空闲时，考虑智能单任务多线程下载
