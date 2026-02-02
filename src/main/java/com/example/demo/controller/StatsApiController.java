package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.TagStatsDTO;
import com.example.demo.entity.SiteUser;
import com.example.demo.repository.QuestionRecordRepository;
import com.example.demo.repository.UserRepository;

@RestController
public class StatsApiController {

    @Autowired
    private QuestionRecordRepository questionRecordRepository;
    
    @Autowired
    private UserRepository userRepository;

    // 分野別・試験区分別の正答率データを返すAPI
    @GetMapping("/api/stats/accuracy")
    public Map<String, Object> getAccuracyStats(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(value = "examType", defaultValue = "基本") String examType) {

        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("error", "Not logged in");
            return response;
        }

        String email = principal.getAttribute("email");
        SiteUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return response;
        }

        // 1. DBからタグごとの集計データを取得
        // ※ここが動くためには QuestionRecordRepository に findStatsByExamType が必要です
        List<TagStatsDTO> statsList = questionRecordRepository.findStatsByExamType(user.getId(), examType);

        // 2. グラフ用にリストを分解
        List<String> labels = new ArrayList<>(); // X軸（分野名）
        List<Double> data = new ArrayList<>();   // Y軸（正答率）

        for (TagStatsDTO stat : statsList) {
            labels.add(stat.getTagName());
            
            // 正答率計算 (0除算回避)
            if (stat.getTotal() > 0) {
                double rate = (double) stat.getCorrect() / stat.getTotal() * 100;
                data.add(Math.round(rate * 10.0) / 10.0); // 小数点第1位まで
            } else {
                data.add(0.0);
            }
        }

        response.put("labels", labels);
        response.put("data", data);
        response.put("examType", examType); // 確認用
        
        return response;
    }
}