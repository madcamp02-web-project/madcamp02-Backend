package com.madcamp02.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

//======================================
// SajuCalculatorTest - 사주 계산기 단위 테스트
//======================================
// 왜 이 테스트를 추가했는가?
// - Phase 2에서 온보딩은 "사주/띠 계산"이 핵심인데,
//   이 로직이 틀리면 프론트/AI/게임화 모든 화면이 잘못된 데이터를 받음
// - DB/Redis 같은 외부 의존 없이 순수 계산 로직만 검증하는 테스트로 구성
//======================================
class SajuCalculatorTest {

    @Test
    void calculate_1984_isWoodAndRat() {
        // 1984년은 60갑자 기준 "갑자(甲子)"로 널리 알려진 기준 연도
        // - 천간(갑) -> WOOD
        // - 지지(자) -> 쥐
        SajuCalculator calculator = new SajuCalculator();

        SajuCalculator.SajuResult result = calculator.calculate(LocalDate.of(1984, 1, 1));

        assertEquals("WOOD", result.getSajuElement());
        assertEquals("쥐", result.getZodiacSign());
    }

    @Test
    void calculate_2024_isWoodAndDragon() {
        // 2024년은 "갑진(甲辰)"년이니까 이거로 @Test 어노테이션을 이용해 실행
        // - 천간(갑) -> WOOD
        // - 지지(진) -> 용
        SajuCalculator calculator = new SajuCalculator();

        SajuCalculator.SajuResult result = calculator.calculate(LocalDate.of(2024, 6, 1));

        assertEquals("WOOD", result.getSajuElement());
        assertEquals("용", result.getZodiacSign());
    }
}

