@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%~fD"

if not defined JAVA_HOME (
  echo Java 21 nu a fost gasit. Instaleaza-l cu:
  echo winget install EclipseAdoptium.Temurin.21.JDK
  pause
  exit /b 1
)

echo Pornesc Task Management pe http://localhost:8080 ...
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:8080"
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

if errorlevel 1 pause
