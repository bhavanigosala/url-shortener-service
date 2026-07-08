# URL Shortener Service - Start Here

## Welcome! 👋

You now have a **complete, production-grade URL Shortener Service** built with:
- **Spring Boot 3.3** - Latest stable version
- **Java 21+** - Modern language features
- **Gradle 8.8** - Build automation  
- **Agentic Orchestration** - Workflow engine with approval gates
- **Resilience Patterns** - Circuit breaker, retry, rate limiting

---

## 📖 Documentation Index (Start Here!)

### 1️⃣ **QUICK_START.md** ← Start Here! (5-10 minutes)
**What**: Setup instructions and first test
**For**: Getting the application running immediately
**Contains**: Prerequisites, build steps, first API calls, examples

### 2️⃣ **README.md** (15 minutes)
**What**: Complete architecture and feature documentation
**For**: Understanding the full system design
**Contains**: Architecture diagrams, all API endpoints, three scenarios, resilience patterns, production deployment

### 3️⃣ **CODE_EXPLANATION.md** (30-60 minutes)
**What**: Deep technical code walkthrough
**For**: Learning modern Java features and design patterns  
**Contains**: Records, sealed classes, pattern matching, virtual threads, component walkthroughs, resilience patterns

### 4️⃣ **PROJECT_SUMMARY.md** (10 minutes)
**What**: Executive summary of entire project
**For**: Quick overview of what's included and highlights
**Contains**: Project statistics, features list, deliverables, learning paths

### 5️⃣ **QUICK_REFERENCE.md** (Reference)
**What**: Developer cheat sheet and quick commands
**For**: During development for quick lookups
**Contains**: API endpoints, curl examples, file locations, configuration changes

---

## 🚀 Getting Started (3 Minutes)

### Windows Users:
```bash
cd url-shortener-service
demo.bat
```

### Mac/Linux Users:
```bash
cd url-shortener-service
chmod +x demo.sh
./demo.sh
```

This will:
1. ✓ Build the project
2. ✓ Start the application
3. ✓ Run automated test requests
4. ✓ Show API responses

---

## 📁 What You Have

### Project Structure
```
url-shortener-service/
├── 📄 Documentation Files (5 guides, 62 KB)
│   ├── QUICK_START.md           ← Start here
│   ├── README.md                ← Full guide
│   ├── CODE_EXPLANATION.md      ← Deep dive
│   ├── PROJECT_SUMMARY.md       ← Overview  
│   └── QUICK_REFERENCE.md       ← Cheat sheet
│
├── 🏗️  Build & Configuration
│   ├── build.gradle.kts         ← Dependencies & build
│   ├── settings.gradle.kts      ← Project settings
│   ├── gradle/wrapper/          ← Gradle wrapper
│   ├── gradlew & gradlew.bat    ← Gradle executables
│   └── demo.sh & demo.bat       ← Demo scripts
│
└── 💻 Source Code
    ├── src/main/java/com/urlshortener/
    │   ├── UrlShortenerApplication.java
    │   ├── controller/           ← REST APIs
    │   ├── service/              ← Business logic
    │   ├── orchestration/        ← State machine
    │   ├── domain/               ← Data models (Records)
    │   ├── repository/           ← Data access
    │   ├── analytics/            ← Analytics models
    │   └── config/               ← Configuration
    │
    └── src/test/java/
        └── UrlShortenerServiceTest.java  ← 12+ tests
```

### Key Files by Role

| Who | Read First | Then | Then |
|-----|-----------|------|------|
| **Quick Start** | QUICK_START.md | Run demo.bat | Try API calls |
| **Architect** | README.md | CODE_EXPLANATION.md | Check orchestration |
| **Developer** | CODE_EXPLANATION.md | QUICK_REFERENCE.md | Explore src/main/java |
| **Manager** | PROJECT_SUMMARY.md | README.md | Review features |

---

## 🎯 First 10 Minutes

```
Minute 1-2: cd url-shortener-service
Minute 3-5: Run demo.bat (or demo.sh on Mac/Linux)
Minute 6-8: Review curl output, see API responses
Minute 9-10: Try your own curl request
```

