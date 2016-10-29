package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 12/19/15.
 */
public class HybridTestActivity extends AppCompatActivity {

    private final String TAG = "Demo.HybridActivity";
    private Handler uiHandler;
    private final int WHAT_NEED_AUTO_2_BOTTOM = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybrid_test);

        uiHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == WHAT_NEED_AUTO_2_BOTTOM) {
                    needAuto2Bottom = true;
                }
                return false;
            }
        });

        assignViews();
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uiHandler.removeMessages(WHAT_NEED_AUTO_2_BOTTOM);
                    needAuto2Bottom = false;
                }

                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    uiHandler.removeMessages(WHAT_NEED_AUTO_2_BOTTOM);
                    uiHandler.sendEmptyMessageDelayed(WHAT_NEED_AUTO_2_BOTTOM, 1000);
                }

                return false;
            }
        });
    }

    private boolean needAuto2Bottom = true;

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
            updateDisplay(getString(R.string.del_file_error_empty));
            return;
        }

        for (File file1 : files) {
            file1.delete();
            updateDisplay(getString(R.string.hybrid_test_deleted_file, file1.getName()));
        }
    }

    private int totalCounts = 0;
    private int finalCounts = 0;

    // =================================================== demo area ========================================================

    /**
     * Start single download task
     * <p>
     * 启动单任务下载
     *
     * @param view
     */
    public void onClickStartSingleDownload(final View view) {
        updateDisplay(getString(R.string.hybrid_test_start_single_task, Constant.BIG_FILE_URLS[2]));
        totalCounts++;
        FileDownloader.getImpl().create(Constant.BIG_FILE_URLS[2])
                .setListener(createListener())
                .setTag(1)
                .start();
    }

    /**
     * Start multiple download tasks parallel
     * <p>
     * 启动并行多任务下载
     *
     * @param view
     */
    public void onClickMultiParallel(final View view) {
        updateDisplay(getString(R.string.hybrid_test_start_multiple_tasks_parallel, Constant.URLS.length));

        // 以相同的listener作为target，将不同的下载任务绑定起来
        final FileDownloadListener parallelTarget = createListener();
        final List<BaseDownloadTask> taskList = new ArrayList<>();
        int i = 0;
        for (String url : Constant.URLS) {
            taskList.add(FileDownloader.getImpl().create(url)
                    .setTag(++i));
        }
        totalCounts += taskList.size();

        new FileDownloadQueueSet(parallelTarget)
                .setCallbackProgressTimes(1)
                .downloadTogether(taskList)
                .start();
    }

    /**
     * Start multiple download tasks serial
     * <p>
     * 启动串行多任务下载
     *
     * @param view
     */
    public void onClickMultiSerial(final View view) {
        updateDisplay(getString(R.string.hybrid_test_start_multiple_tasks_serial, Constant.URLS.length));

        // 以相同的listener作为target，将不同的下载任务绑定起来
        final List<BaseDownloadTask> taskList = new ArrayList<>();
        final FileDownloadListener serialTarget = createListener();
        int i = 0;
        for (String url : Constant.URLS) {
            taskList.add(FileDownloader.getImpl().create(url)
                    .setTag(++i));
        }
        totalCounts += taskList.size();

        new FileDownloadQueueSet(serialTarget)
                .setCallbackProgressTimes(1)
                .downloadSequentially(taskList)
                .start();
    }

    private FileDownloadListener createListener() {
        return new FileDownloadListener() {

            @Override
            protected boolean isInvalid() {
                return isFinishing();
            }

            @Override
            protected void pending(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                updateDisplay(String.format("[pending] id[%d] %d/%d", task.getId(), soFarBytes, totalBytes));
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                updateDisplay(String.format("[connected] id[%d] %s %B %d/%d", task.getId(), etag, isContinue, soFarBytes, totalBytes));
            }

            @Override
            protected void progress(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                updateDisplay(String.format("[progress] id[%d] %d/%d", task.getId(), soFarBytes, totalBytes));
            }

            @Override
            protected void blockComplete(final BaseDownloadTask task) {
                downloadMsgTv.post(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay(String.format("[blockComplete] id[%d]", task.getId()));
                    }
                });
            }

            @Override
            protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
                super.retry(task, ex, retryingTimes, soFarBytes);
                updateDisplay(String.format("[retry] id[%d] %s %d %d",
                        task.getId(), ex, retryingTimes, soFarBytes));
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                finalCounts++;
                updateDisplay(String.format("[completed] id[%d] oldFile[%B]",
                        task.getId(),
                        task.isReusedOldFile()));
                updateDisplay(String.format("---------------------------------- %d", (Integer) task.getTag()));
            }

            @Override
            protected void paused(final BaseDownloadTask task, final int soFarBytes, final int totalBytes) {
                finalCounts++;
                updateDisplay(String.format("[paused] id[%d] %d/%d", task.getId(), soFarBytes, totalBytes));
                updateDisplay(String.format("############################## %d", (Integer) task.getTag()));
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                finalCounts++;
                updateDisplay(Html.fromHtml(String.format("[error] id[%d] %s %s",
                        task.getId(),
                        e,
                        FileDownloadUtils.getStack(e.getStackTrace(), false))));

                updateDisplay(String.format("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! %d", (Integer) task.getTag()));
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                finalCounts++;
                updateDisplay(String.format("[warn] id[%d]", task.getId()));
                updateDisplay(String.format("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ %d", (Integer) task.getTag()));
            }
        };
    }

    // -------------------------------------------------------- something just for display ------------------------------------------------------

    private void updateDisplay(final CharSequence msg) {
        if (downloadMsgTv.getLineCount() > 2500) {
            downloadMsgTv.setText("");
        }
        downloadMsgTv.append(String.format("\n %s", msg));
        tipMsgTv.setText(String.format("%d/%d", finalCounts, totalCounts));
        if (needAuto2Bottom) {
            scrollView.post(scroll2Bottom);
        }
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
