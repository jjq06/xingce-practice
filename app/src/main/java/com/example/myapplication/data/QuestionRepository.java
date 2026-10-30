package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.myapplication.model.Question;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class QuestionRepository {
    private final AppDatabaseHelper helper;

    public QuestionRepository(Context context) {
        helper = AppDatabaseHelper.getInstance(context);
    }

    QuestionRepository(AppDatabaseHelper helper) { this.helper = helper; }

    public int count(String category) {
        if (category == null) return count();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM questions WHERE category = ?", new String[]{category})) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    public Map<String, Integer> categoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT category, COUNT(*) FROM questions GROUP BY category ORDER BY category", null)) {
            while (cursor.moveToNext()) counts.put(cursor.getString(0), cursor.getInt(1));
        }
        return counts;
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
                if (insert(db, question) != -1) inserted++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inserted;
    }

    static long insert(SQLiteDatabase db, Question question) {
        if (question == null || question.category == null || question.category.trim().isEmpty()
                || question.stem == null || question.stem.trim().isEmpty()
                || question.optionA == null || question.optionA.trim().isEmpty()
                || question.optionB == null || question.optionB.trim().isEmpty()
                || question.optionC == null || question.optionC.trim().isEmpty()
                || question.optionD == null || question.optionD.trim().isEmpty()
                || question.answer == null || !question.answer.matches("[ABCD]")) {
            throw new IllegalArgumentException("题目必填字段为空或答案不合法");
        }
        question.fingerprint = QuestionImporter.fingerprint(question);
        ContentValues values = new ContentValues();
        values.put("category", question.category);
        values.put("stem", question.stem);
        values.put("option_a", question.optionA);
        values.put("option_b", question.optionB);
        values.put("option_c", question.optionC);
        values.put("option_d", question.optionD);
        values.put("answer", question.answer);
        values.put("explanation", question.explanation == null ? "" : question.explanation);
        values.put("source", question.source == null ? "" : question.source);
        values.put("fingerprint", question.fingerprint);
        return db.insertWithOnConflict("questions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
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