**Try this:**
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com"}'
```

**Expected response:**
```json
{
  "shortCode": "a1b2c3d",
  "shortUrl": "http://localhost:8080/s/a1b2c3d",
  "originalUrl": "https://github.com",
  "clicks": 0
}
```

---

## 📚 What's Included

### Core Features
- ✅ URL shortening (auto-generated codes)
- ✅ Custom aliases (with approval workflow)
- ✅ Click tracking (async, non-blocking)
- ✅ URL expiration (time-limited links)
- ✅ Audit trail (complete execution history)

### Architecture Highlights
- ✅ 6-layer clean architecture
- ✅ State machine orchestration (6 states)
- ✅ Sealed classes + pattern matching
- ✅ Circuit breaker + retry + rate limiting
- ✅ Async processing with virtual threads
- ✅ Full test coverage

### Modern Java
- ✅ Records (DTO data carriers)
- ✅ Sealed interfaces (bounded types)
- ✅ Pattern matching (exhaustive switches)
- ✅ Virtual threads ready
- ✅ Text blocks ready

### Documentation
- ✅ 62 KB (5 comprehensive guides)
- ✅ Architecture diagrams
- ✅ Code walkthroughs
- ✅ Quick reference
- ✅ Running examples

---

## 🔄 Three Scenarios Included

### Scenario 1: Simple (Greenfield)
```bash
POST /api/urls
{"originalUrl": "https://github.com"}
→ 201 Created with short code
```
**Time**: ~10ms | **Workflow**: PENDING → RUNNING → COMPLETED

### Scenario 2: With Approval (Brownfield) 
```bash
POST /api/urls/orchestrated
{"originalUrl": "https://github.com", "customAlias": "gh"}
→ 202 Accepted (waiting for approval)
POST /api/workflows/exec-1/approve
→ 200 OK (completed)
```
**Time**: ~100ms + approval time | **Workflow**: Has APPROVAL_PENDING gate

### Scenario 3: Complex (Ambiguous)
```bash
POST /api/urls/orchestrated
{
  "originalUrl": "https://temp.link",
  "customAlias": "temp",
  "expiresAt": "2024-07-07T00:00:00Z"
}
```
**Workflow**: Validates, requests approval, sets expiration, completes

---

## 💡 Key Concepts Explained

### Records
```java
public record ShortenUrlRequest(
    String originalUrl,
    String customAlias,
    Instant expiresAt
) {}
// Automatic: toString(), equals(), hashCode()
// Immutable data carrier
```

### Sealed Classes
```java
public sealed interface WorkflowState {
    record PendingState(...) implements WorkflowState {}
    record RunningState(...) implements WorkflowState {}
    record ApprovalPendingState(...) implements WorkflowState {}
    record CompletedState(...) implements WorkflowState {}
    record FailedState(...) implements WorkflowState {}
    record RolledBackState(...) implements WorkflowState {}
}
// Only 6 states allowed, compiler enforces
```

### Pattern Matching
```java
String result = switch (state) {
    case ApprovalPendingState approval -> "waiting for approval";
    case CompletedState completed -> "done";
    case FailedState failed -> "failed";
    // ... all 6 cases required by compiler
};
```

---

## 🛠️ Quick Command Reference

```bash
# Build
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test

# Run application
java -jar build/libs/url-shortener-service-1.0.0.jar

# Or run via gradle
./gradlew bootRun

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests UrlShortenerServiceTest

# Health check
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics
```

---

## 📞 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Port 8080 in use" | Change in application.properties: `server.port=8081` |
| "Build fails" | Run `./gradlew clean` and try again |
| "Java not found" | Install Java 21+ from oracle.com |
| "Slow first build" | First run downloads Gradle & dependencies - normal |

---

## 🎓 Learning Path

**Day 1:**
1. Read QUICK_START.md
2. Run demo.bat
3. Try example API calls
4. Check health endpoint

**Day 2:**
1. Read README.md
2. Study CODE_EXPLANATION.md
3. Explore src/main/java files
4. Run tests

**Day 3:**
1. Review WorkflowOrchestrator.java
2. Understand state machine
3. Study resilience patterns
4. Try modifying code

**Day 4-5:**
1. Add custom validation
2. Write more tests
3. Deploy with PostgreSQL
4. Set up monitoring

---

## 🏆 What You'll Learn

- ✅ Modern Java (Records, Sealed Classes, Pattern Matching)
- ✅ Spring Boot 3.3 best practices
- ✅ Agentic orchestration & state machines
- ✅ Resilience engineering patterns
- ✅ Clean architecture principles
- ✅ API design & REST
- ✅ Testing strategies
- ✅ Production deployment

---

## ✨ Ready to Start?

### Option 1: Fastest Way (5 minutes)
```bash
cd url-shortener-service
demo.bat                    # Windows
# OR
./demo.sh                   # Mac/Linux
```

### Option 2: Manual Setup (10 minutes)
```bash
cd url-shortener-service
./gradlew clean build -x test
java -jar build/libs/url-shortener-service-1.0.0.jar
```

### Option 3: Gradle Dev Mode (Continuous)
```bash
cd url-shortener-service
./gradlew bootRun
```

---

## 📖 Next Step: Read QUICK_START.md

That file has everything you need to build and run the application.

**Happy coding! 🚀**

---

## 📍 File Map

```
START HERE
    ↓
QUICK_START.md (build & run)
    ↓
README.md (understand architecture)
    ↓
CODE_EXPLANATION.md (deep dive)
    ↓
QUICK_REFERENCE.md (cheat sheet)
    ↓
PROJECT_SUMMARY.md (overview)
    ↓
src/main/java (explore code)
    ↓
Write tests & deploy!
```

---

**Status**: ✅ Complete and ready to build
**Java Version**: 21+ (tested with Java 25)
**Framework**: Spring Boot 3.3
**Build**: Gradle 8.8
**Location**: `C:\Users\1035804\Documents\gitnew\url-shortener-service`

**Start now with: `demo.bat` (Windows) or `./demo.sh` (Mac/Linux)**
