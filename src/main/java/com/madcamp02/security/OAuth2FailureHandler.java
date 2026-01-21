package com.madcamp02.security;

//======================================
// OAuth2FailureHandler - OAuth2 로그인 실패 핸들러
//======================================
// OAuth2 로그인(Google/Kakao) 실패 후 처리를 담당하는 핸들러
//
// 백엔드 주도 OAuth2 흐름에서 실패 시:
//   1. 사용자가 /oauth2/authorization/kakao (또는 google) 접속
//   2. 카카오/구글 로그인 페이지로 리다이렉트
//   3. 로그인 실패 또는 사용자 거부 → 이 핸들러 호출
//   4. 프론트엔드로 리다이렉트 (에러 쿼리 파라미터 포함)
//
// 처리 과정:
//   1. 인증 실패 예외 정보 로깅
//   2. 프론트엔드로 리다이렉트 (에러 쿼리 파라미터 포함)
//======================================

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    // 프론트엔드 리다이렉트 URL (로그인 실패 후 이동할 페이지)
    @Value("${app.oauth2.redirect-uri:http://madcampstock.duckdns.org/oauth/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        log.error("OAuth2 로그인 실패 - 에러: {}", exception.getMessage(), exception);

        // 프론트엔드로 리다이렉트 (에러 쿼리 파라미터 포함)
        // URL: http://madcampstock.duckdns.org/oauth/callback?error=auth_failed
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "auth_failed")
                .build().toUriString();

        log.info("프론트엔드로 리다이렉트 (에러) - URL: {}", targetUrl);

        response.sendRedirect(targetUrl);
    }
}
