FROM bellsoft/liberica-openjdk-alpine:21-jdk AS build
RUN apk add --no-cache gradle
WORKDIR /app
COPY . .
RUN gradle build --no-daemon --stacktrace --info --console=plain --refresh-dependencies -x test

FROM bellsoft/liberica-runtime-container:jre-21-slim-musl
ARG APP_DIR=app
WORKDIR /$APP_DIR
COPY --from=build /app/build/libs/*.jar transaction.jar
ENV PROFILE_MODE=prod
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$PROFILE_MODE -jar transaction.jar"]