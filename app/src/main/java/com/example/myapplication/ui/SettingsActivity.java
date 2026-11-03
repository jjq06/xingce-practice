package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.AppContract;
import com.example.myapplication.R;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.home_mint));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
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
