package com.madcamp02.security;

//Spring Security 환경에서 JWT(JSON Web Token)를 사용하여
//사용자의 요청을 인증(Authentication)하는 핵심 필터 클래스

//클라이언트가 보낸 요청의 헤더(Header)를 가로채서 유효한 토큰이 있는지 확인하고,
//유효하다면 해당 사용자를 인증된 상태로 만들어주는 역할을 함

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor //JwtTokenProvider에서 설명함
//final이 붙은 필드(jwtTokenProvider)에 대한 생성자를 롬복(Lombok)이 자동으로 만들어 의존성을 주입(DI)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    //OncePerRequestFilter 상속: OncePerRequestFilter는 HTTP 요청 하나당 딱 한 번만 실행되는 것을 보장하는 필터로 이걸 상속해서 사용한다

    private final JwtTokenProvider jwtTokenProvider;
    //JWT 토큰을 생성, 검증, 파싱하는 로직이 담긴 별도의 컴포넌트


    //이게 핵심
    //스프링 시큐리티에서 JWT로 사용자 필터링을 직접적으로 수행하는 메서드임
    //이녀석이 JWT로 사용자들을 필터링함.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Request Header에서 JWT 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);
        //이걸 실행할 경우 주로 Authorization 헤더에서 Bearer로 시작하는 토큰 문자열을 가져오게 됨

        String requestPath = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        
        log.debug("JWT 필터 실행 - 경로: {}, Authorization 헤더 존재: {}", requestPath, authHeader != null);

        // 2. 토큰 유효성 검사
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            //토큰이 비어있지 않은지(hasText), 그리고 위조되거나 만료되지 않았는지(validateToken) 확인

            // 3. 토큰 타입 확인 (Access Token만 허용)
            String tokenType = jwtTokenProvider.getTokenType(token);
            //이 필터는 오직 Access Token을 가진 요청만 인증 처리
            //(Refresh Token을 통한 재발급은 별도 로직으로 처리됨을 암시)

            log.debug("토큰 타입 확인 - Type: {}", tokenType);

            if ("ACCESS".equals(tokenType)) {
                // 4. 토큰에서 Authentication 객체 가져오기
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // 5. SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                //즉, 여기서는 토큰에서 사용자 정보(ID, 권한 등)를 꺼내 Authentication 객체를 만들고,
                //SecurityContextHolder에 저장해서 Spring Security가 이후의 로직(Controller 등)에서 "이 사용자는 로그인했다"라고 인식

                log.debug("Security Context에 인증 정보 저장: {}", authentication.getName());
            } else {
                log.warn("Access Token이 아닌 토큰 타입: {}", tokenType);
            }
        } else {
            if (!StringUtils.hasText(token)) {
                log.debug("요청에 토큰이 없습니다 - 경로: {}", requestPath);
            } else {
                log.warn("유효하지 않은 토큰 - 경로: {}", requestPath);
            }
        }

        filterChain.doFilter(request, response);
        //인증 작업이 끝나면 다음 필터로 요청을 넘김(이렇게 계속 연속적으로 필터를 거는 건 "필터 체인"이라고 함)
    }


//예외 처리 로직: shouldNotFilter
//여기서는 이 필터를 적용하지 말아야 할 경로를 정의
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 인증이 필요 없는 경로는 필터 스킵
        return path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/refresh") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/ws");
    }
    /*
    /api/v1/auth/login: 로그인 요청 시에는 아직 토큰이 없으므로 검사할 필요가 없음

    /api/v1/auth/refresh: 토큰 재발급 요청은 만료된 Access Token이나 Refresh Token을 보내므로,
                          일반적인 Access Token 검증 로직을 타면 안 됨

    /swagger-ui, /v3/api-docs: API 문서 접근은 인증 없이 가능해야 하니까!

    /ws: 웹소켓 연결 요청에 대한 예외 처리
     */
}