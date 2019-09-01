package com.liulishuo.filedownloader.demo;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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

        llsApkFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmpdir1" + File.separator +
                Constant.LIULISHUO_CONTENT_DISPOSITION_FILENAME;
        llsApkDir = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmpdir1";
        normalTaskFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp2";
        chunkedFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "chunked_data_tmp1";

        assignViews();

        initFilePathEqualDirAndFileName();
        initNormalDataAction();
        initChunkTransferEncodingDataAction();
    }


    // test for the file path = dir path / content-disposition-filename
    // task1: set {@code llsApkFilePath}
    // task2: set {@code llsApkDir}
    private void initFilePathEqualDirAndFileName() {
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
                new File(llsApkFilePath).delete();
                new File(FileDownloadUtils.getTempPath(llsApkFilePath)).delete();
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
                new File(llsApkDir).delete();
            }
        });
    }

    // test for normal task.
    private void initNormalDataAction() {
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
                new File(normalTaskFilePath).delete();
                new File(FileDownloadUtils.getTempPath(normalTaskFilePath)).delete();
            }
        });
    }

    // test for chunked downloading.
    private void initChunkTransferEncodingDataAction() {
        // task 4
        startBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadId4 = createDownloadTask(4).start();
            }
        });

        pauseBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloader.getImpl().pause(downloadId4);
            }
        });

        deleteBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(chunkedFilePath).delete();
                new File(FileDownloadUtils.getTempPath(chunkedFilePath)).delete();
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
        boolean isDir = false;
        String path;

        switch (position) {
            case 1:
                url = Constant.LIULISHUO_APK_URL;
                tag = new ViewHolder(new WeakReference<>(this), progressBar1, null, speedTv1, 1);
                path = llsApkFilePath;
                tag.setFilenameTv(filenameTv1);
                break;
            case 2:
                url = Constant.LIULISHUO_APK_URL;
                tag = new ViewHolder(new WeakReference<>(this), progressBar2, null, speedTv2, 2);
                path = llsApkDir;
                isDir = true;
                tag.setFilenameTv(filenameTv2);
                break;
            case 3:
                url = Constant.BIG_FILE_URLS[2];
                tag = new ViewHolder(new WeakReference<>(this), progressBar3, null, speedTv3, 3);
                path = normalTaskFilePath;
                break;
            default:
                url = Constant.CHUNKED_TRANSFER_ENCODING_DATA_URLS[0];
                tag = new ViewHolder(new WeakReference<>(this), progressBar4, detailTv4, speedTv4, 4);
                path = chunkedFilePath;
                break;

        }

        return FileDownloader.getImpl().create(url)
                .setPath(path, isDir)
                .setCallbackProgressTimes(300)
                .setMinIntervalUpdateSpeed(400)
                .setTag(tag)
                .setListener(new FileDownloadSampleListener() {

                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.pending(task, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).updatePending(task);
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.progress(task, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).updateProgress(soFarBytes, totalBytes,
                                task.getSpeed());
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        super.error(task, e);
                        ((ViewHolder) task.getTag()).updateError(e, task.getSpeed());
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                        super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).updateConnected(etag, task.getFilename());
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        super.paused(task, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).updatePaused(task.getSpeed());
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        super.completed(task);
                        ((ViewHolder) task.getTag()).updateCompleted(task);
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        super.warn(task);
                        ((ViewHolder) task.getTag()).updateWarn();
                    }
                });
    }

    private static class ViewHolder {
        private ProgressBar pb;
        private TextView detailTv;
        private TextView speedTv;
        private int position;
        private TextView filenameTv;

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

        public void setFilenameTv(TextView filenameTv) {
            this.filenameTv = filenameTv;
        }

        private void updateSpeed(int speed) {
            speedTv.setText(String.format("%dKB/s", speed));
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

        public void updatePending(BaseDownloadTask task) {
            if (filenameTv != null) {
                filenameTv.setText(task.getFilename());
            }
        }

        public void updatePaused(final int speed) {
            toast(String.format("paused %d", position));
            updateSpeed(speed);
            pb.setIndeterminate(false);
        }

        public void updateConnected(String etag, String filename) {
            if (filenameTv != null) {
                filenameTv.setText(filename);
            }
        }

        public void updateWarn() {
            toast(String.format("warn %d", position));
            pb.setIndeterminate(false);
        }

        public void updateError(final Throwable ex, final int speed) {
            toast(String.format("error %d %s", position, ex));
            updateSpeed(speed);
            pb.setIndeterminate(false);
            ex.printStackTrace();
        }

        public void updateCompleted(final BaseDownloadTask task) {

            toast(String.format("completed %d %s", position, task.getTargetFilePath()));

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
                Snackbar.make(weakReferenceContext.get().startBtn1, msg, Snackbar.LENGTH_LONG).show();
            }
        }

    }

    private int downloadId1;
    private int downloadId2;
    private int downloadId3;
    private int downloadId4;


    private String llsApkFilePath;
    private String llsApkDir;
    private String normalTaskFilePath;
    private String chunkedFilePath;

    private Button startBtn1;
    private Button pauseBtn1;
    private Button deleteBtn1;
    private TextView filenameTv1;
    private TextView speedTv1;
    private ProgressBar progressBar1;
    private Button startBtn2;
    private Button pauseBtn2;
    private Button deleteBtn2;
    private TextView filenameTv2;
    private TextView speedTv2;
    private ProgressBar progressBar2;
    private Button startBtn3;
    private Button pauseBtn3;
    private Button deleteBtn3;
    private TextView speedTv3;
    private ProgressBar progressBar3;
    private Button startBtn4;
    private Button pauseBtn4;
    private Button deleteBtn4;
    private TextView detailTv4;
    private TextView speedTv4;
    private ProgressBar progressBar4;

    private void assignViews() {
        startBtn1 = (Button) findViewById(R.id.start_btn_1);
        pauseBtn1 = (Button) findViewById(R.id.pause_btn_1);
        deleteBtn1 = (Button) findViewById(R.id.delete_btn_1);
        filenameTv1 = (TextView) findViewById(R.id.filename_tv_1);
        speedTv1 = (TextView) findViewById(R.id.speed_tv_1);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar_1);
        startBtn2 = (Button) findViewById(R.id.start_btn_2);
        pauseBtn2 = (Button) findViewById(R.id.pause_btn_2);
        deleteBtn2 = (Button) findViewById(R.id.delete_btn_2);
        filenameTv2 = (TextView) findViewById(R.id.filename_tv_2);
        speedTv2 = (TextView) findViewById(R.id.speed_tv_2);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar_2);
        startBtn3 = (Button) findViewById(R.id.start_btn_3);
        pauseBtn3 = (Button) findViewById(R.id.pause_btn_3);
        deleteBtn3 = (Button) findViewById(R.id.delete_btn_3);
        speedTv3 = (TextView) findViewById(R.id.speed_tv_3);
        progressBar3 = (ProgressBar) findViewById(R.id.progressBar_3);
        startBtn4 = (Button) findViewById(R.id.start_btn_4);
        pauseBtn4 = (Button) findViewById(R.id.pause_btn_4);
        deleteBtn4 = (Button) findViewById(R.id.delete_btn_4);
        detailTv4 = (TextView) findViewById(R.id.detail_tv_4);
        speedTv4 = (TextView) findViewById(R.id.speed_tv_4);
        progressBar4 = (ProgressBar) findViewById(R.id.progressBar_4);
    }


}
