package com.example.myapplication.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;

public class ImportActivity extends AppCompatActivity {
    private ImportViewModel model;
    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) model.importFile(result.getData().getData());
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_import);
        View root = findViewById(R.id.import_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        TextView status = findViewById(R.id.import_status);
        View button = findViewById(R.id.pick_file);
        model = new ViewModelProvider(this).get(ImportViewModel.class);
        model.state().observe(this, value -> {
            status.setText(value.message);
            button.setEnabled(!value.busy);
            findViewById(R.id.import_progress).setVisibility(value.busy ? View.VISIBLE : View.GONE);
        });
        button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            // 部分文件管理器把 TXT 标为 application/octet-stream，选择后按文件名和编码校验。
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                picker.launch(intent);
            } catch (ActivityNotFoundException e) {
                status.setText("未找到系统文件选择器，请启用文件管理应用后重试。");
            }
        });
    }
}
