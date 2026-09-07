FROM openjdk:8 

MAINTAINER Matt Prestegaard or (mpresteg@nmdp.org)

WORKDIR /app

EXPOSE 8080

COPY ./target/ld-service-0.0.1-SNAPSHOT.jar /app/ld-service-0.0.1-SNAPSHOT.jar
# A custom frequencyFiles upload (POST /genotypes/file) is parsed fully into memory by
# HLAFrequenciesLoader.loadStandardReferenceData, at ~4-6x the file size -- the real NMDP
# nine-locus release (~1GB) alone needs 4-6g of heap. The old fixed -Xms3g -Xmx3g could not
# load it at all, and an OutOfMemoryError here takes down every in-flight job, not just the
# one bad request. Default high (uploads are deliberately uncapped -- see
# application.properties -- and this is scoped as a personal/local tool); override with
# JAVA_OPTS for a smaller host.
ENTRYPOINT exec java ${JAVA_OPTS:--Xmx8g} -jar /app/ld-service-0.0.1-SNAPSHOT.jar
COPY bash-start-java-tomcat.sh /app/bash-start-java-tomcat.sh
