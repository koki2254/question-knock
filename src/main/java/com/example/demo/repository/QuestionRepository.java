package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    // 既存：タグと試験区分で検索（1つのタグ指定用）
    List<Question> findByTagIdAndExamCategory(Integer tagId, String examCategory);
    // 3つの項目で重複を確認するメソッドを追加
    boolean existsByYearAndSeasonAndQuestionNumber(Integer year, String season, Integer questionNumber);
    
    // 既存：カテゴリ＋時間帯＋複数のタグIDで検索（通常モード・複数選択用）
    List<Question> findByExamCategoryAndExamSessionAndTagIdIn(
            String examCategory, 
            String examSession, 
            List<Integer> tagIds
    );

    // ★★★ 今回追加（ランクマッチ用） ★★★
    // "基本" や "応用" などの区分だけで、全問題を検索するメソッド
    List<Question> findByExamCategory(String examCategory);
    
}