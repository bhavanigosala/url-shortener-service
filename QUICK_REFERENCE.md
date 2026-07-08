# URL Shortener Service - Developer Quick Reference

## 🚀 Quick Commands

```bash
# Build project
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test

# Run application
java -jar build/libs/url-shortener-service-1.0.0.jar
# or
./gradlew bootRun

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests UrlShortenerServiceTest

# Clean build cache
./gradlew clean
```

## 📡 API Endpoints Cheat Sheet

### Create Shortened URL (Direct)
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com"}'
```

### Create with Orchestration (Workflow + Approval)
```bash
curl -X POST http://localhost:8080/api/urls/orchestrated \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com/copilot",
    "customAlias": "copilot",
    "expiresAt": "2024-07-08T00:00:00Z"
  }'
```

### Get URL Details
```bash
curl http://localhost:8080/api/urls/{shortCode}
```

### Redirect to Original (Tracking Click)
```bash
curl -v http://localhost:8080/s/{shortCode}
# Returns: HTTP 301 Permanent Redirect
```

### Approve Pending Workflow
```bash
curl -X POST http://localhost:8080/api/workflows/{executionId}/approve
```

### Reject Pending Workflow
```bash
curl -X POST http://localhost:8080/api/workflows/{executionId}/reject \
  -H "Content-Type: application/json" \
  -d '{"reason":"Invalid alias"}'
```

### Get Workflow Status
```bash
curl http://localhost:8080/api/workflows/{executionId}
```

### Delete URL
```bash
curl -X DELETE http://localhost:8080/api/urls/{shortCode}
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

## 🏗️ Key File Locations

| File | Purpose | Key Content |
|------|---------|------------|
| `UrlShortenerApplication.java` | Entry point | Spring Boot main class |
| `UrlShortenerController.java` | REST APIs | HTTP endpoints |
| `RedirectController.java` | Redirects | 301 redirect + click tracking |
| `UrlShortenerService.java` | Core logic | URL shortening with resilience |
| `WorkflowOrchestrator.java` | Orchestration | State machine + approval gates |
| `WorkflowState.java` | States | Sealed class with 6 state types |
| `ShortenUrlRequest.java` | Request DTO | Record with validation |
| `ShortenUrlResponse.java` | Response DTO | Record for API responses |
| `ClickAnalyticsService.java` | Analytics | @Async click tracking |
| `InMemoryUrlRepository.java` | Data access | In-memory implementation |
| `AsyncConfig.java` | Configuration | Virtual threads setup |
| `application.properties` | Settings | All app configuration |

## 🔍 Code Navigation Guide

### Understanding URL Shortening Flow
1. Start at `UrlShortenerController.shortenUrl()`
2. Calls `UrlShortenerService.shortenUrl()`
3. Uses `generateShortCode()` for code creation
4. Persists via `InMemoryUrlRepository.save()`
5. Returns `ShortenUrlResponse` (Record)

### Understanding Orchestration Flow
1. Start at `UrlShortenerController.shortenUrlOrchestrated()`
2. Calls `WorkflowOrchestrator.executeUrlShorteningWorkflow()`
3. Creates `WorkflowExecution` with tracking
4. Sets state: `PendingState` → `RunningState`
5. Validates request via entry gate
6. Checks if approval needed (custom alias)
7. If approval needed → `ApprovalPendingState` (paused)
8. If auto-approved → continues to `CompletedState`
9. On error → `FailedState` or `RolledBackState`
10. Logs all transitions in audit trail

### Understanding Click Tracking
1. User accesses `GET /s/{code}`
2. `RedirectController.redirect()` processes request
3. Calls `UrlShortenerService.resolveUrl()` (resolves URL)
4. Returns HTTP 301 redirect
5. Calls `ClickAnalyticsService.recordClickAsync()` (async)
6. Analytics processed non-blocking in thread pool

## 📝 Workflow State Machine States

```
PendingState
  └─ [Validate]
     └─ RunningState
        └─ [Check Approval Needed?]
           ├─ [Yes] → ApprovalPendingState
           │            └─ [Human Approves]
           │               └─ CompletedState
           │
           └─ [No] → Execute Logic
                      └─ [Success?]
                         ├─ [Yes] → CompletedState
                         └─ [No] → FailedState
                                    └─ RolledBackState
```

## 🛠️ Resilience Patterns Applied

### Circuit Breaker
```java
@CircuitBreaker(name = "urlShortener")
public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) { ... }
```
**Config**: 50% failure → Open for 30s

### Retry
```java
@Retry(name = "urlShortener")
public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) { ... }
```
**Config**: 3 attempts with 1s wait

### Rate Limiting
```java
@RateLimiter(name = "urlShortener")
public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) { ... }
```
**Config**: 100 requests per minute

### Async Processing
```java
@Async
public void recordClickAsync(String shortCode) { ... }
```
**Config**: Thread pool 10 core / 20 max threads

## 🧪 Test Cases

