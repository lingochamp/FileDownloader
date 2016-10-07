/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.demo.performance;

import android.os.Bundle;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.liulishuo.filedownloader.demo.R;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import okio.Buffer;
import okio.Okio;
import okio.Sink;
import okio.Source;

/**
 * Created by Jacksgong on 1/4/16.
 */
public class PerformanceTestActivity extends AppCompatActivity {

    private final static int TIMES = 100000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);
        assignViews();
    }

    public void onClickLongOperate(final View view) {
        final long start = System.currentTimeMillis();

        final LongParcel longParcel = new LongParcel();
        for (int i = 0; i < TIMES; i++) {
            longParcel.operate();
        }

        infoAppend("Long Operate", start);
    }

    public void onClickLongParcel(final View view) {
        final long start = System.currentTimeMillis();

        final LongParcel longParcel = new LongParcel();
        for (int i = 0; i < TIMES; i++) {
            Parcel p = Parcel.obtain();
            longParcel.writeToParcel(p, 0);
            LongParcel longParcelCopy = new LongParcel(p);
        }

        infoAppend("Long Parcel and Alloc [and GC]", start);
    }

    public void onClickIntOperate(final View view) {
        final long start = System.currentTimeMillis();

        final IntParcel intParcel = new IntParcel();
        for (int i = 0; i < TIMES; i++) {
            intParcel.operate();
        }

        infoAppend("Int Operate", start);
    }

    public void onClickIntParcel(final View view) {
        final long start = System.currentTimeMillis();

        final IntParcel intParcel = new IntParcel();
        for (int i = 0; i < TIMES; i++) {
            Parcel p = Parcel.obtain();
            intParcel.writeToParcel(p, 0);
            IntParcel intParcelCopy = new IntParcel(p);
        }

        infoAppend("Int Parcel and Alloc [and GC]", start);
    }

    private static final int BUFFER_SIZE = 1024 * 4;
    private String writePerformanceTestPath = FileDownloadUtils.getDefaultSaveRootPath()
            + File.separator + "performance";

    private static final int TENTH_MILLI_TO_NANO = 100000;

    public void onClickWriteTest(final View view) {
        FileOutputStream fos = null;
        InputStream inputStream = initPerformanceTest();
        byte[] buff = new byte[BUFFER_SIZE];
        long start = System.currentTimeMillis();


        int tenthMilliSec = ioPerformanceSb.getProgress();
        int sleepMilliSec = tenthMilliSec / 10;
        int sleepNanoSec = (tenthMilliSec - (tenthMilliSec / 10) * 10) * TENTH_MILLI_TO_NANO;

        infoTv.append(String.format("Output test with %.1f ms extra operate\n",
                tenthMilliSec / 10.0f));

        // ---------------------- FileOutputStream
        try {
            fos = new FileOutputStream(writePerformanceTestPath, true);
            do {
                int byteCount = inputStream.read(buff);
                if (byteCount == -1) {
                    break;
                }
                fos.write(buff, 0, byteCount);

                if (sleepMilliSec > 0 || sleepNanoSec > 0) {
                    try {
                        Thread.sleep(sleepMilliSec, sleepNanoSec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } while (true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.getFD().sync();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        infoAppend("FileOutputStream", start);

        BufferedOutputStream bos = null;
        inputStream = initPerformanceTest();
        start = System.currentTimeMillis();

        // ---------------------- BufferedOutputStream
        try {
            bos = new BufferedOutputStream(new FileOutputStream(writePerformanceTestPath, true));
            do {
                int byteCount = inputStream.read(buff);
                if (byteCount == -1) {
                    break;
                }
                bos.write(buff, 0, byteCount);

                if (sleepMilliSec > 0 || sleepNanoSec > 0) {
                    try {
                        Thread.sleep(sleepMilliSec, sleepNanoSec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        infoAppend("BufferOutputStream", start);


        RandomAccessFile raf = null;
        inputStream = initPerformanceTest();
        start = System.currentTimeMillis();

        // ---------------------- RandomAccessFile
        try {
            raf = new RandomAccessFile(writePerformanceTestPath, "rw");
            do {
                int byteCount = inputStream.read(buff);
                if (byteCount == -1) {
                    break;
                }
                raf.write(buff, 0, byteCount);

                if (sleepMilliSec > 0 || sleepNanoSec > 0) {
                    try {
                        Thread.sleep(sleepMilliSec, sleepNanoSec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        infoAppend("RandomAccessFile", start);

        Sink sink = null;
        inputStream = initPerformanceTest();
        Source source = Okio.source(inputStream);
        Buffer buffer = new Buffer();
        start = System.currentTimeMillis();

        try {
            sink = Okio.sink(new File(writePerformanceTestPath));
            sink = Okio.buffer(sink);

            do {
                long byteCount = source.read(buffer, BUFFER_SIZE);
                if (byteCount == -1) {
                    break;
                }

                sink.write(buffer, byteCount);

                if (sleepMilliSec > 0 || sleepNanoSec > 0) {
                    try {
                        Thread.sleep(sleepMilliSec, sleepNanoSec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        infoAppend("okio", start);
    }

    private InputStream initPerformanceTest() {
        try {
            final File file = new File(writePerformanceTestPath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            return getResources().getAssets().open("performance_test_data");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void infoAppend(final String msg, final long start) {
        infoTv.append(String.format(" %s: %d\n", msg, System.currentTimeMillis() - start));
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (scrollView != null) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }

    private AppCompatSeekBar ioPerformanceSb;
    private ScrollView scrollView;
    private TextView infoTv;

    private void assignViews() {
        ioPerformanceSb = (AppCompatSeekBar) findViewById(R.id.io_performance_sb);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        infoTv = (TextView) findViewById(R.id.info_tv);
    }

}
