FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew installDist -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/visualboard-tournament-back/ ./
RUN chmod +x ./bin/visualboard-tournament-back
EXPOSE 8080
ENTRYPOINT ["./bin/visualboard-tournament-back"]
