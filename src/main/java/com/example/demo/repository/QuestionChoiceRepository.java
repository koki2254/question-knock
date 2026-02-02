package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.QuestionChoice; // ← ここで QuestionChoice を import

import jakarta.transaction.Transactional;

@Repository
public interface QuestionChoiceRepository extends JpaRepository<QuestionChoice, Integer> {
    // (中身は空でOK)
	@Transactional
    @Modifying
    @Query("DELETE FROM QuestionChoice c WHERE c.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Integer questionId);
}