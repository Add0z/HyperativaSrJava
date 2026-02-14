FROM openjdk:25-ea-jdk-oraclelinux9 AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Resolve dependencies
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM openjdk:25-ea-jdk-oraclelinux9
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
