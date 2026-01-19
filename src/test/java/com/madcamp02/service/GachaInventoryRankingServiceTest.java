package com.madcamp02.service;

import com.madcamp02.domain.item.Inventory;
import com.madcamp02.domain.item.InventoryRepository;
import com.madcamp02.domain.item.Item;
import com.madcamp02.domain.item.ItemRepository;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.InventoryResponse;
import com.madcamp02.dto.response.RankingResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.GameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Phase 5 서비스 레벨 기본 검증
 * - 카테고리 검증
 * - 장착 토글 시 단일 장착 보장
 * - 랭킹 수익률 계산
 */
@ExtendWith(MockitoExtension.class)
class GachaInventoryRankingServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WalletRepository walletRepository;

    private GachaService gachaService;
    private InventoryService inventoryService;
    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gachaService = new GachaService(itemRepository, inventoryRepository, walletRepository, null);
        inventoryService = new InventoryService(inventoryRepository);
        rankingService = new RankingService(walletRepository);
    }

    @Test
    @DisplayName("아이템 조회: 잘못된 카테고리 요청 시 GameException")
    void getItems_invalidCategory_throws() {
        assertThatThrownBy(() -> gachaService.getItems("INVALID"))
                .isInstanceOf(GameException.class);
    }

    @Test
    @DisplayName("가챠 실행: 게임 코인이 부족하면 GAME_001 에러가 발생한다")
    void draw_insufficientCoin_throwsGame001() {
        Wallet wallet = buildWallet(1L, 10000.0);
        setField(wallet, "gameCoin", 0);

        when(walletRepository.findByUserIdWithLock(1L))
                .thenReturn(Optional.of(wallet));
        when(itemRepository.findAll())
                .thenReturn(List.of(buildItem(1L, "Frame A", Item.Category.NAMEPLATE, Item.Rarity.RARE)));

        assertThatThrownBy(() -> gachaService.draw(1L))
                .isInstanceOf(GameException.class)
                .hasMessageContaining(ErrorCode.GAME_INSUFFICIENT_COIN.getCode());
    }

    @Test
    @DisplayName("가챠 실행: 가챠에 사용할 아이템이 없으면 GAME_003 에러가 발생한다")
    void draw_noItems_throwsGame003() {
        Wallet wallet = buildWallet(1L, 10000.0);
        setField(wallet, "gameCoin", 100);

        when(walletRepository.findByUserIdWithLock(1L))
                .thenReturn(Optional.of(wallet));
        when(itemRepository.findAll())
                .thenReturn(List.of());

        assertThatThrownBy(() -> gachaService.draw(1L))
                .isInstanceOf(GameException.class)
                .hasMessageContaining(ErrorCode.GAME_ITEM_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("가챠 실행: 모든 아이템이 이미 보유 상태이면 GAME_002 에러가 발생한다")
    void draw_allOwnedItems_throwsGame002() {
        Wallet wallet = buildWallet(1L, 10000.0);
        setField(wallet, "gameCoin", 100);

        Item item = buildItem(1L, "Frame A", Item.Category.NAMEPLATE, Item.Rarity.RARE);

        when(walletRepository.findByUserIdWithLock(1L))
                .thenReturn(Optional.of(wallet));
        when(itemRepository.findAll())
                .thenReturn(List.of(item));
        when(inventoryRepository.existsByUserUserIdAndItemItemId(1L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> gachaService.draw(1L))
                .isInstanceOf(GameException.class)
                .hasMessageContaining(ErrorCode.GAME_ITEM_ALREADY_OWNED.getCode());
    }

    @Test
    @DisplayName("장착 토글: 같은 카테고리는 단일 장착으로 유지")
    void toggleEquip_singleEquipPerCategory() {
        User user = User.builder()
                .email("test@test.com")
                .password("pwd")
                .nickname("tester")
                .provider("LOCAL")
                .build();

        Item itemA = buildItem(1L, "Frame A", Item.Category.NAMEPLATE, Item.Rarity.RARE);
        Item itemB = buildItem(2L, "Frame B", Item.Category.NAMEPLATE, Item.Rarity.EPIC);

        Inventory invA = Inventory.builder().user(user).item(itemA).build();
        setField(invA, "invId", 1L);
        invA.equip();

        Inventory invB = Inventory.builder().user(user).item(itemB).build();
        setField(invB, "invId", 2L);

        when(inventoryRepository.findByUserUserIdAndItemItemId(anyLong(), anyLong()))
                .thenReturn(Optional.of(invB));
        when(inventoryRepository.findEquippedByCategory(anyLong(), any()))
                .thenReturn(Optional.of(invA));
        when(inventoryRepository.findByUserUserId(anyLong()))
                .thenReturn(List.of(invA, invB));

        InventoryResponse response = inventoryService.toggleEquip(10L, 2L);

        boolean equippedA = response.getItems().stream()
                .filter(i -> i.getItemId().equals(1L))
                .findFirst()
                .map(InventoryResponse.Item::getEquipped)
                .orElse(false);
        boolean equippedB = response.getItems().stream()
                .filter(i -> i.getItemId().equals(2L))
                .findFirst()
                .map(InventoryResponse.Item::getEquipped)
                .orElse(false);

        assertThat(equippedA).isFalse();
        assertThat(equippedB).isTrue();
    }

    @Test
    @DisplayName("랭킹: 총자산 기준 수익률 계산")
    void ranking_calculatesReturnPercent() {
        Wallet wallet = buildWallet(1L, 12000.0);
        setField(wallet, "user", User.builder()
                .email("rank@test.com")
                .password("pwd")
                .nickname("ranker")
                .provider("LOCAL")
                .build());

        when(walletRepository.findRankingWallets(PageRequest.of(0, 50)))
                .thenReturn(List.of(wallet));
        when(walletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(wallet));

        RankingResponse response = rankingService.getRanking(1L);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getReturnPercent()).isEqualTo(20.0);
        assertThat(response.getMy().getReturnPercent()).isEqualTo(20.0);
        assertThat(response.getItems().get(0).getRank()).isEqualTo(1);
    }

    // ===== helpers =====

    private Item buildItem(Long id, String name, Item.Category category, Item.Rarity rarity) {
        Item item = Item.builder()
                .name(name)
                .description("")
                .category(category)
                .rarity(rarity)
                .probability(1.0f)
                .imageUrl("")
                .build();
        setField(item, "itemId", id);
        setField(item, "createdAt", LocalDateTime.now());
        return item;
    }

    private Wallet buildWallet(Long userId, Double totalAssets) {
        Wallet wallet = Wallet.builder()
                .user(null)
                .build();
        setField(wallet, "walletId", 1L);
        setField(wallet, "cashBalance", BigDecimal.valueOf(totalAssets));
        setField(wallet, "realizedProfit", BigDecimal.ZERO);
        setField(wallet, "totalAssets", BigDecimal.valueOf(totalAssets));
        return wallet;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

