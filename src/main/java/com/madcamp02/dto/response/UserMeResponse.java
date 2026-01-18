package com.madcamp02.dto.response;

//======================================
// UserMeResponse - 내 프로필 상세 응답 DTO
//======================================
// 정밀 사주 정보 포함
//
// 역할:
// - 프론트의 마이페이지(/mypage)에서 프로필/설정 화면을 그리기 위한 최소 필드 제공
// - 온보딩 완료 여부(사주/띠/생년월일 존재 여부)를 판단할 수 있게 해줌
//
// 주의:
// - 이 응답은 "내 정보"이므로 email까지 포함
// - (주의) 타인 프로필 공개 API(향후)에서는 성별/양력음력/시간까지 포함한 정밀 사주 계산 결과 저장 사용해야 함 (email 제외)
//======================================

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class UserMeResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String provider;

    private LocalDate birthDate;
    private LocalTime birthTime;
    private String gender; // MALE | FEMALE | OTHER
    private String calendarType; // SOLAR | LUNAR | LUNAR_LEAP
    private String sajuElement; // FIRE | WATER | WOOD | GOLD | EARTH
    private String zodiacSign;  // 쥐 | 소 | ... | 돼지

    private String avatarUrl;
    private Boolean isPublic;
    private Boolean isRankingJoined;
}

