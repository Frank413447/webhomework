# ====== 阶段1: 构建 ======
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层，改源码时不用重新下载）
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ====== 阶段2: 运行 ======
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户运行
RUN addgroup --system app && adduser --system --ingroup app app

# 从构建阶段复制 jar
COPY --from=builder /build/target/*.jar app.jar

# 用非 root 用户
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
