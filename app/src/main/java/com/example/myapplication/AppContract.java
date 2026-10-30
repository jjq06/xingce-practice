package com.example.myapplication;

/** 跨页面参数与本地设置的唯一键定义。 */
public final class AppContract {
    public static final String EXTRA_CATEGORY = "com.example.myapplication.extra.CATEGORY";
    public static final String EXTRA_PRACTICE_MODE = "com.example.myapplication.extra.PRACTICE_MODE";
    public static final String EXTRA_RECORD_ID = "com.example.myapplication.extra.RECORD_ID";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_WRONG_BOOK = "wrong_book";
    public static final String PREFS_NAME = "practice_settings";
    public static final String PREF_DEFAULT_QUESTION_COUNT = "default_question_count";
    public static final int DEFAULT_QUESTION_COUNT = 10;

    private AppContract() { }
}
