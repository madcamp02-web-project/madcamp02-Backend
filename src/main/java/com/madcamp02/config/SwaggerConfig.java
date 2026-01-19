package com.madcamp02.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .components(new Components()
                                                .addSecuritySchemes("bearer-key",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("JWT Access Token을 입력하세요. 로그인 API로 토큰을 발급받을 수 있습니다.")))
                                // 기본 SecurityRequirement 제거: Public API는 인증 불필요
                                // 인증이 필요한 API는 각 Controller에서 @SecurityRequirement 어노테이션 사용
                                .info(new Info()
                                                .title("MadCamp02 Backend API")
                                                .description("MadCamp02 백엔드 API 명세서")
                                                .version("v1.0.0"));
        }
}
