package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    // 1. ユーザーIDでブックマーク一覧を取得（作成日時の降順）
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 2. ユーザーIDと問題IDで検索（登録済みチェック用）
    Optional<Bookmark> findByUserIdAndQuestionId(Long userId, Long questionId);

    // 3. ユーザーIDと問題IDを指定して削除（トグル解除用で使用する場合）
    void deleteByUserIdAndQuestionId(Long userId, Long questionId);
    
    // 4. 特定ユーザーのブックマークを全削除
    void deleteByUserId(Long userId);
}