```bash
# Run all tests
./gradlew test

# All covered scenarios:
testShortenUrl()              # Basic shortening
testShortenUrlWithCustomAlias() # Custom code
testResolveUrl()              # URL lookup
testExpiredUrl()              # Expiration handling
testDuplicateShortCodePrevention() # Uniqueness
testDeleteUrl()               # Deletion
testRejectBlankUrl()          # Validation
testClickTracking()           # Analytics
```

## 📊 Monitoring

### Health Endpoint
```bash
curl http://localhost:8080/actuator/health | jq '.'
```
Shows: Database, Circuit Breaker, Application status

### Metrics Endpoint
```bash
curl http://localhost:8080/actuator/metrics | jq '.names[] | select(. | contains("http"))'
```
Shows: Request counts, latencies, errors

### Prometheus
```bash
curl http://localhost:8080/actuator/metrics/prometheus
```
Prometheus-formatted metrics for dashboards

## 🔐 Security Checklist

- ✅ Input validation on all URLs
- ✅ XSS prevention (URL encoding)
- ✅ Rate limiting enabled
- ✅ Circuit breaker for protection
- ✅ Expiration support
- ✅ Audit logging enabled
- ✅ No hardcoded secrets
- ✅ Externalized config

## 🚀 Performance Tips

1. **Click Analytics**: Runs async, doesn't block requests
2. **URL Resolution**: <1ms lookup via in-memory index
3. **Virtual Threads**: Spring 3.3+ ready for 1M+ concurrency
4. **Caching**: Add Redis for <100μs lookups
5. **Database**: Use PostgreSQL for persistence
6. **Rate Limiting**: Adjust per deployment needs

## 📚 Modern Java Features Used

| Feature | Example | Location |
|---------|---------|----------|
| **Records** | `record ShortenUrlRequest(...)` | `ShortenUrlRequest.java` |
| **Sealed Classes** | `sealed interface WorkflowState` | `WorkflowState.java` |
| **Pattern Matching** | `switch (state) { case ... -> ... }` | `WorkflowOrchestrator.java` line 89 |
| **@Async** | `@Async public void recordClick()` | `ClickAnalyticsService.java` |
| **Virtual Threads** | Spring 3.3+ auto-support | `AsyncConfig.java` |

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 in use | Change in `application.properties`: `server.port=8081` |
| Build fails | Run `./gradlew clean` then rebuild |
| Tests fail | Ensure Java 21+ installed: `java -version` |
| Slow startup | First run downloads Gradle/dependencies - normal |
| Circuit breaker open | Service is failing - check health endpoint |
| Approval pending | Approve via: `POST /api/workflows/{id}/approve` |

## 🔄 Configuration Changes Quick Guide

### Change Database
```properties
# In application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/urls
spring.datasource.username=user
spring.datasource.password=pass
spring.jpa.hibernate.ddl-auto=validate
```

### Change Port
```properties
server.port=9000
```

### Enable Debug Logging
```properties
logging.level.com.urlshortener=DEBUG
```

### Adjust Rate Limit
```properties
resilience4j.ratelimiter.instances.urlShortener.limit-for-period=200
resilience4j.ratelimiter.instances.urlShortener.limit-refresh-period=1m
```

### Adjust Async Pool
```properties
spring.task.execution.pool.core-size=20
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=200
```

## 📖 Documentation Map

| Document | Purpose | Read When |
|----------|---------|-----------|
| `README.md` | Full architecture & deployment | First, understand system |
| `QUICK_START.md` | Setup & getting started | Need to run application |
| `CODE_EXPLANATION.md` | Deep code walkthrough | Want to understand code |
| `PROJECT_SUMMARY.md` | Complete project overview | Need comprehensive summary |
| This file | Quick reference | During development |

## 🎯 Development Workflow

```
1. Make changes to Java files
2. Run tests: ./gradlew test
3. Build: ./gradlew clean build
4. Run: java -jar build/libs/url-shortener-service-1.0.0.jar
5. Test APIs with curl
6. Check health: curl http://localhost:8080/actuator/health
7. Review logs in console output
8. Repeat!
```

## 💡 Pro Tips

- Use `--info` flag for verbose gradle output: `./gradlew build --info`
- Run tests with output: `./gradlew test --info`
- Use jq for pretty JSON: `curl http://localhost:8080/api/... | jq '.'`
- Check metrics dashboard: Create Grafana dashboard from Prometheus
- Deploy to Docker: `docker run -p 8080:8080 url-shortener:latest`
- Enable virtual threads when Spring 3.3 stable: Add `spring.threads.virtual.enabled=true`

## 🆘 Getting Help

1. Check logs in console output
2. Review `CODE_EXPLANATION.md` for detailed walkthroughs
3. Check health endpoint: `curl http://localhost:8080/actuator/health`
4. Run tests: `./gradlew test --info`
5. Review specific Java file mentioned in error
6. Check `application.properties` for configuration issues

---

**Quick Start**: See QUICK_START.md
**Architecture**: See README.md
**Deep Dive**: See CODE_EXPLANATION.md
**This Reference**: Bookmark this file! 📌
