package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.myapplication.model.AnswerDetail;
import com.example.myapplication.model.PracticeRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 记录、逐题答案和错题在同一个事务中保存。所有查询返回独立模型。 */
public class PracticeRepository {
    private final AppDatabaseHelper helper;

    public PracticeRepository(Context context) {
        helper = AppDatabaseHelper.getInstance(context);
    }

    PracticeRepository(AppDatabaseHelper helper) { this.helper = helper; }

    public long save(PracticeRecord record, List<AnswerDetail> answers) {
        if (record == null || answers == null) throw new IllegalArgumentException("记录和答案不能为空");
        synchronized (record) {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            long recordId;
            int correctCount = 0;
            List<Boolean> correctness = new ArrayList<>();
            try {
                // 调用方保留 save 返回的 ID（或同一模型），重复提交返回旧记录。
                if (record.id > 0) {
                    if (findById(record.id) == null) throw new IllegalArgumentException("记录 ID 不存在");
                    db.setTransactionSuccessful();
                    return record.id;
                }
                if (answers.isEmpty() || record.questionCount != answers.size()
                        || record.startedAt < 0 || record.submittedAt < record.startedAt) {
                    throw new IllegalArgumentException("题数或起止时间不合法");
                }
                Set<Long> seen = new HashSet<>();
                for (AnswerDetail answer : answers) {
                    if (answer == null || !seen.add(answer.questionId)
                            || (answer.userAnswer != null && !answer.userAnswer.matches("[ABCD]"))) {
                        throw new IllegalArgumentException("题目重复或用户答案不合法");
                    }
                    try (Cursor cursor = db.query("questions", new String[]{"answer"}, "id = ?",
                            new String[]{String.valueOf(answer.questionId)}, null, null, null)) {
                        if (!cursor.moveToFirst()) throw new IllegalArgumentException("题目不存在：" + answer.questionId);
                        boolean correct = cursor.getString(0).equals(answer.userAnswer);
                        correctness.add(correct);
                        if (correct) correctCount++;
                    }
                }
                ContentValues values = new ContentValues();
                values.put("category", record.category == null ? "全部" : record.category);
                values.put("started_at", record.startedAt);
                values.put("submitted_at", record.submittedAt);
                values.put("question_count", answers.size());
                values.put("correct_count", correctCount);
                values.put("score", correctCount * 10);
                values.put("duration_seconds", (record.submittedAt - record.startedAt) / 1000);
                recordId = db.insertOrThrow("practice_records", null, values);
                for (int i = 0; i < answers.size(); i++) {
                    AnswerDetail answer = answers.get(i);
                    boolean correct = correctness.get(i);
                    values.clear();
                    values.put("record_id", recordId);
                    values.put("question_id", answer.questionId);
                    values.put("user_answer", answer.userAnswer);
                    values.put("is_correct", correct ? 1 : 0);
                    db.insertOrThrow("answer_details", null, values);
                    String[] questionId = {String.valueOf(answer.questionId)};
                    if (correct) {
                        db.delete("wrong_questions", "question_id = ?", questionId);
                    } else {
                        values.clear();
                        values.put("question_id", answer.questionId);
                        values.put("last_wrong_at", record.submittedAt);
                        values.put("wrong_count", 1);
                        long inserted = db.insertWithOnConflict("wrong_questions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                        if (inserted == -1) db.execSQL("UPDATE wrong_questions SET last_wrong_at = ?, "
                                + "wrong_count = wrong_count + 1 WHERE question_id = ?",
                                new Object[]{record.submittedAt, answer.questionId});
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // 仅在事务成功提交后回填，失败时可以安全重试。
            record.id = recordId;
            record.correctCount = correctCount;
            record.score = correctCount * 10;
            record.durationSeconds = (record.submittedAt - record.startedAt) / 1000;
            for (int i = 0; i < answers.size(); i++) {
                answers.get(i).recordId = recordId;
                answers.get(i).correct = correctness.get(i);
            }
            return recordId;
        }
    }

    public PracticeRecord findById(long recordId) {
        try (Cursor cursor = helper.getReadableDatabase().query("practice_records", null, "id = ?",
                new String[]{String.valueOf(recordId)}, null, null, null)) {
            return cursor.moveToFirst() ? readRecord(cursor) : null;
        }
    }

    public List<PracticeRecord> history() {
        List<PracticeRecord> records = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("practice_records", null, null, null,
                null, null, "submitted_at DESC, id DESC")) {
            while (cursor.moveToNext()) records.add(readRecord(cursor));
        }
        return records;
    }

    public List<Long> wrongQuestionIds() {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("wrong_questions", new String[]{"question_id"},
                null, null, null, null, "last_wrong_at DESC, question_id ASC")) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }

    public List<AnswerDetail> findAnswers(long recordId) {
        List<AnswerDetail> answers = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("answer_details", null, "record_id = ?",
                new String[]{String.valueOf(recordId)}, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                AnswerDetail answer = new AnswerDetail();
                answer.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                answer.recordId = recordId;
                answer.questionId = cursor.getLong(cursor.getColumnIndexOrThrow("question_id"));
                answer.userAnswer = cursor.getString(cursor.getColumnIndexOrThrow("user_answer"));
                answer.correct = cursor.getInt(cursor.getColumnIndexOrThrow("is_correct")) != 0;
                answers.add(answer);
            }
        }
        return answers;
    }

    private PracticeRecord readRecord(Cursor cursor) {
        PracticeRecord record = new PracticeRecord();
        record.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        record.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        record.startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("started_at"));
        record.submittedAt = cursor.getLong(cursor.getColumnIndexOrThrow("submitted_at"));
        record.questionCount = cursor.getInt(cursor.getColumnIndexOrThrow("question_count"));
        record.correctCount = cursor.getInt(cursor.getColumnIndexOrThrow("correct_count"));
        record.score = cursor.getInt(cursor.getColumnIndexOrThrow("score"));
        record.durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("duration_seconds"));
        return record;
    }
}
