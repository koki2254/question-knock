package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.QuestionDto;
import com.example.demo.entity.Bookmark;
import com.example.demo.entity.Question;
import com.example.demo.entity.SiteUser;
import com.example.demo.repository.BookmarkRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuestionDto> findBookmarkedQuestionsByUser(String email) {
        SiteUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return new ArrayList<>();

        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Integer> questionIds = bookmarks.stream()
                .map(b -> b.getQuestionId().intValue())
                .collect(Collectors.toList());

        if (questionIds.isEmpty()) return new ArrayList<>();

        List<Question> questions = questionRepository.findAllById(questionIds);
        return questions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Entity → DTO 変換
    private QuestionDto convertToDto(Question q) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(q.getQuestionId()); // getQuestionId()など名称確認
        dto.setQuestionText(q.getQuestionText());
        dto.setImageUrl(q.getImageUrl());
        dto.setExplanation(q.getExplanation());
        
        if (q.getChoices() != null) {
            List<QuestionDto.ChoiceDto> choiceDtos = q.getChoices().stream().map(c -> {
                QuestionDto.ChoiceDto cDto = new QuestionDto.ChoiceDto();
                cDto.setChoiceId(c.getChoiceId());
                cDto.setChoiceText(c.getChoiceText());
                cDto.setCorrect(c.isCorrect());
                return cDto;
            }).collect(Collectors.toList());
            dto.setChoices(choiceDtos);
        }
        return dto;
    }
}