# syntax=docker/dockerfile:1
# 파라미터화된 멀티스테이지 빌드 — 모든 Spring 모듈이 이 Dockerfile 하나를 공유한다.
#   GRADLE_MODULE : Gradle 모듈 경로 (예: :services:user-service)
#   MODULE_DIR    : 산출물 디렉터리 (예: services/user-service)
# docker-compose.app.yml의 build.args 로 주입된다.

# ── build stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY infrastructure ./infrastructure
COPY services ./services

ARG GRADLE_MODULE
ARG MODULE_DIR
# Gradle 캐시를 BuildKit 캐시 마운트로 공유 → 여러 서비스 빌드 시 의존성 재다운로드 방지
# gradlew가 Windows(CRLF) 체크아웃이면 shebang이 깨지므로 CR 제거 후 실행
RUN --mount=type=cache,target=/root/.gradle \
    sed -i 's/\r$//' gradlew && chmod +x gradlew && \
    ./gradlew ${GRADLE_MODULE}:bootJar -x test --no-daemon
# 실행 가능한 bootJar만 선택 (-plain.jar 제외)
RUN cp "$(find ${MODULE_DIR}/build/libs -name '*.jar' ! -name '*-plain.jar' | head -n1)" /workspace/app.jar

# ── runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
# curl: compose 헬스체크용
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
