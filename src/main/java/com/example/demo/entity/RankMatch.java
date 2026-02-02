package com.example.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "rank_match_history") // DBにはこの名前でテーブルが作られます
@Data
public class RankMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private SiteUser user; // これで HTML の r.user が使えるようになります
    

    // 誰の記録か (UserテーブルのID)
    @Column(name = "user_id")
    private Long userId;

    // ★★★ 追加: 試験区分 (基本 or 応用) ★★★
    // これがないと、どっちの試験のランクか分からなくなってしまうため追加しました
    @Column(name = "exam_category")
    private String examCategory; 

    // 総合スコア（正解数ボーナス + タイムボーナス）
    private Integer score;

    // 判定されたランク (S, A, B...)
    @Column(name = "rank_grade")
    private String rankGrade;

    // 正解数
    @Column(name = "correct_count")
    private Integer correctCount;

    // かかった時間(秒)
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    // いつの記録か (保存時に自動で現在日時が入ります)
    @CreationTimestamp
    @Column(name = "played_at")
    private LocalDateTime playedAt;
}