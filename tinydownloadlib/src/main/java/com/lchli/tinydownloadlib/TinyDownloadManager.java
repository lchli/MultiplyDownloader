package com.lchli.tinydownloadlib;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.apkfuns.logutils.LogUtils;

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

    void removeDownloader(String key) {
        runningDownloaders.remove(key);
    }

    boolean isIdle() {
        synchronized (this) {
            return runningDownloaders.isEmpty();
        }
    }

    /**
     * note:this run in binder-Thread.
     *
     * @param task
     */
    void addTask(final TinyDownloadTask task) {
        synchronized (this) {
            TinyDownloader downloader = runningDownloaders.get(task.uid);
            final String errorMsg = "task already exist!";
            if (downloader != null) {
                LogUtils.e("task already downloading task=:" + task);
                onTaskStateChanged(new TaskStateChangedCallback() {
                    @Override
                    public void run(IDownloadListener listener) throws RemoteException {
                        listener.onDownloadError(task, TinyDownloadConfig.ERROR_CODE_TASK_EXIST, errorMsg);
                    }
                });
                return;
            }
            if (TaskTable.isTaskExist(task.uid, TinyDownloadConfig.context())) {
                LogUtils.e("task already exist task=:" + task);
                onTaskStateChanged(new TaskStateChangedCallback() {
                    @Override
                    public void run(IDownloadListener listener) throws RemoteException {
                        listener.onDownloadError(task, TinyDownloadConfig.ERROR_CODE_TASK_EXIST, errorMsg);
                    }
                });
                return;
            }
            TaskTable.addTask(task, TinyDownloadConfig.context());

            onTaskStateChanged(new TaskStateChangedCallback() {
                @Override
                public void run(IDownloadListener listener) throws RemoteException {
                    listener.onTaskAdded(task);
                }
            });

            TinyDownloader newDownloader = new TinyDownloader(this);
            runningDownloaders.put(task.uid, newDownloader);
            newDownloader.download(task);
        }

    }

    void continueTask(TinyDownloadTask task) {
        synchronized (this) {
            TinyDownloader downloader = runningDownloaders.get(task.uid);
            if (downloader != null) {
                LogUtils.e("task already downloading task=:" + task);
                return;
            }
            TinyDownloader newDownloader = new TinyDownloader(this);
            runningDownloaders.put(task.uid, newDownloader);
            newDownloader.download(task);
        }
    }

    void pauseTask(TinyDownloadTask task) {
        synchronized (this) {
            TinyDownloader downloader = runningDownloaders.get(task.uid);
            if (downloader == null) {
                return;
            }
            downloader.cancel();
            removeDownloader(task.uid);
        }
    }

    void deleteTask(final TinyDownloadTask task) {
        synchronized (this) {
            TinyDownloader downloader = runningDownloaders.get(task.uid);
            if (downloader != null) {
                downloader.cancel();
            }
            //delete from db.
            TaskTable.deleteTask(task.uid, TinyDownloadConfig.context());

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

            removeDownloader(task.uid);
        }
    }

    synchronized void onTaskStateChanged(TaskStateChangedCallback cb) {
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
        return TaskTable.queryFinishedTasks(TinyDownloadConfig.context());
    }

    public static List<TinyDownloadTask> queryUnFinishedTasks() {
        return TaskTable.queryUnFinishedTasks(TinyDownloadConfig.context());
    }

    public static List<TinyDownloadTask> queryAllTasks() {
        return TaskTable.queryAllTasks(TinyDownloadConfig.context());
    }


}
