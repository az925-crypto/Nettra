#!/bin/sh
# Minimal gradlew stub — delegates to local gradle if available, else downloads wrapper
set -e
GRADLE_VERSION="8.12"
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
# Fallback: try wrapper jar
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
  echo "Gradle wrapper jar not found. Install Gradle ${GRADLE_VERSION} or run via GitHub Actions (see notes.txt: build berat di CI)."
  echo "Attempting to use sdkmanager/gradle download..."
  exit 1
fi
