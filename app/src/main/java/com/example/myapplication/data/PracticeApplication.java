package com.example.myapplication.data;

import android.app.Application;

/** 首次启动即建库和导入小型内置题库，之后启动只打开现有数据库。 */
public class PracticeApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppDatabaseHelper.getInstance(this).getWritableDatabase();
    }
}
