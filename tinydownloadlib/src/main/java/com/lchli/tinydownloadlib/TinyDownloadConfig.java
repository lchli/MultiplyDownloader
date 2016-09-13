package com.lchli.tinydownloadlib;

import android.content.Context;

import com.apkfuns.logutils.LogUtils;

import org.greenrobot.greendao.database.Database;

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

    private static final int DEF_DOWNLOAD_BUFFER = 10240;
    static int sDownloadBuffer = DEF_DOWNLOAD_BUFFER;

    private static final int DEF_DOWNLOAD_THREAD_COUNT = 3;
    static int sDownloadThreadCounts = DEF_DOWNLOAD_THREAD_COUNT;

    private static final int DEF_PROGRESS_UPDATE_INTERVAL = 1000;//ms.
    static int sTaskProgressUpdateInterval = DEF_PROGRESS_UPDATE_INTERVAL;

    static Context context;
    static DaoSession daoSession;


    public static void init(Context ctx) {
        if (context != null) {//already inited.
            return;
        }
        context = ctx.getApplicationContext();
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(ctx, "download-db");
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
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
