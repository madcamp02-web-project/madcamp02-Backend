package com.madcamp02.service;

//======================================
// SajuCalculator - 사주(오행/띠) 계산기
//======================================
// Phase 2 온보딩에서 사용자의 생년월일을 입력받아
//  1) 오행 (FIRE/WATER/WOOD/GOLD/EARTH)
//  2) 띠 (쥐/소/호랑이/토끼/용/뱀/말/양/원숭이/닭/개/돼지)
// 를 계산해서 User 엔티티에 저장하기 위한 유틸성 컴포넌트입니다.
//
// 왜 이게 필요한가?
// - docs/FULL_SPECIFICATION.md 에서 users.saju_element, users.zodiac_sign 이 "온보딩 입력/계산 결과"로 고정되어 있음
// - 프론트는 온보딩 후 바로 프로필/AI/게임화 화면을 그릴 수 있어야 함
//
 // 여기 수정 필 무조건 시간 계산 할 수 있어야 함
//======================================

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SajuCalculator {

    //------------------------------------------
    // 반환 타입: SajuResult
    //------------------------------------------
    // Service 레이어가 "오행/띠" 계산 결과를 안전하게 받기 위한 결과 DTO(내부용)
    //------------------------------------------
    @Getter
    @Builder
    public static class SajuResult {
        private final String sajuElement; // FIRE | WATER | WOOD | GOLD | EARTH
        private final String zodiacSign;  // 쥐 | 소 | 호랑이 | ... | 돼지
    }

    //------------------------------------------
    // 오행 계산 규칙(천간 기반)
    //------------------------------------------
    // 10간(천간) -> 오행 매핑
    //  0,1: WOOD
    //  2,3: FIRE
    //  4,5: EARTH
    //  6,7: GOLD
    //  8,9: WATER
    //------------------------------------------
    private static final String[] STEM_TO_ELEMENT = {
            "WOOD", "WOOD",
            "FIRE", "FIRE",
            "EARTH", "EARTH",
            "GOLD", "GOLD",
            "WATER", "WATER"
    };

    //------------------------------------------
    // 띠 계산 규칙(지지 기반)
    //------------------------------------------
    // 12지(지지) -> 띠(한국어) 매핑
    //------------------------------------------
    private static final String[] BRANCH_TO_ZODIAC = {
            "쥐", "소", "호랑이", "토끼", "용", "뱀",
            "말", "양", "원숭이", "닭", "개", "돼지"
    };

    //------------------------------------------
    // 기준 연도
    //------------------------------------------
    // 1984년은 60갑자 기준 "갑자(甲子)"로 널리 사용되는 기준점
    // 여기서는 1984년을 (천간=0, 지지=0) 기준으로 잡고, 입력 연도와의 차이를 모듈러 연산으로 계산
    //------------------------------------------
    private static final int BASE_YEAR = 1984;

    public SajuResult calculate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("birthDate는 null일 수 없습니다.");
        }

        int year = birthDate.getYear();
        int diff = year - BASE_YEAR;

        int stemIndex = mod(diff, 10);   // 0~9
        int branchIndex = mod(diff, 12); // 0~11

        String element = STEM_TO_ELEMENT[stemIndex];
        String zodiac = BRANCH_TO_ZODIAC[branchIndex];

        return SajuResult.builder()
                .sajuElement(element)
                .zodiacSign(zodiac)
                .build();
    }

    //------------------------------------------
    // 모듈러 보정 함수
    //------------------------------------------
    // Java의 %는 음수에서 음수 결과가 나올 수 있으므로,
    // 항상 0 이상으로 보정해 주는 함수가 필요합니다.
    //------------------------------------------
    private int mod(int value, int mod) {
        int r = value % mod;
        return r < 0 ? r + mod : r;
    }
}

