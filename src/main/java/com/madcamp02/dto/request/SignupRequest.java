package com.madcamp02.dto.request;

//======================================
// SignupRequest - 회원가입 요청 DTO
//======================================
// 일반 회원가입(이메일/비밀번호)을 위한 요청 데이터를 담는 클래스
//
// 사용 위치:
//   - AuthController.signup() 메서드의 @RequestBody
//   - 클라이언트가 POST /api/v1/auth/signup 으로 전송하는 JSON과 매핑
//
// 포함 필드:
//   - email: 사용자 이메일 (로그인 ID로 사용)
//   - password: 비밀번호 (8~20자, BCrypt로 암호화되어 저장)
//   - nickname: 사용자 닉네임 (2~10자)
//
// 유효성 검사:
//   - @NotBlank: 빈 값 불가
//   - @Email: 이메일 형식 검증
//   - @Size: 문자열 길이 제한
//======================================

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    // 사용자 이메일 (로그인 ID 역할)
    // 형식: xxx@xxx.xxx
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    // 비밀번호 (평문 → BCrypt 암호화 후 저장)
    // 길이 제한: 8~20자
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String password;

    // 사용자 닉네임 (앱 내 표시 이름)
    // 길이 제한: 2~10자
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
    private String nickname;
}
