# QUICK START GUIDE - URL Shortener Service

## 📋 Prerequisites

- **Java 21+** (preferably Java 25 or latest LTS)
  - Download from: https://www.oracle.com/java/technologies/downloads/
  - Verify: `java -version`
  
- **Internet connection** (for Gradle to download dependencies first time)

## 🚀 Option 1: Quick Start (Windows)

```bash
cd url-shortener-service
demo.bat
```

This will:
1. Build the project
2. Start the application
3. Run automated demo requests
4. Show API responses

## 🚀 Option 2: Quick Start (Mac/Linux)

```bash
cd url-shortener-service
chmod +x demo.sh
./demo.sh
```

## 🏗️ Manual Build & Run

### Step 1: Build Project

```bash
cd url-shortener-service

# Using Gradle wrapper (first run downloads Gradle)
./gradlew clean build -x test    # Unix/Mac
gradlew.bat clean build -x test  # Windows

# Expected output: "BUILD SUCCESSFUL in ~60s"
```

### Step 2: Run Application

```bash
# Option A: Run JAR directly
java --enable-preview -jar build/libs/url-shortener-service-1.0.0.jar

# Option B: Run via Gradle
./gradlew bootRun  # Unix/Mac
gradlew.bat bootRun  # Windows

# Expected output: Application starts on port 8080
```

### Step 3: Test Application

Open a new terminal:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Shorten a URL
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com"}'

# Expected response:
# {
#   "shortCode": "a1b2c3d",
#   "originalUrl": "https://github.com",
#   "shortUrl": "http://localhost:8080/s/a1b2c3d",
#   "clicks": 0
# }

# Use the short code to redirect
curl -v http://localhost:8080/s/a1b2c3d
```

## 🎯 Key API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/urls` | Create shortened URL |
| `POST` | `/api/urls/orchestrated` | Create with workflow orchestration |
| `GET` | `/api/urls/{code}` | Get URL details |
| `GET` | `/s/{code}` | Redirect to original URL |
| `DELETE` | `/api/urls/{code}` | Delete URL |
| `POST` | `/api/workflows/{id}/approve` | Approve pending workflow |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/metrics` | Metrics |

## 📊 Example Workflows

### Workflow 1: Simple URL Shortening

```bash
# Request
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com/very/long/url/path"
  }'

# Response (201 Created)
{
  "shortCode": "x9y8z7",
  "originalUrl": "https://github.com/very/long/url/path",
  "shortUrl": "http://localhost:8080/s/x9y8z7",
  "createdAt": "2024-07-06T19:22:00Z",
  "expiresAt": null,
  "clicks": 0
}
```

### Workflow 2: Orchestrated with Custom Alias (Requires Approval)

```bash
# Request - Custom alias triggers approval workflow
curl -X POST http://localhost:8080/api/urls/orchestrated \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com",
    "customAlias": "gh"
  }'

# Response (202 Accepted - Waiting for approval)
{
  "executionId": "exec-123",
  "state": "ApprovalPendingState",
  "result": null,
  "auditLog": [
    {"timestamp": "...", "message": "Workflow created", "stateName": "PendingState"},
    {"timestamp": "...", "message": "Validation passed", "stateName": "RunningState"},
    {"timestamp": "...", "message": "Approval required for custom alias: gh", "stateName": "ApprovalPendingState"}
  ]
}

# Human reviews and approves
curl -X POST http://localhost:8080/api/workflows/exec-123/approve

# Response (200 OK - Approved)
{
  "executionId": "exec-123",
  "state": "CompletedState",
  "result": {
    "shortCode": "gh",
    "shortUrl": "http://localhost:8080/s/gh"
  }
}
```

### Workflow 3: Click Tracking & Analytics

```bash
# Access the shortened URL
curl http://localhost:8080/s/x9y8z7
# Returns: HTTP 301 Permanent Redirect
# Location: https://github.com/very/long/url/path
# Automatically tracks click in background

# Check URL details (clicks incremented)
curl http://localhost:8080/api/urls/x9y8z7
# Response shows: "clicks": 1 (or more)
```

## 🔍 Monitoring & Observability

```bash
# Application Health
curl http://localhost:8080/actuator/health | jq '.'

# Application Metrics
curl http://localhost:8080/actuator/metrics | jq '.names[]' | head -10

# Prometheus Metrics
curl http://localhost:8080/actuator/metrics/prometheus

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.'
```

## 🧪 Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests UrlShortenerServiceTest

# Run with output
./gradlew test --info
```

## 📝 Logs & Debugging

By default, logs are shown in the console. To see debug logs:

```bash
# Option 1: Set via environment variable
export LOGGING_LEVEL_COM_URLSHORTENER=DEBUG
./gradlew bootRun

# Option 2: Edit src/main/resources/application.properties
logging.level.com.urlshortener=DEBUG
```

## 🛑 Stopping the Application

```bash
# If running in foreground: Ctrl+C

# If running in background (Windows):
taskkill /FI "WINDOWTITLE eq URL Shortener*"

# If running in background (Mac/Linux):
kill %1  # or use the displayed PID
```

## 📚 Documentation

See **README.md** for:
- Architecture deep-dive
- Modern Java features explained
- Resilience patterns details
- Production deployment
- Troubleshooting

## 💡 Common Issues

| Issue | Solution |
|-------|----------|
| `Java not found` | Install Java 21+ from oracle.com |
| `Port 8080 already in use` | Kill existing process or change port in application.properties |
| `BUILD FAILED` | Ensure Java 21+ is installed, run `./gradlew clean` |
| `Application won't start` | Check logs for errors, ensure port 8080 is available |
| `Cannot find API endpoints` | Wait 5-10 seconds after start, application needs time to initialize |

## 🎓 Learning Path

1. **Start here**: Run the quick start demo
2. **Explore APIs**: Test each endpoint with curl
3. **Read code**: Check `src/main/java/com/urlshortener/`
4. **Understand orchestration**: Study `WorkflowOrchestrator.java`
5. **Try modifications**: Change business logic and rebuild
6. **Write tests**: Add tests in `src/test/java/`

## 📞 Need Help?

1. Check application logs (see Logs & Debugging section)
2. Review README.md for detailed documentation
3. Check health endpoint: `curl http://localhost:8080/actuator/health`
4. Review API response codes and error messages

---

**Ready to build and test? Start with `demo.bat` (Windows) or `demo.sh` (Mac/Linux)! 🚀**
