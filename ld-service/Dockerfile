FROM openjdk:8 

MAINTAINER Matt Prestegaard or (mpresteg@nmdp.org)

WORKDIR /app

EXPOSE 8080

COPY ./target/ld-service-0.0.1-SNAPSHOT.jar /app/ld-service-0.0.1-SNAPSHOT.jar
ENTRYPOINT exec java -Xms3g -Xmx3g -jar /app/ld-service-0.0.1-SNAPSHOT.jar
COPY bash-start-java-tomcat.sh /app/bash-start-java-tomcat.sh
