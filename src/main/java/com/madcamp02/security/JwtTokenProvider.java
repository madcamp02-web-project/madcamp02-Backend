package com.madcamp02.security;

//이 클래스 자체는 JWT(JSON Web Token)의 '생성', '검증', '정보 추출'을 전담하는 공장 겸 관리자 클래스

//Spring Security가 요청을 검사할 때,
//"이 토큰 진짜야? 안에 내용 뭐야?" 라고 물어보는 대상이 바로 이 JwtTokenProvider
//즉 JWT를 가공하고 검사하고 뜯어보는 역할 한다 생각하셈(공항에서 탈세잡는 세관원인가?)











//JWT 설명(면접 대비)

//JWT는 유저의 로그인 상태를 서버(DB/메모리)에 저장하지 않고,
//토큰 자체에 유저 정보를 담아서 클라이언트에게 넘겨주는 """무상태(Stateless)""" 인증 방식

//즉, JWT는 Payload에 데이터를 담아 Base64로 인코딩하여 전달하며, 서버만 아는 Secret Key로 생성한 Signature를 통해 데이터의 위변조 여부를 검증하는 Stateless 인증 토큰

/*
JWT는 aaaa.bbbb.cccc 형태의 세 부분으로 나뉨.
    Header (헤더): 암호화 알고리즘(HS256 등)과 토큰 타입 정보.
    Payload (페이로드): 실제 유저 정보(ID, 권한, 만료시간 등). 암호화되지 않고 Base64로 인코딩
    (누구나 복호화해서 볼수 있음)
    Signature (서명): 데이터 위변조를 막기 위한 보안 장치.
*/

/*
서버가 DB 조회 없이 이 토큰을 믿을 수 있는 이유는 수학적 서명 검증 때문임.

생성: 서버는 Header + Payload + 서버만 아는 Secret Key를 조합해 해시값(Signature)을 만듬.
(애초에 이렇게 단방향 함수로 암호화를 시켜버리니까 믿고 암호화로 보호할 수 있는 것)

검증:
클라이언트가 보낸 토큰의 Header와 Payload를 가져옴
서버가 가진 Secret Key를 써서 똑같이 해시값을 계산해 봅니다.
계산된 값과 토큰에 적힌 Signature가 일치하면 위조되지 않았음을 증명
*/

//JWT는 탈취되면 만료될 때까지 막을 방법이 없다는 단점이 있습니다.
//이를 보완하기 위한 "Access Token과 Refresh Token의 이중 보안 전략"이 있습니다.

/*
//Access Token과 Refresh Token 이중 보안 전략은 보안성과 사용자 편의성을 모두 잡기 위해 수명이 다른 두 개의 토큰을 사용하는 방식
두 토큰의 역할과 차이
Access Token (출입증)
용도:         실제 API 요청 시 인증용 (글쓰기, 조회 등)
유효 기간:     아주 짧음 (예: 30분 ~ 1시간)
보관 위치:     주로 로컬 스토리지 또는 메모리
특징:         탈취당해도 금방 만료되어 피해가 적음

Refresh Token (재발급권)
용도:         Access Token이 만료됐을 때 새 토큰 발급용
유효 기간:     김 (예: 2주 ~ 1달)
보관 위치:     주로 HttpOnly Cookie (보안 강화)
특징:         탈취 위험을 줄이기 위해 엄격하게 관리함
-----------------------------------------------------------------


동작 프로세스 (Flow)

로그인:                    사용자가 로그인하면 서버는 Access Token과 Refresh Token을 동시에 발급해 줍니다.
API 요청:                  사용자는 Access Token을 헤더에 싣고 서버에 요청합니다.
만료 (401 Error):          시간이 지나 Access Token이 만료되면, 서버는 "유효기간 끝났다"는 에러를 보냅니다.
재발급 (Silent Refresh):   클라이언트는 즉시 Refresh Token을 서버로 보내 "새 Access Token 주세요"라고 요청합니다.
갱신:                     서버는 Refresh Token이 유효한지(DB 확인) 검사한 후, 새로운 Access Token을 발급해 줍니다. (로그인 유지)
-----------------------------------------------------------------


왜 이렇게 하는가? (핵심 이유)

Access Token 탈취 시 피해 최소화:
해커가 Access Token을 훔쳐가도, 유효 기간이 30분밖에 안 되므로 그 이후에는 무용지물이 됩니다.

Refresh Token 제어 가능 (Server-Side Control):
Refresh Token은 보통 서버 DB에도 저장해 둡니다.
만약 해킹이 의심되거나 사용자가 '로그아웃'을 하면, 서버는 DB에서 Refresh Token을 삭제해 버립니다. 그러면 해커는 더 이상 새로운 Access Token을 발급받을 수 없게 됩니다.

결론: 자주 사용하는 열쇠(Access)는 잃어버려도 금방 못 쓰게 만들고,
진짜 중요한 예비 열쇠(Refresh)는 금고(DB)에 넣어두고 관리하는 전략입니다.
 */


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
//1. 준비 단계 (초기화)
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey; // application.properties에서 비밀키 가져옴

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    private final CustomUserDetailsService userDetailsService;

    @PostConstruct
    protected void init() {
        // 비밀키를 암호화 알고리즘에 쓸 수 있는 객체 형태로 변환
        // Base64 디코딩하여 키 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    //1. 준비 단계 (초기화)
    //서버만 아는 비밀 키(secretKey)를 준비
    //@PostConstruct를 써서, 스프링이 이 클래스를 생성하자마자 딱 한 번만 실행되도록 설정
    //문자열로 된 키를 HMAC-SHA 암호화 객체로 바꿔둠

//2.토큰 생성 (공장 가동)
//참고로 요즘에는 무조건 이중 보안 전략(Access Token과 Refresh Token 이중 보안 전략)을 무조건 쓴다고 보면 됨
    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId, String email, String nickname) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("type", "ACCESS") // 나는 출입증이다!
                .issuedAt(now)
                .expiration(expiry) // 유효기간 짧게 (예: 30분)
                .signWith(key)
                .compact();
    }
    //유저의 email, nickname 등 자주 쓰는 정보를 안에 담는다(Payload)
    //type을 "ACCESS"로 지정하여 용도를 명시

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "REFRESH") // 나는 재발급권이다!
                .issuedAt(now)
                .expiration(expiry) // 유효기간 길게 (예: 2주)
                .signWith(key)
                .compact();
    }
