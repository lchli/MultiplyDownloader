package com.lchli.tinydownloader;

import android.Manifest;
import android.app.Application;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.lchli.tinydownloadlib.TinyDownloadConfig;

/**
 * Created by lchli on 2016/8/20.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.currentThread().setUncaughtExceptionHandler(new AppExceptionHandler());
        Dexter.initialize(this);
        TinyDownloadConfig.init(this);

        Dexter.checkPermission(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {

            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    }
}
