@echo off
set "JAVA_HOME=d:\work\LyttleNametag-main\jdk21_extracted\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew clean build -x test
