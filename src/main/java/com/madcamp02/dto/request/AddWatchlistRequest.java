package com.madcamp02.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/**
 * 관심종목 추가 요청 DTO
 */
@Getter
public class AddWatchlistRequest {

    @NotBlank(message = "종목 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z0-9.:\\s]+$", message = "유효하지 않은 종목 코드 형식입니다")
    private String ticker;
}
