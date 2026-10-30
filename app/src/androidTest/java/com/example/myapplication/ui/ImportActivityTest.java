package com.example.myapplication.ui;

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabaseHelper;
import com.example.myapplication.data.QuestionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ImportActivityTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private File file(String suffix, byte[] bytes) throws Exception {
        File file = File.createTempFile("import-test-", suffix, context().getCacheDir());
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
        return file;
    }
    private String runImport(File file, boolean rotate) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        String[] message = {null};
        ImportViewModel[] model = {null};
        Observer<ImportViewModel.State> observer = state -> {
            if (!state.busy && (state.message.startsWith("文件：") || state.message.startsWith("导入失败"))) {
                message[0] = state.message; done.countDown();
            }
        };
        try (ActivityScenario<ImportActivity> scenario = ActivityScenario.launch(ImportActivity.class)) {
            scenario.onActivity(activity -> {
                model[0] = new ViewModelProvider(activity).get(ImportViewModel.class);
                model[0].state().observeForever(observer);
                model[0].importFile(Uri.fromFile(file));
            });
            if (rotate) scenario.recreate();
            assertTrue("导入超时", done.await(20, TimeUnit.SECONDS));
            onView(withId(R.id.import_status)).check(matches(withText(message[0])));
            onView(withId(R.id.pick_file)).check(matches(isEnabled()));
            scenario.onActivity(activity -> model[0].state().removeObserver(observer));
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (model[0] != null) model[0].state().removeObserver(observer);
            });
        }
        return message[0];
    }
    @Test public void partialImportDuplicatesAndRotation() throws Exception {
        String category = "导入界面测试-" + UUID.randomUUID();
        String row = category + "\t测试题\t甲\t乙\t丙\t丁\tA\t解释";
        File file = file(".txt", (row + "\n缺列\n" + row).getBytes(StandardCharsets.UTF_8));
        try {
            String message = runImport(file, true);
            assertTrue(message.contains("新增 1 题；重复跳过 1 题；格式错误 1 行"));
            assertTrue(message.contains("第 2 行"));
            assertEquals(1, new QuestionRepository(context()).count(category));
            assertTrue(runImport(file, false).contains("新增 0 题；重复跳过 2 题"));
        } finally {
            AppDatabaseHelper.getInstance(context()).getWritableDatabase().delete("questions", "category = ?", new String[]{category});
            file.delete();
        }
    }
    @Test public void rejectsInvalidEncodingWithoutWriting() throws Exception {
        int before = new QuestionRepository(context()).count();
        File file = file(".txt", new byte[]{(byte)0xc3, (byte)0x28});
        try {
            assertTrue(runImport(file, false).contains("不是有效的 UTF-8"));
            assertEquals(before, new QuestionRepository(context()).count());
        } finally { file.delete(); }
    }
    @Test public void rejectsOtherExtensionAndReportsEmptyFile() throws Exception {
        File wrong = file(".pdf", new byte[0]);
        File empty = file(".txt", new byte[0]);
        try {
            assertTrue(runImport(wrong, false).contains("仅支持 .txt"));
            assertTrue(runImport(empty, false).contains("文件为空"));
        } finally { wrong.delete(); empty.delete(); }
    }
}
