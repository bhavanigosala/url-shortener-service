@echo off
REM Setup and Demo Script for URL Shortener Service (Windows)
REM This script builds, starts, and demonstrates the URL Shortener

setlocal enabledelayedexpansion

echo.
echo ==========================================
echo URL Shortener Service - Setup and Demo
echo ==========================================
echo.

REM 1. Check Java version
echo 1. Checking Java installation...
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| find "version"') do set JAVA_VERSION=%%i
echo    Java version: %JAVA_VERSION%
echo.

REM 2. Build project
echo 2. Building project with Gradle...
if exist "gradlew.bat" (
    call gradlew.bat clean build -x test --quiet
    if errorlevel 1 (
        echo    Building with gradlew...
        call gradlew.bat clean build -x test
    )
) else (
    gradle clean build -x test
)
echo    [OK] Build complete
echo.

REM 3. Start application
echo 3. Starting application...
if exist "build\libs\url-shortener-service-1.0.0.jar" (
    echo    Starting JAR: build\libs\url-shortener-service-1.0.0.jar
    start "URL Shortener Service" java --enable-preview -jar build\libs\url-shortener-service-1.0.0.jar
    set APP_STARTED=true
) else (
    echo    Starting via gradle...
    start "URL Shortener Service" cmd /k gradlew.bat bootRun
    set APP_STARTED=true
)
echo    [OK] Application started
echo.

REM 4. Wait for application
echo 4. Waiting for application to be ready (this may take 5-10 seconds)...
timeout /t 5 /nobreak
echo.

REM 5. Health check
echo 5. Performing health check...
echo    GET http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/health
echo.
echo.

REM 6. Shorten URL
echo 6. Demo: Shorten URL ^(Direct API^)
echo    POST http://localhost:8080/api/urls
echo.
for /f "usebackq delims=" %%i in (`curl -s -X POST http://localhost:8080/api/urls -H "Content-Type: application/json" -d "{\"originalUrl\":\"https://github.com/copilot\"}"`) do set RESULT=%%i
echo %RESULT%
echo.

REM Extract short code using findstr (basic extraction)
echo 7. URL Details
echo    The short code was generated. Check http://localhost:8080/api/urls/[shortCode]
echo.

REM 8. Orchestrated workflow
echo 8. Demo: Orchestrated Workflow ^(with custom alias^)
echo    POST http://localhost:8080/api/urls/orchestrated
echo.
curl -s -X POST http://localhost:8080/api/urls/orchestrated -H "Content-Type: application/json" -d "{\"originalUrl\":\"https://github.com/features\",\"customAlias\":\"gh-features\"}"
echo.
echo.

REM 9. Metrics
echo 9. Observability Endpoints
echo    Health: http://localhost:8080/actuator/health
echo    Metrics: http://localhost:8080/actuator/metrics
echo    Prometheus: http://localhost:8080/actuator/metrics/prometheus
echo.

echo ==========================================
echo [OK] Demo Complete!
echo ==========================================
echo.
echo The application is now running on http://localhost:8080
echo.
echo To stop the application, close the "URL Shortener Service" window
echo or run: taskkill /FI "WINDOWTITLE eq URL Shortener Service*"
echo.
pause
