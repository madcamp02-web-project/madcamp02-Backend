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
//   2. 토큰 갱신 (Refresh Token 검증 → 새 토큰 발급)
//   3. 로그아웃 (Redis에서 Refresh Token 삭제)
//
// 의존 관계:
//   - UserRepository: 사용자 조회/저장
//   - WalletRepository: 신규 사용자 지갑 생성
//   - JwtTokenProvider: JWT 생성/검증
//   - RedisTemplate: Refresh Token 저장/조회/삭제
//
// 트랜잭션:
//   - login(): 사용자 생성과 지갑 생성이 함께 처리 (원자성 보장)
//   - refresh(): 읽기 전용 트랜잭션
//======================================

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.request.LoginRequest;
import com.madcamp02.dto.response.AuthResponse;
import com.madcamp02.exception.AuthException;
import com.madcamp02.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
    private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰을 만들고, 검증하고, 해석하는 도구
    private final RedisTemplate<String, String> redisTemplate; // Refresh Token을 저장할 메모리 DB(Redis) 도구

    //==========================================
    // 설정값 주입 (application.yml에서 가져옴)
    //==========================================

    // Google OAuth2 클라이언트 ID (토큰 검증용)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // Refresh Token 만료 시간 (밀리초)
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

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
                    .birthDate(null)  // 온보딩에서 입력
                    .build();
            user = userRepository.save(user);

            // 지갑 생성 (초기 자금 $10,000)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .build();
            walletRepository.save(wallet); // DB에 지갑 저장 (INSERT)
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
    // Private Helper Methods
    //==========================================

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
}
