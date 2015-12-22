package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;

/**
 * Created by Jacksgong on 12/19/15.
 */
public class MixTestActivity extends AppCompatActivity {

    private final String TAG = "Demo.MixActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mix);

        assignViews();
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    public void onClickDel(final View view) {
        File file = new File(FileDownloadUtils.getDefaultSaveRootPath());
        if (!file.exists()) {
            Log.w(TAG, String.format("check file files not exists %s", file.getAbsolutePath()));
            return;
        }

        if (!file.isDirectory()) {
            Log.w(TAG, String.format("check file files not directory %s", file.getAbsolutePath()));
            return;
        }

        File[] files = file.listFiles();

        if (files == null) {
            updateDisplay("已经为空文件夹");
            return;
        }

        for (File file1 : files) {
            file1.delete();
            updateDisplay(String.format("delete: %s", file1.getName()));
        }
    }

    private int totalCounts = 0;
    private int finalCounts = 0;

    /**
     * 启动单任务下载
     *
     * @param view
     */
    public void onClickStartSingleDownload(final View view) {
        updateDisplay(String.format("点击 单任务下载 %s", Constant.BIG_FILE_URLS[0]));
        totalCounts++;
        FileDownloader.getImpl().create(Constant.BIG_FILE_URLS[0])
                .setListener(createListener())
                .setTag(1)
                .start();
    }

    /**
     * 启动并行多任务下载
     *
     * @param view
     */
    public void onClickMultiParallel(final View view) {
        updateDisplay(String.format("点击 %d个不同的任务并行下载", Constant.URLS.length));
        updateDisplay("以相同的listener作为target，将不同的下载任务绑定起来");

        // 以相同的listener作为target，将不同的下载任务绑定起来
        final FileDownloadListener parallelTarget = createListener();
        int i = 0;
        for (String url : Constant.URLS) {
            totalCounts++;
            FileDownloader.getImpl().create(url)
                    .setListener(parallelTarget)
                    .setCallbackProgressTimes(3)
                    .setTag(++i)
                    .ready();
        }

        FileDownloader.getImpl().start(parallelTarget, false);
    }

    /**
     * 启动串行多任务下载
     *
     * @param view
     */
    public void onClickMultiSerial(final View view) {
        updateDisplay(String.format("点击 %d个不同的任务并行下载", Constant.URLS.length));
        updateDisplay("以相同的listener作为target，将不同的下载任务绑定起来");

        // 以相同的listener作为target，将不同的下载任务绑定起来
        final FileDownloadListener serialTarget = createListener();
        int i = 0;
        for (String url : Constant.URLS) {
            totalCounts++;
            FileDownloader.getImpl().create(url)
                    .setListener(serialTarget)
                    .setCallbackProgressTimes(3)
                    .setTag(++i)
                    .ready();
        }

        FileDownloader.getImpl().start(serialTarget, true);
    }

    private FileDownloadListener createListener() {
        return new FileDownloadListener() {
            @Override
            protected void progress(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                updateDisplay(String.format("[progress] id[%d] %d/%d", task.getDownloadId(), soFarBytes, totalBytes));
            }

            @Override
            protected void pending(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                updateDisplay(String.format("[pending] id[%d] %d/%d", task.getDownloadId(), soFarBytes, totalBytes));
            }

            @Override
            protected void blockComplete(final BaseDownloadTask task) {
                downloadMsgTv.post(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay(String.format("[blockComplete] id[%d]", task.getDownloadId()));
                    }
                });
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                finalCounts++;
                updateDisplay(String.format("[completed] id[%d] oldFile[%B]",
                        task.getDownloadId(),
                        task.isReusedOldFile()));
                updateDisplay(String.format("---------------------------------- %d", (Integer) task.getTag()));
            }

            @Override
            protected void paused(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                finalCounts++;
                updateDisplay(String.format("[paused] id[%d] %d/%d", task.getDownloadId(), soFarBytes, totalBytes));
                updateDisplay(String.format("############################## %d", (Integer) task.getTag()));
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                finalCounts++;
                updateDisplay(Html.fromHtml(String.format("[error] id[%d] %s %s",
                        task.getDownloadId(),
                        e.getMessage(),
                        FileDownloadUtils.getStack(e.getStackTrace(), false))));

                updateDisplay(String.format("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! %d", (Integer) task.getTag()));
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                finalCounts++;
                updateDisplay(String.format("[warm] id[%d]", task.getDownloadId()));
                updateDisplay(String.format("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ %d", (Integer) task.getTag()));
            }
        };
    }

    private void updateDisplay(final CharSequence msg) {
        downloadMsgTv.append(String.format("\n %s", msg));
        tipMsgTv.setText(String.format("%d/%d", finalCounts, totalCounts));
        scrollView.post(scroll2Bottom);
    }

    private Runnable scroll2Bottom = new Runnable() {
        @Override
        public void run() {
            if (scrollView != null) {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.getImpl().pauseAll();
    }

    private LinearLayout topGroup;
    private ScrollView scrollView;
    private TextView downloadMsgTv;
    private TextView tipMsgTv;

    private void assignViews() {
        topGroup = (LinearLayout) findViewById(R.id.top_group);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        downloadMsgTv = (TextView) findViewById(R.id.download_msg_tv);
        tipMsgTv = (TextView) findViewById(R.id.tip_msg_tv);
    }

}
