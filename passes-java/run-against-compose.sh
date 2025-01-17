#!/bin/bash

export DD_SERVICE=passes-java
export DD_ENV=

java -jar target/quarkus-app/quarkus-run.jar
