package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.StudyTimeRecord;

public interface StudyTimeRepository extends JpaRepository<StudyTimeRecord, Long> {

    // ユーザーIDを指定して、分野ごとの合計秒数を集計する魔法のSQL
    // 結果は ["セキュリティ", 3600], ["ネットワーク", 120] のようなリストになります
    @Query("SELECT s.tagName, SUM(s.durationSeconds) FROM StudyTimeRecord s WHERE s.userId = :userId GROUP BY s.tagName")
    List<Object[]> sumDurationByUserId(@Param("userId") Long userId);
}