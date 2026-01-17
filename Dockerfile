# ============================================
# MadCamp02 Backend - Multi-stage Dockerfile
# ============================================

# ============================================
# Stage 1: Build (Use official Gradle image)
# ============================================
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app

# 캐싱을 위해 의존성 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./

# 의존성 다운로드 (실패 시 에러 발생하도록 || true 제거)
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 빌드 실행 (테스트 제외)
RUN gradle build -x test --no-daemon

# ============================================
# Stage 2: Run
# ============================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# curl 설치 (HEALTHCHECK용)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 복사
# Gradle 빌드 산출물 위치: /app/build/libs/
COPY --from=builder /app/build/libs/*.jar app.jar

# 비루트 사용자 생성
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
RUN chown appuser:appgroup app.jar
USER appuser

# 환경 변수 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV TZ="Asia/Seoul"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
