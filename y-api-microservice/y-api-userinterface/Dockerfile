FROM eclipse-temurin:8-jdk
RUN apt-get update && apt-get install -y --no-install-recommends tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY /y-api-userinterface-1.0-SNAPSHOT.jar app.jar
EXPOSE 8212
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]