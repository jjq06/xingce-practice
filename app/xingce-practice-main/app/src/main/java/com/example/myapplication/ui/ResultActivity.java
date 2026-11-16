package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.AnswerDetail;
import com.example.myapplication.model.PracticeRecord;
import com.example.myapplication.model.Question;
import com.example.myapplication.practice.ScoreCalculator;

import java.util.List;

/**
 * 结果页：按练习记录 ID 回看成绩与逐题解析。
 *
 * <p>入参为 {@link AppContract#EXTRA_RECORD_ID}；提交后的答题页和历史记录页都用同一个键，
 * 旧结果因此可以随时重新打开。{@link #EXTRA_RECORD_ID} 是 {@code docs/interfaces.md}
 * 约定的向后兼容别名。</p>
 *
 * <p>成绩来自 {@code PracticeRepository.findById}，逐题解析来自
 * {@code PracticeRepository.findAnswers}。两个方法目前都是“已定签名、待实现”，
 * 本页对 {@link UnsupportedOperationException} 做了降级提示，数据层实现后无需再改本页。</p>
 */
public class ResultActivity extends AppCompatActivity {

    /** 向后兼容别名；新代码请使用 {@link AppContract#EXTRA_RECORD_ID}。 */
    public static final String EXTRA_RECORD_ID = AppContract.EXTRA_RECORD_ID;

    private TextView statusView;
    private TextView categoryView;
    private TextView scoreView;
    private TextView correctView;
    private TextView accuracyView;
    private TextView timeView;
    private TextView detailsTitleView;
    private LinearLayout summaryView;
    private LinearLayout detailsView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_result);
        bindViews();

        long recordId = getIntent().getLongExtra(AppContract.EXTRA_RECORD_ID, -1L);
        if (recordId <= 0L) {
            showMessage("没有收到练习记录 ID，无法显示本次成绩。");
            return;
        }

        PracticeRecord record = null;
        String error = null;
        try {
            record = new PracticeRepository(this).findById(recordId);
        } catch (UnsupportedOperationException e) {
            error = "练习记录查询尚未接入：PracticeRepository.findById() 仍是待实现状态，"
                    + "该文件由成员 A 负责，接入说明见根目录 division-b.md。";
        } catch (RuntimeException e) {
            error = "读取练习记录失败：" + e.getMessage();
        }

        if (record == null) {
            String message = "找不到练习记录（ID=" + recordId + "）。";
            if (!TextUtils.isEmpty(error)) {
                message = message + "\n" + error;
            }
            showMessage(message);
            return;
        }
        renderSummary(record);
        renderDetails(recordId);
    }

    private void bindViews() {
        statusView = findViewById(R.id.page_status);
        categoryView = findViewById(R.id.result_category);
        scoreView = findViewById(R.id.result_score);
        correctView = findViewById(R.id.result_correct);
        accuracyView = findViewById(R.id.result_accuracy);
        timeView = findViewById(R.id.result_time);
        detailsTitleView = findViewById(R.id.result_details_title);
        summaryView = findViewById(R.id.result_summary);
        detailsView = findViewById(R.id.result_details);
    }

    private void renderSummary(PracticeRecord record) {
        statusView.setVisibility(View.GONE);
        summaryView.setVisibility(View.VISIBLE);
        detailsTitleView.setVisibility(View.VISIBLE);
        detailsView.setVisibility(View.VISIBLE);

        int fullScore = ScoreCalculator.fullScore(record.questionCount);
        categoryView.setText(TextUtils.isEmpty(record.category) ? "练习记录" : record.category);
        scoreView.setText(record.score + " / " + fullScore + " 分");
        correctView.setText("答对 " + record.correctCount + " / " + record.questionCount + " 题");
        accuracyView.setText("正确率 "
                + ScoreCalculator.accuracyPercent(record.correctCount, record.questionCount) + "%");
        timeView.setText("用时 " + ScoreCalculator.formatDuration(record.durationSeconds));
    }

    /** 逐题解析：数据层就绪后自动生效，本页无需再改。 */
    private void renderDetails(long recordId) {
        detailsView.removeAllViews();

        List<AnswerDetail> details;
        try {
            details = new PracticeRepository(this).findAnswers(recordId);
        } catch (UnsupportedOperationException e) {
            addNote("逐题解析尚未接入：PracticeRepository.findAnswers(recordId) 仍是待实现状态（成员 A 负责）。");
            return;
        } catch (RuntimeException e) {
            addNote("读取作答明细失败：" + e.getMessage());
            return;
        }

        if (details == null || details.isEmpty()) {
            addNote("这条记录没有作答明细。");
            return;
        }

        QuestionRepository questions = new QuestionRepository(this);
        int index = 0;
        for (AnswerDetail detail : details) {
            index++;
            Question question = questions.findById(detail.questionId);
            detailsView.addView(buildDetailView(index, detail, question));
        }
    }

    private TextView buildDetailView(int index, AnswerDetail detail, Question question) {
        String userAnswer = detail.userAnswer;
        String correctAnswer = question == null ? "—" : question.answer;

        StringBuilder text = new StringBuilder();
        text.append(index).append(". ");
        if (question == null) {
            text.append("题目已不存在（ID=").append(detail.questionId).append("）");
        } else {
            text.append(question.stem);
        }
        text.append('\n').append("你的答案：")
                .append(TextUtils.isEmpty(userAnswer) ? "未作答" : userAnswer)
                .append("　正确答案：").append(correctAnswer);
        if (TextUtils.isEmpty(userAnswer)) {
            text.append("\n— 未作答，按 0 分计");
        } else if (detail.correct) {
            text.append("\n√ 回答正确");
        } else {
            text.append("\n× 回答错误");
        }
        if (question != null && !TextUtils.isEmpty(question.explanation)) {
            text.append("\n解析：").append(question.explanation);
        }

        String color;
        if (TextUtils.isEmpty(userAnswer)) {
            color = "#757575";
        } else if (detail.correct) {
            color = "#2E7D32";
        } else {
            color = "#C62828";
        }

        TextView view = new TextView(this);
        view.setText(text.toString());
        view.setTextSize(14f);
        view.setLineSpacing(0f, 1.25f);
        view.setTextColor(Color.parseColor(color));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        view.setLayoutParams(params);
        return view;
    }

    private void addNote(String message) {
        TextView note = new TextView(this);
        note.setText(message);
        note.setTextSize(14f);
        note.setLineSpacing(0f, 1.2f);
        note.setTextColor(Color.parseColor("#757575"));
        detailsView.addView(note);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        summaryView.setVisibility(View.GONE);
        detailsTitleView.setVisibility(View.GONE);
        detailsView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(message);
    }
}
