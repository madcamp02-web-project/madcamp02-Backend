package com.madcamp02.domain.item;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventory_user_item",
                columnNames = {"user_id", "item_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inv_id")
    private Long invId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    // ========== 생성자 ==========

    @Builder
    public Inventory(User user, Item item) {
        this.user = user;
        this.item = item;
        this.isEquipped = false;
        this.acquiredAt = LocalDateTime.now();
    }

    // ========== 비즈니스 메서드 ==========

    public void equip() {
        this.isEquipped = true;
    }

    public void unequip() {
        this.isEquipped = false;
    }

    public void toggleEquip() {
        this.isEquipped = !this.isEquipped;
    }
}