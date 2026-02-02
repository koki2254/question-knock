package com.example.demo.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Admin;
import com.example.demo.repository.AdminRepository;

// AdminRepositoryは後で作成します (JPAを想定)
@Service("adminDetailsService") // "adminDetailsService"という名前でBean登録
public class AdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository; // Adminエンティティ用のJPAリポジトリ

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // 1. emailを使ってDB(adminテーブル)を検索
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> 
                    new UsernameNotFoundException("管理者が見つかりません: " + email));

        // 2. Spring Securityが使うUserオブジェクトを返す
        // "ROLE_ADMIN" という権限を付与する
        return new User(
            admin.getEmail(),                 // ユーザー名 (email)
            admin.getPassword(),              // DBに保存されている「ハッシュ化済みパスワード」
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")) // 権限
        );
    }
}