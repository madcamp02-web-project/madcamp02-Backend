package com.madcamp02.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig (STOMP)
 *
 * - STOMP Endpoint: /ws-stomp으로 내가 문서에 작성해둠(이 엔드포인트로 하기로 함!)
 * - Subscriptions:
 *   - /topic/stock.indices
 *   - /topic/stock.ticker.{ticker}
 *   - /user/queue/trade
 *
 * - 엔드포인트/브로커 prefix "고정"이 목적
 * - 인증(JWT) 기반의 user destination 보안 강화는 Phase 6에서 추가
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                // CORS/WebSocket Origin 허용 목록
                // - 도메인 운영 환경: http/https 둘 다 허용
                // - 로컬 개발 환경: localhost / 127.0.0.1 전 포트 허용
                //   (Docker로 백엔드만 띄우고, 프론트는 로컬에서 돌릴 때 필요)
                .setAllowedOriginPatterns(
                        "http://madcampstock.duckdns.org",
                        "https://madcampstock.duckdns.org",
                        "http://madcampbackend.duckdns.org",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                // 어려운 말: 프론트 환경에 따라 SockJS fallback이 필요할 수 있어 기본 활성화
                // 모든 환경이 웹소켓을 지원하지는 않기 때문에 .withSockJS()을 사용해서 총 3가지 통신을 시도함
                /*
                1. WebSocket: 가장 먼저 웹소켓 연결 시도.

                2. HTTP Streaming: 안 되면 데이터를 끊기지 않게 계속 스트리밍하는 방식 시도.

                3. HTTP Long Polling: 이것도 안 되면 데이터를 짧게 여러 번 요청해서 가져오는 방식으로 최종 후퇴.(일종의 최후의 수단임)
                 */
                /*
                이걸 설정할 경우
                내부에선 이렇게 동작한다
                서버 측: Spring이 /ws-stomp/info 같은 추가적인 엔드포인트를 열어서 클라이언트와 "어떤 통신 방식(Transport)을 쓸 수 있는지" 협상
                클라이언트(프론트) 측: 브라우저 기본 객체인 new WebSocket('...') 대신, sockjs-client 라이브러리를 사용해서 new SockJS('...')로 연결

                 (실무 팁)
                프론트엔드 라이브러리 필수: 서버에서 .withSockJS()를 켰다면,
                프론트에서도 반드시 SockJS 클라이언트를 써야 통신이 됨 (그냥 웹소켓 객체로 붙으려고 하면 연결 에러가 난다)
                혹시 안되면 이거 꼭 참고하라고 하기

                URL 패턴: SockJS를 사용하면 내부적으로 URL 뒤에 ID값이나 세션 정보가 붙기 때문에
                프록시(Nginx 등) 설정 시 경로 처리에 유의
                 */
                .withSockJS();
    }




    //Spring의 STOMP(Simple Text Oriented Messaging Protocol)
    //메시지가 어디로 가야 할지 길을 정해주는 이정표
    //일단 웹소켓 사용할 위치(엔드포인트=파일구조)를 고정한다고 생각하면 된다
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client -> Server (send) prefix (일반적으로 /app 사용) --> 클라이언트가 서버의 특정 처리 로직(@MessageMapping)으로 메시지를 보낼 때 붙이는 접두사
        //프론트에서 전송 목적지를 /app/chat/send로 보내면, 서버의 @MessageMapping("/chat/send")가 붙은 메서드가 이 메시지를 낚아채서 처리
        registry.setApplicationDestinationPrefixes("/app");

        // Server -> Client (subscribe) prefixes --> 메시지 브로커(전달자)를 활성화하고, 브로커가 관리할 주소 범위를 지정
        //서버가 /topic/room1로 메시지를 던지면, 이 주소를 구독(Subscribe)하고 있는 모든 클라이언트에게 메시지가 전달
        // /topic: 보통 1:N (Pub/Sub) 방식
        // /queue: 보통 1:1 방식
        registry.enableSimpleBroker("/topic", "/queue");

        // User-specific destination prefix --> 특정 세션이나 특정 사용자에게만 메시지를 보낼 때 사용하는 특수 접두사
        //여러 사용자가 접속해 있을 때, "나에게만 오는 알림"을 처리하기 위해 필요합니다. 내부적으로 Spring Security와 연동되어 해당 사용자의 세션을 찾아 메시지를 보내줌
        registry.setUserDestinationPrefix("/user");
    }
}


