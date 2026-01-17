package com.madcamp02.dto.request;

//======================================
// RefreshRequest - 토큰 갱신 요청 DTO
//======================================
// Access Token 만료 시 새 토큰 발급을 위한 요청 데이터 구조
//
// 사용 시점: POST /api/v1/auth/refresh 요청 시 Body에 포함
//
// 왜 필요한가?
//   - Access Token은 보안상 유효기간이 짧음 (1시간)
//   - 만료될 때마다 재로그인하면 사용자 경험 저하
//   - Refresh Token으로 자동 갱신하여 로그인 상태 유지
//
// Silent Refresh 과정:
//   1. 클라이언트가 API 요청
//   2. 서버가 401 Unauthorized 응답 (Access Token 만료)
//   3. 클라이언트가 이 DTO로 /refresh 요청
//   4. 새 Access Token + Refresh Token 수신
//   5. 원래 요청 재시도
//======================================

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshRequest {

    //------------------------------------------
    // Refresh Token (필수)
    //------------------------------------------
    // 로그인 시 발급받은 재발급용 토큰
    //
    // 특징:
    //   - 유효기간 김 (7일)
    //   - Redis에 저장되어 서버가 관리
    //   - 사용 후 새 토큰으로 교체됨 (Token Rotation)
    //
    // 보안 고려:
    //   - 탈취 시 피해 최소화를 위해 한 번 사용하면 폐기
    //   - 서버 DB(Redis)와 일치해야만 유효
    //------------------------------------------
    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
