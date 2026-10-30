package com.example.myapplication.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import com.example.myapplication.model.Question;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "xingce.db";
    public static final int DATABASE_VERSION = 2;
    private static volatile AppDatabaseHelper instance;
    private final Context context;

    public static AppDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabaseHelper.class) {
                if (instance == null) instance = new AppDatabaseHelper(context.getApplicationContext());
            }
        }
        return instance;
    }

    public AppDatabaseHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    AppDatabaseHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
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
        seed(db);
    }

    private void seed(SQLiteDatabase db) {
        try {
            QuestionImporter.ParseResult parsed = new QuestionImporter().parseUtf8(
                    context.getAssets().open("sample_questions.txt"), "项目自编示例 v1");
            if (!parsed.errors.isEmpty() || parsed.questions.isEmpty()) {
                throw new IllegalStateException("内置题库格式错误：" + parsed.errors);
            }
            for (Question question : parsed.questions) QuestionRepository.insert(db, question);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取内置题库", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 框架没有自动导入；v2 保留全部旧题、历史和错题，只补入自编题库。
        if (oldVersion == 1 && newVersion == 2) {
            seed(db);
        } else {
            throw new IllegalStateException("数据库升级规则尚未定义：" + oldVersion + " → " + newVersion);
        }
    }
}
