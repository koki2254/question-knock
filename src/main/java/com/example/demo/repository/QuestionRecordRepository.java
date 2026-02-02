package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.TagStatsDTO;
import com.example.demo.entity.QuestionRecord;

@Repository
public interface QuestionRecordRepository extends JpaRepository<QuestionRecord, Long> {
    
    // 特定ユーザーの正解数をカウント
    long countByUserIdAndIsCorrectTrue(Long userId);

    // 特定ユーザーの全回答数をカウント
    long countByUserId(Long userId);
    
    // ★修正: 生のSQL (nativeQuery = true) に戻して、カラム名を修正
    @Query(value = "SELECT " +
            "t.tag_name AS tagName, " +        // DBのカラム名 tag_name
            "COUNT(qr.id) AS total, " +        // 全件数
            "SUM(CASE WHEN qr.is_correct = 1 THEN 1 ELSE 0 END) AS correct " + // 正解数
            "FROM questionrecord qr " +        // テーブル名 (小文字)
            "JOIN question q ON qr.question_id = q.question_id " + // ID同士で結合
            "JOIN tag t ON q.tag_id = t.tag_id " +                 // ID同士で結合
            "WHERE qr.user_id = :userId " +
            "AND q.exam_category = :examType " + // ★ここ！ exam_type を exam_category に変更
            "GROUP BY t.tag_name", nativeQuery = true)
     List<TagStatsDTO> findStatsByExamType(@Param("userId") Long userId, @Param("examType") String examType);
}