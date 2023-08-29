FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -Pprod -pl ruoyi-admin -am clean package -DskipTests

FROM eclipse-temurin:8-jre

WORKDIR /app
ENV TZ=Asia/Shanghai \
    SERVER_PORT=9060 \
    JAVA_OPTS=""

RUN mkdir -p /app/logs /app/upload /ruoyi/server/temp

COPY --from=build /workspace/ruoyi-admin/target/ruoyi-admin.jar /app/app.jar

EXPOSE 9060

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} -jar /app/app.jar"]
