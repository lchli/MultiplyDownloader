package com.lchli.tinydownloader;

import com.apkfuns.logutils.LogUtils;

/**
 * Created by lchli on 2016/8/14.
 */

public class AppExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        ex.printStackTrace();
        LogUtils.e(ex.getMessage());

//        try {
//            FileUtils.write(new File(LocalConst.RecentExceptionFile), ex.getMessage());
//        } catch (IOException e) {
//            e.printStackTrace();
//            LogUtils.e("app exception log save fail.");
//        }
    }
}
