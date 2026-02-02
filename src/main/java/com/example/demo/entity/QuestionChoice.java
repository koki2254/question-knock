package com.example.demo.entity;

// ↓↓↓ ここに Question の import がなくても、
// 同じパッケージなので自動認識されるはずですが、
// Eclipseが賢くない場合もあるので、念のため import してもOKです。
// import com.example.demo.entity.Question; 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_choice")
public class QuestionChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer choiceId;
    
    // (TEXT型にするため @Column を推奨)
    @Column(columnDefinition="TEXT")
    private String choiceText;
    
    // (boolean (プリミティブ型) にすると NULL を許容しない)
    private boolean isCorrect;

    // "question" という名前で Question オブジェクトと連携
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id") // DBのカラム名
    private Question question;
    
    @Column(name = "image_url")
    private String imageUrl;

    // --- (コンストラクタ、ゲッター、セッター) ---
    
    public QuestionChoice() {} // ← 空のコンストラクTA

    // --- ゲッター・セッター ---
    // (IDEで自動生成してもOK)

    public Integer getChoiceId() {
        return choiceId;
    }

    public void setChoiceId(Integer choiceId) {
        this.choiceId = choiceId;
    }

    public String getChoiceText() {
        return choiceText;
    }

    public void setChoiceText(String choiceText) {
        this.choiceText = choiceText;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}