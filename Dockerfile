FROM registry.redhat.io/ubi9/openjdk-21:latest AS build
USER root
RUN microdnf install -y findutils wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
    unzip gradle-8.5-bin.zip && \
    mv gradle-8.5 /opt/gradle && \
    ln -s /opt/gradle/bin/gradle /usr/bin/gradle && \
    microdnf clean all
USER 185
WORKDIR /home/jboss
COPY --chown=185:0 . .
RUN gradle build --no-daemon --stacktrace --info --console=plain --refresh-dependencies -x test

FROM registry.redhat.io/ubi9/openjdk-21-runtime:latest
COPY --from=build --chown=185:0 /home/jboss/build/libs/*.jar /deployments/transaction.jar
ENV PROFILE_MODE=prod
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${PROFILE_MODE}", "-jar", "/deployments/transaction.jar"]