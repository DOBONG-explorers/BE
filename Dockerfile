# 1️⃣ JDK 17 (Debian Slim 기반)
FROM openjdk:17-jdk-slim

# 2️⃣ 헬스체크용 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 3️⃣ 작업 디렉터리
WORKDIR /app

# 4️⃣ 빌드된 JAR 복사
COPY build/libs/*.jar app.jar

# 5️⃣ 기본 포트
EXPOSE 8080

# 6️⃣ JVM 옵션(선택) — 로그 버퍼링 완화 + UTF-8 강제
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dspring.output.ansi.enabled=ALWAYS"

# 7️⃣ 실행 명령
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
