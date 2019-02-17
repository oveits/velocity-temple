### STAGE 1: Build ###

# We label our stage as 'builder'
FROM gradle:5.2.1-jdk8-alpine as builder

RUN gradle jar

### STAGE 2: Setup ###

FROM java:8-jre-alpine

RUN mkdir /app

## Copy our resources
COPY --from=builder build/libs /app/

WORKDIR /app

# ENTRYPOINT java -jar velocity-temple-0.0.1-SNAPSHOT.jar

CMD ["java", "-jar", "velocity-temple-0.0.1-SNAPSHOT.jar"]
