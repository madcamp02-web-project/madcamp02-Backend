package com.madcamp02.config;

//Spring Bean(스프링 빈) = Spring이라는 거대한 관리자(Container)가 관리하는 자바 객체
/*
우리가 흔히 자바에서 new 키워드로 객체를 생성하는 것과는 다르게,
Spring이 "내가 미리 만들어 놓을 테니 필요할 때 가져다 써!"라고 하는 객체

일종의 Spring에서 사용하라고 권장하는 객체 정도로 생각하면 됨.

일반적인 객체와 다른점
- 일반적인 자바 객체 (POJO):
    개발자가 직접 new를 해서 만듭니다.
    User user = new User();
    관리가 끝나면 개발자가(혹은 GC가) 치워야 합니다.

- 스프링 빈 (Spring Bean):
    앱이 시작될 때 Spring이 알아서 생성해서 "Spring Container(콩 주머니)"라는 곳에 넣어둡니다.
    개발자는 "저거 주세요"라고 요청(주입)만 하면 됩니다.
    이것을 전문 용어로 **IoC (제어의 역전)**라고 합니다.

- 싱글톤 (Singleton) - 딱 하나만 만든다! ?
    스프링 빈의 가장 큰 특징이자 사용하는 이유입니다.

    상황: 만약 쇼핑몰에 100만 명의 유저가 동시에 접속해서 "상품 조회"를 한다고 가정해 봅시다.
    일반 객체: 100만 개의 ProductService 객체가 생성되어 메모리가 터져버릴 수 있습니다.
    스프링 빈:   Spring은 앱 시작할 때 ProductService를 딱 1개만 만들어 둡니다.
                100만 명의 요청이 들어와도 이 단 하나의 객체를 돌려가며 공유해서 씁니다.
    결과: 메모리를 엄청나게 아낄 수 있고 성능이 좋아집니다.

- 빈을 등록하는 방법 (명찰 붙이기)
    Spring한테 "이거 빈으로 등록해줘!"라고 말하는 방법은 크게 두 가지입니다.

    ① 클래스 위에 어노테이션 붙이기 (자동 등록)
        가장 흔한 방법입니다. "나 여기 있어!"라고 손을 드는 것과 같습니다.
            @Component: 가장 기본.
            @Controller, @Service, @Repository: 역할에 따라 구체적인 이름을 붙인 것 (내부적으론 @Component와 같음).

    ② 설정 파일(@Configuration)에서 수동 등록
        아까 보신 SecurityConfig 파일 방식을 보면 쉽게 알 수 있음.

        @Configuration
        public class SecurityConfig {

            @Bean // "이 메서드가 반환하는 결과물을 빈으로 등록해줘!"
            public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
            }
        }
    BCryptPasswordEncoder는 내가 만든 클래스가 아니라
    라이브러리(남이 만든 코드)라서 소스 코드 위에 @Component를 붙일 수 없습니다.
    이럴 때 @Bean을 써서 수동으로 등록합니다.
 */

import com.madcamp02.security.JwtAuthenticationFilter;
import com.madcamp02.security.JwtTokenProvider;
import com.madcamp02.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration //설정 파일(Bean 설정소)이야"라고 스프링에게 알리는 어노테이션
@EnableWebSecurity //스프링 시큐리티 기능을 활성화하는 것
@RequiredArgsConstructor //final이 붙은 필드(jwtTokenProvider)를 생성자로 주입받아 자동으로 채워주는 어노테이션
                         // (JWT 검증 도구를 미리 준비해둠)
//의존성 주입 (DI: Dependency Injection)
/*
DI가 없을 때 (자급자족):
    Controller가 일하려면 Service가 필요해서 직접 뽑습니다.
    Service service = new Service();
    문제점: 매번 새로 뽑아야 하고 관리가 안 됩니다.

DI를 사용할 때 (배급제):
    Controller: "사장님(Spring), 저 일해야 하는데 Service 담당자 좀 보내주세요."
    Spring: "이미 채용해 둔 김대리(Bean)가 있으니 걔랑 일해."
    결과: Controller 안으로 Service가 쏙 들어옵니다(Inject).
*/

