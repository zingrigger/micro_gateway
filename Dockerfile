# 使用 Eclipse Temurin 的 JDK JRE slim 镜像（轻量、安全、生产级）
# java -Xms128m -Xmx128m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/app/dump/gateway -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=10m -Xloggc:/var/app/gc/gateway/gc_%p.log -jar xxx.jar
FROM eclipse-temurin:20-jre-alpine

# 维护者信息
LABEL maintainer="bosyon"

# 设置工作目录
WORKDIR /bosyon_cloud

# 复制 JAR 文件
COPY target/gateway-1.0-SNAPSHOT.jar app.jar

# 创建 GC 日志和堆转储目录
RUN mkdir -p /bosyon_cloud/gc/gateway /bosyon_cloud/dump/gateway

# 设置 JVM 参数（通过环境变量，便于运行时覆盖）
ENV JAVA_OPTS="-Xms128m -Xmx128m -XX:+UseG1GC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/bosyon_cloud/dump/gateway/gateway_heapdump.hprof \
               -XX:+PrintGCDetails \
               -XX:+PrintGCDateStamps \
               -XX:+PrintGCTimeStamps \
               -XX:+UseGCLogFileRotation \
               -XX:NumberOfGCLogFiles=10 \
               -XX:GCLogFileSize=10M \
               -Xloggc:/bosyon_cloud/gc/gateway/gc_%p.log"

# 设置 Spring Boot 激活的 profile
ENV SPRING_PROFILES_ACTIVE=test

# 暴露服务端口
EXPOSE 8062

# 启动命令：使用 sh -c 包装，支持信号传递和环境变量展开
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]