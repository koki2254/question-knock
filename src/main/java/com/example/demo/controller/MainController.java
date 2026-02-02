package com.example.demo.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.RankMatch;
import com.example.demo.entity.SiteUser;
import com.example.demo.repository.ProgressRepository;
import com.example.demo.repository.RankMatchRepository;
import com.example.demo.repository.UserRepository;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RankMatchRepository rankMatchRepository;
    
    @Autowired
    private ProgressRepository progressRepository; // ★追加：進捗リポジトリを注入

 // ==========================================
    //  ルートパス ("/") へのアクセスで top.html を表示
    // ==========================================
    @GetMapping("/")
    public String top() {
        return "top"; // templates/top.html を返す
    }
    
    // ==========================================
    //  ★修正: 練習問題のコース選択画面を表示する
    // ==========================================
    @GetMapping("/select-course")
    public String selectCourse(Model model, @AuthenticationPrincipal OAuth2User principal) {
        
        if (principal != null) {
            SiteUser user = getSiteUser(principal); // 既存のヘルパーメソッドを使用
            model.addAttribute("user", user);

            // ★追加：このユーザーの進捗データが存在するかチェック
            boolean hasProgress = progressRepository.findByUserId(user.getId()).isPresent();
            model.addAttribute("hasProgress", hasProgress);
        }

        return "course_select"; 
    }
    

    // ② ホーム画面
    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            SiteUser user = getSiteUser(principal);
            if (user != null) {
                model.addAttribute("user", user);

                // DBに保存されているランクを表示 (なければ"-")
                model.addAttribute("basicRank", getRank(user, "基本"));
                model.addAttribute("appliedRank", getRank(user, "応用"));
                
                // 目標 (なければデフォルト文言)
                String currentGoal = (user.getGoal() != null && !user.getGoal().isEmpty()) 
                                     ? user.getGoal() : "目標を設定しよう！";
                model.addAttribute("userGoal", currentGoal);
            }
        }
        return "home"; 
    }

    // ③ ランク＆ランキング詳細閲覧画面
    @GetMapping("/rank/view")
    public String viewRankTier(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return "redirect:/login";

        SiteUser user = getSiteUser(principal);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        // ランク情報の取得
        model.addAttribute("basicRank", getRank(user, "基本"));
        model.addAttribute("appliedRank", getRank(user, "応用"));

        // ランキングTOP10を取得
        List<RankMatch> basicRanking = rankMatchRepository.findTop10ByExamCategoryOrderByScoreDesc("基本");
        List<RankMatch> appliedRanking = rankMatchRepository.findTop10ByExamCategoryOrderByScoreDesc("応用");

        model.addAttribute("basicRanking", basicRanking);
        model.addAttribute("appliedRanking", appliedRanking);
        
        return "rank_tier";
    }

    // ④ プロフィール画像・ニックネーム更新処理
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("nickname") String nickname,
            @RequestParam("iconFile") MultipartFile iconFile,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal != null) {
            SiteUser user = getSiteUser(principal);
            if (user != null) {
                user.setNickname((nickname == null || nickname.trim().isEmpty()) ? "匿名" : nickname);

                if (iconFile != null && !iconFile.isEmpty()) {
                    try {
                        String base64 = Base64.getEncoder().encodeToString(iconFile.getBytes());
                        String mimeType = iconFile.getContentType();
                        user.setIconBase64("data:" + mimeType + ";base64," + base64);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                userRepository.save(user);
            }
        }
        return "redirect:/home";
    }

    // ⑤ 目標(Goal)だけの直接更新処理
    @PostMapping("/profile/goal")
    public String updateGoal(
            @RequestParam("goal") String goal,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal != null) {
            SiteUser user = getSiteUser(principal);
            if (user != null) {
                user.setGoal(goal);
                userRepository.save(user);
            }
        }
        return "redirect:/home";
    }

    // --- ヘルパーメソッド ---

    private SiteUser getSiteUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseGet(() -> {
            SiteUser newUser = new SiteUser();
            newUser.setEmail(email);
            newUser.setName(principal.getAttribute("name"));
            newUser.setNickname("匿名");
            return userRepository.save(newUser);
        });
    }

    private String getRank(SiteUser user, String type) {
        Optional<RankMatch> opt = rankMatchRepository.findByUserIdAndExamCategory(user.getId(), type);
        return opt.map(RankMatch::getRankGrade).orElse("E");
    }
}