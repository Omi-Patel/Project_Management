## Deployment Setup

- Clean the package - run this command

```
./mvnw clean package
```

- set env variable in the terminal

```
$env:DATASOURCE_URL="jdbc:postgresql://localhost:5432/pms"
$env:DATASOURCE_USERNAME="postgres"
$env:DATASOURCE_PASSWORD="Om@123"
```

- build package to generate target file

```
./mvnw package
```

- write Dockerfile

```
# Use an Official Maven image to build the Spring Boot app
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and install dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Use an Official OpenJDK image to run the application
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the build JAR file from the build stage
COPY --from=build /app/target/pms-0.0.1-SNAPSHOT.jar .

# Expose port 8080
EXPOSE 8080

# Specify the command to run the application
ENTRYPOINT ["java", "-jar", "/app/pms-0.0.1-SNAPSHOT.jar"]
```

- run this command to build docker

```
docker build -t Project_Management .
```

- to add tag to the repository

```
docker tag project_management omipatel07/project_management:latest
```

- finally push the repository to dockerhub

```
docker push omipatel07/project_management:latest
```


### DB setup on cloud base - [ NeonDB ]

#### Connection String : 
```
jdbc:postgresql://ep-dry-sea-a1l7tcax-pooler.ap-southeast-1.aws.neon.tech/pms?user=pms_owner&password=npg_qdGm2g9UCALJ&sslmode=require


DATASOURCE_URL=jdbc:postgresql://ep-dry-sea-a1l7tcax-pooler.ap-southeast-1.aws.neon.tech/pms
DATASOURCE_USERNAME=pms_owner
DATASOURCE_PASSWORD=npg_qdGm2g9UCALJ
```