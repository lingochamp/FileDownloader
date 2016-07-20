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

package com.liulishuo.filedownloader.demo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadConnectListener;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 1/9/16.
 */
public class TasksManagerDemoActivity extends AppCompatActivity {

    private TaskItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_manager_demo);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter = new TaskItemAdapter());


        TasksManager.getImpl().onCreate(new WeakReference<>(this));
    }

    public void postNotifyDataChanged() {
        if (adapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        TasksManager.getImpl().onDestroy();
        adapter = null;
        FileDownloader.getImpl().pauseAll();
        super.onDestroy();
    }


    // ============================================================================ view adapter ===

    private static class TaskItemViewHolder extends RecyclerView.ViewHolder {
        public TaskItemViewHolder(View itemView) {
            super(itemView);
            assignViews();
        }

        private View findViewById(final int id) {
            return itemView.findViewById(id);
        }

        /**
         * viewHolder position
         */
        private int position;
        /**
         * download id
         */
        private int id;

        public void update(final int id, final int position) {
            this.id = id;
            this.position = position;
        }


        public void updateDownloaded() {
            taskPb.setMax(1);
            taskPb.setProgress(1);

            taskStatusTv.setText(R.string.tasks_manager_demo_status_completed);
            taskActionBtn.setText(R.string.delete);
        }

        public void updateNotDownloaded(final int status, final long sofar, final long total) {
            if (sofar > 0 && total > 0) {
                final float percent = sofar
                        / (float) total;
                taskPb.setMax(100);
                taskPb.setProgress((int) (percent * 100));
            } else {
                taskPb.setMax(1);
                taskPb.setProgress(0);
            }

            switch (status) {
                case FileDownloadStatus.error:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_error);
                    break;
                case FileDownloadStatus.paused:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_paused);
                    break;
                default:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_not_downloaded);
                    break;
            }
            taskActionBtn.setText(R.string.start);
        }

        public void updateDownloading(final int status, final long sofar, final long total) {
            final float percent = sofar
                    / (float) total;
            taskPb.setMax(100);
            taskPb.setProgress((int) (percent * 100));

            switch (status) {
                case FileDownloadStatus.pending:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_pending);
                    break;
                case FileDownloadStatus.started:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_started);
                    break;
                case FileDownloadStatus.connected:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_connected);
                    break;
                case FileDownloadStatus.progress:
                    taskStatusTv.setText(R.string.tasks_manager_demo_status_progress);
                    break;
                default:
                    taskStatusTv.setText(DemoApplication.CONTEXT.getString(
                            R.string.tasks_manager_demo_status_downloading, status));
                    break;
            }

            taskActionBtn.setText(R.string.pause);
        }

        private TextView taskNameTv;
        private TextView taskStatusTv;
        private ProgressBar taskPb;
        private Button taskActionBtn;

        private void assignViews() {
            taskNameTv = (TextView) findViewById(R.id.task_name_tv);
            taskStatusTv = (TextView) findViewById(R.id.task_status_tv);
            taskPb = (ProgressBar) findViewById(R.id.task_pb);
            taskActionBtn = (Button) findViewById(R.id.task_action_btn);
        }

    }

    private static class TaskItemAdapter extends RecyclerView.Adapter<TaskItemViewHolder> {

        private FileDownloadListener taskDownloadListener = new FileDownloadSampleListener() {

            private TaskItemViewHolder checkCurrentHolder(final BaseDownloadTask task) {
                final TaskItemViewHolder tag = (TaskItemViewHolder) task.getTag();
                if (tag.id != task.getId()) {
                    return null;
                }

                return tag;
            }

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.pending(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.pending, soFarBytes
                        , totalBytes);
                tag.taskStatusTv.setText(R.string.tasks_manager_demo_status_pending);
            }

            @Override
            protected void started(BaseDownloadTask task) {
                super.started(task);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.taskStatusTv.setText(R.string.tasks_manager_demo_status_started);
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.connected, soFarBytes
                        , totalBytes);
                tag.taskStatusTv.setText(R.string.tasks_manager_demo_status_connected);
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.progress(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.progress, soFarBytes
                        , totalBytes);
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                super.error(task, e);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateNotDownloaded(FileDownloadStatus.error, task.getLargeFileSoFarBytes()
                        , task.getLargeFileTotalBytes());
                TasksManager.getImpl().removeTaskForViewHolder(task.getId());
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.paused(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateNotDownloaded(FileDownloadStatus.paused, soFarBytes, totalBytes);
                tag.taskStatusTv.setText(R.string.tasks_manager_demo_status_paused);
                TasksManager.getImpl().removeTaskForViewHolder(task.getId());
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                super.completed(task);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if (tag == null) {
                    return;
                }

                tag.updateDownloaded();
                TasksManager.getImpl().removeTaskForViewHolder(task.getId());
            }
        };
        private View.OnClickListener taskActionOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() == null) {
                    return;
                }

                TaskItemViewHolder holder = (TaskItemViewHolder) v.getTag();

                CharSequence action = ((TextView) v).getText();
                if (action.equals(v.getResources().getString(R.string.pause))) {
                    // to pause
                    FileDownloader.getImpl().pause(holder.id);
                } else if (action.equals(v.getResources().getString(R.string.start))) {
                    // to start
                    // to start
                    final TasksManagerModel model = TasksManager.getImpl().get(holder.position);
                    final BaseDownloadTask task = FileDownloader.getImpl().create(model.getUrl())
                            .setPath(model.getPath())
                            .setCallbackProgressTimes(100)
                            .setListener(taskDownloadListener);

                    TasksManager.getImpl()
                            .addTaskForViewHolder(task);

                    TasksManager.getImpl()
                            .updateViewHolder(holder.id, holder);

                    task.start();
                } else if (action.equals(v.getResources().getString(R.string.delete))) {
                    // to delete
                    new File(TasksManager.getImpl().get(holder.position).getPath()).delete();
                    holder.taskActionBtn.setEnabled(true);
                    holder.updateNotDownloaded(FileDownloadStatus.INVALID_STATUS, 0, 0);
                }
            }
        };

        @Override
        public TaskItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskItemViewHolder holder = new TaskItemViewHolder(
                    LayoutInflater.from(
                            parent.getContext())
                            .inflate(R.layout.item_tasks_manager, parent, false));

            holder.taskActionBtn.setOnClickListener(taskActionOnClickListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(TaskItemViewHolder holder, int position) {
            final TasksManagerModel model = TasksManager.getImpl().get(position);

            holder.update(model.getId(), position);
            holder.taskActionBtn.setTag(holder);
            holder.taskNameTv.setText(model.getName());

            TasksManager.getImpl()
                    .updateViewHolder(holder.id, holder);

            holder.taskActionBtn.setEnabled(true);


            if (TasksManager.getImpl().isReady()) {
                final int status = TasksManager.getImpl().getStatus(model.getId(), model.getPath());
                if (status == FileDownloadStatus.pending || status == FileDownloadStatus.started ||
                        status == FileDownloadStatus.connected) {
                    // start task, but file not created yet
                    holder.updateDownloading(status, TasksManager.getImpl().getSoFar(model.getId())
                            , TasksManager.getImpl().getTotal(model.getId()));
                } else if (!new File(model.getPath()).exists() &&
                        !new File(FileDownloadUtils.getTempPath(model.getPath())).exists()) {
                    // not exist file
                    holder.updateNotDownloaded(status, 0, 0);
                } else if (TasksManager.getImpl().isDownloaded(status)) {
                    // already downloaded and exist
                    holder.updateDownloaded();
                } else if (status == FileDownloadStatus.progress) {
                    // downloading
                    holder.updateDownloading(status, TasksManager.getImpl().getSoFar(model.getId())
                            , TasksManager.getImpl().getTotal(model.getId()));
                } else {
                    // not start
                    holder.updateNotDownloaded(status, TasksManager.getImpl().getSoFar(model.getId())
                            , TasksManager.getImpl().getTotal(model.getId()));
                }
            } else {
                holder.taskStatusTv.setText(R.string.tasks_manager_demo_status_loading);
                holder.taskActionBtn.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return TasksManager.getImpl().getTaskCounts();
        }
    }


    // ============================================================================ controller ====

    public static class TasksManager {
        private final static class HolderClass {
            private final static TasksManager INSTANCE
                    = new TasksManagerDemoActivity.TasksManager();
        }

        public static TasksManager getImpl() {
            return HolderClass.INSTANCE;
        }

        private TasksManagerDBController dbController;
        private List<TasksManagerModel> modelList;

        private TasksManager() {
            dbController = new TasksManagerDBController();
            modelList = dbController.getAllTasks();

            initDemo();
        }

        private void initDemo() {
            if (modelList.size() <= 0) {
                final int demoSize = Constant.BIG_FILE_URLS.length;
                for (int i = 0; i < demoSize; i++) {
                    final String url = Constant.BIG_FILE_URLS[i];
                    addTask(url);
                }
            }
        }

        private SparseArray<BaseDownloadTask> taskSparseArray = new SparseArray<>();

        public void addTaskForViewHolder(final BaseDownloadTask task) {
            taskSparseArray.put(task.getId(), task);
        }

        public void removeTaskForViewHolder(final int id) {
            taskSparseArray.remove(id);
        }

        public void updateViewHolder(final int id, final TaskItemViewHolder holder) {
            final BaseDownloadTask task = taskSparseArray.get(id);
            if (task == null) {
                return;
            }

            task.setTag(holder);
        }

        public void releaseTask() {
            taskSparseArray.clear();
        }

        private FileDownloadConnectListener listener;

        private void registerServiceConnectionListener(final WeakReference<TasksManagerDemoActivity>
                                                               activityWeakReference) {
            if (listener != null) {
                FileDownloader.getImpl().removeServiceConnectListener(listener);
            }

            listener = new FileDownloadConnectListener() {

                @Override
                public void connected() {
                    if (activityWeakReference == null
                            || activityWeakReference.get() == null) {
                        return;
                    }

                    activityWeakReference.get().postNotifyDataChanged();
                }

                @Override
                public void disconnected() {
                    if (activityWeakReference == null
                            || activityWeakReference.get() == null) {
                        return;
                    }

                    activityWeakReference.get().postNotifyDataChanged();
                }
            };

            FileDownloader.getImpl().addServiceConnectListener(listener);
        }

        private void unregisterServiceConnectionListener() {
            FileDownloader.getImpl().removeServiceConnectListener(listener);
            listener = null;
        }

        public void onCreate(final WeakReference<TasksManagerDemoActivity> activityWeakReference) {
            if (!FileDownloader.getImpl().isServiceConnected()) {
                FileDownloader.getImpl().bindService();
                registerServiceConnectionListener(activityWeakReference);
            }
        }

        public void onDestroy() {
            unregisterServiceConnectionListener();
            releaseTask();
        }

        public boolean isReady() {
            return FileDownloader.getImpl().isServiceConnected();
        }

        public TasksManagerModel get(final int position) {
            return modelList.get(position);
        }

        public TasksManagerModel getById(final int id) {
            for (TasksManagerModel model : modelList) {
                if (model.getId() == id) {
                    return model;
                }
            }

            return null;
        }

        /**
         * @param status Download Status
         * @return has already downloaded
         * @see FileDownloadStatus
         */
        public boolean isDownloaded(final int status) {
            return status == FileDownloadStatus.completed;
        }

        public int getStatus(final int id, String path) {
            return FileDownloader.getImpl().getStatus(id, path);
        }

        public long getTotal(final int id) {
            return FileDownloader.getImpl().getTotal(id);
        }

        public long getSoFar(final int id) {
            return FileDownloader.getImpl().getSoFar(id);
        }

        public int getTaskCounts() {
            return modelList.size();
        }

        public TasksManagerModel addTask(final String url) {
            return addTask(url, createPath(url));
        }

        public TasksManagerModel addTask(final String url, final String path) {
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
                return null;
            }

            final int id = FileDownloadUtils.generateId(url, path);
            TasksManagerModel model = getById(id);
            if (model != null) {
                return model;
            }
            final TasksManagerModel newModel = dbController.addTask(url, path);
            if (newModel != null) {
                modelList.add(newModel);
            }

            return newModel;
        }

        public String createPath(final String url) {
            if (TextUtils.isEmpty(url)) {
                return null;
            }

            return FileDownloadUtils.getDefaultSaveFilePath(url);
        }
    }

    private static class TasksManagerDBController {
        public final static String TABLE_NAME = "tasksmanger";
        private final SQLiteDatabase db;

        private TasksManagerDBController() {
            TasksManagerDBOpenHelper openHelper = new TasksManagerDBOpenHelper(DemoApplication.CONTEXT);

            db = openHelper.getWritableDatabase();
        }

        public List<TasksManagerModel> getAllTasks() {
            final Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

            final List<TasksManagerModel> list = new ArrayList<>();
            try {
                if (!c.moveToLast()) {
                    return list;
                }

                do {
                    TasksManagerModel model = new TasksManagerModel();
                    model.setId(c.getInt(c.getColumnIndex(TasksManagerModel.ID)));
                    model.setName(c.getString(c.getColumnIndex(TasksManagerModel.NAME)));
                    model.setUrl(c.getString(c.getColumnIndex(TasksManagerModel.URL)));
                    model.setPath(c.getString(c.getColumnIndex(TasksManagerModel.PATH)));
                    list.add(model);
                } while (c.moveToPrevious());
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            return list;
        }

        public TasksManagerModel addTask(final String url, final String path) {
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
                return null;
            }

            // have to use FileDownloadUtils.generateId to associate TasksManagerModel with FileDownloader
            final int id = FileDownloadUtils.generateId(url, path);

            TasksManagerModel model = new TasksManagerModel();
            model.setId(id);
            model.setName(DemoApplication.CONTEXT.getString(R.string.tasks_manager_demo_name, id));
            model.setUrl(url);
            model.setPath(path);

            final boolean succeed = db.insert(TABLE_NAME, null, model.toContentValues()) != -1;
            return succeed ? model : null;
        }


    }

    // ----------------------- model
    private static class TasksManagerDBOpenHelper extends SQLiteOpenHelper {
        public final static String DATABASE_NAME = "tasksmanager.db";
        public final static int DATABASE_VERSION = 2;

        public TasksManagerDBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "
                    + TasksManagerDBController.TABLE_NAME
                    + String.format(
                    "("
                            + "%s INTEGER PRIMARY KEY, " // id, download id
                            + "%s VARCHAR, " // name
                            + "%s VARCHAR, " // url
                            + "%s VARCHAR " // path
                            + ")"
                    , TasksManagerModel.ID
                    , TasksManagerModel.NAME
                    , TasksManagerModel.URL
                    , TasksManagerModel.PATH

            ));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                db.delete(TasksManagerDBController.TABLE_NAME, null, null);
            }
        }
    }

    private static class TasksManagerModel {
        public final static String ID = "id";
        public final static String NAME = "name";
        public final static String URL = "url";
        public final static String PATH = "path";

        private int id;
        private String name;
        private String url;
        private String path;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public ContentValues toContentValues() {
            ContentValues cv = new ContentValues();
            cv.put(ID, id);
            cv.put(NAME, name);
            cv.put(URL, url);
            cv.put(PATH, path);
            return cv;
        }
    }


}
