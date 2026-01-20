package com.madcamp02.security;

//======================================
// OAuth2SuccessHandler - OAuth2 로그인 성공 핸들러
//======================================
// OAuth2 로그인(Google/Kakao) 성공 후 처리를 담당하는 핸들러
//
// 백엔드 주도 OAuth2 흐름:
//   1. 사용자가 /oauth2/authorization/kakao (또는 google) 접속
//   2. 카카오/구글 로그인 페이지로 리다이렉트
//   3. 로그인 성공 → /login/oauth2/code/kakao 로 Authorization Code 전달
//   4. Spring Security가 자동으로 Access Token 교환 + 사용자 정보 조회
//   5. 이 핸들러에서 JWT 발급 + 프론트엔드로 리다이렉트
//
// 처리 과정:
//   1. OAuth2User에서 이메일/닉네임 추출
//   2. DB에서 사용자 조회 (없으면 자동 생성)
//   3. JWT Access Token + Refresh Token 발급
//   4. Refresh Token을 Redis에 저장
//   5. 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
//======================================

import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.domain.watchlist.Watchlist;
import com.madcamp02.domain.watchlist.WatchlistRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final WatchlistRepository watchlistRepository;

    // 프론트엔드 리다이렉트 URL (로그인 성공 후 이동할 페이지)
    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String frontendRedirectUri;

    // Refresh Token 만료 시간 (밀리초)
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // google 또는 kakao

        log.info("OAuth2 로그인 성공 - Provider: {}", registrationId);

        // [1단계: OAuth2 제공자별 사용자 정보 추출]
        String email;
        String nickname;
        String provider = registrationId.toUpperCase(); // GOOGLE 또는 KAKAO

        if ("kakao".equals(registrationId)) {
            // Kakao 사용자 정보 구조:
            // { "id": 123, "kakao_account": { "email": "...", "profile": { "nickname": "..." } } }
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

            email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            nickname = profile != null ? (String) profile.get("nickname") : null;
        } else {
            // Google 사용자 정보 구조:
            // { "email": "...", "name": "...", "picture": "..." }
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
        }

        log.info("OAuth2 사용자 정보 - Email: {}, Nickname: {}, Provider: {}", email, nickname, provider);

        // [2단계: 사용자 조회 또는 생성]
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            // 신규 사용자 생성
            user = User.builder()
                    .email(email)
                    .nickname(nickname != null ? nickname : email.split("@")[0])
                    .provider(provider)
                    .build();
            user = userRepository.save(user);

            // 지갑 생성 (초기 자금 $10,000)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .build();
            walletRepository.save(wallet);

            // 기본 관심종목 설정
            addDefaultWatchlist(user);

            log.info("신규 사용자 생성 - UserId: {}, Email: {}", user.getUserId(), email);
        }

        // [3단계: JWT 토큰 발급]
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [4단계: Redis에 Refresh Token 저장]
        redisTemplate.opsForValue().set(
                "refresh:" + user.getUserId(),
                refreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        // [5단계: 프론트엔드로 리다이렉트 (토큰 전달)]
        // URL: http://localhost:3000/oauth/callback?accessToken=xxx&refreshToken=xxx&isNewUser=true
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isNewUser", isNewUser)
                .build().toUriString();

        log.info("프론트엔드로 리다이렉트 - URL: {}", targetUrl);
        log.info("발급된 Access Token (처음 20자): {}...", accessToken != null && accessToken.length() > 20 ? accessToken.substring(0, 20) : accessToken);
        log.info("발급된 Refresh Token (처음 20자): {}...", refreshToken != null && refreshToken.length() > 20 ? refreshToken.substring(0, 20) : refreshToken);
        log.info("사용자 정보 - UserId: {}, Email: {}, Nickname: {}", user.getUserId(), user.getEmail(), user.getNickname());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * OAuth2 신규 사용자에게 기본 관심종목을 부여한다.
     */
    private void addDefaultWatchlist(User user) {
        List<String> defaultTickers = List.of("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA");

        for (String ticker : defaultTickers) {
            if (watchlistRepository.existsByUserUserIdAndTicker(user.getUserId(), ticker)) {
                continue;
            }

            Watchlist watchlist = Watchlist.builder()
                    .user(user)
                    .ticker(ticker)
                    .build();
            watchlistRepository.save(watchlist);
        }
    }
}
