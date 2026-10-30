package com.example.myapplication.data;

import android.content.Context;
import com.example.myapplication.model.AnswerDetail;
import com.example.myapplication.model.PracticeRecord;
import java.util.List;

/** 练习记录的数据入口；后续在此集中实现事务保存和历史查询。 */
public class PracticeRepository {
    private final AppDatabaseHelper helper;

    public PracticeRepository(Context context) {
        helper = new AppDatabaseHelper(context);
    }

    public long save(PracticeRecord record, List<AnswerDetail> answers) {
        throw new UnsupportedOperationException("练习记录持久化待实现");
    }

    public PracticeRecord findById(long recordId) {
        throw new UnsupportedOperationException("练习记录查询待实现");
    }

    public List<PracticeRecord> history() {
        throw new UnsupportedOperationException("历史记录查询待实现");
    }

    public List<Long> wrongQuestionIds() {
        throw new UnsupportedOperationException("错题查询待实现");
    }

    public List<AnswerDetail> findAnswers(long recordId) {
        throw new UnsupportedOperationException("练习答案查询待实现");
    }
}
