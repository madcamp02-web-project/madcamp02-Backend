package com.madcamp02.dto.request;

//======================================
// EmailLoginRequest - 이메일/비밀번호 로그인 요청 DTO
//======================================
// 일반 로그인(이메일/비밀번호)을 위한 요청 데이터를 담는 클래스
//
// 사용 위치:
//   - AuthController.emailLogin() 메서드의 @RequestBody
//   - 클라이언트가 POST /api/v1/auth/login 으로 전송하는 JSON과 매핑
//
// OAuth 로그인과의 차이:
//   - OAuth: 외부 제공자(Google/Kakao)의 토큰을 사용
//   - 일반 로그인: 서버에 저장된 이메일/비밀번호로 직접 인증
//
// 포함 필드:
//   - email: 가입 시 사용한 이메일
//   - password: 가입 시 설정한 비밀번호 (평문으로 전송, 서버에서 BCrypt로 비교)
//======================================

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailLoginRequest {

    // 사용자 이메일 (로그인 ID)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    // 비밀번호 (평문 전송 → 서버에서 BCrypt 해시와 비교)
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
