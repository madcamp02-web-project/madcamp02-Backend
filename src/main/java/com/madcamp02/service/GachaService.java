package com.madcamp02.service;

import com.madcamp02.domain.item.Inventory;
import com.madcamp02.domain.item.InventoryRepository;
import com.madcamp02.domain.item.Item;
import com.madcamp02.domain.item.ItemRepository;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.GachaResponse;
import com.madcamp02.dto.response.ItemsResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.GameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 가챠/상점 관련 서비스.
 * - GAME_001: 코인 부족
 * - GAME_002: 재추첨 실패(모든 아이템이 이미 보유)
 * - GAME_003: 가챠에 사용할 아이템이 없을 때 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GachaService {

    private static final int GACHA_COST = 100; // 기본 가챠 비용 (게임 코인)
    private static final int MAX_REROLL = 10;  // 중복 방지를 위한 최대 재추첨 횟수

    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * 아이템 목록 조회.
     * 선택적 카테고리 파라미터로 NAMEPLATE/AVATAR/THEME 필터링을 지원한다.
     */
    @Transactional(readOnly = true)
    public ItemsResponse getItems(String categoryString) {
        List<Item> items;
        if (categoryString != null && !categoryString.isBlank()) {
            try {
                Item.Category category = Item.Category.valueOf(categoryString.toUpperCase());
                items = itemRepository.findByCategory(category);
            } catch (IllegalArgumentException e) {
                throw new GameException(ErrorCode.GAME_ITEM_NOT_FOUND, "지원하지 않는 카테고리입니다.");
            }
        } else {
            items = itemRepository.findAll();
        }

        List<ItemsResponse.Item> itemDtos = items.stream()
                .sorted(Comparator.comparing(Item::getRarity)) // 희귀도 순 정렬
                .map(item -> ItemsResponse.Item.builder()
                        .itemId(item.getItemId())
                        .name(item.getName())
                        .category(item.getCategory().name())
                        .rarity(item.getRarity().name())
                        .probability(item.getProbability())
                        .imageUrl(item.getImageUrl())
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ItemsResponse.builder()
                .asOf(LocalDateTime.now().toString())
                .items(itemDtos)
                .build();
    }

    /**
     * 가챠 실행.
     * - 게임 코인 100을 차감하고
     * - 확률 기반으로 아이템을 추첨하며
     * - 중복만 존재하면 GAME_002 예외를 발생시킨다.
     */
    @Transactional
    public GachaResponse draw(Long userId) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (wallet.getGameCoin() < GACHA_COST) {
            throw new GameException(ErrorCode.GAME_INSUFFICIENT_COIN);
        }

        List<Item> allItems = itemRepository.findAll();
        if (allItems.isEmpty()) {
            throw new GameException(ErrorCode.GAME_ITEM_NOT_FOUND, "가챠에 사용할 아이템이 없습니다.");
        }

        Item drawn = pickItem(allItems, userId);

        // 지갑 차감
        wallet.deductGameCoin(GACHA_COST);
        walletRepository.save(wallet);

        // 인벤토리 추가
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Inventory inventory = Inventory.builder()
                .user(user)
                .item(drawn)
                .build();
        inventoryRepository.save(inventory);

        return GachaResponse.builder()
                .itemId(drawn.getItemId())
                .name(drawn.getName())
                .category(drawn.getCategory().name())
                .rarity(drawn.getRarity().name())
                .imageUrl(drawn.getImageUrl())
                .remainingCoin(wallet.getGameCoin())
                .build();
    }

    /**
     * 확률 기반 아이템 추첨 (중복 방지 재추첨)
     */
    private Item pickItem(List<Item> items, Long userId) {
        float totalProb = (float) items.stream()
                .mapToDouble(item -> Optional.ofNullable(item.getProbability()).orElse(0f))
                .sum();
        if (totalProb <= 0) {
            throw new GameException(ErrorCode.INTERNAL_SERVER_ERROR, "아이템 확률이 설정되지 않았습니다.");
        }

        for (int i = 0; i < MAX_REROLL; i++) {
            float r = ThreadLocalRandom.current().nextFloat() * totalProb;
            float cumulative = 0f;
            for (Item item : items) {
                cumulative += Optional.ofNullable(item.getProbability()).orElse(0f);
                if (r <= cumulative) {
                    boolean owned = inventoryRepository.existsByUserUserIdAndItemItemId(userId, item.getItemId());
                    if (!owned) {
                        return item;
                    }
                    log.debug("이미 보유한 아이템 재추첨: userId={}, itemId={}", userId, item.getItemId());
                    break;
                }
            }
        }

        throw new GameException(ErrorCode.GAME_ITEM_ALREADY_OWNED, "중복 아이템만 존재하여 가챠에 실패했습니다.");
    }
}

