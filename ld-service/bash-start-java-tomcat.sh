#!/bin/bash

sleep 10
# See Dockerfile for why the heap default is high and overridable: a large custom
# frequencyFiles upload is held in memory at ~4-6x its size, and the old fixed -Xmx3g
# could not load the NMDP nine-locus release at all.
java ${JAVA_OPTS:--Xmx8g} -jar /app/ld-service-0.0.1-SNAPSHOT.jar
