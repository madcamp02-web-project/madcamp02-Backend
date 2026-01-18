package com.madcamp02;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI에서 "테스트가 실제로 실행"되는지 확인하는 최소 Smoke Test.
 *
 * 통합 테스트(@SpringBootTest + Postgres/Redis/Flyway 의존)는 CI/CD 단계에서
 * (A) GitHub Actions 서비스 컨테이너 추가 or (B) 테스트 프로파일/컨테이너 전략
 * 중 하나로 고정해서 진행합니다. (docs 참고)
 */
class Madcamp02BackendApplicationTests {

    @Test
    void smokeTest() {
        assertTrue(true);
    }
}

