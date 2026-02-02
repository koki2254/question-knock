package com.example.demo.dto;

public interface TagStatsDTO {
    String getTagName(); // 分野名
    Long getTotal();     // 解いた総数
    Long getCorrect();   // 正解数
}