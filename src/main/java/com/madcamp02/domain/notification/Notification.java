package com.madcamp02.domain.notification;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notif_id")
    private Long notifId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== Enum ==========

    public enum NotificationType {
        TRADE_COMPLETE,   // 거래 체결
        PRICE_ALERT,      // 가격 알림
        GACHA_RESULT,     // 가챠 결과
        RANKING_CHANGE,   // 랭킹 변동
        DIVIDEND_ALERT,   // 배당 알림
        SYSTEM            // 시스템 알림
    }

    // ========== 생성자 ==========

    @Builder
    public Notification(User user, NotificationType type, String title, String message) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // ========== 비즈니스 메서드 ==========

    public void markAsRead() {
        this.isRead = true;
    }
}