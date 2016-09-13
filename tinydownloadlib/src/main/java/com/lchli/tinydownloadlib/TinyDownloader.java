package com.lchli.tinydownloadlib;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lchli
 *         multiply thread download  engine.
 *         this support resume from break point.
 */

class TinyDownloader {

    private static final int THREAD_COUNT = TinyDownloadConfig.sDownloadThreadCounts;
    private static final int MAX_RUNNABLE = THREAD_COUNT + 1;
    private static final int CORE_POOL_SIZE = MAX_RUNNABLE;
    private static final int MAX_POOL_SIZE = MAX_RUNNABLE;
    private static final long KEEP_ALIVE_TIME = 5;
    private final TinyDownloadManager mTinyDownloadManager;
    private BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>(MAX_RUNNABLE);
    private ThreadPoolExecutor mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue);
    private TinyDownloadTask mTinyDownloadTask;
    private File downloadFile;
    private File infoFile;
    //per thread should download length.
    private int perThreadLength;
    private AtomicLong totalFinish = new AtomicLong(0);
    private AtomicInteger finishedThreadCount = new AtomicInteger(0);
    private AtomicBoolean isUserCancel = new AtomicBoolean(false);
    private AtomicBoolean isException = new AtomicBoolean(false);
    private static final int UPDATE_PROGRESS_INTERVAL_SECOND = TinyDownloadConfig.sTaskProgressUpdateInterval;
    private long previousFinished = -1;
    private long previousTime = -1;

    TinyDownloader(TinyDownloadManager tinyDownloadManager) {
        mTinyDownloadManager = tinyDownloadManager;
    }

    void download(final TinyDownloadTask task) {
        mTinyDownloadTask = task;
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
                    return;
                }
            }
            //get remote file info.
            URL url;
            try {
                url = new URL(mTinyDownloadTask.url);
            } catch (MalformedURLException e) {
                LogUtils.e(e.getMessage());
                downloadError();
                return;

            }

            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                downloadError();
                LogUtils.e(e.getMessage());
                return;

            }
            conn.setConnectTimeout(TinyDownloadConfig.DOWNLOAD_CONNECT_TIME_OUT);
            final int downloadFileTotalLength = conn.getContentLength();
            if (downloadFileTotalLength == -1) {
                LogUtils.e("cannot get download file contentLength.");
                downloadError();
                return;
            }

            mTinyDownloadTask.totalLength = downloadFileTotalLength;
            conn.disconnect();
            perThreadLength = (downloadFileTotalLength + THREAD_COUNT - 1) / THREAD_COUNT;
            //create download file.
            downloadFile = new File(dir, mTinyDownloadTask.name);
            infoFile = new File(dir, mTinyDownloadTask.name + ".info");
            if (!downloadFile.exists()) {
                try {
                    infoFile.delete();//if file not exist,should delete its info file.
                    downloadFile.createNewFile();
                    RandomAccessFile randomDownloadFile = new RandomAccessFile(downloadFile, "rwd");
                    randomDownloadFile.setLength(downloadFileTotalLength);
                    randomDownloadFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadError();
                    return;
                }
            }
            //create info file.
            if (!infoFile.exists()) {
                try {
                    infoFile.createNewFile();
                    RandomAccessFile randomInfoFile = new RandomAccessFile(infoFile, "rwd");
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        randomInfoFile.writeLong(0);
                    }
                    randomInfoFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadError();
                    return;
                }
            }
            //commit download task.
            for (int i = 0; i < THREAD_COUNT; i++) {
                mDownloadThreadPool.execute(new DownloadRunnable(i));
            }
            //publish speed and progress.
            while (true) {
                if (isException.get()) {
                    break;
                }
                if (isUserCancel.get()) {
                    break;
                }
                if (mTinyDownloadTask.currentFinish == mTinyDownloadTask.totalLength) {
                    break;
                }
                mTinyDownloadTask.currentFinish = totalFinish.get();
                //save to db.
                TinyDownloadConfig.daoSession.getTinyDownloadTaskDao().update(mTinyDownloadTask);

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

                int finishCount = finishedThreadCount.incrementAndGet();
                if (isException.get() && finishCount == THREAD_COUNT) {
                    downloadError();
                    return;
                }
                if (isUserCancel.get() && finishCount == THREAD_COUNT) {
                    return;
                }
                if (finishCount == THREAD_COUNT) {//indicate finish success.
                    downloadSuccess();
                }

            } catch (Exception e) {
                isException.set(true);//if someone thread happen error,we should close all thread.

                e.printStackTrace();
                closeFile(randomFile);
                closeFile(randomInfoFile);
                closeStream(inputStream);

                int finishCount = finishedThreadCount.incrementAndGet();
                if (finishCount == THREAD_COUNT) {//ensure only one thread call downloadError(...)
                    downloadError();
                }

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
        mDownloadThreadPool.shutdownNow();
        mTinyDownloadManager.runningDownloaders.remove(mTinyDownloadTask.id);
        infoFile.delete();
        //update db.
        mTinyDownloadTask.state = TinyDownloadConfig.TASK_STATE_FINISHED;
        TinyDownloadConfig.daoSession.getTinyDownloadTaskDao().update(mTinyDownloadTask);
        //update state.
        mTinyDownloadManager.onTaskStateChanged(new TinyDownloadManager.TaskStateChangedCallback() {
            @Override
            public void run(IDownloadListener listener) throws RemoteException {
                listener.onDownloadSuccess(mTinyDownloadTask);
            }
        });
    }


    private void downloadError() {
        mDownloadThreadPool.shutdownNow();
        mTinyDownloadManager.runningDownloaders.remove(mTinyDownloadTask.id);

        mTinyDownloadManager.onTaskStateChanged(new TinyDownloadManager.TaskStateChangedCallback() {
            @Override
            public void run(IDownloadListener listener) throws RemoteException {
                listener.onDownloadError(mTinyDownloadTask, TinyDownloadConfig.ERROR_CODE_TASK_EXCEPTION);
            }
        });
    }


    void cancel() {
        mTinyDownloadManager.runningDownloaders.remove(mTinyDownloadTask.id);
        isUserCancel.set(true);
        mDownloadThreadPool.shutdownNow();
    }

}
