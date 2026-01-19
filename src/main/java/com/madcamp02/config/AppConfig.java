package com.madcamp02.config;

//======================================
// AppConfig - 애플리케이션 공통 설정
//======================================
// 애플리케이션 전반에서 사용되는 공통 Bean들을 정의하는 설정 클래스
//
// 등록된 Bean:
//   - RestTemplate: 외부 API 호출용 HTTP 클라이언트
//     → 사용처: AuthService.kakaoLogin() (Kakao 사용자 정보 조회)
//     → 사용처: FinnhubClient (주식 시세 조회)
//
// Bean이란?
//   - 스프링이 관리하는 객체
//   - @Bean으로 등록하면 애플리케이션 전체에서 주입받아 사용 가능
//   - 싱글톤으로 관리되어 메모리 효율적
//======================================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration  // 이 클래스가 Bean 설정 파일임을 스프링에게 알림
public class AppConfig {

    /**
     * RestTemplate Bean 등록
     * 
     * RestTemplate이란?
     *   - 스프링에서 제공하는 HTTP 클라이언트
     *   - 외부 API에 GET, POST 등 HTTP 요청을 보내고 응답을 받을 수 있음
     *   - 동기(Synchronous) 방식으로 동작
     * 
     * 사용 예시:
     *   - Kakao API 호출: https://kapi.kakao.com/v2/user/me
     *   - Finnhub API 호출: https://finnhub.io/api/v1/quote
     * 
     * 주의사항:
     *   - RestTemplate은 스프링 5.0부터 deprecated 예고 (WebClient 권장)
     *   - 하지만 간단한 동기 호출에는 여전히 많이 사용됨
     *   - 비동기가 필요하면 WebClient로 교체 고려
     * 
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ObjectMapper Bean 등록
     * 
     * Jackson ObjectMapper를 명시적으로 등록하여
     * TradePriceBroadcastService 등에서 주입받아 사용
     * 
     * @return ObjectMapper 인스턴스
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
