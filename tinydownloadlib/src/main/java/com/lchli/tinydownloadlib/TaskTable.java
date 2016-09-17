package com.lchli.tinydownloadlib;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.List;

class TaskTable {


    private static DatabaseHelper getHelper(Context context) {
        return OpenHelperManager.getHelper(context, DatabaseHelper.class);
    }

    public static List<TinyDownloadTask> queryAllTasks(Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<TinyDownloadTask> uploads = null;
        try {
            final Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            uploads = dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }
        if (uploads != null) {
            for (TinyDownloadTask task : uploads) {
                setTaskProgress(task);
            }
        }
        return uploads;

    }

    static void setTaskProgress(TinyDownloadTask task) {
        if (task.state == TinyDownloadConfig.TASK_STATE_UNFINISHED) {
            long progress = 0;
            File infoFile = new File(task.saveDir, task.name + ".info");
            if (infoFile.exists()) {
                try {
                    RandomAccessFile randomInfoFile = new RandomAccessFile(infoFile, "rwd");
                    for (int i = 0; i < task.threadCount; i++) {
                        progress += randomInfoFile.readLong();
                    }
                    randomInfoFile.close();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
            task.currentFinish = progress;
        } else {
            task.currentFinish = task.totalLength;
        }
    }


    public static List<TinyDownloadTask> queryFinishedTasks(Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<TinyDownloadTask> uploads = null;
        try {
            final Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            uploads = dao.queryForEq("state", TinyDownloadConfig.TASK_STATE_FINISHED);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }
        return uploads;

    }

    public static List<TinyDownloadTask> queryUnFinishedTasks(Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<TinyDownloadTask> uploads = null;
        try {
            final Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            uploads = dao.queryForEq("state", TinyDownloadConfig.TASK_STATE_UNFINISHED);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }
        return uploads;

    }

    public static void addTask(TinyDownloadTask task, Context context) {
        final DatabaseHelper helper = getHelper(context);
        try {
            Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            int affect = dao.create(task);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }

    }

    public static void updateTask(TinyDownloadTask task, Context context) {
        final DatabaseHelper helper = getHelper(context);
        try {
            Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            UpdateBuilder<TinyDownloadTask, String> builder = dao.updateBuilder();
            builder.updateColumnValue("state", task.state);
            builder.updateColumnValue("totalLength", task.totalLength);
            builder.updateColumnValue("threadCount", task.threadCount);

            builder.where().eq("uid", task.uid);

            builder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }

    }

    public static void deleteTask(String taskUid, Context context) {
        final DatabaseHelper helper = getHelper(context);
        try {
            Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            DeleteBuilder<TinyDownloadTask, String> builder = dao.deleteBuilder();
            builder.where().eq("uid", taskUid);
            builder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }
    }

    public static boolean isTaskExist(String taskUid, Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<TinyDownloadTask> tasks = null;
        try {
            final Dao<TinyDownloadTask, String> dao = helper.getTaskDao();
            tasks = dao.queryForEq("uid", taskUid);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            OpenHelperManager.releaseHelper();
        }
        if (tasks != null && !tasks.isEmpty())
            return true;
        else
            return false;
    }

}
