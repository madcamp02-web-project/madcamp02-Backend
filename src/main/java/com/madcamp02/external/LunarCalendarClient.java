package com.madcamp02.external;

//======================================
// LunarCalendarClient - 한국천문연구원 음력 변환 API 클라이언트
//======================================
// 엔드포인트:
// 1) getSolCalInfo: 음력 → 양력 변환 (양력일정보 조회)
//    - 요청: lunYear(필수), lunMonth(필수), lunDay(옵션), ServiceKey(필수)
// 2) getLunCalInfo: 양력 → 음력 변환 (음력일정보 조회)
//    - 요청: solYear(필수), solMonth(필수), solDay(옵션), ServiceKey(필수)
//======================================

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class LunarCalendarClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/LrsrCldInfoService";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");

    private final RestClient restClient;
    private final String serviceKey;

    public LunarCalendarClient(@Value("${kasi.service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    //------------------------------------------
    // 음력 → 양력 변환 결과 DTO
    //------------------------------------------
    @Getter
    @Builder
    public static class SolarDateResult {
        private final LocalDate solarDate; // 양력 날짜
        private final String leapMonth;    // 평/윤 (평달/윤달)
    }

    //------------------------------------------
    // 양력 → 음력 변환 결과 DTO
    //------------------------------------------
    @Getter
    @Builder
    public static class LunarDateResult {
        private final int lunarYear;   // 음력 연도
        private final int lunarMonth;  // 음력 월
        private final int lunarDay;    // 음력 일
        private final String leapMonth; // 평/윤
    }

    //------------------------------------------
    // 음력 → 양력 변환
    //------------------------------------------
    // API: getSolCalInfo (양력일정보 조회)
    // 요청 파라미터:
    //   - lunYear (필수): 연(음력), 4자리
    //   - lunMonth (필수): 월(음력), 2자리
    //   - lunDay (옵션): 일(음력), 2자리
    //   - ServiceKey (필수): 서비스 키
    // 예시: getSolCalInfo?lunYear=2015&lunMonth=01&lunDay=01&ServiceKey=서비스키
    //------------------------------------------
    public SolarDateResult convertLunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        if (serviceKey == null || serviceKey.isEmpty()) {
            log.warn("한국천문연구원 API 서비스 키가 설정되지 않았습니다. 양력 날짜를 그대로 반환합니다.");
            // 서비스 키가 없으면 양력으로 간주하고 그대로 반환
            return SolarDateResult.builder()
                    .solarDate(LocalDate.of(lunarYear, lunarMonth, lunarDay))
                    .leapMonth("평")
                    .build();
        }

        try {
            // API 명세에 따르면 leapMonth 파라미터는 없음
            // 윤달 여부는 응답의 lunLeapmonth 필드로 확인 가능
            String url = String.format(
                    "%s/getSolCalInfo?lunYear=%d&lunMonth=%02d&lunDay=%02d&ServiceKey=%s",
                    BASE_URL, lunarYear, lunarMonth, lunarDay, serviceKey
            );

            log.debug("한국천문연구원 API 호출 (음력→양력): lunYear={}, lunMonth={}, lunDay={}, isLeapMonth={}", 
                    lunarYear, lunarMonth, lunarDay, isLeapMonth);

            String xmlResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            log.debug("API 응답: {}", xmlResponse);

            return parseSolarDateResponse(xmlResponse);
        } catch (Exception e) {
            log.error("음력 → 양력 변환 실패: {}/{}/{}", lunarYear, lunarMonth, lunarDay, e);
            // 실패 시 양력으로 간주
            return SolarDateResult.builder()
                    .solarDate(LocalDate.of(lunarYear, lunarMonth, lunarDay))
                    .leapMonth("평")
                    .build();
        }
    }

    //------------------------------------------
    // 양력 → 음력 변환
    //------------------------------------------
    // API: getLunCalInfo (음력일정보 조회)
    // 요청 파라미터:
    //   - solYear (필수): 연, 4자리
    //   - solMonth (필수): 월, 2자리
    //   - solDay (옵션): 일, 2자리
    //   - ServiceKey (필수): 서비스 키
    // 예시: getLunCalInfo?solYear=2015&solMonth=01&solDay=01&ServiceKey=서비스키
    //------------------------------------------
    public LunarDateResult convertSolarToLunar(LocalDate solarDate) {
        if (serviceKey == null || serviceKey.isEmpty()) {
            log.warn("한국천문연구원 API 서비스 키가 설정되지 않았습니다. 음력 변환을 건너뜁니다.");
            return null;
        }

        try {
            String url = String.format(
                    "%s/getLunCalInfo?solYear=%s&solMonth=%s&solDay=%s&ServiceKey=%s",
                    BASE_URL,
                    solarDate.format(YEAR_FORMATTER),
                    solarDate.format(MONTH_FORMATTER),
                    solarDate.format(DAY_FORMATTER),
                    serviceKey
            );

            log.debug("한국천문연구원 API 호출 (양력→음력): solYear={}, solMonth={}, solDay={}", 
                    solarDate.getYear(), solarDate.getMonthValue(), solarDate.getDayOfMonth());

            String xmlResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            log.debug("API 응답: {}", xmlResponse);

            return parseLunarDateResponse(xmlResponse);
        } catch (Exception e) {
            log.error("양력 → 음력 변환 실패: {}", solarDate, e);
            return null;
        }
    }

    //------------------------------------------
    // XML 응답 파싱: 음력 → 양력 (getSolCalInfo)
    //------------------------------------------
    // 응답 필드:
    //   - solYear, solMonth, solDay: 양력 날짜 (필수)
    //   - lunLeapmonth: 윤달구분 (평/윤)
    //   - lunYear, lunMonth, lunDay: 음력 날짜
    //   - solLeapyear: 윤년구분 (평/윤)
    //   - solWeek: 요일
    //   - lunSecha, lunWolgeon, lunIljin: 간지 정보
    //   - solJd: 율리우스적일
    // 응답 예시:
    // <item>
    //   <solYear>2015</solYear>
    //   <solMonth>02</solMonth>
    //   <solDay>19</solDay>
    //   <lunLeapmonth>평</lunLeapmonth>
    //   ...
    // </item>
    //------------------------------------------
    private SolarDateResult parseSolarDateResponse(String xmlResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xmlResponse)));

        // resultCode 확인
        NodeList resultCodeList = doc.getElementsByTagName("resultCode");
        if (resultCodeList.getLength() > 0) {
            String resultCode = resultCodeList.item(0).getTextContent().trim();
            if (!"00".equals(resultCode)) {
                String resultMsg = "";
                NodeList resultMsgList = doc.getElementsByTagName("resultMsg");
                if (resultMsgList.getLength() > 0) {
                    resultMsg = resultMsgList.item(0).getTextContent().trim();
                }
                throw new RuntimeException(String.format("API 오류: resultCode=%s, resultMsg=%s", resultCode, resultMsg));
            }
        }

        // items/item 추출
        NodeList itemList = doc.getElementsByTagName("item");
        if (itemList.getLength() == 0) {
            throw new RuntimeException("응답에 item이 없습니다. XML: " + xmlResponse);
        }

        Element item = (Element) itemList.item(0);
        String solYearStr = getTextContent(item, "solYear").trim();
        String solMonthStr = getTextContent(item, "solMonth").trim();
        String solDayStr = getTextContent(item, "solDay").trim();
        String leapMonth = getTextContent(item, "lunLeapmonth").trim();

        if (solYearStr.isEmpty() || solMonthStr.isEmpty() || solDayStr.isEmpty()) {
            throw new RuntimeException("응답에 필수 필드(solYear/solMonth/solDay)가 없습니다.");
        }

        int solYear = Integer.parseInt(solYearStr);
        int solMonth = Integer.parseInt(solMonthStr);
        int solDay = Integer.parseInt(solDayStr);

        return SolarDateResult.builder()
                .solarDate(LocalDate.of(solYear, solMonth, solDay))
                .leapMonth(leapMonth.isEmpty() ? "평" : leapMonth)
                .build();
    }

    //------------------------------------------
    // XML 응답 파싱: 양력 → 음력 (getLunCalInfo)
    //------------------------------------------
    // 응답 필드:
    //   - lunYear, lunMonth, lunDay: 음력 날짜 (필수)
    //   - lunLeapmonth: 윤달구분 (평/윤)
    //   - solYear, solMonth, solDay: 양력 날짜
    //   - solLeapyear: 윤년구분 (평/윤)
    //   - solWeek: 요일
    //   - lunSecha, lunWolgeon, lunIljin: 간지 정보
    //   - solJd: 율리우스적일
    // 응답 예시:
    // <item>
    //   <lunYear>2014</lunYear>
    //   <lunMonth>11</lunMonth>
    //   <lunDay>11</lunDay>
    //   <lunLeapmonth>평</lunLeapmonth>
    //   ...
    // </item>
    //------------------------------------------
    private LunarDateResult parseLunarDateResponse(String xmlResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xmlResponse)));

        // resultCode 확인
        NodeList resultCodeList = doc.getElementsByTagName("resultCode");
        if (resultCodeList.getLength() > 0) {
            String resultCode = resultCodeList.item(0).getTextContent().trim();
            if (!"00".equals(resultCode)) {
                String resultMsg = "";
                NodeList resultMsgList = doc.getElementsByTagName("resultMsg");
                if (resultMsgList.getLength() > 0) {
                    resultMsg = resultMsgList.item(0).getTextContent().trim();
                }
                throw new RuntimeException(String.format("API 오류: resultCode=%s, resultMsg=%s", resultCode, resultMsg));
            }
        }

        // items/item 추출
        NodeList itemList = doc.getElementsByTagName("item");
        if (itemList.getLength() == 0) {
            throw new RuntimeException("응답에 item이 없습니다. XML: " + xmlResponse);
        }

        Element item = (Element) itemList.item(0);
        String lunYearStr = getTextContent(item, "lunYear").trim();
        String lunMonthStr = getTextContent(item, "lunMonth").trim();
        String lunDayStr = getTextContent(item, "lunDay").trim();
        String leapMonth = getTextContent(item, "lunLeapmonth").trim();

        if (lunYearStr.isEmpty() || lunMonthStr.isEmpty() || lunDayStr.isEmpty()) {
            throw new RuntimeException("응답에 필수 필드(lunYear/lunMonth/lunDay)가 없습니다.");
        }

        int lunYear = Integer.parseInt(lunYearStr);
        int lunMonth = Integer.parseInt(lunMonthStr);
        int lunDay = Integer.parseInt(lunDayStr);

        return LunarDateResult.builder()
                .lunarYear(lunYear)
                .lunarMonth(lunMonth)
                .lunarDay(lunDay)
                .leapMonth(leapMonth.isEmpty() ? "평" : leapMonth)
                .build();
    }

    //------------------------------------------
    // XML 요소 텍스트 추출 헬퍼
    //------------------------------------------
    private String getTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String content = nodeList.item(0).getTextContent();
            return content != null ? content.trim() : "";
        }
        return "";
    }
}
