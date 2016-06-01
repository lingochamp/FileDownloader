package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Jacksgong on 12/21/15.
 */
public class SingleTaskTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);

        savePath1 = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp1";
        savePath2 = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp2";
        savePath3 = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "chunked_data_tmp1";

        assignViews();

        initNormalDataAction();
        initChunkTransferEncodingDataAction();
    }


    private void initNormalDataAction() {
        // task 1
        startBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId1 = createDownloadTask(1).start();
            }
        });

        pauseBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId1);
            }
        });

        deleteBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(savePath1).delete();
            }
        });

        // task 2
        startBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId2 = createDownloadTask(2).start();
            }
        });

        pauseBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId2);
            }
        });

        deleteBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(savePath2).delete();
            }
        });

    }

    private void initChunkTransferEncodingDataAction() {
        // task 3
        startBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId3 = createDownloadTask(3).start();
            }
        });

        pauseBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId3);
            }
        });

        deleteBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(savePath3).delete();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.getImpl().pause(downloadId1);
        FileDownloader.getImpl().pause(downloadId2);
    }

    private BaseDownloadTask createDownloadTask(final int position) {
        final ViewHolder tag;
        final String url;
        final String path;

        switch (position) {
            case 1:
                url = Constant.BIG_FILE_URLS[4];
                tag = new ViewHolder(new WeakReference<>(this), progressBar1, null, speedTv1, 1);
                path = savePath1;
                break;
            case 2:
                url = Constant.BIG_FILE_URLS[2];
                tag = new ViewHolder(new WeakReference<>(this), progressBar2, null, speedTv2, 2);
                path = savePath2;
                break;
            default:
                url = Constant.CHUNKED_TRANSFER_ENCODING_DATA_URLS[0];
                tag = new ViewHolder(new WeakReference<>(this), progressBar3, detailTv3, speedTv3, 3);
                path = savePath3;
                break;

        }

        return FileDownloader.getImpl().create(url)
                .setPath(path)
                .setCallbackProgressTimes(300)
                .setMinIntervalUpdateSpeed(400)
                .setTag(tag)
                .setListener(new FileDownloadSampleListener() {
                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.progress(task, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).updateProgress(soFarBytes, totalBytes,
                                task.getSpeed());
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        super.error(task, e);
                        ((ViewHolder)task.getTag()).updateError(e, task.getSpeed());
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.paused(task, soFarBytes, totalBytes);
                        ((ViewHolder)task.getTag()).updatePaused(task.getSpeed());
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        super.completed(task);
                        ((ViewHolder)task.getTag()).updateCompleted(task);
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        super.warn(task);
                        ((ViewHolder)task.getTag()).updateWarn();
                    }
                });

    }

    private static class ViewHolder {
        private ProgressBar pb;
        private TextView detailTv;
        private TextView speedTv;
        private int position;

        private WeakReference<SingleTaskTestActivity> weakReferenceContext;

        public ViewHolder(WeakReference<SingleTaskTestActivity> weakReferenceContext,
                          final ProgressBar pb, final TextView detailTv, final TextView speedTv,
                          final int position) {
            this.weakReferenceContext = weakReferenceContext;
            this.pb = pb;
            this.detailTv = detailTv;
            this.position = position;
            this.speedTv = speedTv;
        }

        private void updateSpeed(int speed) {
            speedTv.setText(String.format("%dKB/s",speed));
        }

        public void updateProgress(final int sofar, final int total, final int speed) {
            if (total == -1) {
                // chunked transfer encoding data
                pb.setIndeterminate(true);
            } else {
                pb.setMax(total);
                pb.setProgress(sofar);
            }

            updateSpeed(speed);

            if (detailTv != null) {
                detailTv.setText(String.format("sofar: %d total: %d", sofar, total));
            }
        }

        public void updatePaused(final int speed) {
            toast(String.format("paused %d", position));
            updateSpeed(speed);
            pb.setIndeterminate(false);
        }

        public void updateWarn() {
            toast(String.format("warn %d", position));
            pb.setIndeterminate(false);
        }

        public void updateError(final Throwable ex, final int speed) {
            toast(String.format("error %d %s", position, ex));
            updateSpeed(speed);
            pb.setIndeterminate(false);
        }

        public void updateCompleted(final BaseDownloadTask task) {


            toast(String.format("completed %d %s", position, task.getPath()));

            if (detailTv != null) {
                detailTv.setText(String.format("sofar: %d total: %d",
                        task.getSmallFileSoFarBytes(), task.getSmallFileTotalBytes()));
            }

            updateSpeed(task.getSpeed());
            pb.setIndeterminate(false);
            pb.setMax(task.getSmallFileTotalBytes());
            pb.setProgress(task.getSmallFileSoFarBytes());
        }

        private void toast(final String msg) {
            if (this.weakReferenceContext != null && this.weakReferenceContext.get() != null) {
                Toast.makeText(this.weakReferenceContext.get(), msg, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private int downloadId1;
    private int downloadId2;
    private int downloadId3;


    private String savePath1;
    private String savePath2;
    private String savePath3;

    private Button startBtn1;
    private Button pauseBtn1;
    private Button deleteBtn1;
    private TextView speedTv1;
    private ProgressBar progressBar1;
    private Button startBtn2;
    private Button pauseBtn2;
    private Button deleteBtn2;
    private TextView speedTv2;
    private ProgressBar progressBar2;
    private Button startBtn3;
    private Button pauseBtn3;
    private Button deleteBtn3;
    private TextView detailTv3;
    private TextView speedTv3;
    private ProgressBar progressBar3;

    private void assignViews() {
        startBtn1 = (Button) findViewById(R.id.start_btn_1);
        pauseBtn1 = (Button) findViewById(R.id.pause_btn_1);
        deleteBtn1 = (Button) findViewById(R.id.delete_btn_1);
        speedTv1 = (TextView) findViewById(R.id.speed_tv_1);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar_1);
        startBtn2 = (Button) findViewById(R.id.start_btn_2);
        pauseBtn2 = (Button) findViewById(R.id.pause_btn_2);
        deleteBtn2 = (Button) findViewById(R.id.delete_btn_2);
        speedTv2 = (TextView) findViewById(R.id.speed_tv_2);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar_2);
        startBtn3 = (Button) findViewById(R.id.start_btn_3);
        pauseBtn3 = (Button) findViewById(R.id.pause_btn_3);
        deleteBtn3 = (Button) findViewById(R.id.delete_btn_3);
        detailTv3 = (TextView) findViewById(R.id.detail_tv_3);
        speedTv3 = (TextView) findViewById(R.id.speed_tv_3);
        progressBar3 = (ProgressBar) findViewById(R.id.progressBar_3);
    }


}
