FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat

# Dọn dẹp ứng dụng mặc định của Tomcat
RUN rm -rf webapps/*

# Copy file WAR ứng dụng vào Tomcat
COPY target/SWP391_CafeManagementSystem.war webapps/SWP391_CafeManagementSystem.war

# Tạo thư mục ROOT với index.jsp tự redirect về /SWP391_CafeManagementSystem/
RUN mkdir -p webapps/ROOT && \
    echo '<% response.sendRedirect(request.getContextPath() + "/SWP391_CafeManagementSystem/"); %>' > webapps/ROOT/index.jsp

EXPOSE 8080
CMD ["catalina.sh", "run"]
