package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

// 必要なDTO, Entity, Repository, Serviceをインポート
import com.example.demo.dto.QuestionDto;
import com.example.demo.entity.Bookmark;
import com.example.demo.entity.Progress;
import com.example.demo.entity.Question;
import com.example.demo.entity.QuestionChoice;
import com.example.demo.entity.QuestionRecord;
import com.example.demo.entity.RankMatch;
import com.example.demo.entity.SiteUser;
import com.example.demo.entity.StudyTimeRecord;
import com.example.demo.repository.BookmarkRepository;
import com.example.demo.repository.ProgressRepository;
import com.example.demo.repository.QuestionRecordRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.RankMatchRepository;
import com.example.demo.repository.StudyTimeRepository;
import com.example.demo.repository.TagRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.QuestionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/challenge")
@RequiredArgsConstructor // finalフィールドのコンストラクタを自動生成
public class ChallengeController {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionRecordRepository questionRecordRepository;
    private final RankMatchRepository rankMatchRepository;
    private final StudyTimeRepository studyTimeRepository;
    private final TagRepository tagRepository;
    private final BookmarkRepository bookmarkRepository; // 追加
    private final QuestionService questionService;       // 追加
    private final ProgressRepository progressRepository;

    // ==========================================
    //   ★ 新規追加: 苦手対策（ブックマーク問題）クイズ画面 (GET)
    //   パス: /challenge/bookmark
    // ==========================================
    @GetMapping("/bookmark")
    public String showBookmarkChallenge(@AuthenticationPrincipal Object principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login";

        // サービス経由でブックマーク問題を取得（DTO変換済み）
        List<QuestionDto> bookmarkedQuestions = questionService.findBookmarkedQuestionsByUser(email);

        model.addAttribute("questions", bookmarkedQuestions);
        return "bookmark_challenge";
    }

    // ==========================================
    //   ★ 新規追加: ブックマーク一覧リスト画面 (GET)
    //   パス: /challenge/bookmarks
    // ==========================================
    @GetMapping("/bookmarks")
    public String showBookmarks(Model model, @AuthenticationPrincipal Object principal) {
        if (principal == null) return "redirect:/";
        
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/";
        
        SiteUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return "redirect:/";

        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<Integer> questionIds = bookmarks.stream()
                .map(b -> b.getQuestionId().intValue())
                .collect(Collectors.toList());

        List<Question> bookmarkedQuestions = questionRepository.findAllById(questionIds);
        
        model.addAttribute("bookmarkedQuestions", bookmarkedQuestions);

        return "bookmark_list";
    }

    // ==========================================
    //   ★ 新規追加: 選択したブックマーク問題でスタート (POST)
    //   パス: /challenge/start_selected
    // ==========================================
    @PostMapping("/start_selected")
    public String startSelectedQuestions(
            @RequestParam(name = "selectedQuestionIds", required = false) List<Integer> ids,
            HttpSession session,
            Model model) {

        if (ids == null || ids.isEmpty()) {
            return "redirect:/challenge/bookmarks";
        }

        List<Question> questions = questionRepository.findAllById(ids);
        Collections.shuffle(questions);
        
        session.setAttribute("challengeQuestions", questions);
        session.setAttribute("currentTagName", "苦手克服");
        session.setAttribute("normalModeStartTime", System.currentTimeMillis());

        for (Question q : questions) {
            if (q.getChoices() != null) Collections.shuffle(q.getChoices());
        }

        List<String> labels = List.of("ア", "イ", "ウ", "エ");
        model.addAttribute("labels", labels);
        model.addAttribute("questions", questions);

        return "challenge";
    }

