package com.example.demo.repository; // パッケージ名はご自身の環境に合わせてください

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {

    // emailを条件にAdminエンティティを検索するメソッド
    // AdminDetailsServiceの loadUserByUsername で使用する
    Optional<Admin> findByEmail(String email);
}