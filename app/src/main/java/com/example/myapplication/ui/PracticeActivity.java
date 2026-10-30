package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.AppContract;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.Question;
import java.util.List;

public class PracticeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_practice);
        String category = getIntent().getStringExtra(AppContract.EXTRA_CATEGORY);
        int count = getSharedPreferences(AppContract.PREFS_NAME, MODE_PRIVATE)
                .getInt(AppContract.PREF_DEFAULT_QUESTION_COUNT, AppContract.DEFAULT_QUESTION_COUNT);
        List<Question> questions = new QuestionRepository(this).findByCategory(category, count);
        TextView status = findViewById(R.id.practice_status);
        if (questions.isEmpty()) {
            status.setText("题库为空，请先导入题目。\n练习作答与提交页面待接入。");
        } else {
            status.setText("题库已选出 " + questions.size() + " 题。\n练习作答与提交页面待接入。");
        }
    }
}
