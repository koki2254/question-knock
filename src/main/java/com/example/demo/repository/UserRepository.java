package com.example.demo.repository;

import java.util.Optional; // ← これが必要

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.SiteUser;

@Repository
public interface UserRepository extends JpaRepository<SiteUser, Long> {
    
    // ↓ この1行を追加してください！
    Optional<SiteUser> findByEmail(String email);
}