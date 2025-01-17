#!/bin/bash

if [ ! -f "dd-agent-java.jar" ] ; then
  wget -O dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'
fi

quarkus build

java -jar