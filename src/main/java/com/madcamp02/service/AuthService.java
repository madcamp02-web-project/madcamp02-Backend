package com.madcamp02.service;

//======================================
// AuthService - 인증 서비스
//======================================
// 인증과 관련된 모든 비즈니스 로직을 처리하는 핵심 서비스 클래스
//  "이 앱의 출입국 관리소" 역할을 하는 핵심 클래스입니다.
//  사용자가 앱에 들어올 때(로그인), 머무를 때(토큰 갱신), 나갈 때(로그아웃) 필요한 모든 보안 절차를 담당
//
// 담당 기능:
//   1. Google OAuth2 로그인 (ID Token 검증 → 회원가입/로그인 → JWT 발급)
//   2. Kakao OAuth2 로그인 (Access Token 검증 → 회원가입/로그인 → JWT 발급)
//   3. 일반 회원가입 (이메일/비밀번호 → 비밀번호 암호화 후 저장 → JWT 발급)
//   4. 일반 로그인 (이메일/비밀번호 → 비밀번호 검증 → JWT 발급)
//   5. 토큰 갱신 (Refresh Token 검증 → 새 토큰 발급)
//   6. 로그아웃 (Redis에서 Refresh Token 삭제)
//
// 의존 관계:
//   - UserRepository: 사용자 조회/저장
//   - WalletRepository: 신규 사용자 지갑 생성
//   - JwtTokenProvider: JWT 생성/검증
//   - RedisTemplate: Refresh Token 저장/조회/삭제
//   - PasswordEncoder: 비밀번호 암호화/검증 (BCrypt)
//   - RestTemplate: 외부 API 호출 (Kakao 사용자 정보 조회)
//
// 트랜잭션:
//   - login(): 사용자 생성과 지갑 생성이 함께 처리 (원자성 보장)
//   - refresh(): 읽기 전용 트랜잭션
//======================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.domain.watchlist.Watchlist;
import com.madcamp02.domain.watchlist.WatchlistRepository;
import com.madcamp02.dto.request.EmailLoginRequest;
import com.madcamp02.dto.request.LoginRequest;
import com.madcamp02.dto.request.SignupRequest;
import com.madcamp02.dto.response.AuthResponse;
import com.madcamp02.exception.AuthException;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

//어노테이션에 대해서...
/*
코드(클래스, 메서드, 변수) 앞에 @ 기호를 붙여서 사용하는데, 이 자체로는 프로그램 로직에 직접적인 영향을 주지 않지만,
이 코드를 컴파일하거나 실행하는 도구(컴파일러, 스프링 프레임워크 등)에게 "이 코드는 이런 역할이야!"라고 정보를 전달

1. 컴파일러에게 알려주기 (문법 체크)
        가장 기본적인 역할입니다. 컴파일러에게 "나 이거 의도한 거니까 확인해 줘"라고 말하는 것입니다.

        @Override:
        의미: "이 메서드는 부모 클래스에 있는 걸 덮어쓰기(재정의)한 거야."
        효과: 만약 메서드 이름에 오타가 나서 덮어쓰기가 제대로 안 되면, 컴파일러가 빨간 줄(에러)을 띄워 알려줍니다. (개발자의 실수를 방지)

2. 코드를 대신 짜주기 (빌드 시점 자동화)
        코드를 컴파일할 때, 어노테이션을 보고 보이지 않는 코드를 자동으로 생성해 줍니다. **Lombok(롬복)**이 대표적입니다.

        @Getter:
        의미: "이 변수들의 get...() 메서드를 네가 알아서 만들어 줘."
        효과: 코드에는 없지만, 실제 실행될 때는 getUserId(), getEmail() 같은 메서드가 자동으로 생겨 있습니다.

@RequiredArgsConstructor:
        의미: "final이 붙은 필드들을 초기화하는 생성자를 자동으로 만들어 줘."

3. 스프링 프레임워크에게 알려주기 (런타임 처리)
        스프링 개발에서 가장 중요한 역할입니다. 스프링이 실행될 때 이 라벨을 보고 특별한 대우를 해줍니다.

        @Service:
        의미: "스프링아, 이 클래스는 비즈니스 로직을 담당하는 서비스야."
        효과: 스프링이 시작될 때 이 클래스를 메모리에 띄우고(Bean 등록), 관리해 줍니다.

        @Transactional:
        의미: "이 메서드는 트랜잭션으로 묶어줘."
        효과: 메서드 시작할 때 트랜잭션을 열고, 끝날 때 커밋(저장)하거나 에러 나면 롤백(취소)하는 복잡한 코드를 스프링이 몰래 앞뒤로 끼워 넣어 줍니다.
*/


