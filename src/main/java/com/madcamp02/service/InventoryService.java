package com.madcamp02.service;

import com.madcamp02.domain.item.Inventory;
import com.madcamp02.domain.item.InventoryRepository;
import com.madcamp02.domain.item.Item;
import com.madcamp02.dto.response.InventoryResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.GameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인벤토리/장착 관련 서비스.
 * - 카테고리별 단일 장착 보장
 * - 아이템 미보유 시 GAME_003 예외 발생
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * 인벤토리 조회.
     * 사용자의 전체 인벤토리와 장착 여부를 InventoryResponse로 변환한다.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long userId) {
        List<Inventory> inventories = inventoryRepository.findByUserUserId(userId);
        return toResponse(inventories);
    }

    /**
     * 장착/해제 토글 (카테고리당 단일 장착 보장).
     * 같은 카테고리에 이미 장착된 다른 아이템이 있으면 자동으로 해제한다.
     */
    @Transactional
    public InventoryResponse toggleEquip(Long userId, Long itemId) {
        Inventory inventory = inventoryRepository.findByUserUserIdAndItemItemId(userId, itemId)
                .orElseThrow(() -> new GameException(ErrorCode.GAME_ITEM_NOT_FOUND));

        Item.Category category = inventory.getItem().getCategory();

        // 동일 카테고리에서 다른 장착 아이템 해제
        inventoryRepository.findEquippedByCategory(userId, category)
                .filter(inv -> !inv.getInvId().equals(inventory.getInvId()))
                .ifPresent(Inventory::unequip);

        inventory.toggleEquip();

        List<Inventory> updated = inventoryRepository.findByUserUserId(userId);
        return toResponse(updated);
    }

    private InventoryResponse toResponse(List<Inventory> inventories) {
        List<InventoryResponse.Item> items = inventories.stream()
                .map(inv -> InventoryResponse.Item.builder()
                        .itemId(inv.getItem().getItemId())
                        .name(inv.getItem().getName())
                        .category(inv.getItem().getCategory().name())
                        .rarity(inv.getItem().getRarity().name())
                        .imageUrl(inv.getItem().getImageUrl())
                        .equipped(inv.getIsEquipped())
                        .build())
                .collect(Collectors.toList());

        return InventoryResponse.builder()
                .asOf(LocalDateTime.now().toString())
                .items(items)
                .build();
    }
}

