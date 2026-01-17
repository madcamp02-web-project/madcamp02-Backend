package com.madcamp02.domain.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // 카테고리별 아이템 조회
    List<Item> findByCategory(Item.Category category);

    // 등급별 아이템 조회
    List<Item> findByRarity(Item.Rarity rarity);

    // 카테고리와 등급으로 조회
    List<Item> findByCategoryAndRarity(Item.Category category, Item.Rarity rarity);

    // 확률 기준 정렬 (가챠용)
    List<Item> findAllByOrderByProbabilityDesc();

    // 등급별 아이템 (확률순 정렬)
    List<Item> findByRarityOrderByProbabilityDesc(Item.Rarity rarity);

    // 아이템 이름으로 검색
    List<Item> findByNameContaining(String keyword);

    // 전체 아이템 (등급순 정렬)
    @Query("SELECT i FROM Item i ORDER BY " +
            "CASE i.rarity " +
            "WHEN 'LEGENDARY' THEN 1 " +
            "WHEN 'EPIC' THEN 2 " +
            "WHEN 'RARE' THEN 3 " +
            "WHEN 'COMMON' THEN 4 END")
    List<Item> findAllOrderByRarity();
}