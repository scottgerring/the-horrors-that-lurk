#!/bin/bash

./mvnw package
docker-compose build
docker-compose up