package com.example.myapplication.practice;

import com.example.myapplication.model.Question;
import java.util.List;
import java.util.Map;

public class ScoreCalculator {
    public static int correctCount(List<Question> questions, Map<Long, String> answers) {
        int count = 0;
        for (Question question : questions) {
            if (question.answer.equals(answers.get(question.id))) count++;
        }
        return count;
    }

    public static int score(List<Question> questions, Map<Long, String> answers) {
        return correctCount(questions, answers) * 10;
    }
}