/*
@RequiredArgsConstructor의 역할
이 어노테이션은 롬복(Lombok) 라이브러리 기능으로, "꼭 필요한(Required) 재료들로만 채워진 생성자를 자동으로 만들어줍니다."

여기서 "꼭 필요한 재료"란 final이 붙은 변수를 말합니다.

--여기에서 사용하는 코드 (Lombok 사용)
        @RequiredArgsConstructor
        public class SecurityConfig { private final JwtTokenProvider provider; }

--실제 동작하는 코드 (Java 원래 모습)
        public class SecurityConfig { private final JwtTokenProvider provider;
        // 생성자 (Constructor)의 경우
        public SecurityConfig(JwtTokenProvider provider) { this.provider = provider; } }

스프링 프레임워크에는 3가지 주입 방식이 있지만, 지금 쓰신 생성자 주입 방식이 업계 표준(Best Practice)입니다.
    Spring의 규칙: 클래스에 생성자가 딱 하나만 있으면, 스프링이 알아서 그 생성자를 통해 빈(Bean)을 주입해 줍니다. (그래서 @Autowired도 생략 가능)
    final의 안전함: private final로 선언했기 때문에, 이 변수는 생성될 때 딱 한 번만 값이 채워지고 절대 바뀌지 않습니다(불변성).
                    앱 실행 도중에 실수로 provider = null; 처럼 코드를 바꿔버리는 사고를 원천 봉쇄합니다.
    간결함: @RequiredArgsConstructor 덕분에 코드가 아주 짧고 깔끔해집니다.
 */


//요약: 스프링한테 안전하게 빈(Bean)을 받아오기 위해 생성자를 써야 하는데, 귀찮으니까 롬복(Lombak)으로 한 방에 처리했다


