package com.example.myapplication.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.Question;
import java.util.ArrayList;
import java.util.List;

public class WrongBookActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_wrong_book);
        findViewById(R.id.retry_wrong).setOnClickListener(v -> {
            Intent intent = new Intent(this, PracticeActivity.class);
            intent.putExtra(AppContract.EXTRA_PRACTICE_MODE, AppContract.MODE_WRONG_BOOK);
            startActivity(intent);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        new Thread(() -> {
            List<Long> ids = new PracticeRepository(this).wrongQuestionIds();
            QuestionRepository repository = new QuestionRepository(this);
            List<Question> questions = new ArrayList<>();
            for (Long id : ids) {
                Question question = repository.findById(id);
                if (question != null) questions.add(question);
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                LinearLayout list = findViewById(R.id.wrong_list);
                list.removeAllViews();
                TextView status = findViewById(R.id.page_status);
                status.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                status.setText(R.string.wrong_empty);
                findViewById(R.id.retry_wrong).setEnabled(!questions.isEmpty());
                for (Question question : questions) {
                    TextView row = new TextView(this);
                    row.setText(getString(R.string.wrong_entry, question.category, question.stem));
                    row.setTextSize(17);
                    row.setPadding(0, 16, 0, 16);
                    list.addView(row);
                }
            });
        }).start();
    }
}
