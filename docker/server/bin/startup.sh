#!/bin/sh
# startup.sh - startup script for the server docker image

echo "Starting Conductor server"

# Start the server
cd /app/libs

echo "Using java options config: $JAVA_OPTS"

java ${JAVA_OPTS} -jar conductor-server-*-boot.jar
