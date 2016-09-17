/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lchli.tinydownloadlib;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;


/**
 * Database helper class used to manage the creation and upgrading of your database. This class also
 * usually provides the DAOs used by the other classes.
 * note:if use greendao,upgrade is boring.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final Class<?>[] DATA_CLASSES = {TinyDownloadTask.class,

    };

    public static final String DATABASE_NAME = "download.db";
    private static final int DATABASE_VERSION = 1;


    private Dao<TinyDownloadTask, String> mDownloadTaskDao = null;
    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
    }

    /**
     * This is called when the database is first created. Usually you should call createTable
     * statements here to create the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {

            for (Class<?> dataClass : DATA_CLASSES) {
                TableUtils.createTable(connectionSource, dataClass);
            }

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows
     * you to adjust the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion,
                          int newVersion) {
        try {

            for (Class<?> dataClass : DATA_CLASSES) {
                TableUtils.dropTable(connectionSource, dataClass, true);
            }

            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<TinyDownloadTask, String> getTaskDao() throws SQLException {
        if (mDownloadTaskDao == null) {
            mDownloadTaskDao = getDao(TinyDownloadTask.class);
        }
        return mDownloadTaskDao;
    }


    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        mDownloadTaskDao = null;
        super.close();
    }

}