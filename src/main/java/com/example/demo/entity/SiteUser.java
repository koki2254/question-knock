package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Googleの本名
    private String email;

    // ★追加: ニックネーム
    private String nickname;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT") // 長いデータが入るように指定
    private String iconBase64;
    
 // クラスの中に以下を追加
    @Column(length = 50)
    private String goal; // 目標テキスト

    
    public String getNickname() {
        if (nickname == null || nickname.isEmpty()) {
            return "匿名";
        }
        return nickname;
    }
}