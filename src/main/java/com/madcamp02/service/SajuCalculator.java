package com.madcamp02.service;

//======================================
// SajuCalculator - 정밀 사주(오행/띠) 계산기
//======================================
// 성별/양력음력/시간까지 포함한 정밀 사주 계산
//
// 입력:
// - birthDate: 생년월일 (양력 또는 음력)
// - birthTime: 생년월일시 (모르면 00:00:00)
// - gender: 성별 (MALE/FEMALE/OTHER)
// - calendarType: 양력/음력 구분 (SOLAR/LUNAR/LUNAR_LEAP)
//
// 출력:
// - sajuElement: 오행 (FIRE/WATER/WOOD/GOLD/EARTH)
// - zodiacSign: 띠 (쥐/소/호랑이/.../돼지)
//
// 계산 방식:
// - 연주(年柱): 연도 기준 천간/지지
// - 월주(月柱): 월 기준 천간/지지
// - 일주(日柱): 일 기준 천간/지지
// - 시주(時柱): 시간 기준 천간/지지
// - 최종 오행은 일주(日柱)의 천간을 기준으로 산출
//======================================

import com.madcamp02.external.LunarCalendarClient;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SajuCalculator {

    private final LunarCalendarClient lunarCalendarClient;

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
    // 입력 파라미터 DTO
    //------------------------------------------
    @Getter
    @Builder
    public static class SajuInput {
        private final LocalDate birthDate;
        private final LocalTime birthTime;
        private final String gender; // MALE | FEMALE | OTHER
        private final String calendarType; // SOLAR | LUNAR | LUNAR_LEAP
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

    //------------------------------------------
    // 월주 계산용: 월(지지) -> 천간 오프셋
    //------------------------------------------
    // 각 월의 지지에 대응하는 천간 인덱스 오프셋
    // 1월(인월)부터 12월(축월)까지
    //------------------------------------------
    private static final int[] MONTH_STEM_OFFSET = {
            2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3  // 1월~12월
    };

    //------------------------------------------
    // 시주 계산용: 시간(지지) -> 천간 오프셋
    //------------------------------------------
    // 각 시간대의 지지에 대응하는 천간 인덱스 오프셋
    // 자시(23-1)부터 해시(21-23)까지
    //------------------------------------------
    private static final int[] HOUR_STEM_OFFSET = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1  // 자시(23-1) ~ 해시(21-23)
    };

    //------------------------------------------
    // 정밀 사주 계산
    //------------------------------------------
    // 성별/양력음력/시간까지 포함한 정밀 계산
    // 연주/월주/일주/시주 모두 계산하여 최종 오행 도출
    //------------------------------------------
    public SajuResult calculatePrecise(SajuInput input) {
        if (input.getBirthDate() == null) {
            throw new IllegalArgumentException("birthDate는 null일 수 없습니다.");
        }

        // 1) 양력/음력 변환 처리
        LocalDate solarDate = convertToSolar(input.getBirthDate(), input.getCalendarType());

        // 2) 연주(年柱) 계산 (띠는 연주 지지 기준)
        int yearStemIndex = calculateYearStem(solarDate.getYear());
        int yearBranchIndex = calculateYearBranch(solarDate.getYear());
        String zodiac = BRANCH_TO_ZODIAC[yearBranchIndex];

        // 3) 월주(月柱) 계산
        int monthStemIndex = calculateMonthStem(solarDate, yearStemIndex);
        int monthBranchIndex = calculateMonthBranch(solarDate);

        // 4) 일주(日柱) 계산 (오행은 일주의 천간 기준)
        int dayStemIndex = calculateDayStem(solarDate);
        int dayBranchIndex = calculateDayBranch(solarDate);

        // 5) 시주(時柱) 계산
        LocalTime birthTime = input.getBirthTime() != null ? input.getBirthTime() : LocalTime.of(0, 0);
        int hourStemIndex = calculateHourStem(birthTime, dayStemIndex);
        int hourBranchIndex = calculateHourBranch(birthTime);

        // 최종 오행은 일주(日柱)의 천간을 기준으로 산출
        String element = STEM_TO_ELEMENT[dayStemIndex];

        log.debug("사주 계산 결과 - 연주: {}{}, 월주: {}{}, 일주: {}{}, 시주: {}{}, 오행: {}, 띠: {}",
                yearStemIndex, yearBranchIndex,
                monthStemIndex, monthBranchIndex,
                dayStemIndex, dayBranchIndex,
                hourStemIndex, hourBranchIndex,
                element, zodiac);

        return SajuResult.builder()
                .sajuElement(element)
                .zodiacSign(zodiac)
                .build();
    }

    //------------------------------------------
    // 양력 변환 (음력 -> 양력)
    //------------------------------------------
    // 한국천문연구원 API를 사용하여 음력을 양력으로 변환
    //------------------------------------------
    private LocalDate convertToSolar(LocalDate inputDate, String calendarType) {
        if ("SOLAR".equals(calendarType)) {
            return inputDate; // 이미 양력
        }

        // 음력/음력윤달의 경우 한국천문연구원 API 호출
        boolean isLeapMonth = "LUNAR_LEAP".equals(calendarType);
        LunarCalendarClient.SolarDateResult result = lunarCalendarClient.convertLunarToSolar(
                inputDate.getYear(),
                inputDate.getMonthValue(),
                inputDate.getDayOfMonth(),
                isLeapMonth
        );

        return result.getSolarDate();
    }

    //------------------------------------------
    // 연주 천간 계산
    //------------------------------------------
    private int calculateYearStem(int year) {
        int diff = year - BASE_YEAR;
        return mod(diff, 10);
    }

    //------------------------------------------
    // 연주 지지 계산
    //------------------------------------------
    private int calculateYearBranch(int year) {
        int diff = year - BASE_YEAR;
        return mod(diff, 12);
    }

    //------------------------------------------
    // 월주 천간 계산
    //------------------------------------------
    // 연주 천간과 월의 지지 오프셋을 조합하여 계산
    //------------------------------------------
    private int calculateMonthStem(LocalDate date, int yearStemIndex) {
        int month = date.getMonthValue();
        int offset = MONTH_STEM_OFFSET[month - 1];
        return mod(yearStemIndex + offset, 10);
    }

    //------------------------------------------
    // 월주 지지 계산
    //------------------------------------------
    // 월(1월=인월, 2월=묘월, ...)에 대응하는 지지 인덱스
    //------------------------------------------
    private int calculateMonthBranch(LocalDate date) {
        int month = date.getMonthValue();
        // 1월=인(2), 2월=묘(3), 3월=진(4), ... 12월=축(11)
        return mod(month + 1, 12);
    }

    //------------------------------------------
    // 일주 천간 계산
    //------------------------------------------
    // 1900-01-01을 기준으로 일수 차이를 계산하여 천간 도출
    //------------------------------------------
    private int calculateDayStem(LocalDate date) {
        LocalDate baseDate = LocalDate.of(1900, 1, 1);
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date);
        // 1900-01-01은 경진(庚辰)으로 알려져 있음 (천간=6)
        int baseStem = 6;
        return mod((int) daysDiff + baseStem, 10);
    }

    //------------------------------------------
    // 일주 지지 계산
    //------------------------------------------
    // 1900-01-01을 기준으로 일수 차이를 계산하여 지지 도출
    //------------------------------------------
    private int calculateDayBranch(LocalDate date) {
        LocalDate baseDate = LocalDate.of(1900, 1, 1);
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date);
        // 1900-01-01은 경진(庚辰)으로 알려져 있음 (지지=4)
        int baseBranch = 4;
        return mod((int) daysDiff + baseBranch, 12);
    }

    //------------------------------------------
    // 시주 천간 계산
    //------------------------------------------
    // 일주 천간과 시간의 지지 오프셋을 조합하여 계산
    //------------------------------------------
    private int calculateHourStem(LocalTime time, int dayStemIndex) {
        int hourIndex = getHourIndex(time);
        int offset = HOUR_STEM_OFFSET[hourIndex];
        return mod(dayStemIndex * 2 + offset, 10);
    }

    //------------------------------------------
    // 시주 지지 계산
    //------------------------------------------
    // 시간대에 대응하는 지지 인덱스
    //------------------------------------------
    private int calculateHourBranch(LocalTime time) {
        int hourIndex = getHourIndex(time);
        return hourIndex;
    }

    //------------------------------------------
    // 시간대 인덱스 계산
    //------------------------------------------
    // 자시(23-1) = 0, 축시(1-3) = 1, ..., 해시(21-23) = 11
    //------------------------------------------
    private int getHourIndex(LocalTime time) {
        int hour = time.getHour();
        // 자시(23-1): hour=23 or 0 -> index=0
        // 축시(1-3): hour=1 or 2 -> index=1
        // ...
        // 해시(21-23): hour=21 or 22 -> index=11
        if (hour == 23 || hour == 0) {
            return 0; // 자시
        }
        return (hour + 1) / 2;
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