public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // 공개 엔드포인트 (인증 불필요) //일종의 예외라고 생각하면 됨, 출입 명부 면제 리스트이다.
    private static final String[] PUBLIC_ENDPOINTS = {
            // ========== 인증 API ==========
            "/api/v1/auth/signup",       // 일반 회원가입 (이메일/비밀번호)
            "/api/v1/auth/login",        // 일반 로그인 (이메일/비밀번호)
            "/api/v1/auth/oauth/google", // Google OAuth2 로그인
            "/api/v1/auth/oauth/kakao",  // Kakao OAuth2 로그인
            "/api/v1/auth/refresh",      // 토큰 갱신

            // ========== OAuth2 관련 ==========
            "/oauth2/**",
            "/login/oauth2/**",

            // ========== API 문서 (Swagger) ==========
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",

            // ========== 서버 상태 ==========
            "/actuator/health",

            // ========== WebSocket ==========
            "/ws/**"
    };
    /*
        인증 관련 API:
          - 회원가입, 로그인, OAuth(Google/Kakao), 토큰갱신은 인증 없이 접근 가능
        
        소셜 로그인 (/oauth2/**)
        API 문서 (/swagger-ui/**)
        서버 상태 체크 (/actuator/health)
        WebSocket 연결 (/ws/**)
     */


    //여기가 바로 핵심 보안 규칙이다!!!
    //요청이 서버에 들어오면 거쳐야 할 관문들을 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API는 Stateless)
                .csrf(AbstractHttpConfigurer::disable)
                //CSRF는 주로 '브라우저 세션'을 이용할 때 필요한데, 우리는 JWT(토큰) 방식을 쓰기 때문에 필요가 없음
                //켜두면 오히려 복잡하다고 함(어쩌피 난 JWT지 세션이나 쿠키 방식이 아니잖아)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //"CORS(도메인 간 통신) 규칙은 저 아래에 있는 corsConfigurationSource() 메서드를 따르라"고 지시

                // 세션 사용 안함 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                //서버에 세션(기억)을 만들지 마라, 즉 Stateless를 사용해라
                //REST API + JWT 방식은 원래 Stateless방식이 기본이다.
                //클라이언트가 요청할 때마다 토큰을 들고 와야 하며, 서버는 클라이언트의 상태를 저장한다.
                //JWT는 토큰이라 생각하고 토큰으로 인증한다고 생각하면 됨(JWT 설정 제대로 해보자)

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()  // 위에서 만든 목록은 무조건 통과
                        .anyRequest().authenticated() // 아닐경우 나머지는 전부 인증(로그인) 필요(이 메서드 실행시키기)
                )
                //경비원에게 "명단에 있는 사람(PUBLIC_ENDPOINTS)은 그냥 들여보내고, 나머지는 출입증(토큰) 확인해!"라고 지시


                // JWT 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                )
                //스프링 시큐리티의 기본 로그인 필터(UsernamePassword...)가 동작하기 전에 우리가 만든 JwtAuthenticationFilter를 먼저 실행
                //JWT 필터를 사용하는 이유는 ID/PW 검사하기 전에, 가져온 토큰이 유효한지 먼저 검사해서 유효하면 통과시켜주기 위함
                //토큰 유효하면 빠르게 ok

                // OAuth2 로그인 설정 (백엔드 주도 방식)
                // 사용자가 /oauth2/authorization/kakao 또는 /oauth2/authorization/google 접근 시
                // 해당 OAuth2 제공자의 로그인 페이지로 리다이렉트
                // 로그인 성공 시 OAuth2SuccessHandler가 JWT 발급 후 프론트엔드로 리다이렉트
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)  // 로그인 성공 시 JWT 발급 핸들러
                        .failureHandler((request, response, exception) -> {
                            // 로그인 실패 시 에러 응답
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"OAuth2 Login Failed\", \"message\": \"" + exception.getMessage() + "\"}");
                        })
                )

                // 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증이 필요합니다.\"}");
                        })
                        //AuthenticationEntryPoint (401 에러):
                        //로그인이 안 된 사람이 들어오려 할 때,
                        //HTML 에러 페이지 대신 {"message": "인증이 필요합니다."} 같은 깔끔한 JSON을 내려주는 것


                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"접근 권한이 없습니다.\"}");
                        })
                        //AccessDeniedHandler (403 에러):
                        //로그인은 했는데 권한이 없는 곳(예: 일반 유저가 관리자 페이지 접근)에 갈 때 {"message": "접근 권한이 없습니다."}를 내림
                );

        return http.build();
    }

    //프론트엔드(React 등)가 다른 주소(포트)에서 서버로 요청을 보낼 때 브라우저가 막지 않게 허락해 주는 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://madcamp02.com"
        ));
        //setAllowedOrigins: 허용할 사이트 주소. (localhost:3000 등 개발/운영 주소)

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        //setAllowedMethods: 허용할 행동 (GET, POST, PUT, DELETE 등)

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명 허용 (쿠키, Authorization 헤더)
        configuration.setAllowCredentials(true);
        //setAllowCredentials(true): 이게 JWT 사용할 경우 매우 중요.
        //이게 true여야 프론트엔드가 토큰이나 쿠키를 실어서 보낼 수 있음.

        // preflight 캐시 시간
        configuration.setMaxAge(3600L);

        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "X-Refresh-Token"
        ));
        //setExposedHeaders: 프론트엔드 자바스크립트가 응답 헤더 중 Authorization(토큰) 값을 읽을 수 있게 허락함.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    //로그인 로직(Service)에서 "이 ID/PW 맞는지 확인해줘"라고 시킬 때 사용하는 관리자를 스프링 빈으로 등록

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    //BCrypt라는 강력한 알고리즘으로 암호화(해싱)해 주는 도구를 등록합니다.
    //"1234"를 입력하면 sdj2189dsa... 같은 난수로 바꿔줌
}