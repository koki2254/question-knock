package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.example.demo.repository.TagRepository; // ← TagRepositoryのimport

@Controller // ← ★★★ この @Controller アノテーションが必須です ★★★
public class QuestionController {

    @Autowired // ← ★★★ TagRepository の @Autowired が必須です ★★★
    private TagRepository tagRepository;
    
    /**
     * 分野選択画面 (exam-options.html) を表示する
     * (GET /fields?exam=...)
     * @param exam "基本" または "応用" が入る
     */
    //@GetMapping("/fields") // ← ★★★ このマッピングが /fields になっているか確認 ★★★
    //public String showFieldsPage(@RequestParam("exam") String exam, Model model) {
        
        // 1. DBから「Tagオブジェクト」のリストを取得
      //  List<Tag> tags = tagRepository.findAll(); 
        
        // 2. モデルに「exam」と「fields」を渡す
        //model.addAttribute("exam", exam);
        //model.addAttribute("fields", tags); // ★ List<Tag> を渡す
        
        // 3. exam-options.html を表示
     //   return "exam-options";
   // }
}