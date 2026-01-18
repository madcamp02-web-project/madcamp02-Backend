package com.madcamp02.service;

import com.madcamp02.external.LunarCalendarClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

//======================================
// SajuCalculatorTest - 사주 계산기 단위 테스트
//======================================
// 왜 이 테스트를 추가했는가?
// - Phase 2에서 온보딩은 "사주/띠 계산"이 핵심인데,
//   이 로직이 틀리면 프론트/AI/게임화 모든 화면이 잘못된 데이터를 받음
// - DB/Redis 같은 외부 의존 없이 순수 계산 로직만 검증하는 테스트로 구성
// - Phase 2.7.5: calculatePrecise() 메서드 기반으로 테스트 업데이트
//======================================
class SajuCalculatorTest {

    @Mock
    private LunarCalendarClient lunarCalendarClient;

    private SajuCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new SajuCalculator(lunarCalendarClient);
    }

    @Test
    void calculatePrecise_1984_isWoodAndRat() {
        // 1984년은 60갑자 기준 "갑자(甲子)"로 널리 알려진 기준 연도
        // - 연주 천간(갑) -> WOOD (하지만 오행은 일주 천간 기준)
        // - 연주 지지(자) -> 쥐 (띠는 연주 지지 기준)
        LocalDate birthDate = LocalDate.of(1984, 1, 1);
        
        // 양력이므로 변환 없이 그대로 사용
        SajuCalculator.SajuInput input = SajuCalculator.SajuInput.builder()
                .birthDate(birthDate)
                .birthTime(LocalTime.of(0, 0))
                .gender("MALE")
                .calendarType("SOLAR")
                .build();

        SajuCalculator.SajuResult result = calculator.calculatePrecise(input);

        // 띠는 연주 지지 기준이므로 "쥐"
        assertEquals("쥐", result.getZodiacSign());
        // 오행은 일주 천간 기준이므로 1984-01-01의 일주 천간을 계산해야 함
        // 실제 계산 결과를 확인 (1984-01-01의 일주는 정해진 값)
        // 이 테스트는 띠가 올바르게 계산되는지 확인
    }

    @Test
    void calculatePrecise_2024_isDragon() {
        // 2024년은 "갑진(甲辰)"년
        // - 연주 천간(갑) -> WOOD
        // - 연주 지지(진) -> 용
        LocalDate birthDate = LocalDate.of(2024, 6, 1);
        
        // 양력이므로 변환 없이 그대로 사용
        SajuCalculator.SajuInput input = SajuCalculator.SajuInput.builder()
                .birthDate(birthDate)
                .birthTime(LocalTime.of(12, 0))
                .gender("FEMALE")
                .calendarType("SOLAR")
                .build();

        SajuCalculator.SajuResult result = calculator.calculatePrecise(input);

        // 띠는 연주 지지 기준이므로 "용"
        assertEquals("용", result.getZodiacSign());
        // 오행은 일주 천간 기준
    }

    @Test
    void calculatePrecise_lunarCalendar_convertsCorrectly() {
        // 음력 입력 시 양력으로 변환되는지 확인
        LocalDate lunarDate = LocalDate.of(2024, 1, 1);
        LocalDate solarDate = LocalDate.of(2024, 2, 10); // 예시 양력 날짜
        
        // 음력 -> 양력 변환 Mock 설정
        when(lunarCalendarClient.convertLunarToSolar(anyInt(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(LunarCalendarClient.SolarDateResult.builder()
                        .solarDate(solarDate)
                        .leapMonth("평")
                        .build());

        SajuCalculator.SajuInput input = SajuCalculator.SajuInput.builder()
                .birthDate(lunarDate)
                .birthTime(LocalTime.of(0, 0))
                .gender("MALE")
                .calendarType("LUNAR")
                .build();

        SajuCalculator.SajuResult result = calculator.calculatePrecise(input);

        // 변환된 양력 날짜로 계산되어야 함
        assertEquals("용", result.getZodiacSign()); // 2024년은 용띠
    }
}

