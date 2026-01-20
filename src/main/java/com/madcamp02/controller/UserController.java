package com.madcamp02.controller;

//======================================
// UserController - 사용자 API 컨트롤러
//======================================
// Phase 2에서 구현하는 /api/v1/user/* 요청을 처리하는 REST 컨트롤러
//
// 역할:
// - 클라이언트 요청(JSON)을 DTO로 받고(@RequestBody)
// - 인증된 사용자 정보를 꺼내고(@AuthenticationPrincipal)
// - Service에 일을 시킨 뒤
// - 결과 DTO를 JSON으로 반환합니다.
//
// 주의:
// - SecurityConfig에서 PUBLIC_ENDPOINTS에 포함되지 않기 때문에
//   이 컨트롤러는 "로그인(Access Token)"이 반드시 필요하니까 주의하셈
//======================================

import com.madcamp02.dto.request.AddWatchlistRequest;
import com.madcamp02.dto.request.UserOnboardingRequest;
import com.madcamp02.dto.request.UserUpdateRequest;
import com.madcamp02.dto.response.UserMeResponse;
import com.madcamp02.dto.response.UserWalletResponse;
import com.madcamp02.dto.response.UserWatchlistResponse;
import com.madcamp02.security.CustomUserDetails;
import com.madcamp02.service.UserService;
import com.madcamp02.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WatchlistService watchlistService;

    //------------------------------------------
    // 내 프로필 상세 조회
    //------------------------------------------
    // 요청: GET /api/v1/user/me
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "내 프로필 조회", description = "로그인된 사용자의 프로필/설정 상세 조회", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponse response = userService.getMe(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 내 프로필/설정 수정
    //------------------------------------------
    // 요청: PUT /api/v1/user/me
    // 헤더: Authorization: Bearer {accessToken}
    // Body: { "nickname": "...", "isPublic": true, "isRankingJoined": true, "avatarUrl": "..." }
    //------------------------------------------
    @Operation(summary = "내 프로필 수정", description = "닉네임/공개여부/랭킹참여/아바타URL 등 설정 수정", security = @SecurityRequirement(name = "bearer-key"))
    @PutMapping("/me")
    public ResponseEntity<UserMeResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserMeResponse response = userService.updateMe(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 온보딩
    //------------------------------------------
    // 요청: POST /api/v1/user/onboarding
    // 헤더: Authorization: Bearer {accessToken}
    // Body: { "birthDate": "2000-01-01", "birthTime": "13:05" }
    //------------------------------------------
    @Operation(summary = "온보딩", description = "생년월일 입력 후 사주(오행/띠) 계산 및 저장")
    @PostMapping("/onboarding")
    public ResponseEntity<UserMeResponse> onboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserOnboardingRequest request
    ) {
        UserMeResponse response = userService.onboarding(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 지갑 정보 조회
    //------------------------------------------
    // 요청: GET /api/v1/user/wallet
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "지갑 조회", description = "로그인된 사용자의 지갑(예수금/코인 등) 조회", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/wallet")
    public ResponseEntity<UserWalletResponse> wallet(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserWalletResponse response = userService.getWallet(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 관심종목 조회
    //------------------------------------------
    // 요청: GET /api/v1/user/watchlist
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "관심종목 조회", description = "로그인된 사용자의 관심종목 목록 조회", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/watchlist")
    public ResponseEntity<UserWatchlistResponse> getWatchlist(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserWatchlistResponse response = watchlistService.getMyWatchlistResponse(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 관심종목 추가
    //------------------------------------------
    // 요청: POST /api/v1/user/watchlist
    // 헤더: Authorization: Bearer {accessToken}
    // Body: { "ticker": "AAPL" }
    //------------------------------------------
    @Operation(summary = "관심종목 추가", description = "관심종목에 종목 추가 (중복 시 무시)", security = @SecurityRequirement(name = "bearer-key"))
    @PostMapping("/watchlist")
    public ResponseEntity<UserWatchlistResponse> addWatchlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddWatchlistRequest request
    ) {
        watchlistService.addTicker(userDetails.getUserId(), request.getTicker());
        UserWatchlistResponse response = watchlistService.getMyWatchlistResponse(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 관심종목 삭제
    //------------------------------------------
    // 요청: DELETE /api/v1/user/watchlist/{ticker}
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "관심종목 삭제", description = "관심종목에서 종목 제거", security = @SecurityRequirement(name = "bearer-key"))
    @DeleteMapping("/watchlist/{ticker}")
    public ResponseEntity<UserWatchlistResponse> removeWatchlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String ticker
    ) {
        watchlistService.removeTicker(userDetails.getUserId(), ticker);
        UserWatchlistResponse response = watchlistService.getMyWatchlistResponse(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}

