package com.madcamp02.controller;

// 말 그대로 Controller 역할
// 사용자의 요청을 가장 먼저 받는 창구(Reception)
// 클라이언트의 요청을 제일 먼저 받는(http를 받는) 제일 앞 창구같은 역할

//AuthService는 실제 업무를 하고
//AuthController는 클라이언트의 요청을 접수하고, 결과물을 전달해 주는 역할


//Lombak과 @RequiredArgsConstructor에 대한 나의 이해
//"final이 붙은 친구들(AuthService)을 자동으로 연결(주입)

//내 질문: Lombak은 그냥 객체를 쓸때 생성자 내가 안만들고 알아서 만들어주면 거기의 메서드만 불러서 사용하는 거냐?
/*
맞긴 한데 예시를 보면서 더 이해를 하라고 함.

1. 내가 표현상에서 쓰는 Lombak을 이용한 코드 (소스 코드) 👀
님은 코드에 변수(재료)만 선언하고, 위에 어노테이션(지시사항)만 붙였습니다.
생성자나 get... 같은 메서드는 코드를 짜지 않았죠.

        Java
        // 롬복에게 지시: "생성자랑, Getter 다 만들어 줘"
        @RequiredArgsConstructor
        @Getter
        public class User {
            private final String name;  // 변수만 딸랑 있음
            private final int age;
        }


2. 롬복이 뒤에서 몰래 해준 일 (컴파일 시점) 👻
컴퓨터가 이 코드를 읽을 때(컴파일할 때),
롬복이 슥 나타나서 님이 안 짠 코드를 몰래 끼워 넣습니다.

        Java
        // 실제 실행되는 코드 (롬복이 만들어준 결과물)
        public class User {
            private final String name;
            private final int age;

            // 1. @RequiredArgsConstructor가 만든 생성자
            public User(String name, int age) {
                this.name = name;
                this.age = age;
            }

            // 2. @Getter가 만든 메서드들
            public String getName() {
                return this.name;
            }
            public int getAge() {
                return this.age;
            }
        }

3. 님이 사용하는 방법 🛠️
말씀하신 대로, "어? 나는 메서드 안 만들었는데?" 싶어도 그냥 불러서 쓰면 됩니다.

        Java
        // 다른 곳에서 사용할 때
        public void printUser() {
            // 생성자가 자동으로 만들어졌으니 이렇게 객체 생성 가능!
            User user = new User("철수", 20);

            // getName()을 짠 적은 없지만, 롬복이 만들어놨으니 호출 가능!
            System.out.println(user.getName());
        }


💡 우리의 Service파일에 있는 코드인 AuthService(실질적 비즈니스 코드)에서의 핵심!
//이게 뭐하는 건지 궁금하면 docs의 node vs spring을 참고하자

아까 보셨던 AuthService나 AuthController에서
@RequiredArgsConstructor를 쓴 이유는
""""""스프링한테 일 시키기 위해서""""""

        (내가 작성한 코드)
        변수 선언: private final UserRepository userRepository; (텅 빈 변수)

        (Lombak이 해주는 일)
        롬복의 마법: @RequiredArgsConstructor가
            public AuthService(UserRepository repo) { ... } 라는 생성자를 자동으로 만듦.

        (spring boot라는 코드가 수행하는 일)
        스프링의 동작: 스프링은 "어? 생성자가 있네?
            내가 관리하는 userRepository를 저기에 넣어줘야겠다(DI)" 하고 알아서 연결해 줌.
 */











//======================================
// AuthController - 인증 API 컨트롤러
//======================================
// 사용자 인증과 관련된 모든 HTTP 요청을 처리하는 REST 컨트롤러
//
//
// 담당 기능:
//   1. 회원가입 (이메일/비밀번호 → JWT 발급)
//   2. 일반 로그인 (이메일/비밀번호 → JWT 발급)
//   3. Google OAuth2 로그인 (ID Token 검증 후 JWT 발급)
//   4. Kakao OAuth2 로그인 (Access Token으로 사용자 정보 조회 후 JWT 발급)
//   5. 토큰 갱신 (Refresh Token으로 새 Access Token 발급)
//   6. 로그아웃 (Refresh Token 무효화)
//   7. 현재 사용자 정보 조회
//
// API 엔드포인트: /api/v1/auth/*
//======================================

