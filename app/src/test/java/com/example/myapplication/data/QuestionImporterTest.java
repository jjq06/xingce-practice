package com.example.myapplication.data;

import com.example.myapplication.model.Question;
import org.junit.Test;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class QuestionImporterTest {
    private static final String VALID = "常识判断\t题干，含逗号\t甲\t乙\t丙\t丁\tB\t解析";
    private QuestionImporter.ParseResult parse(String text) throws Exception {
        return new QuestionImporter().parse(new StringReader(text), "测试来源");
    }
    @Test public void acceptsBomWhitespaceLowercaseAndEmptyExplanation() throws Exception {
        QuestionImporter.ParseResult result = parse("\uFEFF" + VALID.replace("B\t解析", " b \t"));
        assertTrue(result.errors.isEmpty());
        assertEquals(1, result.questions.size());
        assertEquals("B", result.questions.get(0).answer);
        assertEquals("", result.questions.get(0).explanation);
        assertEquals("测试来源", result.questions.get(0).source);
    }
    @Test public void keepsValidRowsAndPhysicalLineNumbers() throws Exception {
        QuestionImporter.ParseResult result = parse("\n缺列\n" + VALID + "\r\n" + VALID.replace("B\t解析", "E\t解析"));
        assertEquals(1, result.questions.size());
        assertEquals(2, result.errors.size());
        assertTrue(result.errors.get(0).startsWith("第 2 行"));
        assertTrue(result.errors.get(1).startsWith("第 4 行"));
    }
    @Test public void reportsSpecificMissingFields() throws Exception {
        QuestionImporter.ParseResult result = parse(VALID.replace("题干，含逗号", " ").replace("甲", ""));
        assertTrue(result.questions.isEmpty());
        assertTrue(result.errors.get(0).contains("题干不能为空"));
        assertTrue(result.errors.get(0).contains("选项A不能为空"));
    }
    @Test public void rejectsMultipleAnswersAndExtraColumns() throws Exception {
        assertEquals(1, parse(VALID.replace("B\t解析", "AB\t解析")).errors.size());
        assertEquals(1, parse(VALID + "\t多列").errors.size());
    }
    @Test public void fingerprintIgnoresAnswerExplanationAndSource() throws Exception {
        Question a = parse(VALID).questions.get(0);
        Question b = parse(VALID.replace("B\t解析", "C\t新解析")).questions.get(0);
        assertEquals(a.fingerprint, b.fingerprint);
        assertEquals(64, a.fingerprint.length());
        assertNotEquals(a.fingerprint, parse(VALID.replace("常识判断", "数量关系")).questions.get(0).fingerprint);
    }
    @Test public void closesReaderAndHandlesEmptyInput() throws Exception {
        final boolean[] closed = {false};
        StringReader reader = new StringReader(VALID) {
            @Override public void close() { closed[0] = true; super.close(); }
        };
        new QuestionImporter().parse(reader, "test");
        assertTrue(closed[0]);
        assertTrue(parse("\n  \n").questions.isEmpty());
    }
    @Test public void strictUtf8AcceptsChineseAndRejectsBrokenBytes() throws Exception {
        assertEquals(1, new QuestionImporter().parseUtf8(
                new ByteArrayInputStream(VALID.getBytes(StandardCharsets.UTF_8)), "test").questions.size());
        assertThrows(java.nio.charset.CharacterCodingException.class, () -> new QuestionImporter().parseUtf8(
                new ByteArrayInputStream(new byte[]{(byte) 0xc3, (byte) 0x28}), "test"));
    }
}
