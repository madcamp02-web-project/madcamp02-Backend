package com.madcamp02.domain.watchlist;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_watchlist_user_ticker",
                columnNames = {"user_id", "ticker"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_id")
    private Long watchlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    // ========== »ý¼ºÀÚ ==========

    @Builder
    public Watchlist(User user, String ticker) {
        this.user = user;
        this.ticker = ticker;
        this.addedAt = LocalDateTime.now();
    }
}