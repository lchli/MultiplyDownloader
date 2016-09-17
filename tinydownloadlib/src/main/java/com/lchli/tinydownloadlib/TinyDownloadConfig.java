package com.lchli.tinydownloadlib;

import android.content.Context;

import com.apkfuns.logutils.LogUtils;

/**
 * @author lchli
 *         this class is used to config download.
 */

public final class TinyDownloadConfig {

    public static final int ERROR_CODE_TASK_EXIST = 1;
    public static final int ERROR_CODE_TASK_EXCEPTION = 2;

    public static final int TASK_STATE_FINISHED = 1;
    public static final int TASK_STATE_UNFINISHED = 2;

    static final int DOWNLOAD_CONNECT_TIME_OUT = 10_000;//in millSecond.

    static int sDownloadBuffer = 10240;
    static int sDownloadThreadCounts = 3;
    static int sTaskProgressUpdateInterval = 1000;

    private static Context context;


     public static void init(Context ctx) {
        if (context != null) {
            return;
        }
        context = ctx.getApplicationContext();
    }

     static Context context() {
        if (context == null) {
            throw new IllegalStateException("Download lib not init,you must call TinyDownloadConfig#init first!");
        }
        return context;
    }

    public static void setDownloadBufferSize(int sizeInBytes) {
        sDownloadBuffer = sizeInBytes;
    }

    public static void setTaskProgressUpdateInterval(int intervalInMillSeconds) {
        sTaskProgressUpdateInterval = intervalInMillSeconds;
    }

    public static void setIsLogOpen(boolean isLogOpen) {
        LogUtils.configAllowLog = isLogOpen;
    }
}
