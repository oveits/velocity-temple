### STAGE 1: Build ###

# We label our stage as 'builder'
#FROM gradle:5.2.1-jdk8-alpine as builder
#FROM gradle:3.4-jdk7-alpine as builder
FROM oveits/gradle-with-velocity-jars:3.4-jdk7-alpine-v2 as builder

 USER root

 RUN [ -d /project ] || mkdir /project

 WORKDIR /project
 
 COPY . .
 
 RUN gradle jar -p /project

### STAGE 2: Setup ###

FROM java:7-jre-alpine

RUN mkdir /app

## Copy our resources
#COPY --from=builder /home/gradle/build/libs /app/
COPY --from=builder /project/build/libs /app/

RUN mkdir -p /app/src/main/resources/ && mv /app/templates /app/src/main/resources/

WORKDIR /app

# ENTRYPOINT java -jar velocity-temple-0.0.1-SNAPSHOT.jar

CMD ["java", "-jar", "velocity-temple-0.0.1-SNAPSHOT.jar"]
