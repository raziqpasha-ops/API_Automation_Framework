# Restful-Booker API Automation Framework

A **100% reusable, FAANG-style API automation framework** built for the [Restful-Booker](https://restful-booker.herokuapp.com) playground API — designed to be plugged onto any REST API with minimal changes.

**Tech stack:** Java 11 · RestAssured (BDD) · Maven · TestNG · Jackson POJOs · Log4j2 · Allure · Jenkins

**Current status:** 11/11 tests passing against the live API · Allure report generated · CI-ready

---

## 1. Why this framework (interview pitch)

| Problem in naive API tests | How this framework solves it |
|---|---|
| Repeated base URL / headers in every test | `RequestSpecification` built once in `SpecFactory`, injected via `.spec()` |
| Endpoints scattered as raw strings | Single `Endpoints` constants class — a path change is a 1-line fix, typos become compile errors |
| Hand-written JSON bodies | **Serialization** — Jackson converts POJOs to JSON automatically |
| Fragile string parsing of responses | **Deserialization** — `.as(Booking.class)` maps JSON back to typed POJOs |
| Flaky CI builds from rate limits / network blips | Two retry layers: `RetryExecutor` (request level) + `RetryAnalyzer` (test level) |
| "Where did it fail?" debugging pain | Log4j2 rolling logs + `TestListener` logs full failure diagnosis (type, message, location, stack trace) |
| Raw `Assert` calls everywhere | Reusable `AssertionActions` keyword library — one place to evolve assertion behaviour |
| Tests that only check one endpoint | True end-to-end test: create → read → update → patch → delete → verify gone |
| Data collisions on shared servers | `BookingTestDataFactory` generates random unique data per run |
| Environment hard-coded | `ConfigManager` reads `config.properties`; Jenkins `-DbaseUrl=...` overrides without code change |

---

## 2. Project structure

```
API_Automation_Framework/
├── pom.xml                                  ← Maven build: all dependencies + surefire + Allure agent
├── testng.xml                               ← Suite plan: listeners + test execution order
├── Jenkinsfile                              ← CI pipeline-as-code (build → test → Allure report)
├── README.md                                ← this file
├── logs/                                    ← generated per run (gitignored)
│   └── framework.log                        ← rolling log: every request, assertion, failure
└── src/
    ├── main/java/com/booking/               ← FRAMEWORK ENGINE (reusable, jar-publishable)
    │   ├── assertions/AssertionActions      ← reusable assertion keywords (log + delegate to TestNG)
    │   ├── config/ConfigManager             ← reads config.properties, -D system property overrides
    │   ├── constants/Endpoints.java         ← all API paths as constants
    │   ├── listeners/TestListener           ← TestNG events → Log4j2 (incl. full failure diagnosis)
    │   ├── listeners/RetryListener          ← attaches RetryAnalyzer to EVERY test globally
    │   ├── pojo/                            ← Booking, BookingDates, BookingResponse, AuthRequest, AuthResponse
    │   ├── retry/RetryAnalyzer              ← reruns a failed test up to 2 times (flaky protection)
    │   ├── services/AuthService             ← POST /auth keyword → returns token String
    │   ├── services/BookingService          ← one reusable method per booking endpoint
    │   ├── spec/SpecFactory                 ← reusable RequestSpecification + ResponseSpecification
    │   ├── testdata/BookingTestDataFactory  ← random valid test data (default + override pattern)
    │   └── utils/
    │       ├── LoggerManager                ← single factory for Log4j2 loggers
    │       └── RetryExecutor                ← retries API calls on 429/5xx only
    ├── main/resources/
    │   └── log4j2.xml                       ← console + rolling-file logging configuration
    └── test/
        ├── java/com/booking/tests/          ← TESTS ONLY (pure scenarios, no plumbing)
        │   ├── AuthAndNegativeTest          ← token, 403 security, 404, /ping health
        │   ├── BookingCrudTest              ← component-level CRUD, one endpoint per test
        │   └── E2E_BookingFlowTest          ← full life cycle: create→read→update→patch→delete
        └── resources/
            └── config.properties            ← baseUrl, username, password (env-switchable)
```

**Key design rule:** `src/main` = reusable engine, `src/test` = scenarios only. The engine can be built into a jar and reused by other teams — exactly how production API frameworks are organized.

---

## 3. Framework layers (how a request flows)

```
TEST (scenario, readable English steps)
  ↓ calls
SERVICE LAYER (one keyword per endpoint: createBooking, getBooking, deleteBooking...)
  ↓ uses
SPEC FACTORY (RequestSpecification: baseUri + JSON + logging filters)
  ↓ serializes
POJO + JACKSON (Booking object → JSON request body → response JSON → POJO)
  ↓ wrapped by
RETRY EXECUTOR (auto-retries 429/5xx — never masks real 4xx bugs)
  ↓ validated by
ASSERTION ACTIONS (logs every check, delegates to TestNG hard asserts)
  ↓ observed by
LISTENERS (TestListener logs failures with full diagnosis; RetryListener reruns flaky tests)
  ↓ reported to
ALLURE (steps, epics, features, stories, request/response evidence)
  ↓ logged to
LOG4J2 (console for live Jenkins view + logs/framework.log for post-run analysis)
```

---

## 4. Getting started

### Prerequisites
- **JDK 11** (tested with Amazon Corretto 11)
- **Maven 3.8+**
- Internet access (the API is hosted at restful-booker.herokuapp.com)
- Optional: `allure` CLI to view reports locally (`brew install allure`)

### Run the full suite

```bash
mvn clean test
```

That single command: compiles → runs `testng.xml` (all 11 tests: smoke → CRUD → E2E) → writes Allure results to `target/allure-results` → writes logs to `logs/framework.log`.

### View the Allure report

```bash
allure serve target/allure-results
```

or generate static HTML:

```bash
allure generate target/allure-results -o target/site/allure-report --clean
open target/site/allure-report/index.html
```

### Run a single test class

```bash
mvn test -Dtest=E2E_BookingFlowTest -Dsurefire.failIfNoSpecifiedTests=false
```

### Point the suite at a different environment (no code change)

```bash
mvn test -DbaseUrl=https://staging.myserver.com
```

`ConfigManager` checks system properties FIRST, then falls back to `config.properties` — this is how Jenkins overrides the environment per job.

---

## 5. The test suite (11 tests)

| Test class | Tests | What it proves |
|---|---|---|
| `AuthAndNegativeTest` | 4 | Token creation works · PUT **without** token → 403 (security) · unknown id → 404 · `/ping` health check → 201 |
| `BookingCrudTest` | 6 | GET all ids · filter by firstname · GET one (POJO deserialization) · PUT full update · PATCH partial update (only sent fields change) · DELETE → 201 then 404 |
| `E2E_BookingFlowTest` | 1 | Complete life cycle: **create → read → PUT → PATCH → delete → verify 404** — state (bookingid) flows through every step |

### The E2E flow (the interview centrepiece)

```
POST /auth        → token                    (auth state)
POST /booking     → bookingid                (server-generated state)
GET  /booking/:id → verify data persisted    (reads state back)
PUT  /booking/:id → all fields replaced      (uses token + id)
PATCH /booking/:id → only names changed,     (proves partial-update
                    price UNTOUCHED            contract, @JsonInclude)
DELETE /booking/:id → 201                    (uses token + id)
GET  /booking/:id → 404                      (proves delete persisted)
```

---

## 6. Key concepts explained (interview quick answers)

**Serialization / Deserialization**
Serialization = POJO → JSON request body (`.body(bookingPojo)` — Jackson does it). Deserialization = JSON response → POJO (`.as(Booking.class)`). Benefits: compile-time safety, no hand-written JSON, one model class serves both directions.

**RequestSpecification (`SpecFactory`)**
Answers "what is COMMON to every request?" — base URI, content type, logging filters. Built once, injected into every call with `.spec(...)`. The single biggest source of reusability in RestAssured.

**BDD style (given/when/then)**
`given()` returns `RequestSpecification` (setup), `when()` marks the action (`.get/.post/...` returns `Response`), `then()` returns `ValidatableResponse` (assertions), `.extract()` returns `ExtractableResponse`. Every chain in this framework is commented with these return types.

**POJOs with getters/setters**
Private fields + public getters/setters = encapsulation. Jackson maps JSON keys to fields by name. `BookingDates` is a nested POJO — Jackson handles nested objects (object graph mapping) automatically.

**`@JsonInclude(NON_NULL)` + wrapper types — the real bug we caught**
For PATCH (partial update), unset fields must be *skipped*, not sent as `0`/`null`. Two things make that work: `@JsonInclude(NON_NULL)` skips null fields during serialization, and `Integer`/`Boolean` wrapper types (not `int`/`boolean` primitives, which can never be null). Without this, PATCH silently sent `"totalprice": 0` and destroyed bookings — found and fixed during development of this framework.

**RetryAnalyzer (test level)**
TestNG's `IRetryAnalyzer`. On failure, `retry()` returns true up to 2 times → TestNG reruns the whole test. `RetryListener` (an `IAnnotationTransformer`) injects it into EVERY test globally via `testng.xml` — test writers can't forget it.

**RetryExecutor (request level)**
Wraps an API call in a `Supplier<Response>` lambda; retries only **429/5xx** (temporary), never 404/403 (real answers). Interview line: *"we retry infrastructure flakiness, never mask real API bugs."*

**TestListener + Log4j2**
On failure, the listener logs ERROR-level diagnosis: exception type, message, exact class.method, parameters, full stack trace — to console (live Jenkins view) and `logs/framework.log` (rolling file, survives console scroll-back).

**AssertionActions (reusable assertions)**
Framework keywords that LOG every check then delegate to TestNG: `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, plus composite `assertThatStatusIs(response, 200)` and `assertThatBodyMapsTo(response, Booking.class)`. Interview line: *"switching TestNG → JUnit means changing one file, not 200 call sites."*

**ConfigManager**
Reads `config.properties` once (static block). Priority: system property `-Dkey=value` (from Jenkins) beats the file value. Environment switching with zero code change.

**Allure reporting**
`@Epic → @Feature → @Story` hierarchy, `@Description` per test, `@Step` on every service method so each API call appears as a named step with request/response evidence attached.

**Jenkins pipeline**
`Jenkinsfile` = pipeline-as-code, versioned with the tests: Checkout → `mvn clean test` → generate Allure report → publish (Allure plugin) + archive results. Runs on every push/PR automatically.

---

## 7. CI/CD setup (Jenkins)

### One-time Jenkins configuration

1. **Install plugin:** Manage Jenkins → Plugins → install **Allure Jenkins Plugin**
2. **Tools** (Manage Jenkins → Tools), names must match the Jenkinsfile exactly:
   - JDK: name **`JDK11`**
   - Maven: name **`Maven`**
   - Allure Commandline: name **`Allure`** (version 2.25.0)
3. **New Item → Multibranch Pipeline** → add your GitHub repo (credentials if private) → Save. Jenkins detects the `Jenkinsfile` automatically.

### Pipeline stages

| Stage | What happens |
|---|---|
| Checkout | Pulls the exact triggering commit |
| Build & Run API Tests | `mvn clean test` (runs testng.xml suite) |
| Generate Allure Report | `allure generate target/allure-results ...` |
| post → always | Publishes Allure report (sidebar link + trend graphs) + archives raw results — **runs on failure too**, so a red build always has evidence |

### Pipeline environment override example

Change the Deploy stage or job parameter to pass `-DbaseUrl=https://staging-env` and the same job tests a different environment.

---

## 8. How to reuse this framework on ANY API

1. **Endpoints** — replace the constants in `Endpoints.java` with your API's paths.
2. **POJOs** — create POJOs matching your request/response JSON shapes (private fields + getters/setters, no-arg constructor for Jackson).
3. **Services** — one method per endpoint in a service class; follow the existing pattern (`@Step`, `.spec(SpecFactory.getRequestSpec())`, typed return or raw `Response`).
4. **Tests** — write scenarios calling only service keywords + `AssertionActions`. No RestAssured plumbing in tests.
5. **Config** — update `config.properties` (baseUrl, credentials).
6. Done — retry, listeners, logging, reporting, and CI all come for free.

---

## 9. Roadmap (what I'd add next — the FAANG follow-up answers)

| Extension | Why / how |
|---|---|
| Parallel execution | TestNG `parallel="tests"` + `ThreadLocal<RequestSpecification>` (RestAssured specs are not thread-safe) |
| JSON Schema validation | `rest-assured json-schema-validator` module — validate response SHAPE, not just values |
| WireMock contract tests | Isolate tests from downstream service outages |
| `@DataProvider` / JSON fixtures | Data-driven tests reading input sets from files |
| Custom report on failure | Attach failing request/response as an Allure attachment from the listener |
| Distributed runs | Selenium Grid-equivalent: pipeline sharding by test tag |

---

## 10. Commands cheat-sheet

```bash
mvn clean test                          # full suite
mvn test -Dtest=BookingCrudTest -Dsurefire.failIfNoSpecifiedTests=false   # one class
mvn test -DbaseUrl=https://other-env    # override environment
allure serve target/allure-results      # interactive report
allure generate target/allure-results -o target/site/allure-report --clean   # static report
tail -f logs/framework.log              # live log during a run
```

---

## 11. Results snapshot

```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
Report successfully generated to target/site/allure-report
```

Sample log lines (`logs/framework.log`):

```
INFO  TestListener     - STARTED: ...updateWithoutTokenIsForbidden
INFO  BookingService   - CREATE booking for Ravi Kumar
INFO  BookingService   - CREATED booking id=407 for Ravi Kumar
INFO  BookingService   - DELETE booking id=407 -> status 201
INFO  AssertionActions - ASSERT equals [Health check /ping should return 201] :: actual='201', expected='201'
INFO  TestListener     - PASSED : updateWithoutTokenIsForbidden (2695 ms)
INFO  TestListener     - RESULTS -> Passed: 4, Failed: 0, Skipped: 0
```
