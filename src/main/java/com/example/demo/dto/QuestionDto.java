package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuestionDto {
    private Integer questionId; // LongではなくIntegerにしておく（エラー回避）
    private String questionText;
    private String imageUrl;
    private String explanation;
    private List<ChoiceDto> choices;

    @Data
    public static class ChoiceDto {
        private Integer choiceId;
        private String choiceText;
        private boolean isCorrect;
    }
}