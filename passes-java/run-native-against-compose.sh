#!/bin/bash

export DD_SERVICE=passes-java
export DD_ENV=
export DD_GIT_REPOSITORY_URL=github.com/scottgerring/systems-to-services
export DD_GET_COMMIT_SHA=70764ee11cab051fcb6174b1fa2023811c19cea5

target/passes-java-1.0.0-SNAPSHOT-runner