import com.madcamp02.dto.request.EmailLoginRequest;
import com.madcamp02.dto.request.LoginRequest;
import com.madcamp02.dto.request.RefreshRequest;
import com.madcamp02.dto.request.SignupRequest;
import com.madcamp02.dto.response.AuthResponse;
import com.madcamp02.security.CustomUserDetails;
import com.madcamp02.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API") // 1. Swagger(API 문서)에 "Auth"라는 그룹으로 표시해 줘.
@RestController // 2. "나는 화면(HTML)을 주는 게 아니라, 데이터(JSON)를 주고받는 컨트롤러야."
//이렇게 모든 형식을 Json으로 주고 받는것을 Rest api라고 한다
@RequestMapping("/api/v1/auth") // 3. "내 관할 구역은 'http://서버주소/api/v1/auth' 로 시작하는 모든 요청이야."
//이렇게 Controller는 관할하는 api 도메인의 범위를 항상 지정해줘야 함

//여기 위에 이 3개의 어노테이션을 먼저 선언을 해줘야 프론트에서 POST /api/v1/auth/login로 요청을 보낼수 있게 됨

@RequiredArgsConstructor // 4. "final이 붙은 친구들(AuthService)을 자동으로 연결(주입)해 줘." --> 항상 쓰는 Lombak
public class AuthController {

    // AuthService 의존성 주입 (생성자 주입 방식)
    private final AuthService authService; //AuthService의 객체와 메서드들로 연결시켜주자

