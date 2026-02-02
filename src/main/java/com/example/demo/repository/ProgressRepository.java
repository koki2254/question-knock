package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Progress;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    
    /**
     * ユーザーIDに基づいて中断データを取得します。
     * @param userId サイトユーザーのID
     * @return 中断データ（存在しない場合は空のOptional）
     */
    Optional<Progress> findByUserId(Long userId);
    
    /**
     * 必要に応じて：特定の試験区分（examType）の中断データを取得したい場合
     */
    Optional<Progress> findByUserIdAndExamType(Long userId, String examType);
}