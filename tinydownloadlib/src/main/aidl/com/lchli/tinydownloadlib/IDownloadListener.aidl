// IDownloadListener.aidl
package com.lchli.tinydownloadlib;
import com.lchli.tinydownloadlib.TinyDownloadTask;
// Declare any non-default types here with import statements

interface IDownloadListener {

            void onProgressChanged(inout TinyDownloadTask task);
            void onTaskAdded(inout TinyDownloadTask task);
            void onTaskDeleted(inout TinyDownloadTask task);
            void onDownloadSuccess(inout TinyDownloadTask task);
            void onDownloadError(inout TinyDownloadTask task, int errorCode,String errorMsg);

}
