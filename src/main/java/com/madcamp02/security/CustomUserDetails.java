package com.madcamp02.security;

//CustomUserDetails 클래스는 도메인 객체(User)와 Spring Security의 표준 사용자 인터페이스(UserDetails)를 연결해 주는
//객체와 인터페이스를 연결하는 어댑터(Adapter) 역할을 하는 클래스

//Spring Security는 개발자가 만든 User 객체의 구조를 모르기 때문에,(이거 JPA Entity로 내가 DB에서 가져와서 만드는 user Table의 객체가 되니까)
//DB시간에도 배우지만 DB에선 하나의 Table을 Entity로 부른다(행은 tuple, 열은 attribute)
//시큐리티가 이해할 수 있는 형태인 UserDetails로 감싸서 사용

import com.madcamp02.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
//어댑터 패턴 (Wrapper)
public class CustomUserDetails implements UserDetails {
    //implements UserDetails: Spring Security에서 인증된 사용자의 정보를 담는 표준 인터페이스를 구현

    private final User user;
    //이 명령어 쓰면 실제 데이터베이스에 저장된 도메인 엔티티(com.madcamp02.domain.user.User)를 필드로 가지고 있게 된다
    //이를 통해 시큐리티 로직 내부에서도 우리 서비스의 고유한 사용자 정보(닉네임, 이메일 등)에 접근할 수 있음

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    //일단 내가 작성한건 모든 사용자에게 ROLE_USER 라는 단일 권한을 부여하고는 있는데
    //관리자(ROLE_ADMIN) 기능이 필요하다면, user.getRole() 등을 통해 동적으로 권한을 반환하도록 수정해야 함


    //만약 여기서 내가 일반적인 로그인 비밀번호 설정하려면 반환값으로 암호화된 비밀번호를 반환해야 함
    @Override
    public String getPassword() {
        return null;  // OAuth2 로그인이므로 비밀번호 없음
    }
    //OAuth2 특화 설정: 일반적인 아이디/비밀번호 로그인이었다면 암호화된 비밀번호를 반환
    //혹시 일반적인 로그인을 만드려면 여기서 암호화된 비밀번호를 반환해야함

    @Override
    public String getUsername() {
        return String.valueOf(user.getUserId());
    }
    //userId (PK, Primary Key)인게 중요하다
    //JWT 토큰을 생성할 때도 보통 이 값을 Subject에 넣음(보통 이럼)

    //계정 만료, 잠금, 비밀번호 만료, 활성화 여부를 묻는 메서드
    //일단 지금 상태는 모든 계정은 항상 활성화되어 있고 문제가 없다는 것임

    //기능 추가 염두!!!)
    //만약 내가 여기서 휴면 계정이나 정지된 계정을 처리해야 한다면
    //user.getStatus() 등을 연동하여 false를 반환하게 하면 로그인을 막을 수 있음
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Getter 함수들
    public Long getUserId() {
        return user.getUserId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getNickname() {
        return user.getNickname();
    }
}