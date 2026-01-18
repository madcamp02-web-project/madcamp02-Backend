package com.madcamp02.dto.request;

//======================================
// UserUpdateRequest - 내 프로필/설정 수정 요청 DTO
//======================================
// Phase 2에서 구현하는 PUT /api/v1/user/me 요청 Body를 담는 클래스
//
// 왜 Request DTO를 쓰는가?
// - 프론트엔드(JSON) -> 스프링(Java Object)으로 안전하게 변환(역직렬화)하기 위해
// - @Valid를 통해 입력값 검증을 "컨트롤러 진입 전에" 자동으로 걸러내기 위해
//
// 주의:
// - 이 DTO의 필드는 "부분 업데이트(Partial Update)"를 허용하기 위해 전부 nullable
// - 즉, null인 값은 "변경하지 않겠다"는 의미로 처리
//======================================

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    //------------------------------------------
    // nickname (선택)
    //------------------------------------------
    // - null: 변경 안 함
    // - 값이 있으면: 공백만 있는 문자열은 금지, 길이 제한 준수
    //------------------------------------------
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    @Pattern(regexp = "^\\S(.*\\S)?$", message = "닉네임은 공백만으로 구성될 수 없습니다.")
    private String nickname;

    //------------------------------------------
    // avatarUrl (선택)
    //------------------------------------------
    // - null: 변경 안 함
    // - 값이 있으면: 프로필 이미지 URL을 저장 (TEXT 컬럼)
    //------------------------------------------
    private String avatarUrl;

    //------------------------------------------
    // isPublic (선택)
    //------------------------------------------
    // - null: 변경 안 함
    // - true/false: 프로필 공개 여부 설정
    //------------------------------------------
    private Boolean isPublic;

    //------------------------------------------
    // isRankingJoined (선택)
    //------------------------------------------
    // - null: 변경 안 함
    // - true/false: 랭킹 참여 여부 설정
    //------------------------------------------
    private Boolean isRankingJoined;
}

