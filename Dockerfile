FROM --platform=linux/x86_64 eclipse-temurin:17-jdk-alpine as jdk_alpine

FROM --platform=linux/x86_64 docker
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jdk_alpine $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apk --no-cache upgrade && apk --no-cache add curl ca-certificates

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]