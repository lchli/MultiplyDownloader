package com.lchli.tinydownloadlib;

import android.accounts.NetworkErrorException;
import android.os.RemoteException;
import android.os.SystemClock;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lchli
 *         multiply thread download  engine.
 *         this support resume from break point.
 */

class TinyDownloader {

    private static final int THREAD_COUNT = TinyDownloadConfig.sDownloadThreadCounts;
    private static final long KEEP_ALIVE_TIME = 5;
    private static final int UPDATE_PROGRESS_INTERVAL_SECOND = TinyDownloadConfig.sTaskProgressUpdateInterval;

    private final TinyDownloadManager mTinyDownloadManager;
    private BlockingQueue<Runnable> workQueue;
    private ThreadPoolExecutor mDownloadThreadPool;
    private TinyDownloadTask mTinyDownloadTask;
    private File downloadFile;
    private File infoFile;
    private int perThreadLength;//per thread should download length.
    private AtomicLong totalFinish = new AtomicLong(0);
    private AtomicBoolean isUserCancel = new AtomicBoolean(false);
    private AtomicBoolean isException = new AtomicBoolean(false);
    private long previousFinished = -1;
    private long previousTime = -1;
    private Exception downloadException;

    TinyDownloader(TinyDownloadManager tinyDownloadManager) {
        mTinyDownloadManager = tinyDownloadManager;
    }

    void download(final TinyDownloadTask task) {
        mTinyDownloadTask = task;
        if (mTinyDownloadTask.threadCount <= 0) {//new task.
            mTinyDownloadTask.threadCount = THREAD_COUNT;
        }
        final int maxRunnable = mTinyDownloadTask.threadCount + 1;
        workQueue = new LinkedBlockingDeque<>(maxRunnable);
        mDownloadThreadPool = new ThreadPoolExecutor(maxRunnable, maxRunnable, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue);

        mDownloadThreadPool.execute(new UpdateProgressRunnable());
    }

    private class UpdateProgressRunnable implements Runnable {
        @Override
        public void run() {
            //create save dir.
            File dir = new File(mTinyDownloadTask.saveDir);
            if (!dir.exists()) {
                boolean success = dir.mkdirs();
                if (!success) {
                    downloadError(new IllegalStateException("create task save dir fail."));
                    return;
                }
            }
            //get remote file info.
            URL url;
            try {
                url = new URL(mTinyDownloadTask.url);
            } catch (MalformedURLException e) {
                LogUtils.e(e.getMessage());
                downloadError(e);
                return;
            }

            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                downloadError(e);
                LogUtils.e(e.getMessage());
                return;

            }
            conn.setConnectTimeout(TinyDownloadConfig.DOWNLOAD_CONNECT_TIME_OUT);
            final int downloadFileTotalLength = conn.getContentLength();
            conn.disconnect();
            if (downloadFileTotalLength == -1) {
                LogUtils.e("cannot get download file contentLength.");
                downloadError(new NetworkErrorException("cannot get download file contentLength."));
                return;
            }

            downloadFile = new File(dir, mTinyDownloadTask.name);
            infoFile = new File(dir, mTinyDownloadTask.name + ".info");
            if (mTinyDownloadTask.totalLength == 0) {//new task.
                mTinyDownloadTask.totalLength = downloadFileTotalLength;
                TaskTable.updateTask(mTinyDownloadTask, TinyDownloadConfig.context());
            } else {//old task.
                if (mTinyDownloadTask.totalLength != downloadFileTotalLength) {//remote file changed.
                    downloadFile.delete();
                    infoFile.delete();
                    mTinyDownloadTask.totalLength = downloadFileTotalLength;
                    TaskTable.updateTask(mTinyDownloadTask, TinyDownloadConfig.context());
                }

            }
            perThreadLength = (downloadFileTotalLength + mTinyDownloadTask.threadCount - 1) / mTinyDownloadTask.threadCount;

