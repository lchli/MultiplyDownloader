package com.lchli.tinydownloadlib;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

/**
 * @author lchli
 *         in order to reduce memory occupy,this service run in service process.
 *         so you must bind this service before your call,then you will get a IDownloadManager for use.
 *         at last,you can register broadcast receiver to receive download state,result,etc.
 *         {@link TinyDownloadConfig#getDownloadReceiverFilter()}
 *         {@see TinyDownloadConfig#DownloadBroadCastAction}
 */

public class TinyDownloadService extends Service {

    private TinyDownloadManager mTinyDownloadManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    private class DownloadBinder extends IDownloadManager.Stub {

        @Override
        public boolean isTaskDownloading(String taskId) throws RemoteException {
            return mTinyDownloadManager.runningDownloaders.get(taskId) != null;
        }

        @Override
        public void addTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.addTask(task);
        }

        @Override
        public void continueTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.continueTask(task);
        }

        @Override
        public void pauseTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.pauseTask(task);
        }

        @Override
        public void deleteTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.deleteTask(task);
        }

        @Override
        public void registerDownloadListener(IDownloadListener listener) throws RemoteException {
            mTinyDownloadManager.mDownloadListeners.register(listener);
        }

        @Override
        public void unregisterDownloadListener(IDownloadListener listener) throws RemoteException {
            mTinyDownloadManager.mDownloadListeners.unregister(listener);
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTinyDownloadManager = new TinyDownloadManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTinyDownloadManager = null;
    }
}
