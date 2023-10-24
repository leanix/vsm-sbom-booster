FROM --platform=linux/x86_64 eclipse-temurin:17-jdk-alpine AS jdk_alpine

FROM --platform=linux/x86_64 docker
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jdk_alpine $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apk -U add curl ca-certificates && rm -rf /var/cache/apk/*

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]