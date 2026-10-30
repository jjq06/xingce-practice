package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.myapplication.model.Question;
import java.util.ArrayList;
import java.util.List;

public class QuestionRepository {
    private final AppDatabaseHelper helper;

    public QuestionRepository(Context context) {
        helper = new AppDatabaseHelper(context);
    }

    public int count() {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM questions", null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    /** 返回已导入的分类名称，按名称升序排列。 */
    public List<String> categories() {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT DISTINCT category FROM questions ORDER BY category", null)) {
            while (cursor.moveToNext()) result.add(cursor.getString(0));
        }
        return result;
    }

    public List<Question> findByCategory(String category, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit 必须大于 0");
        List<Question> result = new ArrayList<>();
        String selection = category == null ? null : "category = ?";
        String[] args = category == null ? null : new String[]{category};
        try (Cursor cursor = helper.getReadableDatabase().query("questions", null, selection, args,
                null, null, "RANDOM()", String.valueOf(limit))) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    public Question findById(long id) {
        try (Cursor cursor = helper.getReadableDatabase().query("questions", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    public int insertAll(List<Question> questions) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int inserted = 0;
        db.beginTransaction();
        try {
            for (Question question : questions) {
                ContentValues values = new ContentValues();
                values.put("category", question.category);
                values.put("stem", question.stem);
                values.put("option_a", question.optionA);
                values.put("option_b", question.optionB);
                values.put("option_c", question.optionC);
                values.put("option_d", question.optionD);
                values.put("answer", question.answer);
                values.put("explanation", question.explanation);
                values.put("source", question.source);
                values.put("fingerprint", question.fingerprint);
                if (db.insertWithOnConflict("questions", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1) inserted++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inserted;
    }

    private Question read(Cursor cursor) {
        Question question = new Question();
        question.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        question.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        question.stem = cursor.getString(cursor.getColumnIndexOrThrow("stem"));
        question.optionA = cursor.getString(cursor.getColumnIndexOrThrow("option_a"));
        question.optionB = cursor.getString(cursor.getColumnIndexOrThrow("option_b"));
        question.optionC = cursor.getString(cursor.getColumnIndexOrThrow("option_c"));
        question.optionD = cursor.getString(cursor.getColumnIndexOrThrow("option_d"));
        question.answer = cursor.getString(cursor.getColumnIndexOrThrow("answer"));
        question.explanation = cursor.getString(cursor.getColumnIndexOrThrow("explanation"));
        question.source = cursor.getString(cursor.getColumnIndexOrThrow("source"));
        question.fingerprint = cursor.getString(cursor.getColumnIndexOrThrow("fingerprint"));
        return question;
    }
}
