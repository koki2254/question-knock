package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // 問題IDを「1,5,23,10...」のようなカンマ区切り文字列で保存
    @Column(columnDefinition = "TEXT")
    private String questionIds;

    // 現在何問目か（インデックス）
    private int currentIndex;

    private String examType;
    private String tagName;
    
    private LocalDateTime updatedAt;
    
    private int correctCount; // ★追加：現在の正解数を保存
}