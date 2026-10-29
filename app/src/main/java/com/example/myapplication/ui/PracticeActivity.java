package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.Question;
import java.util.List;

public class PracticeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_practice);
        List<Question> questions = new QuestionRepository(this).findByCategory(null, 10);
        TextView status = findViewById(R.id.practice_status);
        if (questions.isEmpty()) {
            status.setText("题库为空，请先导入题目。\n练习作答与提交页面待接入。");
        } else {
            status.setText("题库已选出 " + questions.size() + " 题。\n练习作答与提交页面待接入。");
        }
    }
}
