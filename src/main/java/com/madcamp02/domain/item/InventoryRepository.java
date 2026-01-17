package com.madcamp02.domain.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // 사용자의 전체 인벤토리 조회
    List<Inventory> findByUserUserId(Long userId);

    // 사용자의 특정 아이템 보유 여부
    Optional<Inventory> findByUserUserIdAndItemItemId(Long userId, Long itemId);

    // 아이템 보유 여부 확인
    boolean existsByUserUserIdAndItemItemId(Long userId, Long itemId);

    // 사용자의 장착된 아이템 목록
    List<Inventory> findByUserUserIdAndIsEquippedTrue(Long userId);

    // 사용자의 특정 카테고리 장착 아이템
    @Query("SELECT inv FROM Inventory inv " +
            "WHERE inv.user.userId = :userId " +
            "AND inv.item.category = :category " +
            "AND inv.isEquipped = true")
    Optional<Inventory> findEquippedByCategory(
            @Param("userId") Long userId,
            @Param("category") Item.Category category
    );

    // 사용자의 카테고리별 아이템 목록
    @Query("SELECT inv FROM Inventory inv " +
            "WHERE inv.user.userId = :userId " +
            "AND inv.item.category = :category")
    List<Inventory> findByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") Item.Category category
    );

    // 사용자의 등급별 아이템 목록
    @Query("SELECT inv FROM Inventory inv " +
            "WHERE inv.user.userId = :userId " +
            "AND inv.item.rarity = :rarity")
    List<Inventory> findByUserIdAndRarity(
            @Param("userId") Long userId,
            @Param("rarity") Item.Rarity rarity
    );

    // 사용자의 아이템 개수
    long countByUserUserId(Long userId);

    // 특정 아이템을 보유한 사용자 수
    long countByItemItemId(Long itemId);
}