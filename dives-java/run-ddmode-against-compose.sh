#!/bin/bash

export OTEL_SDK_DISABLED=true

export DD_GIT_REPOSITORY_URL=github.com/scottgerring/systems-to-services
export DD_GET_COMMIT_SHA=70764ee11cab051fcb6174b1fa2023811c19cea5

java -javaagent:dd-java-agent.jar -Ddd.profiling.enabled=true -XX:FlightRecorderOptions=stackdepth=256 -Ddd.logs.injection=true -Ddd.service=dives -Ddd.env=staging -Ddd.version=1.0 -jar target/quarkus-app/quarkus-run.jar
