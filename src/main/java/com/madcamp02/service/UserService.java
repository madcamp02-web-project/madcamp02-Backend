package com.madcamp02.service;

//======================================
// UserService - 사용자/온보딩 비즈니스 로직
//======================================
// Phase 2에서 구현하는 User API들의 "실제 일"을 담당하는 Service
//
// Controller는 "(요청/응답)"이고,
// Service는 "(비즈니스 로직)"입니다.
//
// 구현 대상 엔드포인트:
// - GET  /api/v1/user/me
// - PUT  /api/v1/user/me
// - POST /api/v1/user/onboarding
// - GET  /api/v1/user/wallet
//======================================

import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.request.UserOnboardingRequest;
import com.madcamp02.dto.request.UserUpdateRequest;
import com.madcamp02.dto.response.UserMeResponse;
import com.madcamp02.dto.response.UserWalletResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SajuCalculator sajuCalculator;

    //------------------------------------------
    // 내 프로필 조회 (GET /api/v1/user/me)
    //------------------------------------------
    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        return toMeResponse(user);
    }

    //------------------------------------------
    // 내 프로필/설정 수정 (PUT /api/v1/user/me)
    //------------------------------------------
    // 부분 업데이트 전략:
    // - Request에서 null인 필드는 "변경하지 않음"
    //------------------------------------------
    @Transactional
    public UserMeResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (request.getNickname() != null) {
            user.updateNickname(request.getNickname());
        }

        if (request.getAvatarUrl() != null) {
            user.updateAvatarUrl(request.getAvatarUrl());
        }

        if (request.getIsPublic() != null) {
            user.updateVisibility(request.getIsPublic());
        }

        if (request.getIsRankingJoined() != null) {
            user.updateRankingJoined(request.getIsRankingJoined());
        }

        // JPA 더티 체킹으로 자동 반영되지만, 명시적으로 save해도 안전
        userRepository.save(user);

        return toMeResponse(user);
    }

    //------------------------------------------
    // 온보딩 (POST /api/v1/user/onboarding)
    //------------------------------------------
    // 처리 과정:
    //  1) 정밀 사주 계산 (성별/양력음력/시간 포함)
    //  2) birthDate, birthTime, gender, calendarType 저장
    //  3) SajuCalculator로 오행/띠 계산
    //  4) users.saju_element, users.zodiac_sign 저장
    //------------------------------------------
    @Transactional
    public UserMeResponse onboarding(Long userId, UserOnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 정밀 사주 계산
        SajuCalculator.SajuInput input = SajuCalculator.SajuInput.builder()
                .birthDate(request.getBirthDate())
                .birthTime(request.getBirthTimeAsLocalTime())
                .gender(request.getGender())
                .calendarType(request.getCalendarType())
                .build();

        SajuCalculator.SajuResult result = sajuCalculator.calculatePrecise(input);

        // 온보딩은 "한 번에" 완료 처리하는게 안전합니다.
        // (중간 저장/중간 실패가 나면, birthDate만 들어가고 saju가 비는 불완전 상태가 될 수 있음)
        // 이를 예방하게 메서드로 체크
        user.completeOnboarding(
                request.getBirthDate(),
                request.getBirthTimeAsLocalTime(),
                request.getGender(),
                request.getCalendarType(),
                result.getSajuElement(),
                result.getZodiacSign()
        );

        userRepository.save(user);

        return toMeResponse(user);
    }

    //------------------------------------------
    // 지갑 정보 조회 (GET /api/v1/user/wallet)
    //------------------------------------------
    @Transactional
    public UserWalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    // 원칙적으로 회원가입 시 지갑이 생성되지만,
                    // 혹시라도 누락된 계정이 있으면 여기서 자동 생성
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
                    Wallet newWallet = Wallet.builder().user(user).build();
                    return walletRepository.save(newWallet);
                });

        return UserWalletResponse.builder()
                .cashBalance(wallet.getCashBalance())
                .realizedProfit(wallet.getRealizedProfit())
                .totalAssets(wallet.getTotalAssets())
                .gameCoin(wallet.getGameCoin())
                .build();
    }

    //------------------------------------------
    // Entity -> Response 변환 메서드
    //------------------------------------------
    private UserMeResponse toMeResponse(User user) {
        return UserMeResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .birthDate(user.getBirthDate())
                .birthTime(user.getBirthTime())
                .gender(user.getGender())
                .calendarType(user.getCalendarType())
                .sajuElement(user.getSajuElement())
                .zodiacSign(user.getZodiacSign())
                .avatarUrl(user.getAvatarUrl())
                .isPublic(user.isPublic())
                .isRankingJoined(user.isRankingJoined())
                .build();
    }
}

