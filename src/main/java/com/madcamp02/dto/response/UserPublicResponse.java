package com.madcamp02.dto.response;

//======================================
// UserPublicResponse - 타인 프로필 공개 응답 DTO
//======================================
// Phase 2 확장: 타인 프로필 조회용 DTO (email 제외)
//
// 사용 시점:
// - GET /api/v1/user/{userId} (향후 구현 예정)
// - 랭킹 API에서 사용자 정보 표시
//
// 보안:
// - email은 "내 정보"에서만 노출되므로 이 DTO에서는 제외
// - isPublic이 false인 사용자는 이 DTO로도 조회 불가 (Service 레이어에서 필터링)
//======================================

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class UserPublicResponse {

    private Long userId;
    // email 제외 (보안)
    private String nickname;
    private String provider;

    // 생년월일/시간은 공개 여부에 따라 선택적 노출
    private LocalDate birthDate;
    private LocalTime birthTime;
    private String gender; // MALE | FEMALE | OTHER
    private String calendarType; // SOLAR | LUNAR | LUNAR_LEAP
    private String sajuElement; // FIRE | WATER | WOOD | GOLD | EARTH
    private String zodiacSign;  // 쥐 | 소 | ... | 돼지

    private String avatarUrl;
    // isPublic, isRankingJoined는 타인에게 노출 불필요
}
