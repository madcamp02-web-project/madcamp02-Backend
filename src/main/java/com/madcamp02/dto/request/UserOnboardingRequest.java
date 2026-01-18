package com.madcamp02.dto.request;

//======================================
// UserOnboardingRequest - 온보딩 요청 DTO
//======================================
// 정밀 사주 계산을 위한 요청 DTO
//
// 필수 입력:
// - birthDate: 생년월일 (양력/음력)
// - gender: 성별 (MALE/FEMALE/OTHER)
// - calendarType: 양력/음력 구분 (SOLAR/LUNAR/LUNAR_LEAP)
//
// 선택 입력:
// - birthTime: 생년월일시 (모르면 자동으로 00:00:00 설정)
//======================================

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

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
    // birthTime (선택, 기본값 00:00:00)
    //------------------------------------------
    // JSON 예시:
    //   { "birthTime": "13:05" }
    //
    // 주의:
    // - 시간을 모르면 null로 보내면 서버에서 00:00:00으로 자동 설정
    //------------------------------------------
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "birthTime은 HH:mm 형식이어야 합니다.")
    private String birthTime;

    //------------------------------------------
    // gender (필수)
    //------------------------------------------
    // MALE | FEMALE | OTHER
    //------------------------------------------
    @NotBlank(message = "gender는 필수입니다.")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "gender는 MALE, FEMALE, OTHER 중 하나여야 합니다.")
    private String gender;

    //------------------------------------------
    // calendarType (필수)
    //------------------------------------------
    // SOLAR (양력) | LUNAR (음력) | LUNAR_LEAP (음력윤달)
    //------------------------------------------
    @NotBlank(message = "calendarType은 필수입니다.")
    @Pattern(regexp = "^(SOLAR|LUNAR|LUNAR_LEAP)$", message = "calendarType은 SOLAR, LUNAR, LUNAR_LEAP 중 하나여야 합니다.")
    private String calendarType;

    //------------------------------------------
    // birthTime을 LocalTime으로 변환하는 헬퍼 메서드
    //------------------------------------------
    // null이면 00:00:00 반환
    //------------------------------------------
    public LocalTime getBirthTimeAsLocalTime() {
        if (birthTime == null || birthTime.isEmpty()) {
            return LocalTime.of(0, 0); // 기본값 0시 정각
        }
        String[] parts = birthTime.split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}

