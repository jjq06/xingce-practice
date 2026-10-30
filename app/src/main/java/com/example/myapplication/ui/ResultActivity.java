package com.example.myapplication.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.AppContract;

public class ResultActivity extends AppCompatActivity {
    public static final String EXTRA_RECORD_ID = AppContract.EXTRA_RECORD_ID;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_result);
    }
}
