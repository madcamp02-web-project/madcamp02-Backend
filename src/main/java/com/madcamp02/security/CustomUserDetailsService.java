package com.madcamp02.security;

//Spring Security의 인증 매니저와 실제 데이터베이스(Repository) 사이를 연결하는 '다리(Bridge)' 역할을 하는 서비스=
//실제로 DB에 접근해서 인증확인을 수행하는 클래스라고 생각하면 됨

//Spring Security는 DB에 사용자가 있는지, 그 사용자가 누구인지 직접 알 수 없으므로,
//"ID를 줄 테니 사용자 정보를 찾아와라"라고 명령할 수 있는 구현체가 필요한데, 그게 바로 이 클래스


import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    //implements UserDetailsService: Spring Security의 표준 인터페이스를 구현함.
    //이 인터페이스를 구현해서 Bean으로 등록하면, Spring Security는 로그인 시도나 인증 과정에서 자동으로 이 서비스를 찾아 사용

    //소웨공 시간에도 배운거지만 인터페이스는 절대로 변경하지 않는다.
    //그리고 class끼린 무조건 interface로만 소통한다(implements를 통해)

    private final UserRepository userRepository;
    //UserRepository 주입: 실제 DB 조회를 위해 리포지토리를 주입받음


    @Override
    @Transactional(readOnly = true) //성능 최적화: @Transactional(readOnly = true)
    //조회(SELECT)만 수행하기 때문에 readOnly = true 옵션을 주면,
    //JPA(Hibernate)가 영속성 컨텍스트의 더티 체킹(변경 감지) 기능을 끄고,
    //스냅샷을 만들지 않아 메모리와 CPU 성능이 최적화
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.parseLong(userId)) //들어온 String 값을 Long으로 변환.
                // 이는 앞서 분석했던 CustomUserDetails.getUsername()이 User ID(PK)를 반환하도록 설계된 것과 짝을 이룸
                .orElseThrow(() -> new UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다. ID: " + userId
                ));

        return new CustomUserDetails(user);
        //DB에서 찾은 User Entity(Table)를 그대로 반환하지 않고, 앞서 분석한 CustomUserDetails로 감싸서 반환
    }
    //메서드 이름은 표준 인터페이스 때문에 loadUserByUsername이지만, 실제 내부 로직은 userId(PK)를 받고 있음

    @Transactional(readOnly = true) //성능 최적화: @Transactional(readOnly = true)
    //조회(SELECT)만 수행하기 때문에 readOnly = true 옵션을 주면,
    //JPA(Hibernate)가 영속성 컨텍스트의 더티 체킹(변경 감지) 기능을 끄고,
    //스냅샷을 만들지 않아 메모리와 CPU 성능이 최적화
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        //이건 우리가 직접 만든 코드(커스텀 메서드 (loadUserByEmail))
        //OAuth2 로그인 초기 단계에 카카오/구글에서 이메일 정보를 받아왔을 때,
        //이미 가입된 회원인지 확인하거나 JWT 토큰을 처음 발급하기 위해 사용자를 식별할 때 쓰임
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다. Email: " + email
                ));

        return new CustomUserDetails(user);
    }
}