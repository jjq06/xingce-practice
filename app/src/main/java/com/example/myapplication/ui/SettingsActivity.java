package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.AppContract;
import com.example.myapplication.R;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        EditText count = findViewById(R.id.default_question_count);
        TextView status = findViewById(R.id.page_status);
        int saved = getSharedPreferences(AppContract.PREFS_NAME, MODE_PRIVATE)
                .getInt(AppContract.PREF_DEFAULT_QUESTION_COUNT, AppContract.DEFAULT_QUESTION_COUNT);
        count.setText(String.valueOf(saved));
        findViewById(R.id.save_settings).setOnClickListener(v -> {
            int parsed;
            try {
                parsed = Integer.parseInt(count.getText().toString().trim());
            } catch (NumberFormatException exception) {
                count.setError(getString(R.string.invalid_question_count));
                return;
            }
            if (parsed <= 0) {
                count.setError(getString(R.string.invalid_question_count));
                return;
            }
            getSharedPreferences(AppContract.PREFS_NAME, MODE_PRIVATE).edit()
                    .putInt(AppContract.PREF_DEFAULT_QUESTION_COUNT, parsed).apply();
            status.setText(R.string.settings_saved);
        });
    }
}
