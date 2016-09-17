package com.lchli.tinydownloader;

import android.os.Environment;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by lchli on 2016/8/14.
 */

public class AppExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        ex.printStackTrace();
        LogUtils.e("error>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/downloadException.txt");
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(file));
            ex.printStackTrace(printWriter);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.e("app exception log save fail.");
        }
    }
}
