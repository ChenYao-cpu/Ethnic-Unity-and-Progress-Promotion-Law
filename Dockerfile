# ============================================
# 法律学习系统 - 多阶段构建（服务器自己编译）
# ============================================

# ==================== 第1阶段：Maven 编译 ====================
FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存，改代码不用重新下载）
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==================== 第2阶段：运行镜像 ====================
FROM openjdk:17-slim

WORKDIR /app

# 从编译阶段复制 JAR
COPY --from=builder /build/target/law-study-system-1.0.0.jar app.jar

EXPOSE 8080

# JVM 参数优化（2G内存服务器）
ENTRYPOINT ["java", \
    "-Xms256m", "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-jar", "app.jar"]