@Slf4j //로그 찍는 기능(log)을 추가하는 어노테이션
@Service // 스프링에게 "이건 비즈니스 로직을 담당하는 핵심 부품(Bean)이야"라고 알려주는 어노테이션
@RequiredArgsConstructor //Lombak을 사용하겠다는 어노테이션
public class AuthService {

    // 의존성 주입(DI) (생성자 주입 방식) = 이 서비스가 일하기 위해 필요한 다른 도구들)
    //@RequiredArgsConstructor를 통해 userRepository 등 4개의 도구를 스프링으로부터 안전하게 받아옴

    private final UserRepository userRepository;      // DB에서 유저 정보를 찾거나 저장할 때 사용
    private final WalletRepository walletRepository;  // 회원가입 시 지갑을 만들어주기 위해 사용
    private final WatchlistRepository watchlistRepository; // 기본 관심종목을 저장하기 위해 사용
    private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰을 만들고, 검증하고, 해석하는 도구
    private final RedisTemplate<String, String> redisTemplate; // Refresh Token을 저장할 메모리 DB(Redis) 도구
    private final PasswordEncoder passwordEncoder;   // 비밀번호를 안전하게 암호화(BCrypt)하고, 검증하는 도구
    private final RestTemplate restTemplate;         // 외부 API(Kakao 등)에 HTTP 요청을 보내는 도구
    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // 임의 이메일/닉네임 생성용

    //==========================================
    // 설정값 주입 (application.yml에서 가져옴)
    //==========================================