            //create download file.
            if (!downloadFile.exists()) {
                RandomAccessFile randomDownloadFile = null;
                try {
                    infoFile.delete();//if file not exist,should delete its info file.
                    downloadFile.createNewFile();
                    randomDownloadFile = new RandomAccessFile(downloadFile, "rwd");
                    randomDownloadFile.setLength(downloadFileTotalLength);
                    randomDownloadFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadFile.delete();
                    downloadError(e);
                    return;
                } finally {
                    closeFile(randomDownloadFile);
                }
            }
            //create info file.
            if (!infoFile.exists()) {
                RandomAccessFile randomInfoFile = null;
                try {
                    infoFile.createNewFile();
                    randomInfoFile = new RandomAccessFile(infoFile, "rwd");
                    for (int i = 0; i < mTinyDownloadTask.threadCount; i++) {
                        randomInfoFile.writeLong(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    infoFile.delete();
                    downloadError(e);
                    return;
                } finally {
                    closeFile(randomInfoFile);
                }
            }
            //commit download task.
            for (int i = 0; i < mTinyDownloadTask.threadCount; i++) {
                mDownloadThreadPool.execute(new DownloadRunnable(i));
            }
            //publish speed and progress.
            while (true) {
                if (isException.get()) {
                    downloadError(downloadException);
                    break;
                }
                if (isUserCancel.get()) {
                    break;
                }
                if (mTinyDownloadTask.currentFinish == mTinyDownloadTask.totalLength) {
                    downloadSuccess();
                    break;
                }
                mTinyDownloadTask.currentFinish = totalFinish.get();

                long time = System.currentTimeMillis();
                if (previousTime != -1 && previousFinished != -1) {
                    long delta = mTinyDownloadTask.currentFinish - previousFinished;
                    long deltaTime = (time - previousTime);
                    mTinyDownloadTask.speed = delta / deltaTime * 1000;//per second.

                    mTinyDownloadManager.onTaskStateChanged(new TinyDownloadManager.TaskStateChangedCallback() {
                        @Override
                        public void run(IDownloadListener listener) throws RemoteException {
                            listener.onProgressChanged(mTinyDownloadTask);
                        }
                    });
                }
                previousTime = time;
                previousFinished = mTinyDownloadTask.currentFinish;

                SystemClock.sleep(UPDATE_PROGRESS_INTERVAL_SECOND);

            }//while end.


        }
    }


    private class DownloadRunnable implements Runnable {

        private int id;

        DownloadRunnable(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            long threadFinish;
            RandomAccessFile randomInfoFile = null;
            RandomAccessFile randomFile = null;
            InputStream inputStream = null;
            try {
                randomInfoFile = new RandomAccessFile(infoFile, "rwd");
                randomInfoFile.seek(id * 8);
                threadFinish = randomInfoFile.readLong();
                totalFinish.addAndGet(threadFinish);

                long start = id * perThreadLength;
                long end = start + perThreadLength - 1;
                start += threadFinish;

                randomFile = new RandomAccessFile(downloadFile, "rwd");
                randomFile.seek(start);

                URL url = new URL(mTinyDownloadTask.url);
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                conn.setConnectTimeout(TinyDownloadConfig.DOWNLOAD_CONNECT_TIME_OUT);
                conn.setDoInput(true);
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                inputStream = conn.getInputStream();

                final byte[] buffer = new byte[TinyDownloadConfig.sDownloadBuffer];
                int len;
                while (!isException.get() && !isUserCancel.get() && (len = inputStream.read(buffer)) != -1) {
                    randomFile.write(buffer, 0, len);

                    threadFinish += len;
                    randomInfoFile.seek(id * 8);
                    randomInfoFile.writeLong(threadFinish);
                    totalFinish.addAndGet(len);
                }//while end.

                closeFile(randomFile);
                closeFile(randomInfoFile);
                closeStream(inputStream);

            } catch (Exception e) {
                downloadException = e;
                isException.set(true);//if someone thread happen error,we should close all thread.
                e.printStackTrace();
                closeFile(randomFile);
                closeFile(randomInfoFile);
                closeStream(inputStream);
            }


        }
    }

    private void closeFile(RandomAccessFile f) {
        if (f != null) {
            try {
                f.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeStream(InputStream f) {
        if (f != null) {
            try {
                f.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void downloadSuccess() {
        synchronized (mTinyDownloadManager) {
            mDownloadThreadPool.shutdown();//not accept new task.
            infoFile.delete();
            //update db.
            mTinyDownloadTask.state = TinyDownloadConfig.TASK_STATE_FINISHED;
            TaskTable.updateTask(mTinyDownloadTask, TinyDownloadConfig.context());
            //update state.
            mTinyDownloadManager.onTaskStateChanged(new TinyDownloadManager.TaskStateChangedCallback() {
                @Override
                public void run(IDownloadListener listener) throws RemoteException {
                    listener.onDownloadSuccess(mTinyDownloadTask);
                }
            });

            mTinyDownloadManager.removeDownloader(mTinyDownloadTask.uid);
        }
    }


    private void downloadError(final Exception e) {
        synchronized (mTinyDownloadManager) {
            mDownloadThreadPool.shutdown();
            mTinyDownloadManager.onTaskStateChanged(new TinyDownloadManager.TaskStateChangedCallback() {
                @Override
                public void run(IDownloadListener listener) throws RemoteException {
                    listener.onDownloadError(mTinyDownloadTask, TinyDownloadConfig.ERROR_CODE_TASK_EXCEPTION, e.getLocalizedMessage());
                }
            });

            mTinyDownloadManager.removeDownloader(mTinyDownloadTask.uid);
        }
    }


    void cancel() {
        isUserCancel.set(true);
        mDownloadThreadPool.shutdown();
    }

}
