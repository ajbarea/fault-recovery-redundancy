# Stage 1: Build the app
FROM maven:3.9-eclipse-temurin-17 AS build
#use the directory /app to store all the files and cd into it
WORKDIR /app

#copy over src annd pom.xml files
COPY pom.xml .
COPY src ./src
#we dont want to run the tests during the build
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]