    // Google OAuth2 클라이언트 ID (토큰 검증용)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // Refresh Token 만료 시간 (밀리초)
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    // Kakao 사용자 정보 조회 API URL
    // Access Token을 헤더에 담아 GET 요청하면 사용자 정보(이메일, 닉네임 등)를 JSON으로 반환
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    //==========================================
    // 로그인 처리
    //==========================================
    /**
     * Google OAuth2 로그인
     * 
     * 처리 과정:
     *   1. Google ID Token 검증 (위변조 확인)
     *   2. 이메일로 기존 사용자 조회
     *   3. 신규 사용자면 자동 회원가입 + 지갑 생성
     *   4. JWT Access Token + Refresh Token 발급
     *   5. Refresh Token을 Redis에 저장
     *   6. 응답 DTO 반환
     * 
     * @param request 로그인 요청 (provider, idToken)
     * @return 인증 응답 (토큰 + 사용자 정보)
     */
    @Transactional //트렌젝션으로 건다.
    // 이 메서드 안의 모든 DB 작업(User저장, Wallet저장)은 하나로 묶음.
    // 하나라도 실패하면 모두 취소(Rollback)
    public AuthResponse login(LoginRequest request) {
        // [1단계: 구글 ID Token 검증]
        // 클라이언트가 보낸 idToken이 위조되지 않았는지 구글 서버 로직을 통해 확인합니다.
        // verifyGoogleToken 메서드는 아래쪽에 따로 정의되어 있습니다.
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // [2단계: 유저 조회]
        // DB에서 이메일로 유저를 찾습니다. 없으면 null을 반환합니다.
        User user = userRepository.findByEmail(email)
                .orElse(null);

        boolean isNewUser = (user == null);

        // [3단계: 신규 회원일 경우 처리 (회원가입 + 지갑생성)]
        if (isNewUser) {
            //------------------------------------------
            // 신규 사용자 처리
            //------------------------------------------
            // - 기본 정보로 User 엔티티 생성
            // - birthDate는 null (온보딩에서 입력)
            // - 초기 자금 $10,000로 Wallet 생성


            // User 객체를 생성(Builder 패턴에서 만들어둔거 사용)(이렇게 쉽게 사용하라고 domain파일 내부의 User.java에 객체의 메서드를 만든거다)
            user = User.builder()
                    .email(email)
                    .nickname(name != null ? name : email.split("@")[0])
                    .provider("GOOGLE")
                    .birthDate(LocalDate.of(2000, 1, 1))  // 임시 기본값 (온보딩에서 수정)
                    .build();
            user = userRepository.save(user);

            // 지갑 생성 (초기 자금 $10,000)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .build();
            walletRepository.save(wallet); // DB에 지갑 저장 (INSERT)
            // 기본 관심종목 설정
            addDefaultWatchlist(user);
        }

        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_USER_NOT_FOUND);
        }

        // [4단계: 우리 앱 전용 토큰(JWT) 발급]
        // Access Token: API 요청용 짧은 수명 토큰
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
        // Refresh Token: 재로그인용 긴 수명 토큰
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [5단계: Redis에 Refresh Token 저장]
        // 나중에 토큰 갱신할 때 비교하기 위해 Redis에 저장

        // 키 형식: "refresh:{userId}"
        // 만료 시간: 7일
        // 일단 이렇게 설정해둠 이거 관련은 yml 고쳐라
        saveRefreshToken(user.getUserId(), refreshToken);

        // [6단계: 결과 반환]
        // 프론트엔드에 내려줄 데이터를 AuthResponse 객체에 담아서 리턴합니다.
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)  // 1시간 (초 단위)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .sajuElement(user.getSajuElement())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser) // 프론트가 온보딩 화면을 띄울지 말지 결정하는 플래그
                .build();
    }

    //==========================================
    // 토큰 갱신 처리
    //==========================================
    /**
     * Refresh Token으로 새 토큰 발급
     * 
     * 보안 전략 - Token Rotation:
     *   - Refresh Token 사용 시 새 Refresh Token도 함께 발급
     *   - 기존 Refresh Token은 폐기
     *   - 탈취된 토큰 재사용 방지
     * 
     * @param refreshToken 갱신용 토큰
     * @return 새 토큰 + 사용자 정보
     */
    @Transactional(readOnly = true) // 여기서는 데이터를 조회만 하므로 readOnly=true로 성능을 높임
    //이런식으로 @Transactional(readOnly = true) 적어주면 select 연산만 가능하게 할 수 있음
    public AuthResponse refresh(String refreshToken) {

        // [1단계: 토큰 자체의 유효성 검사]
        // 토큰이 깨졌거나, 날짜가 지났는지 라이브러리로 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException("유효하지 않은 Refresh Token");
        }

        // [2단계: 토큰 용도 확인]
        // 실수로 Access Token을 넣어서 갱신 요청을 했을 경우를 차단
        // Access Token으로 갱신 시도 방지
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new AuthException("Refresh Token이 아님");
        }

        // [3단계: 저장된 토큰과 비교 (보안 핵심)]
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        //------------------------------------------
        // 4단계: Redis 저장 토큰과 비교
        // Redis에 저장해둔 토큰을 가져옴
        //------------------------------------------
        // 불일치 시 = 탈취된 토큰이거나 이미 사용된 토큰
        // Redis에 토큰이 없거나(이미 로그아웃됨), 클라이언트가 보낸 것과 다르면(탈취 의심) 에러!
        String savedToken = redisTemplate.opsForValue().get("refresh:" + userId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new AuthException("Refresh Token 불일치");
        }

        // [4단계: 유저 확인]
        // 그 사이 회원 탈퇴했을 수도 있으니 DB 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없음"));

        //------------------------------------------
        // 5단계: 새 Access Token 생성
        // 새 토큰 발급 (Rotation)
        //------------------------------------------
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );

        //------------------------------------------
        // 7단계: 새 Refresh Token 생성 (Rotation)
        //------------------------------------------
        // ★ 중요: Refresh Token도 새로 발급. (RTR 방식)
        // 한 번 쓴 Refresh Token은 버리고 새걸 줘서 해킹 당했을 때 피해를 줄이기 위함
        // 기존 토큰 폐기, 새 토큰 저장
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        saveRefreshToken(userId, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(3600)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .sajuElement(user.getSajuElement())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(false)
                .build();
    }

    //==========================================
    // 로그아웃 처리
    //==========================================
    /**
     * 로그아웃
     * 
     * Redis에서 Refresh Token 삭제
     * → 해당 토큰으로 더 이상 갱신 불가
     * 
     * 주의: Access Token은 만료될 때까지 여전히 유효
     *       (Blacklist 구현 시 즉시 차단 가능)
     * 
     * @param userId 로그아웃할 사용자 ID
     */
    // Redis에서 해당 유저의 Refresh Token 키를 아예 삭제
    // 이러면 나중에 refresh() 메서드를 호출해도 savedToken이 null이라서 실패
    public void logout(Long userId) {
        redisTemplate.delete("refresh:" + userId);
    }

    //==========================================
    // 일반 회원가입 처리
    //==========================================
    /**
     * 이메일/비밀번호 회원가입
     * 
     * 처리 과정:
     *   1. 이메일 중복 확인 (이미 가입된 이메일인지)
     *   2. 비밀번호 암호화 (BCrypt)
     *   3. User 엔티티 생성 및 저장
     *   4. Wallet 생성 (초기 자금 $10,000)
     *   5. JWT Access Token + Refresh Token 발급
     *   6. Refresh Token을 Redis에 저장
     *   7. 응답 DTO 반환
     * 
     * @param request 회원가입 요청 (email, password, nickname)
     * @return 인증 응답 (토큰 + 사용자 정보)
     * @throws AuthException 이메일 중복 시 AUTH_008
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // [1단계: 이메일 중복 확인]
        // 이미 같은 이메일로 가입된 사용자가 있는지 확인
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException(ErrorCode.AUTH_EMAIL_DUPLICATION);
        }

        // [2단계: 비밀번호 암호화]
        // BCrypt 알고리즘으로 암호화 (복호화 불가능, 단방향 해시)
        // 예: "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMy..."
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // [3단계: User 엔티티 생성]
        // provider는 "LOCAL" (일반 가입), password는 암호화된 값
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)  // 암호화된 비밀번호 저장
                .nickname(request.getNickname())
                .provider("LOCAL")  // 일반 회원가입
                .build();
        user = userRepository.save(user);

        // [4단계: 지갑 생성 (초기 자금 $10,000)]
        Wallet wallet = Wallet.builder()
                .user(user)
                .build();
        walletRepository.save(wallet);

        // [5단계: JWT 토큰 발급]
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [6단계: Redis에 Refresh Token 저장]
        saveRefreshToken(user.getUserId(), refreshToken);

        // [7단계: 응답 반환]
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)  // 1시간 (초 단위)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .isNewUser(true)  // 신규 사용자
                .build();
    }

    //==========================================
    // 일반 로그인 처리 (이메일/비밀번호)
    //==========================================
    /**
     * 이메일/비밀번호 로그인
     * 
     * 처리 과정:
     *   1. 이메일로 사용자 조회
     *   2. 비밀번호 검증 (BCrypt matches)
     *   3. JWT Access Token + Refresh Token 발급
     *   4. Refresh Token을 Redis에 저장
     *   5. 응답 DTO 반환
     * 
     * @param request 로그인 요청 (email, password)
     * @return 인증 응답 (토큰 + 사용자 정보)
     * @throws AuthException 사용자 없음(AUTH_004) 또는 비밀번호 불일치(AUTH_007)
     */
    @Transactional(readOnly = true)
    public AuthResponse emailLogin(EmailLoginRequest request) {
        // [1단계: 사용자 조회]
        // 이메일로 사용자를 찾음, 없으면 AUTH_004 에러
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        // [2단계: 비밀번호 검증]
        // BCrypt의 matches() 메서드로 평문 비밀번호와 암호화된 비밀번호 비교
        // 주의: OAuth 사용자는 password가 null일 수 있음
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.AUTH_PASSWORD_MISMATCH);
        }

        // [3단계: JWT 토큰 발급]
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [4단계: Redis에 Refresh Token 저장]
        saveRefreshToken(user.getUserId(), refreshToken);

        // [5단계: 응답 반환]
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .sajuElement(user.getSajuElement())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(false)
                .build();
    }

    //==========================================
    // Kakao OAuth2 로그인 처리
    //==========================================
    /**
     * Kakao OAuth2 로그인
     * 
     * 처리 과정:
     *   1. Kakao Access Token으로 사용자 정보 API 호출
     *   2. 응답에서 이메일, 닉네임 추출
     *   3. 이메일로 기존 사용자 조회
     *   4. 신규 사용자면 자동 회원가입 + 지갑 생성
     *   5. JWT Access Token + Refresh Token 발급
     *   6. Refresh Token을 Redis에 저장
     *   7. 응답 DTO 반환
     * 
     * Google OAuth와의 차이:
     *   - Google: ID Token (JWT)을 직접 검증
     *   - Kakao: Access Token으로 Kakao API 호출하여 사용자 정보 획득
     * 
     * @param accessToken Kakao에서 발급받은 Access Token
     * @return 인증 응답 (토큰 + 사용자 정보)
     * @throws AuthException Kakao 토큰 검증 실패 시 AUTH_006
     */
    @Transactional
    public AuthResponse kakaoLogin(String accessToken) {
        // [1단계: Kakao 사용자 정보 조회]
        // Kakao API에 Access Token을 보내서 사용자 정보를 받아옴
        JsonNode kakaoUserInfo = getKakaoUserInfo(accessToken);

        // [2단계: 사용자 정보 추출]
        // Kakao 응답 JSON에서 이메일과 닉네임 추출
        // 응답 구조: { "kakao_account": { "email": "...", "profile": { "nickname": "..." } } }
        String email = kakaoUserInfo.path("kakao_account").path("email").asText(null);
        String nickname = kakaoUserInfo.path("kakao_account").path("profile").path("nickname").asText(null);

        // 이메일을 요청하지 않으므로 응답이 없을 수 있다 → 임의 이메일 생성
        if (email == null || email.isBlank()) {
            email = generateKakaoEmail();
        }
        // 닉네임은 필수 동의로 받아오되, 누락 시 임의 닉네임 생성
        if (nickname == null || nickname.isBlank()) {
            nickname = generateKakaoNickname();
        }

        // [3단계: 기존 사용자 조회]
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = (user == null);

        // [4단계: 신규 회원가입 처리]
        if (isNewUser) {
            // 동일 이메일이 존재하면 충돌 방지를 위해 새 임의 이메일 재생성
            while (userRepository.findByEmail(email).isPresent()) {
                email = generateKakaoEmail();
            }

            user = User.builder()
                    .email(email)
                    .nickname(nickname)
                    .provider("KAKAO")  // Kakao로 가입
                    .build();
            user = userRepository.save(user);

            // 지갑 생성 (초기 자금 $10,000)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .build();
            walletRepository.save(wallet);

            // 기본 관심종목 설정
            addDefaultWatchlist(user);
        }

        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_USER_NOT_FOUND);
        }

        // [5단계: JWT 토큰 발급]
        String jwtAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [6단계: Redis에 Refresh Token 저장]
        saveRefreshToken(user.getUserId(), refreshToken);

        // [7단계: 응답 반환]
        return AuthResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .sajuElement(user.getSajuElement())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser)
                .build();
    }

    //==========================================
    // Private Helper Methods
    //==========================================

    /**
     * 신규 가입 사용자에게 기본 관심종목을 부여한다.
     */
    private void addDefaultWatchlist(User user) {
        List<String> defaultTickers = List.of("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA");

        for (String ticker : defaultTickers) {
            // 중복 방지: 이미 존재하면 건너뜀
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

    /**
     * Google ID Token 검증
     * 
     * Google 공개키로 토큰 서명 검증
     * 
     * 검증 항목:
     *   - 서명 유효성 (위변조 여부)
     *   - 발급자 (accounts.google.com)
     *   - 대상 (우리 앱의 Client ID)
     *   - 만료 시간
     * 
     * @param idToken Google에서 발급받은 ID Token
     * @return 토큰 Payload (이메일, 이름 등 포함)
     */
    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            // 구글 라이브러리를 사용해 검증기(Verifier)를 세팅
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 실제 검증 수행 (서명 확인, 만료시간 확인 등)
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new AuthException("유효하지 않은 Google ID Token");
            }

            return googleIdToken.getPayload(); // 유저 정보가 담긴 본문(Payload) 반환
        } catch (Exception e) {
            log.error("Google 토큰 검증 실패", e); // 에러 로그 남김
            throw new AuthException("Google 토큰 검증 실패");
        }
    }

    /**
     * Refresh Token Redis 저장
     * 
     * 키 형식: "refresh:{userId}"
     * 값: Refresh Token 문자열
     * 만료: refreshTokenExpiration (7일)
     * 
     * @param userId 사용자 ID
     * @param refreshToken 저장할 Refresh Token
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        // Redis에 데이터를 저장
        // key: "refresh:1" (유저ID 1번)
        // value: (토큰 값)
        // timeout: refreshTokenExpiration (예: 7일) -> 7일 지나면 Redis가 알아서 삭제
        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                refreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Kakao 사용자 정보 조회
     * 
     * Kakao API 호출하여 사용자 정보 획득
     * 
     * API 정보:
     *   - URL: https://kapi.kakao.com/v2/user/me
     *   - Method: GET
     *   - Header: Authorization: Bearer {access_token}
     *   - Response: JSON (id, kakao_account, properties 등)
     * 
     * @param accessToken Kakao에서 발급받은 Access Token
     * @return 사용자 정보 JSON (JsonNode)
     * @throws AuthException API 호출 실패 시 AUTH_006
     */
    private JsonNode getKakaoUserInfo(String accessToken) {
        try {
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);  // Authorization: Bearer {access_token}
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // HTTP 요청 엔티티 생성
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Kakao API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // 응답 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("Kakao 사용자 정보 조회 실패", e);
            throw new AuthException(ErrorCode.AUTH_KAKAO_TOKEN_INVALID);
        }
    }

    /**
     * 카카오 로그인용 임의 이메일을 생성한다.
     * 규칙: kakao-{timestamp}-{random}@auth.madcamp02.local
     */
    private String generateKakaoEmail() {
        long timestamp = System.currentTimeMillis();
        int randomNumber = SECURE_RANDOM.nextInt(1_000_000); // 0~999999
        return String.format("kakao-%d-%06d@auth.madcamp02.local", timestamp, randomNumber);
    }

    /**
     * 카카오 로그인 시 닉네임이 없을 때 사용할 임의 닉네임을 생성한다.
     */
    private String generateKakaoNickname() {
        int randomNumber = SECURE_RANDOM.nextInt(1_000_000); // 0~999999
        return String.format("kakao-user-%06d", randomNumber);
    }
}
