#!/bin/bash
# Setup and Demo Script for URL Shortener Service

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=========================================="
echo "URL Shortener Service - Setup & Demo"
echo "=========================================="
echo ""

# Check Java version
echo "1. Checking Java installation..."
JAVA_VERSION=$(java -version 2>&1 | grep 'version' | awk -F'"' '{print $2}')
echo "   ✓ Java version: $JAVA_VERSION"
if [[ ! "$JAVA_VERSION" =~ ^21|25 ]]; then
    echo "   ⚠ Warning: Java 21+ recommended (found $JAVA_VERSION)"
fi
echo ""

# Build project
echo "2. Building project..."
if [[ -f "gradlew" ]]; then
    echo "   Using gradle wrapper..."
    chmod +x gradlew
    ./gradlew clean build -x test --info 2>&1 | tail -20
else
    echo "   Gradle wrapper not found, using installed gradle..."
    gradle clean build -x test
fi
echo "   ✓ Build complete"
echo ""

# Start application
echo "3. Starting application..."
if [[ -f "build/libs/url-shortener-service-1.0.0.jar" ]]; then
    echo "   Starting JAR: build/libs/url-shortener-service-1.0.0.jar"
    java --enable-preview -jar build/libs/url-shortener-service-1.0.0.jar &
    APP_PID=$!
    echo "   ✓ Application started (PID: $APP_PID)"
else
    echo "   ✗ JAR not found, running via gradle..."
    ./gradlew bootRun --enable-preview &
    APP_PID=$!
fi
echo ""

# Wait for application to start
echo "4. Waiting for application to be ready..."
sleep 3
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "   ✓ Application is ready!"
        break
    fi
    echo -n "."
    sleep 1
done
echo ""

# Health check
echo "5. Health Check"
echo "   GET http://localhost:8080/actuator/health"
curl -s http://localhost:8080/actuator/health | jq '.'
echo ""

# Demo: Shorten URL
echo "6. Demo: Shorten URL (Direct API)"
echo "   POST http://localhost:8080/api/urls"
RESULT=$(curl -s -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com/copilot"}')
echo "$RESULT" | jq '.'
SHORT_CODE=$(echo "$RESULT" | jq -r '.shortCode')
echo ""

# Demo: Orchestrated workflow
echo "7. Demo: Orchestrated Workflow (with custom alias)"
echo "   POST http://localhost:8080/api/urls/orchestrated"
WORKFLOW=$(curl -s -X POST http://localhost:8080/api/urls/orchestrated \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com/features","customAlias":"gh-features"}')
echo "$WORKFLOW" | jq '.'
EXEC_ID=$(echo "$WORKFLOW" | jq -r '.executionId')
echo ""

# Approve workflow
echo "8. Demo: Approve Workflow"
echo "   POST http://localhost:8080/api/workflows/$EXEC_ID/approve"
APPROVAL=$(curl -s -X POST http://localhost:8080/api/workflows/$EXEC_ID/approve)
echo "$APPROVAL" | jq '.'
echo ""

# Get URL details
echo "9. Demo: Get URL Details"
echo "   GET http://localhost:8080/api/urls/$SHORT_CODE"
curl -s http://localhost:8080/api/urls/$SHORT_CODE | jq '.'
echo ""

# Redirect test
echo "10. Demo: Redirect (with click tracking)"
echo "    GET http://localhost:8080/s/$SHORT_CODE"
echo "    (This will return HTTP 301 redirect)"
curl -s -i http://localhost:8080/s/$SHORT_CODE | head -10
echo ""

# Metrics
echo "11. Metrics & Observability"
echo "    GET http://localhost:8080/actuator/metrics"
curl -s http://localhost:8080/actuator/metrics | jq '.names | .[:5]'
echo ""

echo "=========================================="
echo "✓ Demo Complete!"
echo "=========================================="
echo ""
echo "Useful endpoints:"
echo "  - Health: http://localhost:8080/actuator/health"
echo "  - Metrics: http://localhost:8080/actuator/metrics"
echo "  - Prometheus: http://localhost:8080/actuator/metrics/prometheus"
echo "  - API Docs: Available in README.md"
echo ""
echo "To stop application:"
echo "  kill $APP_PID"
echo ""
