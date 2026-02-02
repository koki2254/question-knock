package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "study_time_records")
@Data
public class StudyTimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;        // 誰が
    private String tagName;     // どの分野を (例: "セキュリティ")
    private Integer durationSeconds; // 何秒勉強したか
    private LocalDateTime playedAt;  // いつの記録か
}