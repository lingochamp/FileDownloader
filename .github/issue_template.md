#### Before Issue

1. Please search on the [Issues](https://github.com/lingochamp/FileDownloader/issues)
2. Please search on the [wiki](https://github.com/lingochamp/FileDownloader/wiki)
3. Please set `FileDownloadLog.NEED_LOG=true` and review the Logcat output from main process and `:filedownloader` process ( pay attention to Warn and Error level logcat)

#### Issue

1. What problem do you get?
2. Which version of FileDownloader are you using when you produce such problem?
3. How to reproduce such problem?
4. Do you set `FileDownloadLog.NEED_LOG=true`?
5. Could you please reproduce this problem and provide all main process and `:filedownloader` process logcat 
6. Can you fix it by yourself and request PR, if not, what's problem do you get when you try to fix it

>P.S. If you don't know how to get `:filedownloader` process, it's recommended to using `pidcat` to just filter all your application logcat, or define `process.non-separate=true` on [filedownloader.properties](https://github.com/lingochamp/FileDownloader/wiki/filedownloader.properties)

---

请在Issue前认真的跟进上面提到的建议，这样将可以极大的加快你遇到问题的处理。
