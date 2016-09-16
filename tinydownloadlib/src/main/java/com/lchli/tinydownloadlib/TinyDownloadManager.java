package com.lchli.tinydownloadlib;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.apkfuns.logutils.LogUtils;

import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lchli
 *         this class offer download control and task query.
 */

public class TinyDownloadManager {

    final Map<String, TinyDownloader> runningDownloaders = new HashMap<>();
    final RemoteCallbackList<IDownloadListener> mDownloadListeners = new RemoteCallbackList<>();

    TinyDownloadManager() {
    }

    void addTask(final TinyDownloadTask task) {
        TinyDownloader downloader = runningDownloaders.get(task.id);
        if (downloader != null) {
            LogUtils.e("task already downloading task=:" + task);
            onTaskStateChanged(new TaskStateChangedCallback() {
                @Override
                public void run(IDownloadListener listener) throws RemoteException {
                    listener.onDownloadError(task, TinyDownloadConfig.ERROR_CODE_TASK_EXIST);
                }
            });
            return;
        }
        QueryBuilder<TinyDownloadTask> builder = TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().queryBuilder();
        builder.where(TinyDownloadTaskDao.Properties.Id.eq(task.id));
        TinyDownloadTask existTask = builder.build().unique();
        if (existTask != null) {
            LogUtils.e("task already exist task=:" + task);
            onTaskStateChanged(new TaskStateChangedCallback() {
                @Override
                public void run(IDownloadListener listener) throws RemoteException {
                    listener.onDownloadError(task, TinyDownloadConfig.ERROR_CODE_TASK_EXIST);
                }
            });
            return;
        }
        TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().insert(task);

        onTaskStateChanged(new TaskStateChangedCallback() {
            @Override
            public void run(IDownloadListener listener) throws RemoteException {
                listener.onTaskAdded(task);
            }
        });

        TinyDownloader newDownloader = new TinyDownloader(this);
        runningDownloaders.put(task.id, newDownloader);
        newDownloader.download(task);


    }

    void continueTask(TinyDownloadTask task) {
        TinyDownloader downloader = runningDownloaders.get(task.id);
        if (downloader != null) {
            LogUtils.e("task already downloading task=:" + task);
            return;
        }
        TinyDownloader newDownloader = new TinyDownloader(this);
        runningDownloaders.put(task.id, newDownloader);
        newDownloader.download(task);
    }

    void pauseTask(TinyDownloadTask task) {
        TinyDownloader downloader = runningDownloaders.get(task.id);
        if (downloader == null) {
            return;
        }
        downloader.cancel();
        runningDownloaders.remove(task.id);
    }

    void deleteTask(final TinyDownloadTask task) {
        pauseTask(task);
        //delete from db.
        TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().delete(task);

        onTaskStateChanged(new TaskStateChangedCallback() {
            @Override
            public void run(IDownloadListener listener) throws RemoteException {
                listener.onTaskDeleted(task);
            }
        });
        //delete file.
        File file = new File(task.saveDir, task.name);
        if (file.exists()) {
            file.delete();
        }
        File infoFile = new File(task.saveDir, task.name + ".info");
        if (infoFile.exists()) {
            infoFile.delete();
        }
    }

    void onTaskStateChanged(TaskStateChangedCallback cb) {
        final int COUNT = mDownloadListeners.beginBroadcast();
        for (int i = 0; i < COUNT; i++) {
            IDownloadListener listener = mDownloadListeners.getBroadcastItem(i);
            if (listener != null) {
                try {
                    cb.run(listener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mDownloadListeners.finishBroadcast();
    }

    interface TaskStateChangedCallback {

        void run(IDownloadListener listener) throws RemoteException;

    }


    public static List<TinyDownloadTask> queryFinishedTasks() {
        QueryBuilder<TinyDownloadTask> builder = TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().queryBuilder();
        builder.where(TinyDownloadTaskDao.Properties.State.eq(TinyDownloadConfig.TASK_STATE_FINISHED));
        return builder.build().list();
    }

    public static List<TinyDownloadTask> queryUnFinishedTasks() {
        QueryBuilder<TinyDownloadTask> builder = TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().queryBuilder();
        builder.where(TinyDownloadTaskDao.Properties.State.eq(TinyDownloadConfig.TASK_STATE_UNFINISHED));
        return builder.build().list();
    }

    public static List<TinyDownloadTask> queryAllTasks() {
        QueryBuilder<TinyDownloadTask> builder = TinyDownloadConfig.daoSession().getTinyDownloadTaskDao().queryBuilder();
        return builder.build().list();
    }


    public static TinyDownloadTaskDao getTaskDao() {
        return TinyDownloadConfig.daoSession().getTinyDownloadTaskDao();
    }

}
