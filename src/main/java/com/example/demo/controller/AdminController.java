package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Admin;
import com.example.demo.entity.Question;
import com.example.demo.entity.QuestionChoice;
import com.example.demo.entity.Tag;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.QuestionChoiceRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.TagRepository;
import com.example.demo.service.CsvQuestionService;
import com.example.demo.service.FileStorageService;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final QuestionRepository questionRepository;
    private final FileStorageService fileStorageService;
    private final QuestionChoiceRepository questionchoiceRepository;
    private final CsvQuestionService csvQuestionService;
    

    @Autowired
    public AdminController(AdminRepository adminRepository, PasswordEncoder passwordEncoder,TagRepository tagRepository,
            QuestionRepository questionRepository,FileStorageService fileStorageService, 
            QuestionChoiceRepository questionchoiceRepository, CsvQuestionService csvQuestionService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tagRepository = tagRepository;
        this.questionRepository = questionRepository;
        this.fileStorageService = fileStorageService;
        this.questionchoiceRepository = questionchoiceRepository;
        this.csvQuestionService = csvQuestionService;
    }

    /**
     * 管理者用ログインページの表示 (GET /admin/login)
     *
     * SecurityConfigの .loginPage("/admin/login") で指定したURLに対応します。
     * このメソッドが "admin_login" という文字列を返すことで、
     * Spring Boot (Thymeleaf) は templates/admin_login.html を探して表示します。
     */    
    @GetMapping("/login")
    public String showLoginPage() {
        return "admin_login";
    }

    /**
     * 新規登録ページの表示 (GET /admin/register)
     */    
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("admin", new Admin()); 
        return "admin_register";
    }

    /**
     * 新規登録処理 (POST /admin/register)
     * @param admin フォームから送られてきたデータ (name, email, password)
     * @param redirectAttributes リダイレクト先にメッセージを送るため
     */
    @PostMapping("/register")
    public String registerAdmin(@ModelAttribute Admin admin, RedirectAttributes redirectAttributes) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "そのメールアドレスは既に使用されています。");
            return "redirect:/admin/register";
        }
        String hashedPassword = passwordEncoder.encode(admin.getPassword());
        admin.setPassword(hashedPassword);
        adminRepository.save(admin);
        redirectAttributes.addFlashAttribute("successMessage", "管理者の登録が完了しました。ログインしてください。");
        return "redirect:/admin/login";
    }
    
    /**
     * ログイン成功後の管理者ダッシュボード (GET /admin/dashboard)
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = auth.getName();
        List<Admin> adminList = adminRepository.findAll();
        model.addAttribute("admins", adminList);
        model.addAttribute("loggedInEmail", loggedInEmail);
        return "admin_dashboard"; 
    }
    
	/**
	* 分野管理ページの表示 (GET /admin/tags)
	*/
    @GetMapping("/tags")
    public String showTagPage(Model model) {
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("newTag", new Tag());
        return "admin_tags";
    }

    /**
     * 新しい分野(タグ)を追加 (POST /admin/tags/add)
     */
    @PostMapping("/tags/add")
    public String addTag(@ModelAttribute Tag newTag, RedirectAttributes redirectAttributes) {
        if (newTag.getTagName() == null || newTag.getTagName().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "分野名を入力してください。");
        } else {
            tagRepository.save(newTag);
            redirectAttributes.addFlashAttribute("successMessage", "分野「" + newTag.getTagName() + "」を追加しました。");
        }
        return "redirect:/admin/tags";
    }

    /**
     * 分野(タグ)を削除 (POST /admin/tags/delete/{id})
     */
    @PostMapping("/tags/delete/{id}")
    public String deleteTag(@PathVariable("id") Integer tagId, RedirectAttributes redirectAttributes) {
        tagRepository.deleteById(tagId);
        redirectAttributes.addFlashAttribute("successMessage", "分野ID " + tagId + " を削除しました。");
        return "redirect:/admin/tags";
    }
    
    /**
     * 新規問題 登録フォームの表示 (GET /admin/questions/new)
     */
    @GetMapping("/questions/new")
    public String showQuestionForm(Model model) {
        model.addAttribute("question", new Question());
        List<Tag> validTags = tagRepository.findByTagNameIsNotNull();
        model.addAttribute("allTags", validTags); 
        return "admin_question_form";
    }
    
    /**
     * 問題一覧ページ (GET /admin/questionlist)
     */
    @GetMapping("/questionlist")
    public String showQuestionList(Model model) {
        List<Question> list = questionRepository.findAll();
        model.addAttribute("questions", list);
        return "admin_question_list";
    }
    
	/**
	 * 問題編集フォームの表示 (GET /admin/question/edit/{id})
	 */
    @GetMapping("/question/edit/{id}")
    public String editQuestion(@PathVariable("id") Integer id, Model model) {
        Question q = questionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid question ID: " + id));

        model.addAttribute("question", q);
        model.addAttribute("choices", q.getChoices());
        List<Tag> tags = tagRepository.findByTagNameIsNotNull();
        model.addAttribute("allTags", tags);

        return "admin_question_edit";
    }
    
    /**
     * 問題編集
     */
    @PostMapping("/questions/update/{id}")
    public String updateQuestion(
            @PathVariable("id") Integer id,
            @ModelAttribute Question formQuestion,
            @RequestParam(value = "choiceTexts", required = false) List<String> choiceTexts,
            @RequestParam(value = "choiceImageFiles", required = false) List<MultipartFile> choiceImageFiles,
            // ★ここを追加：HTMLから既存の画像パスを受け取る
            @RequestParam(value = "existingChoiceImageUrls", required = false) List<String> existingChoiceImageUrls,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "isCorrect", required = false) Integer correctChoiceIndex,
            RedirectAttributes redirectAttributes
    ) {
        // 1. バリデーションの修正
        // 既存画像がある場合も考慮するように修正
        boolean hasNoTexts = (choiceTexts == null || choiceTexts.isEmpty());
        boolean hasNoNewImages = (choiceImageFiles == null || choiceImageFiles.stream().allMatch(MultipartFile::isEmpty));
        boolean hasNoExistingImages = (existingChoiceImageUrls == null || existingChoiceImageUrls.stream().allMatch(url -> url == null || url.isEmpty()));

        if (hasNoTexts && hasNoNewImages && hasNoExistingImages) {
            redirectAttributes.addFlashAttribute("errorMessage", "エラー: 選択肢（テキストまたは画像）を少なくとも1つ入力してください。");
            return "redirect:/admin/question/edit/" + id;
        }

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("存在しない問題ID: " + id));

        // 問題情報の更新
        question.setQuestionText(formQuestion.getQuestionText());
        question.setExplanation(formQuestion.getExplanation());
        question.setTagId(formQuestion.getTagId());
        question.setExamCategory(formQuestion.getExamCategory());
        question.setExamSession(formQuestion.getExamSession());
        question.setYear(formQuestion.getYear());
        question.setSeason(formQuestion.getSeason());
        question.setQuestionNumber(formQuestion.getQuestionNumber());

        if (imageFile != null && !imageFile.isEmpty()) {
            String savedPath = fileStorageService.store(imageFile);
            question.setImageUrl(savedPath);
        }

        // 古い選択肢削除
        questionchoiceRepository.deleteByQuestionId(id);

        // ループ回数の決定
        int textCount = (choiceTexts != null) ? choiceTexts.size() : 0;
        int imageFileCount = (choiceImageFiles != null) ? choiceImageFiles.size() : 0;
        int existingUrlCount = (existingChoiceImageUrls != null) ? existingChoiceImageUrls.size() : 0;
        int maxCount = Math.max(textCount, Math.max(imageFileCount, existingUrlCount));

        List<QuestionChoice> newChoices = new ArrayList<>();
        for (int i = 0; i < maxCount; i++) {
            String text = (i < textCount) ? choiceTexts.get(i) : null;
            MultipartFile file = (i < imageFileCount) ? choiceImageFiles.get(i) : null;
            String existingUrl = (i < existingUrlCount) ? existingChoiceImageUrls.get(i) : null;

            boolean hasText = text != null && !text.isEmpty();
            boolean hasNewImage = file != null && !file.isEmpty();
            boolean hasExistingImage = existingUrl != null && !existingUrl.isEmpty();

            if (!hasText && !hasNewImage && !hasExistingImage) continue;

            QuestionChoice choice = new QuestionChoice();
            choice.setChoiceText(hasText ? text : null);
            choice.setCorrect(correctChoiceIndex != null && i == correctChoiceIndex);
            choice.setQuestion(question);

            // 画像のセットロジック：新規アップロードを最優先、なければ既存を保持
            if (hasNewImage) {
                String imageUrl = fileStorageService.store(file);
                choice.setImageUrl(imageUrl);
            } else if (hasExistingImage) {
                choice.setImageUrl(existingUrl);
            } else {
                choice.setImageUrl(null);
            }

            newChoices.add(choice);
        }

        question.setChoices(newChoices);
        questionRepository.save(question);

        return "redirect:/admin/questionlist";
    }

    /**
	 * 問題削除
	 */
    @PostMapping("/question/delete/{id}")
    public String deleteQuestionPost(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("存在しない問題ID: " + id));

        questionRepository.delete(question);
        redirectAttributes.addFlashAttribute("successMessage", "問題ID " + id + " を削除しました。");
        return "redirect:/admin/questionlist";
    }

    /**
     * 新規問題の登録処理 (POST /admin/questions/add)
     * 問題画像 + 選択肢テキスト + 選択肢画像 を受け取る
     */
    @PostMapping("/questions/add")
    public String addQuestion(
            @ModelAttribute Question question,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,  // 問題画像
            @RequestParam("choiceTexts") List<String> choiceTexts,                        // 選択肢テキスト
            @RequestParam(value = "choiceImageFiles", required = false) List<MultipartFile> choiceImageFiles, // 選択肢画像
            @RequestParam("isCorrect") int correctChoiceIndex,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // 1. 管理者IDセット
        String loggedInEmail = authentication.getName();
        Admin currentAdmin = adminRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("ログイン中の管理者が見つかりません"));
        question.setCreatedBy(currentAdmin.getAdminId());

        // 2. 画像保存
        if (!imageFile.isEmpty()) {
            String imageUrl = fileStorageService.store(imageFile);
            question.setImageUrl(imageUrl);
        } else {
            question.setImageUrl(null);
        }

        // 3. 選択肢の作成
        List<QuestionChoice> choices = new ArrayList<>();
        int maxSize = Math.max(choiceTexts.size(), (choiceImageFiles != null ? choiceImageFiles.size() : 0));

        for (int i = 0; i < maxSize; i++) {
            String text = i < choiceTexts.size() ? choiceTexts.get(i) : null;
            MultipartFile file = (choiceImageFiles != null && i < choiceImageFiles.size()) ? choiceImageFiles.get(i) : null;

            boolean hasText = text != null && !text.isEmpty();
            boolean hasImage = file != null && !file.isEmpty();
            if (!hasText && !hasImage) continue; // テキストも画像もない選択肢はスキップ

            QuestionChoice choice = new QuestionChoice();
            choice.setChoiceText(hasText ? text : null);
            choice.setCorrect(i == correctChoiceIndex);
            choice.setQuestion(question);

            if (hasImage) {
                String choiceImageUrl = fileStorageService.store(file);
                choice.setImageUrl(choiceImageUrl);
            } else {
                choice.setImageUrl(null);
            }

            choices.add(choice);
        }

        question.setChoices(choices);

        // 4. 保存
        questionRepository.save(question);

        redirectAttributes.addFlashAttribute("successMessage", "新しい問題を追加しました。");
        return "redirect:/admin/dashboard";
    }
    
    /**
     * 新規問題の登録処理 (CSV)
     */   
    @GetMapping("/questions/csv-upload")
    public String csvUploadPage() {
        return "admin_question_csv_upload";
    }

    
    @PostMapping("/questions/upload-csv")
    public String uploadCsv(
            @RequestParam("csv") MultipartFile csvFile,
            @RequestParam(value = "imagesZip", required = false) MultipartFile zipFile,
            Model model
    ) {
        try {
            List<Question> saved = csvQuestionService.importCsv(csvFile, zipFile);
            model.addAttribute("message", saved.size() + "件の問題を登録しました。");
        } catch (Exception e) {
            model.addAttribute("message", "エラー: " + e.getMessage());
        }

        return "admin_question_csv_upload";
    }
}