//2. 토큰 생성 (공장 가동)
//여기엔 불필요한 정보를 최소화하고 userId 정도만 담기
//가장 중요한 건 긴 유효기간


//3. 인증 정보 조회 (신원 확인)
    /**
     * 토큰에서 Authentication 객체 추출
     */
    public Authentication getAuthentication(String token) {
        // 1. 토큰 뜯어서 사용자 ID(PK) 가져옴
        Claims claims = parseClaims(token);
        String userId = claims.getSubject();

        // 2. DB에서 그 사용자 상세 정보(UserDetails)를 로딩
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

        // 3. 스프링 시큐리티가 이해하는 '인증 티켓(Authentication)' 생성
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                userDetails.getAuthorities()
        );
    }
//3. 인증 정보 조회 (신원 확인)
//핵심: 단순히 토큰만 믿는 게 아니라, DB(userDetailsService)를 한 번 더 거쳐서 확실한 유저 객체를 만들어냄
//이 메서드가 리턴한 Authentication 객체가 스프링 시큐리티 컨텍스트(SecurityContext)에 저장되면 "로그인 성공" 상태가 됨



//4. 토큰 해석 getter 메서드
    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * 토큰 타입 확인 (ACCESS / REFRESH)
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }







//4. 토큰 검증 및 추출 (보안 검색대)
    /**
     * Request Header에서 토큰 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    //HTTP 요청 헤더(Authorization)에서 "Bearer " 글자를 떼고 순수한 토큰 값만 꺼냄

    /**
     * 토큰 유효성 검증, 여기가 서명(Signature) 검증이 일어나는 곳임
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰입니다.");
        } catch (SecurityException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다.");
        }
        return false;
    }
    //서버의 비밀 키(key)로 풀리지 않거나, 유효기간이 지났거나, 형식이 이상하면 전부 false를 뱉고 로그를 남김.

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
    //"지금 시계랑 비교했을 때, 이 토큰 유효기간 지났어?"라고 묻는 메서드

    /**
     * 토큰 남은 유효시간 (ms)
     */
    public long getExpiration(String token) {
        Claims claims = parseClaims(token);
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
    //"이 토큰 죽을 때까지 몇 밀리초(ms) 남았어?"를 계산

    //사용처 (실무 꿀팁): 주로 로그아웃(Blacklist) 기능을 구현할 때 씀.(Redis 관련)

    //사용자가 로그아웃을 요청하면, 서버는 이 Access Token이 남은 시간만큼 다시는 못 쓰게
    //Redis 같은 곳에 '사용 금지 목록(Blacklist)'으로 등록해 둬야 합니다.
    //그때 이 메서드로 "남은 시간"을 계산해서 Redis 저장 시간을 설정함

    /**
     * Claims 파싱
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
    //암호화된 토큰을 열어서 내용물(Payload)을 꺼내는 내부 전용(private) 도우미

    //토큰이 만료되면 에러가 터지고 끝나야 하는데,
    //여기서는 "만료된 토큰이라도 일단 그 안의 내용(ID, 이메일 등)은 꺼내준다"는 로직이 들어있음

    //이유: Access Token 재발급(Refresh) 때문임. 유저가 "나 토큰 만료됐어, 재발급해줘"라고 요청할 때,
    //서버는 "네가 누구였는데?"를 알기 위해 만료된 토큰의 내용을 읽어야 할 때가 있음
}