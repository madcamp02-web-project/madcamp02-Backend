package com.madcamp02.controller;

import com.madcamp02.dto.response.GachaResponse;
import com.madcamp02.dto.response.InventoryResponse;
import com.madcamp02.dto.response.ItemsResponse;
import com.madcamp02.dto.response.RankingResponse;
import com.madcamp02.security.CustomUserDetails;
import com.madcamp02.service.GachaService;
import com.madcamp02.service.InventoryService;
import com.madcamp02.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 게임/가챠/인벤토리/랭킹 API 컨트롤러.
 * Phase 5/5.5에서 정의한 `/api/v1/game/*` 엔드포인트를 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final GachaService gachaService;
    private final InventoryService inventoryService;
    private final RankingService rankingService;

    /**
     * 상점 아이템 목록 조회.
     * 카테고리(선택)에 따라 NAMEPLATE/AVATAR/THEME 아이템을 필터링한다.
     * GET /api/v1/game/items
     */
    @GetMapping("/items")
    public ResponseEntity<ItemsResponse> getItems(
            @RequestParam(value = "category", required = false) String category
    ) {
        log.debug("아이템 목록 조회: category={}", category);
        ItemsResponse response = gachaService.getItems(category);
        return ResponseEntity.ok(response);
    }

    /**
     * 가챠 실행.
     * 게임 코인을 차감하고, 중복 아이템만 존재할 경우 GAME_002 예외를 발생시킨다.
     * POST /api/v1/game/gacha
     */
    @PostMapping("/gacha")
    public ResponseEntity<GachaResponse> drawGacha(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("가챠 요청: userId={}", userDetails.getUserId());
        GachaResponse response = gachaService.draw(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 인벤토리 조회.
     * 현재 보유한 아이템과 장착 상태를 반환한다.
     * GET /api/v1/game/inventory
     */
    @GetMapping("/inventory")
    public ResponseEntity<InventoryResponse> getInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("인벤토리 조회: userId={}", userDetails.getUserId());
        InventoryResponse response = inventoryService.getInventory(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 아이템 장착/해제 토글.
     * 같은 카테고리 내에서는 항상 단일 장착만 허용한다.
     * PUT /api/v1/game/equip/{itemId}
     */
    @PutMapping("/equip/{itemId}")
    public ResponseEntity<InventoryResponse> toggleEquip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("itemId") Long itemId
    ) {
        log.debug("아이템 장착/해제: userId={}, itemId={}", userDetails.getUserId(), itemId);
        InventoryResponse response = inventoryService.toggleEquip(userDetails.getUserId(), itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 랭킹 조회.
     * 랭킹 참여(`is_ranking_joined = true`) 사용자만 대상으로 Top N 리스트와 내 순위를 계산한다.
     * GET /api/v1/game/ranking
     */
    @GetMapping("/ranking")
    public ResponseEntity<RankingResponse> getRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("랭킹 조회: userId={}", userDetails.getUserId());
        RankingResponse response = rankingService.getRanking(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}

