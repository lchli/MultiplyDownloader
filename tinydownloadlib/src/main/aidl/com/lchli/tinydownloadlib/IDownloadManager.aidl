// IDownloadManager.aidl
package com.lchli.tinydownloadlib;
import com.lchli.tinydownloadlib.TinyDownloadTask;
import com.lchli.tinydownloadlib.IDownloadListener;

// Declare any non-default types here with import statements

interface IDownloadManager {


            boolean isTaskDownloading(String taskId);
             void addTask(in TinyDownloadTask task);
             void continueTask(in TinyDownloadTask task);
             void pauseTask(in TinyDownloadTask task);
              void deleteTask(in TinyDownloadTask task);
              void registerDownloadListener(in IDownloadListener listener);
              void unregisterDownloadListener(in IDownloadListener listener);
}
