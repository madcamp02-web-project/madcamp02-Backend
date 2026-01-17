# ============================================
# MadCamp02 Backend - Multi-stage Dockerfile
# ============================================

# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle Wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Windows CRLF → LF 변환 (Linux에서 실행 가능하도록)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 의존성 캐시를 위해 먼저 다운로드
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 빌드 (테스트 제외)
RUN ./gradlew build -x test --no-daemon

# ============================================
# Stage 2: Run
# ============================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# curl 설치 (HEALTHCHECK용)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 비루트 사용자 생성 (보안)
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
RUN chown appuser:appgroup app.jar
USER appuser

# 환경 변수 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV TZ="Asia/Seoul"

# 헬스체크 (actuator 엔드포인트 사용)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
