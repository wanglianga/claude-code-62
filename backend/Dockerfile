# ---------- 构建阶段：Maven 编译打包 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先拷贝 pom 利用 Docker 层缓存预下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DexcludeGroupIds=org.junit.jupiter,org.mockito || true
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段：精简 JRE + 非 root 用户 ----------
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app -d /app app
WORKDIR /app
COPY --from=build /app/target/funeral-platform.jar /app/app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=15 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
