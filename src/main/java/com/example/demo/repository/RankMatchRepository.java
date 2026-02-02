package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RankMatch;

@Repository
public interface RankMatchRepository extends JpaRepository<RankMatch, Long> {

    // ----------------------------------------------------------------
    // ★ 今回のメイン修正箇所 ★
    // ----------------------------------------------------------------

    // ① 自分自身のランクデータを探す（ユーザーID ＋ 試験区分）
    // ※「基本」のデータと「応用」のデータを区別して取得するために必要です
    Optional<RankMatch> findByUserIdAndExamCategory(Long userId, String examCategory);

    // ② ランキングTOP10を取得する（試験区分ごとに）
    // ※「基本だけのランキング」「応用だけのランキング」を出すために必要です
    // メソッド名が長いですが、これで「区分で絞って、スコアが高い順に10件」という意味になります
    List<RankMatch> findTop10ByExamCategoryOrderByScoreDesc(String examCategory);


    // ----------------------------------------------------------------
    // 以下は過去のコード（必要なければ削除してもOK）
    // ----------------------------------------------------------------
    
    // 履歴一覧用（区分関係なく全件出る）
    List<RankMatch> findByUserIdOrderByPlayedAtDesc(Long userId);

    // 古いランキング用（区分を無視してしまうので、もう使いません）
    // @Query(value = "SELECT * FROM rank_match_history ORDER BY score DESC LIMIT 10", nativeQuery = true)
    // List<RankMatch> findTop10Scores();
    
    // 古い検索用（区分を無視してしまうので、もう使いません）
    // Optional<RankMatch> findByUserId(Long userId);
}