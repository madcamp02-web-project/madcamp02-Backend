package com.madcamp02.config;

//======================================
// PasswordConfig - 비밀번호 인코더 설정
//======================================
// - BCryptPasswordEncoder를 전역 Bean으로 등록하여
//   AuthService, OAuth2SuccessHandler 등에서 주입받아 사용
// - SecurityConfig와의 순환 참조를 피하기 위해 분리된 설정 클래스
//======================================

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

