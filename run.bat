@echo off
setlocal
rem Use UTF-8 in the console so file names with accents print correctly.
chcp 65001 >nul

set "JAR=%~dp0target\media-tools.jar"
if not exist "%JAR%" (
    echo %JAR% not found. Build it first with: mvn package
    exit /b 1
)

java -jar "%JAR%" %*
