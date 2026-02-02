package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Bookmark;
import com.example.demo.entity.SiteUser;
import com.example.demo.repository.BookmarkRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class BookmarkApiController {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @PostMapping("/toggle")
    public Map<String, Object> toggleBookmark(@RequestParam("questionId") Long questionId,
                                              @AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            return response;
        }

        String email = principal.getAttribute("email");
        SiteUser user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            response.put("success", false);
            return response;
        }

        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndQuestionId(user.getId(), questionId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            response.put("status", "removed");
        } else {
            Bookmark b = new Bookmark();
            b.setUserId(user.getId());
            b.setQuestionId(questionId);
            bookmarkRepository.save(b);
            response.put("status", "added");
        }
        response.put("success", true);
        return response;
    }
}