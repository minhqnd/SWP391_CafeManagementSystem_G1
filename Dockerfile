# ==========================================
# Stage 1: Build file WAR bằng Maven & Java 17
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build project sang file WAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Runtime với Tomcat 10.1 & Java 17
# ==========================================
FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat

# Dọn dẹp ứng dụng mặc định của Tomcat
RUN rm -rf webapps/*

# Copy file WAR ứng dụng vào Tomcat
COPY --from=builder /app/target/SWP391_CafeManagementSystem-*.war webapps/SWP391_CafeManagementSystem.war

# Tạo thư mục ROOT với index.jsp tự redirect về /SWP391_CafeManagementSystem/
RUN mkdir -p webapps/ROOT && \
    echo '<% response.sendRedirect(request.getContextPath() + "/SWP391_CafeManagementSystem/"); %>' > webapps/ROOT/index.jsp

EXPOSE 8080
CMD ["catalina.sh", "run"]
