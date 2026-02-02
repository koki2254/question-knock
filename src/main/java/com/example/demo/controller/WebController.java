package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entity.SiteUser;
import com.example.demo.entity.Tag;
import com.example.demo.repository.TagRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GeminiService;

@Controller
public class WebController {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GeminiService geminiService;
    
 // ★追加: ユーザー情報を取得するために必要
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.example.demo.repository.StudyTimeRepository studyTimeRepository;

 
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }


    // 2. コース選択後に呼ばれる画面
    @GetMapping("/fields")
    public String showExamOptions(@RequestParam("exam") String exam, Model model) {
        List<Tag> tags = tagRepository.findAll();
        
        model.addAttribute("exam", exam);
        model.addAttribute("fields", tags);
        
        return "exam-options";
    }

    // AI先生
    @PostMapping("/api/ai/ask")
    @ResponseBody
    public Map<String, String> askAi(@RequestBody Map<String, String> request) {
        
        String questionText = request.get("questionText");
        String userMessage = request.get("userMessage");
        String imageBase64 = request.get("imageBase64"); 
        String mimeType = request.get("mimeType");

        String aiResponse = geminiService.askGemini(questionText, userMessage, imageBase64, mimeType);

        return Map.of("response", aiResponse);
    }
    
    @GetMapping("/test")
    @ResponseBody
    public String testConnection() {
        return "つながってるよ！変更は反映されています。";
    }
    
    @GetMapping("/mypage")
    public String showMyPage(@AuthenticationPrincipal OAuth2User principal, Model model) {
        
        // ... (既存のユーザー名取得ロジック) ...
        String displayName = "ゲスト";
        SiteUser user = null;
        if (principal != null) {
            // ... (ユーザー取得して user に入れる) ...
            String email = principal.getAttribute("email");
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getNickname() != null) displayName = user.getNickname();
            else displayName = principal.getAttribute("name");
        }
        model.addAttribute("userName", displayName);


        // ★追加: 学習時間の集計データを取得して渡す
        if (user != null) {
            // [ ["セキュリティ", 3600], ["ネットワーク", 120] ] という生データが取れる
            List<Object[]> rawList = studyTimeRepository.sumDurationByUserId(user.getId());
            
            // 画面で表示しやすい形（Mapなど）に変換してもいいですが、
            // 今回はThymeleaf側で計算できるので、そのまま渡します
            model.addAttribute("studyTimeList", rawList);
        }

        return "mypage";
    }
}