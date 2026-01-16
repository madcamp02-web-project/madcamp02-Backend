package com.madcamp02.domain.chat;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String messages;  // JSON 문자열로 저장

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== 생성자 ==========

    @Builder
    public ChatHistory(User user, UUID sessionId, String messages) {
        this.user = user;
        this.sessionId = sessionId;
        this.messages = messages;
        this.createdAt = LocalDateTime.now();
    }

    // ========== 비즈니스 메서드 ==========

    public void updateMessages(String messages) {
        this.messages = messages;
    }

    public void updateSentimentScore(Float score) {
        this.sentimentScore = score;
    }
}