package com.madcamp02.domain.item;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rarity rarity;

    @Column(nullable = false)
    private Float probability;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== 관계 ==========

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<Inventory> inventories = new ArrayList<>();

    // ========== Enum ==========

    public enum Category {
        COSTUME, ACCESSORY, AURA, BACKGROUND
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    // ========== 생성자 ==========

    @Builder
    public Item(String name, String description, Category category,
                Rarity rarity, Float probability, String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.probability = probability;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }
}