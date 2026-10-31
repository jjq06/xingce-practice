package com.example.myapplication.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "xingce.db";
    public static final int DATABASE_VERSION = 1;

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE questions (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL, " +
                "stem TEXT NOT NULL, option_a TEXT NOT NULL, option_b TEXT NOT NULL, " +
                "option_c TEXT NOT NULL, option_d TEXT NOT NULL, answer TEXT NOT NULL, " +
                "explanation TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '', fingerprint TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE INDEX idx_questions_category ON questions(category)");
        db.execSQL("CREATE TABLE practice_records (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL, " +
                "started_at INTEGER NOT NULL, submitted_at INTEGER NOT NULL, question_count INTEGER NOT NULL, " +
                "correct_count INTEGER NOT NULL, score INTEGER NOT NULL, duration_seconds INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE answer_details (id INTEGER PRIMARY KEY AUTOINCREMENT, record_id INTEGER NOT NULL, " +
                "question_id INTEGER NOT NULL, user_answer TEXT, is_correct INTEGER NOT NULL, " +
                "FOREIGN KEY(record_id) REFERENCES practice_records(id), FOREIGN KEY(question_id) REFERENCES questions(id))");
        db.execSQL("CREATE TABLE wrong_questions (question_id INTEGER PRIMARY KEY, last_wrong_at INTEGER NOT NULL, " +
                "wrong_count INTEGER NOT NULL, FOREIGN KEY(question_id) REFERENCES questions(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("数据库升级规则尚未定义：" + oldVersion + " → " + newVersion);
    }
}
