package com.lchli.tinydownloadlib;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.apkfuns.logutils.LogUtils;

import java.lang.ref.WeakReference;

/**
 * @author lchli
 *         in order to reduce memory occupy,this service run in service process.
 *         so you must bind this service before your call,then you will get a IDownloadManager for use.
 */

public class TinyDownloadService extends Service {

    private static final int CLOSE_SERVICE_MSG = 1;
    private static final int CLOSE_SERVICE_TIMER_INTERVAL = 30_000;

    private TinyDownloadManager mTinyDownloadManager;
    private Handler mCloseServiceHandler;
    private boolean isUnbinded = false;

    private static void start(Context context) {
        Intent it = new Intent(context, TinyDownloadService.class);
        context.startService(it);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        binded();
        return new DownloadBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        binded();
        super.onRebind(intent);
    }

    private void binded() {
        isUnbinded = false;
        start(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isUnbinded = true;
        return false;
    }

    private class DownloadBinder extends IDownloadManager.Stub {

        @Override
        public boolean isTaskDownloading(String taskId) throws RemoteException {
            return mTinyDownloadManager.runningDownloaders.get(taskId) != null;
        }

        @Override
        public void addTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.addTask(task);
            beginForeground();
        }

        @Override
        public void continueTask(TinyDownloadTask task) throws RemoteException {
            mTinyDownloadManager.continueTask(task);
            beginForeground();
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
        mCloseServiceHandler = new CloseServiceHandler(mTinyDownloadManager, this);
        mCloseServiceHandler.sendEmptyMessageDelayed(CLOSE_SERVICE_MSG, CLOSE_SERVICE_TIMER_INTERVAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTinyDownloadManager = null;
        mCloseServiceHandler.removeMessages(CLOSE_SERVICE_MSG);
        stopForeground(true);
        LogUtils.e("TinyDownloadService#onDestroy>>>>>>>>>>>>>>>>");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void beginForeground() {
        Notification notification = new Notification();
        startForeground(1, notification);
    }

    /**
     * this handler is used to close process when there is no running tasks and no binders.
     */
    private static class CloseServiceHandler extends Handler {

        private WeakReference<TinyDownloadManager> handlerWeakReference;
        private WeakReference<TinyDownloadService> serviceWeakReference;

        public CloseServiceHandler(TinyDownloadManager downloadManager, TinyDownloadService service) {
            handlerWeakReference = new WeakReference<>(downloadManager);
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CLOSE_SERVICE_MSG) {
                final TinyDownloadManager downloadManager = handlerWeakReference.get();
                if (downloadManager != null && downloadManager.isIdle()) {
                    final TinyDownloadService service = serviceWeakReference.get();
                    if (service != null && service.isUnbinded) {
                        service.stopSelf();
                        return;
                    }
                }
                sendEmptyMessageDelayed(CLOSE_SERVICE_MSG, CLOSE_SERVICE_TIMER_INTERVAL);
            }
        }

    }
}