    //------------------------------------------
    // 회원가입 API
    //------------------------------------------
    // 이메일/비밀번호/닉네임으로 일반 회원가입
    // 
    // 처리 과정:
    //   1. 이메일 중복 확인
    //   2. 비밀번호 암호화 (BCrypt)
    //   3. 사용자 생성 + 지갑 생성
    //   4. JWT 발급
    //
    // 요청: POST /api/v1/auth/signup
    // Body: { "email": "...", "password": "...", "nickname": "..." }
    // 응답: { "accessToken": "...", "refreshToken": "...", ... }
    //------------------------------------------
    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 일반 로그인 API (이메일/비밀번호)
    //------------------------------------------
    // 이메일/비밀번호로 로그인 (일반 회원가입 사용자용)
    //
    // 처리 과정:
    //   1. 이메일로 사용자 조회
    //   2. BCrypt로 비밀번호 검증
    //   3. JWT 발급
    //
    // 요청: POST /api/v1/auth/login
    // Body: { "email": "...", "password": "..." }
    // 응답: { "accessToken": "...", "refreshToken": "...", ... }
    //------------------------------------------
    @Operation(summary = "이메일 로그인", description = "이메일/비밀번호로 로그인")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> emailLogin(@Valid @RequestBody EmailLoginRequest request) {
        AuthResponse response = authService.emailLogin(request);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // Google OAuth2 로그인 API
    //------------------------------------------
    // 클라이언트가 Google에서 받은 ID Token을 전송하면,
    // 서버가 검증 후 자체 JWT(Access + Refresh)를 발급
    //
    // 요청: POST /api/v1/auth/oauth/google
    // Body: { "provider": "google", "idToken": "..." }
    // 응답: { "accessToken": "...", "refreshToken": "...", ... }
    //------------------------------------------
    @Operation(summary = "Google 로그인", description = "Google OAuth2 ID Token으로 로그인")
    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // Kakao OAuth2 로그인 API
    //------------------------------------------
    // 클라이언트가 Kakao에서 받은 Access Token을 전송하면,
    // 서버가 Kakao API를 통해 사용자 정보 조회 후 JWT 발급
    //
    // Google vs Kakao 차이:
    //   - Google: ID Token (JWT 형식) 직접 검증
    //   - Kakao: Access Token으로 Kakao API 호출하여 사용자 정보 획득
    //
    // 요청: POST /api/v1/auth/oauth/kakao
    // Body: { "accessToken": "..." }
    // 응답: { "accessToken": "...", "refreshToken": "...", ... }
    //------------------------------------------
    @Operation(summary = "Kakao 로그인", description = "Kakao OAuth2 Access Token으로 로그인")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<AuthResponse> kakaoLogin(@RequestBody java.util.Map<String, String> request) {
        // 요청 바디에서 accessToken 추출
        String accessToken = request.get("accessToken");
        AuthResponse response = authService.kakaoLogin(accessToken);
        return ResponseEntity.ok(response);
    }


    //------------------------------------------
    // 토큰 갱신 API
    //------------------------------------------
    // Access Token 만료 시 Refresh Token으로 새 토큰 발급
    // 
    // 동작 과정:
    //   1. 클라이언트가 Refresh Token 전송
    //   2. 서버가 Redis에서 저장된 토큰과 비교
    //   3. 유효하면 새 Access Token + Refresh Token 발급 (Rotation)
    //
    // 요청: POST /api/v1/auth/refresh
    // Body: { "refreshToken": "..." }
    //------------------------------------------
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 Access Token 재발급")
    @PostMapping("/refresh") // POST /refresh 요청
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {

        // 1. 요청 본문(Body)에서 Refresh Token 문자열을 꺼내서 서비스에 넘깁니다.
        AuthResponse response = authService.refresh(request.getRefreshToken());

        // 2. 서비스가 만든 새 토큰들을 반환합니다.
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 로그아웃 API
    //------------------------------------------
    // Redis에 저장된 Refresh Token 삭제로 세션 무효화
    // 
    // 주의: Access Token은 만료될 때까지 유효
    //       (Blacklist 구현 시 즉시 차단 가능)
    //
    // 요청: POST /api/v1/auth/logout
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "로그아웃", description = "Refresh Token 무효화", security = @SecurityRequirement(name = "bearer-key"))
    @PostMapping("/logout")
    // ★ 핵심: @AuthenticationPrincipal
    // 이 요청을 보낼 때 헤더에 붙인 Access Token을 스프링 시큐리티가 미리 검사
    // 검사가 통과되면, 토큰 안에 들어있던 유저 정보를 'userDetails'라는 변수에 쏙 넣어줌
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {

        // "userDetails.getUserId()"를 통해 로그아웃하려는 사람의 ID(PK)를 꺼냄
        authService.logout(userDetails.getUserId());

        // 로그아웃은 돌려줄 데이터가 없으므로 "200 OK" 상태만 보냅니다. (build)
        return ResponseEntity.ok().build();

        //클라이언트가 Authorization: Bearer {토큰} 헤더를 달고 요청 ->
        //서버 필터가 토큰 해석 ->
        //userDetails 생성 ->
        //컨트롤러에 전달 ->
        //로그아웃 수행
    }

    //------------------------------------------
    // 현재 사용자 정보 조회 API
    //------------------------------------------
    // JWT에서 추출한 사용자 정보 반환
    // 
    // @AuthenticationPrincipal 어노테이션:
    //   SecurityContext에서 인증된 사용자 정보 자동 주입
    //
    // 요청: GET /api/v1/auth/me
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "현재 사용자 정보", description = "로그인된 사용자 정보 조회", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/me") // GET /me 요청 처리
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // userDetails는 JwtAuthenticationFilter -> JwtTokenProvider -> CustomUserDetailsService를 통해
        // DB에서 User 엔티티를 읽어온 뒤(CustomUserDetails로 감싼 뒤) 컨트롤러로 들어온 결과물
        //
        // 즉, "추가 DB 조회"를 하지 않아도 userDetails 안에는 이미 최신(조회 시점 기준)의
        // 프로필 정보(사주/아바타 등)가 들어가 있게 됨

        // isNewUser의 의미:
        // - 프론트가 온보딩 화면을 띄울지 말지 결정할 때,
        //   hasCompletedOnboarding(user)와 함께 사용하는 힌트 값
        // - 여기서는 도메인 헬퍼(User.hasCompletedOnboarding)를 통해
        //   "온보딩 미완료 여부"를 기준으로 설정
        boolean isNewUser = !userDetails.getUser().hasCompletedOnboarding();

        return ResponseEntity.ok(AuthResponse.builder()
                .userId(userDetails.getUserId())
                .email(userDetails.getEmail())
                .nickname(userDetails.getNickname())
                .sajuElement(userDetails.getUser().getSajuElement())
                .avatarUrl(userDetails.getUser().getAvatarUrl())
                .isNewUser(isNewUser)
                .build());
    }
}
