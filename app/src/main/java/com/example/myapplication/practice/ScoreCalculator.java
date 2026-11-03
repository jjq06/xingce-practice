package com.example.myapplication.practice;

import com.example.myapplication.model.Question;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 练习评分规则（企划案 6.1）：只有四选一单选题，每题答对 10 分，答错或未作答 0 分，
 * 满分是“本次题数 × 10”。结果页同时给出答对数、正确率与满分，避免不同题量的得分
 * 被误当成同一百分制。
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    /** 单题判分：答案为空或非法一律算错，不抛异常。 */
    public static boolean isCorrect(Question question, String userAnswer) {
        if (question == null || question.answer == null || userAnswer == null) {
            return false;
        }
        return question.answer.trim().equalsIgnoreCase(userAnswer.trim());
    }

    public static int correctCount(List<Question> questions, Map<Long, String> answers) {
        if (questions == null) {
            return 0;
        }
        int count = 0;
        for (Question question : questions) {
            String userAnswer = answers == null ? null : answers.get(question.id);
            if (isCorrect(question, userAnswer)) {
                count++;
            }
        }
        return count;
    }

    public static int score(List<Question> questions, Map<Long, String> answers) {
        return scoreOf(correctCount(questions, answers));
    }

    public static int scoreOf(int correctCount) {
        return Math.max(0, correctCount) * PracticeSession.POINTS_PER_QUESTION;
    }

    public static int fullScore(int questionCount) {
        return Math.max(0, questionCount) * PracticeSession.POINTS_PER_QUESTION;
    }

    /** 正确率百分比，四舍五入到整数；题数为 0 时返回 0。 */
    public static int accuracyPercent(int correctCount, int questionCount) {
        if (questionCount <= 0) {
            return 0;
        }
        return (int) Math.round(Math.max(0, correctCount) * 100.0 / questionCount);
    }

    public static long durationSeconds(long startedAt, long submittedAt) {
        return Math.max(0L, (submittedAt - startedAt) / 1000L);
    }

    /** 把秒数格式化成 mm:ss（超过 1 小时为 h:mm:ss），供练习页与结果页共用。 */
    public static String formatDuration(long seconds) {
        long total = Math.max(0L, seconds);
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long secs = total % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }
}
