# WireMock Practical Assessment — "Order Service" Mock API

A JUnit 5 test project that mocks a fictional Order Service using the WireMock Java library
(`WireMockExtension`, one instance per test class on a random port). Each task from the
assessment brief maps to its own test class so the mapping between requirement and
implementation stays obvious.

| Task | Class |
|---|---|
| 1. Basic stubbing | `BasicStubbingTest` |
| 2 & 3. Header/auth + body matching | `AuthAndBodyMatchingTest` |
| 4. Query parameter matching | `QueryParamMatchingTest` |
| 5. Response templating | `ResponseTemplatingTest` |
| 6. Simulating faults | `FaultSimulationTest` |
| 7. Stateful scenario | `OrderCancellationScenarioTest` |
| 8. Verification | `VerificationTest` |
| 9. Stub priority | `StubPriorityTest` |
| Bonus: custom extension | `CustomTransformerTest` + `extension/RequestIdResponseTransformer` |

## How to run it

Requires JDK 17+ and Maven (or use an IDE with Maven support — the project is a standard
Maven layout, `src/test/java` + `pom.xml`).

```bash
mvn test
```

All 16 tests (across 9 classes) start their own in-process WireMock server on a random port,
exercise it with real HTTP calls (via a small OkHttp wrapper — `support/TestHttpClient`), and
assert on the actual responses. Nothing talks to a real network service.

## Design decisions

- **One WireMockExtension per test class**, each on `dynamicPort()`, rather than one shared
  server for the whole suite. This keeps each task's stubs isolated — no risk of an
  overlapping URL pattern from one task's stub bleeding into another task's assertions — and
  lets each class enable only the server options it needs (e.g. global templating only in
  `ResponseTemplatingTest`).
- **Real HTTP calls instead of only `WireMock.verify()`.** Tests hit the mock server over the
  loopback interface with OkHttp, the same way a real client/service would, then assert on the
  actual HTTP status/body/headers. WireMock's own verification API (`wm.verify(...)`) is used
  *in addition*, specifically for task 8, since that's what it's for.
- **Priorities instead of relying on registration order** wherever two stub mappings could
  both match the same request (tasks 2/3's auth-vs-body branches, and task 9's
  generic-vs-specific ID stubs). WireMock resolves ties between equally-matching stubs by
  insertion order (newest wins), which is an implicit, fragile ordering dependency — a stub
  added later for an unrelated reason could silently start winning. An explicit `atPriority(n)`
  (lower number = higher priority) makes the precedence a stated design decision instead of an
  accident of file/test order, and keeps working correctly even if stubs are re-registered in a
  different order later.

## Task 6: chosen fault types

The brief asks for two of: fixed delay, 500 with an error body, or connection reset. This
project implements **fixed delay** and **connection reset**, because together they demonstrate
the two fundamentally different failure modes a client has to handle, whereas "500 with a
body" is really just a variant of the normal request/response cycle (it's included too, but
paired with the delay rather than as its own separate case):

- **Fixed delay (`withFixedDelay(3000)`, paired with a 500 body)** simulates a service that is
  *alive but degraded* — overloaded, slow database, etc. The client still gets back a complete,
  well-formed HTTP response; it just has to wait for it. This is the case that a client's
  request timeout is meant to catch, and it's meaningfully different from an immediate error
  because a retry-with-backoff strategy behaves very differently against a slow-but-alive
  dependency vs. one that's hard down.
- **Connection reset (`Fault.CONNECTION_RESET_BY_PEER`)** simulates a *hard* outage — the
  process crashed, a load balancer dropped the connection, a network partition. The client
  doesn't get an HTTP response at all; the socket is torn down mid-flight and the HTTP client
  throws an `IOException` (see `FaultSimulationTest#connectionResetSimulatesAHardOutage`).
  Nothing about the response — status code, body, headers — is observable, which is the key
  difference from the delay case: any code that assumes "if I got a response object, I can at
  least inspect the status" breaks here, so it's worth testing separately.

## Task 9: why priority matters here

`urlMatching("/orders/[0-9]+")` (generic) and `urlEqualTo("/orders/42")` (specific) both match
a `GET /orders/42` request — WireMock doesn't pick "the more specific pattern" automatically,
it just finds every stub whose request matcher matches and, absent a priority, prefers the
most-recently-added one. That means whether `/orders/42` returns the generic or the specific
response would depend entirely on which stub happened to be registered second — true today,
but silently reversible by anyone who reorders the `stubFor` calls (or loads mapping files in a
different order) later. Setting `atPriority(1)` on the specific stub and `atPriority(5)` on the
generic one makes "the specific match always wins" an explicit, self-documenting rule instead
of an accident of setup order.

## Assumptions

- The `Authorization` header value is compared as a literal string (`Bearer valid-token`);
  there's no JWT parsing/expiry logic since the brief doesn't specify a real token format.
- Task 7's cancellation scenario models exactly the three transitions described in the brief
  (`STARTED` → `cancelling` → stays `cancelling`). A real Order Service would likely also allow
  a terminal `CANCELLED` state, but that wasn't part of the required flow so it was left out to
  avoid inventing unstated behaviour.
- For task 8's negative verification (`DELETE /orders/789` was never called), no stub is even
  registered for that path — `wm.verify(0, ...)` still works because it inspects WireMock's
  request journal, not the stub registry, so an unmapped/unused route correctly reports zero
  requests without needing a stub to exist for it.

## Bonus work completed

- **Custom response transformer** (`extension/RequestIdResponseTransformer`): implements
  `ResponseDefinitionTransformerV2`, registered globally via
  `wireMockConfig().extensions(new RequestIdResponseTransformer())`, and stamps a computed
  `X-Request-Id: req-<uuid>` header onto every response regardless of which stub served it —
  demonstrating that extensions apply cross-cuttingly rather than being wired into each stub
  individually.
- The **record-and-playback** bonus (proxying against `jsonplaceholder.typicode.com`) was not
  attempted, since it requires live outbound network access from the grading environment and
  the brief marks it optional.