    // ==========================================
    //   ① ランクマッチのメニュー画面 (GET)
    // ==========================================
    @GetMapping("/rank/menu")
    public String showRankMenu(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name);
        }
        return "rank_menu";
    }

    // ==========================================
    //   ② ランクマッチ開始処理 (GET)
    // ==========================================
    @GetMapping("/rank/start")
    public String startRankMatch(
            @RequestParam(name = "examType", defaultValue = "基本") String examType,
            HttpSession session, 
            Model model) {
        
        session.setAttribute("currentRankExamType", examType);

        List<Question> allQuestions = questionRepository.findByExamCategory(examType);
        Collections.shuffle(allQuestions);
        
        List<Question> selectedQuestions = allQuestions.stream()
            .limit(100)
            .collect(Collectors.toList());

        for (Question q : selectedQuestions) {
            if (q.getChoices() != null) {
                Collections.shuffle(q.getChoices());
            }
        }

        session.setAttribute("challengeQuestions", selectedQuestions);

        model.addAttribute("questions", selectedQuestions);
        model.addAttribute("examType", examType);
        List<String> labels = List.of("ア", "イ", "ウ", "エ");
        model.addAttribute("labels", labels);
        
        return "rank_play";
    }

    // ==========================================
    //   ③ ランクマッチ結果判定 (POST)
    // ==========================================
    @PostMapping("/rank/submit")
    public String submitRankMatch(
            @RequestParam MultiValueMap<String, String> formData,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(value = "startTime", required = false) Long startTimeStr,
            HttpSession session,
            Model model) {

        if (principal == null) return "redirect:/";
        String email = principal.getAttribute("email");
        
        SiteUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            SiteUser newUser = new SiteUser();
            newUser.setEmail(email);
            newUser.setName(principal.getAttribute("name"));
            newUser.setNickname("匿名"); 
            user = userRepository.save(newUser);
        }

        List<Question> questions = (List<Question>) session.getAttribute("challengeQuestions");
        if (questions == null) return "redirect:/challenge/rank/menu"; 

        int correctCount = 0;
        for (Question q : questions) {
            String ansStr = formData.getFirst("question_" + q.getQuestionId());
            if (ansStr != null && !ansStr.isEmpty()) {
                Integer ansId = Integer.parseInt(ansStr);
                boolean isCorrect = q.getChoices().stream()
                    .anyMatch(c -> c.getChoiceId().equals(ansId) && c.isCorrect());
                if (isCorrect) correctCount++;
            }
        }

        int baseScore = correctCount * 50; // 点数調整
        long timeLimit = 3600; 
        long durationSeconds = timeLimit;
        int rawTimeBonus = 0;
        
        if (startTimeStr != null) {
            long now = System.currentTimeMillis();
            long diffMillis = now - startTimeStr;
            durationSeconds = diffMillis / 1000;
            if (durationSeconds < timeLimit) {
                rawTimeBonus = (int) (timeLimit - durationSeconds); 
            }
        }
        
        double accuracyRate = 0.0;
        if (!questions.isEmpty()) {
            accuracyRate = (double) correctCount / questions.size();
        }

        int finalTimeBonus = (int) (rawTimeBonus * accuracyRate);
        int totalScore = baseScore + finalTimeBonus;

        String rankGrade = "E"; 
        if (totalScore >= 4500) rankGrade = "S";
        else if (totalScore >= 3500) rankGrade = "A"; 
        else if (totalScore >= 2500) rankGrade = "B"; 
        else if (totalScore >= 1500) rankGrade = "C"; 
        else if (totalScore >= 500) rankGrade = "D"; 

        String examType = (String) session.getAttribute("currentRankExamType");
        if (examType == null) examType = "基本";

        java.util.Optional<RankMatch> existingRecordOpt = 
                rankMatchRepository.findByUserIdAndExamCategory(user.getId(), examType);

        if (existingRecordOpt.isPresent()) {
            RankMatch existingRecord = existingRecordOpt.get();
            if (totalScore > existingRecord.getScore()) {
                existingRecord.setScore(totalScore);
                existingRecord.setRankGrade(rankGrade);
                existingRecord.setCorrectCount(correctCount);
                existingRecord.setTimeTakenSeconds((int) durationSeconds);
                existingRecord.setPlayedAt(LocalDateTime.now());
                rankMatchRepository.save(existingRecord); 
            }
        } else {
            RankMatch newRecord = new RankMatch();
            newRecord.setUserId(user.getId());
            newRecord.setExamCategory(examType);
            newRecord.setScore(totalScore);
            newRecord.setRankGrade(rankGrade);
            newRecord.setCorrectCount(correctCount);
            newRecord.setTimeTakenSeconds((int) durationSeconds);
            rankMatchRepository.save(newRecord);
        }

        List<RankMatch> topRanking = rankMatchRepository.findTop10ByExamCategoryOrderByScoreDesc(examType);

        model.addAttribute("myScore", totalScore);
        model.addAttribute("myRank", rankGrade);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("ranking", topRanking);

        session.removeAttribute("challengeQuestions");

        return "rank_result";
    }

    // ==========================================
    //   ④ 通常モード：問題スタート画面 (GET)
    // ==========================================
 // ChallengeController.java 232行目付近の startChallenge メソッド

    @GetMapping("/start")
    public String startChallenge(
            @RequestParam("tagIds") List<Integer> tagIds,
            @RequestParam("examType") String examType, 
            @RequestParam("examSession") String examSession,
            @RequestParam(name = "limit", defaultValue = "100") int limit, // ★追加
            Model model,
            HttpSession session) {

        String tagName = "複合学習";
        if (tagIds.size() == 1) {
            tagName = tagRepository.findById(tagIds.get(0))
                    .map(t -> t.getTagName())
                    .orElse("複合学習");
        }
        
        session.setAttribute("currentTagName", tagName);
        session.setAttribute("normalModeStartTime", System.currentTimeMillis());

        // 1. まず該当する問題を全件取得
        List<Question> allQuestions = questionRepository.findByExamCategoryAndExamSessionAndTagIdIn(
                examType, examSession, tagIds);
        
        // 2. シャッフルする
        Collections.shuffle(allQuestions);

        // 3. ★指定された数で制限をかける
        List<Question> limitedQuestions = allQuestions.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 4. セッションとモデルには制限後のリストを渡す
        session.setAttribute("challengeQuestions", limitedQuestions);

        for (Question q : limitedQuestions) {
            if (q.getChoices() != null) {
                Collections.shuffle(q.getChoices());
            }
        }

        List<String> labels = List.of("ア", "イ", "ウ", "エ");
        model.addAttribute("labels", labels);
        model.addAttribute("questions", limitedQuestions); // ★変更

        return "challenge";
    }
    
    // ==========================================
    //   ⑤ 通常モード：採点処理 (POST)
    // ==========================================
    @PostMapping("/submit")
    public String submitChallenge(
            @RequestParam MultiValueMap<String, String> formData,
            @AuthenticationPrincipal OAuth2User principal,
            HttpSession session,
            Model model) {

        List<Question> correctQuestions = 
                (List<Question>) session.getAttribute("challengeQuestions");

        if (correctQuestions == null) return "redirect:/";

        SiteUser user = null;
        if (principal != null) {
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");
            user = userRepository.findByEmail(email).orElseGet(() -> {
                SiteUser newUser = new SiteUser();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setNickname("匿名");
                return userRepository.save(newUser);
            });
        }

        Long startTime = (Long) session.getAttribute("normalModeStartTime");
        String tagName = (String) session.getAttribute("currentTagName");

        if (user != null && startTime != null && tagName != null) {
            long now = System.currentTimeMillis();
            long diffSeconds = (now - startTime) / 1000; 

            StudyTimeRecord timeRecord = new StudyTimeRecord();
            timeRecord.setUserId(user.getId());
            timeRecord.setTagName(tagName);
            timeRecord.setDurationSeconds((int) diffSeconds);
            timeRecord.setPlayedAt(LocalDateTime.now());
            studyTimeRepository.save(timeRecord);
        }
        
        session.removeAttribute("normalModeStartTime");
        session.removeAttribute("currentTagName");

     // セッションから以前の正解数を取得（再開時以外は null なので 0 にする）
     Integer savedCorrectCount = (Integer) session.getAttribute("correctCount");
     int score = (savedCorrectCount != null) ? savedCorrectCount : 0;

     // 使い終わったセッション情報は削除
     session.removeAttribute("correctCount");

     for (Question correctQ : correctQuestions) {
         String userAnswerChoiceIdStr = formData.getFirst("question_" + correctQ.getQuestionId());
         
         // 【重要】今回の回答データがない問題（中断前の既回答分）はスキップする
         // これをしないと、過去の回答分が「不正解」として上書き記録されてしまいます
         if (userAnswerChoiceIdStr == null || userAnswerChoiceIdStr.isEmpty()) {
             continue;
         }

         Integer userAnswerChoiceId = Integer.parseInt(userAnswerChoiceIdStr);
         boolean isCorrect = false;

         for (QuestionChoice choice : correctQ.getChoices()) {
             if (choice.getChoiceId().equals(userAnswerChoiceId) && choice.isCorrect()) {
                 isCorrect = true;
                 score++; // 保存されていた正解数にプラスされる
                 break;
             }
         }

            if (user != null) {
                QuestionRecord record = new QuestionRecord();
                record.setUserId(user.getId());
                record.setQuestionId(correctQ.getQuestionId().longValue());
                record.setCorrect(isCorrect);
                record.setAnsweredAt(LocalDateTime.now());
                questionRecordRepository.save(record);
            }
        }

        double percentage = 0;
        if (correctQuestions.size() > 0) {
            percentage = (double) score / correctQuestions.size() * 100;
        }

        model.addAttribute("score", score);
        model.addAttribute("total", correctQuestions.size());
        model.addAttribute("percentage", (int) percentage);

        session.removeAttribute("challengeQuestions");
        
        // 【追加】解き終わったら進捗データを削除
        if (user != null) {
            progressRepository.findByUserId(user.getId()).ifPresent(progressRepository::delete);
        }

        return "result";
    }
    
    // ==========================================
    //   ⑥ ランク階級表示 (ランク＆ランキング閲覧画面)
    // ==========================================
    @GetMapping("/rank/view")
    public String viewRankTier(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return "redirect:/";

        String email = principal.getAttribute("email");
        SiteUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new SiteUser();
            user.setEmail(email);
            user.setName(principal.getAttribute("name"));
            user.setNickname("匿名");
            user = userRepository.save(user);
        }

        rankMatchRepository.findByUserIdAndExamCategory(user.getId(), "基本")
            .ifPresent(r -> model.addAttribute("basicRank", r.getRankGrade()));
            
        rankMatchRepository.findByUserIdAndExamCategory(user.getId(), "応用")
            .ifPresent(r -> model.addAttribute("appliedRank", r.getRankGrade()));

        if (!model.containsAttribute("basicRank")) model.addAttribute("basicRank", "E");
        if (!model.containsAttribute("appliedRank")) model.addAttribute("appliedRank", "E");

        List<RankMatch> basicRanking = rankMatchRepository.findTop10ByExamCategoryOrderByScoreDesc("基本");
        List<RankMatch> appliedRanking = rankMatchRepository.findTop10ByExamCategoryOrderByScoreDesc("応用");

        model.addAttribute("basicRanking", basicRanking);
        model.addAttribute("appliedRanking", appliedRanking);
        model.addAttribute("userName", user.getNickname() != null ? user.getNickname() : user.getName());

        return "rank_tier"; 
    }

    // ヘルパーメソッド: Principalからメールアドレスを取得
    private String getEmailFromPrincipal(Object principal) {
        if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttribute("email");
        } else if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return null;
    }
    
	// ==========================================
	//  ★ 追加: 進捗の一時保存 (POST / API)
	//==========================================
	@PostMapping("/save_progress")
	@ResponseBody
	public String saveProgress(
	       @RequestParam int currentIndex,
	       @RequestParam int correctCount, // ★JavaScriptから正解数を受け取る
	       @AuthenticationPrincipal Object principal,
	       HttpSession session) {
	
	   String email = getEmailFromPrincipal(principal);
	   SiteUser user = userRepository.findByEmail(email).orElse(null);
	   if (user == null) return "error";
	
	   List<Question> questions = (List<Question>) session.getAttribute("challengeQuestions");
	   String tagName = (String) session.getAttribute("currentTagName");
	
	   // 問題IDリストを文字列に変換
	   String ids = questions.stream()
	           .map(q -> String.valueOf(q.getQuestionId()))
	           .collect(Collectors.joining(","));
	
	   // 既存の進捗があれば更新、なければ新規作成
	   Progress progress = progressRepository.findByUserId(user.getId())
	           .orElse(new Progress());
	   
	   progress.setUserId(user.getId());
	   progress.setQuestionIds(ids);
	   progress.setCurrentIndex(currentIndex);
	   progress.setTagName(tagName);
	   progress.setUpdatedAt(LocalDateTime.now());
	   progress.setCorrectCount(correctCount); // ★正解数を保存
	   
	   progressRepository.save(progress);
	   return "success";
	}
	
	//==========================================
	//  ★ 追加: 中断したところから再開 (GET)
	//==========================================
	@GetMapping("/resume")
	public String resumeChallenge(@AuthenticationPrincipal Object principal, HttpSession session, Model model) {
	   String email = getEmailFromPrincipal(principal);
	   SiteUser user = userRepository.findByEmail(email).orElse(null);
	   
	   Progress progress = progressRepository.findByUserId(user.getId()).orElse(null);
	   if (progress == null) return "redirect:/";
	
	   // 保存されたID文字列をList<Integer>に戻す
	   List<Integer> idList = Arrays.stream(progress.getQuestionIds().split(","))
	           .map(Integer::parseInt)
	           .collect(Collectors.toList());
	
	   // データベースから取得（IDの順番を維持するため、取得後に並べ替えが必要）
	   List<Question> questions = questionRepository.findAllById(idList);
	   
	   // IDのリスト順に並べ替え（重要！）
	   questions.sort(Comparator.comparingInt(q -> idList.indexOf(q.getQuestionId())));
	
	   session.setAttribute("challengeQuestions", questions);
	   session.setAttribute("currentTagName", progress.getTagName());
	   session.setAttribute("normalModeStartTime", System.currentTimeMillis());
	   session.setAttribute("correctCount", progress.getCorrectCount());
	
	   model.addAttribute("questions", questions);
	   model.addAttribute("startIndex", progress.getCurrentIndex()); // 開始位置を渡す
	   model.addAttribute("savedCorrectCount", progress.getCorrectCount()); // ★JSに渡す
	   model.addAttribute("labels", List.of("ア", "イ", "ウ", "エ"));
	
	   return "challenge";
	}
    
}