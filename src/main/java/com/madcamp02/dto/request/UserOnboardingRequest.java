package com.madcamp02.dto.request;

//======================================
// UserOnboardingRequest - 온보딩 요청 DTO
//======================================
// Phase 2에서 구현하는 POST /api/v1/user/onboarding 요청 Body를 담는 클래스
//
// 문서 기준:
// - docs/FULL_SPECIFICATION.md: "온보딩 (생년월일 입력 및 사주 계산)"
// - DB 스키마: users.birth_date (DATE), users.saju_element, users.zodiac_sign
//
// 프론트 계획서(참고):
// - 생년월일/시간 입력 UI가 있을 수 있으나, DB에는 DATE만 저장됨
// - 따라서 birthTime은 "선택값"으로 받고, 현재 Phase 2에서는 저장하지 않습니다.
//======================================

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserOnboardingRequest {

    //------------------------------------------
    // birthDate (필수)
    //------------------------------------------
    // JSON 예시:
    //   { "birthDate": "2000-01-01" }
    //------------------------------------------
    @NotNull(message = "birthDate는 필수입니다.")
    private LocalDate birthDate;

    //------------------------------------------
    // birthTime (선택)
    //------------------------------------------
    // JSON 예시:
    //   { "birthDate": "2000-01-01", "birthTime": "13:05" }
    //
    // 주의:
    // - 현재 DB(users.birth_date)는 DATE라 시간 저장 불가
    // - 시주까지 포함한 정밀 사주는 Phase 2 범위를 벗어나므로, 지금은 입력만 받고 저장하지 않습니다.
    //------------------------------------------
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "birthTime은 HH:mm 형식이어야 합니다.")
    private String birthTime;
}

