#!/usr/bin/env sh
APP_HOME=$(pwd)
exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
