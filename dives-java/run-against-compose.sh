#!/bin/bash

export DD_SERVICE=dives-java
export DD_ENV=

java -jar target/quarkus-app/quarkus-run.jar
