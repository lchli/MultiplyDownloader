package com.lchli.tinydownloadlib;

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

    private static DaoSession daoSession;

    /**
     * @param downloadDaoSession dao for save download data.
     */
    public static void init(DaoSession downloadDaoSession) {
        if (daoSession != null) {
            return;
        }
        daoSession = downloadDaoSession;
    }

    public static DaoSession daoSession() {
        if (daoSession == null) {
            throw new IllegalStateException("Download lib not init,you must call TinyDownloadConfig#init first!");
        }
        return daoSession;
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
