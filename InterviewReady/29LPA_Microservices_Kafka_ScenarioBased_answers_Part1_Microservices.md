# 29LPA Prep — Answers Part 1: Microservices Basics

Source: `29LPA_MIcroservices_Kafka_ScenarioBased.txt` — is file mein sirf "Microservices Basics" wale saare questions ka theory + Hinglish why/how annotation + code snippets hai. Baaki 2 files: Part 2 (Kafka Fundamentals), Part 3 (Scenario-Based).

---

## Section 1 — Fundamentals

**1. Microservices architecture kya hota hai?**
Application ko chhote, independent, loosely-coupled services mein todna jaha har service apni specific business-capability (bounded context) handle kare, apna data store rakhe, aur independently deploy/scale ho sake. Services ek-doosre se lightweight protocols (REST/gRPC/messaging) se communicate karte hain.
*Why:* Bade teams independently kaam kar sakein, alag components alag rate se scale/deploy ho sakein.
*How:* Domain-Driven Design se bounded contexts identify karke service-boundaries define karte hain — technical layers (DB-service, UI-service) ke around nahi, business capability ke around.

**2. Monolith vs Microservices — trade-offs kya hain?**
Monolith — simple development/testing/deployment (ek codebase, ek deploy), lekin scale karna mushkil (poora app scale karna padta hai chhote hisse ke liye bhi), tight coupling se ek bug poore system ko affect kar sakta hai. Microservices — independent scaling/deployment, technology-diversity, fault isolation — lekin operational complexity (distributed debugging, network latency, data-consistency challenges) badh jaati hai.
*How decide:* Team-size, domain-complexity, aur independent-deployment-need ko weigh karo operational-overhead ke against.

**3. Microservices kab use nahi karne chahiye?**
Chhoti team, unclear/rapidly-changing domain boundaries, early-stage startup jaha requirements bahut change hote hain, ya team ko distributed-systems operational expertise na ho — in cases monolith (ya "modular monolith" — clean internal boundaries, single deployment) better hai. Premature split "distributed monolith" bana deta hai — dono ki worst properties.

**4. Service boundaries kaise define karte ho?**
DDD ke "bounded context" se — business capability ke around (jaise "Patient Management", "Billing"), na ki technical layers ke around. Conway's Law consider karo — team-structure aur service-boundaries aksar align hote hain. Achha signal: service apna data independently own kar sake bina baar-baar doosri services se tight coordination ke.

**5. Database per service kyun important hai?**
Shared database tight coupling create karta hai — ek service ka schema-change doosri services break kar sakta hai, independent deployment possible nahi rehta (migration-coordination chahiye). Database-per-service se har service apna data-model independently evolve kar sakti hai, failure-isolation better hota hai.
*Trade-off:* cross-service joins ab database-level pe possible nahi — application-level (API calls ya event-driven data-replication) se karna padta hai.

---

## Section 2 — Communication

**6. Synchronous vs Asynchronous communication**
Sync (REST/gRPC) — caller turant response ka wait karta hai, simple mental model, lekin caller/callee availability tightly coupled ho jaati hai. Async (Kafka/RabbitMQ) — caller message bhejke aage badh jaata hai, callee apni pace pe process karta hai — better decoupling/resilience, lekin eventual-consistency aur "did it actually happen" verify karna harder ho jaata hai.
```java
// Sync (REST)
ResponseEntity<Patient> response = restTemplate.getForEntity("/patients/1", Patient.class);

// Async (Kafka)
kafkaTemplate.send("patient.created", patientId, patientEvent); // turant return, background mein process
```

**7. REST vs Messaging (Kafka/RabbitMQ) — kab kya use karte ho?**
REST tab jab caller ko turant response chahiye (jaise "GET patient details" UI ko turant dikhana). Messaging tab jab operation fire-and-forget ho sakta hai (notification bhejna), multiple consumers ek hi event pe react karne ho, ya high-throughput event-stream process karni ho — decoupling zyada important hai immediate-response se.

**8. Service-to-service communication kaise secure karte ho?**
mTLS (mutual TLS) service-mesh level pe (Istio jaisa) — dono taraf certificate-verify hota hai. Application-level — internal calls mein bhi JWT propagate karo (sirf external-facing pe nahi), ya "service identity" ke liye OAuth2 client-credentials grant use karo. Network-level — Kubernetes `NetworkPolicy` se restrict karo kaunsi service kis se baat kar sakti hai.

**9. API Gateway ka role kya hota hai?**
Single entry-point external clients ke liye — path-based routing, authentication/authorization centralize karna, rate-limiting, request/response transformation, cross-cutting logging. Individual services ko har cross-cutting concern khud implement nahi karna padta.

**10. Service discovery kya hota hai?**
Dynamic environment mein instances scale-up/down/restart hote rehte hain, IP change hoti rehti hai — service discovery (Eureka/Consul/Kubernetes-native DNS) automatically track karta hai kaunse instances currently healthy hain, taaki caller ko hardcoded IPs pe depend na karna pade.

---

## Section 3 — Data Management

**11. Distributed transactions kaise handle karte ho?**
Traditional 2PC (Two-Phase Commit) distributed systems mein impractical hai — tight coupling, availability-loss, long-held locks. Saga pattern use karte hain — badi transaction ko chhoti local-transactions ki series mein todte hain, har step complete hoke agla trigger karta hai, failure pe compensating actions (undo) chalte hain.

**12. Saga pattern kya hota hai?**
Choreography — koi central coordinator nahi, har service apna kaam karke event publish karta hai, agla service subscribe karke react karta hai (decoupled, lekin poora flow track karna hard). Orchestration — ek central orchestrator explicitly decide karta hai kaunsa step kab chalega aur failure pe kaunsi compensating action (easier to track, orchestrator ek coordination point ban jaata hai).
```java
// Orchestration-style pseudo-flow
sagaOrchestrator.execute(
    () -> inventoryService.reserve(orderId),
    () -> paymentService.charge(orderId),
    () -> orderService.confirm(orderId)
); // koi step fail ho toh pehle-complete steps ke compensating actions chalte hain (release, refund)
```

**13. Event sourcing kya hota hai?**
State ko directly store karne ke bajaye, saare "events" (state-changes) ko ek append-only log mein store karte hain, current-state events ko replay karke derive hota hai. Benefit: complete audit-trail, kisi bhi point-in-time ka state reconstruct kar sakte ho. Trade-off: complexity (event-schema evolution, lambi histories ke liye replay-performance — snapshotting se mitigate karte hain).

**14. CQRS pattern kya hota hai?**
Command Query Responsibility Segregation — read aur write models alag rakhte hain. Write-side normalized/transactional model, Read-side denormalized read-optimized model (aksar event-sourcing ke saath — write-events se read-model async update hota hai). Benefit: read/write independently scale, trade-off: eventual-consistency between read/write model handle karni padti hai.

**15. Data consistency vs availability trade-off**
CAP theorem — network partition ke time Consistency aur Availability mein compromise karna padta hai. Microservices mein zyadatar eventual-consistency accept karte hain (availability prioritize, strong-consistency distributed system mein costly hai — locks/coordination overhead), lekin genuinely critical operations (payment) ke liye stronger guarantees (idempotency + saga-compensations) chahiye.

---

## Section 4 — Resilience & Fault Tolerance

**16. Circuit breaker kya hota hai?**
CLOSED (normal, calls jaati hain, failures count) → threshold cross hone pe OPEN (calls turant fast-fail, downstream ko aur load nahi) → waiting-period baad HALF_OPEN (limited test-calls, recover check) → success pe CLOSED, fail pe wapas OPEN.
```java
@CircuitBreaker(name = "consentService", fallbackMethod = "fallbackConsent")
public ConsentStatus checkConsent(String patientId) {
    return consentClient.check(patientId);
}
public ConsentStatus fallbackConsent(String patientId, Throwable t) {
    return ConsentStatus.unknown(); // graceful degrade
}
```

**17. Retry mechanism kaise design karte ho?**
Exponential backoff + jitter, max-attempts cap, sirf idempotent-safe operations pe, aur transient errors (timeout, 5xx) pe hi retry — permanent errors (400 bad request) pe retry waste hai.
```java
@Retry(name = "openAiCall", fallbackMethod = "fallbackResponse")
public String callModel(String prompt) { ... }
```

**18. Timeout handling kaise karte ho?**
Har external call pe explicit timeout (connect + read) set karo — bina timeout, ek slow dependency poore thread-pool ko block kar sakta hai (cascading failure). Timeout value downstream SLA ke hisaab se tune karo — bahut chhota toh false-failures, bahut bada toh resource-exhaustion.

**19. Bulkhead pattern kya hota hai?**
Ship ke watertight compartments jaisa — ek downstream dependency slow ho jaaye, uske calls ke liye alag thread-pool/connection-pool allocate karo (isolate karo) taaki wo poore application ka shared pool exhaust na kar de, baaki unrelated functionality unaffected rahe.

**20. Graceful degradation kya hota hai?**
Ek non-critical dependency (jaise recommendation-service) fail/slow ho jaaye toh poori request fail mat karo — us part skip/default-value ke saath serve karo aur baaki response de do. Critical path aur non-critical path explicitly identify karke design karo.

---

## Section 5 — Scaling & Performance

**21. Horizontal scaling kaise achieve karte ho?**
Zyada instances/pods add karna (vertical scaling — bada instance — ke against). Requires: stateless service design (session/state DB/cache mein bahar, app-server ke andar nahi), taaki load-balancer kisi bhi instance ko traffic bhej sake.

**22. Load balancing kaise hota hai?**
Multiple instances ke beech traffic distribute karna — algorithms: round-robin (simple sequential), least-connections (jo instance kam busy hai usse bhejo), latency-based. Kubernetes `Service`/ALB automatically handle karte hain.

**23. Caching strategies kya hoti hain?**
Cache-aside (application khud check/populate karta hai — most common), write-through (write cache+DB dono sync — consistency better, write slow), write-behind (write pehle cache, DB async baad mein — fast writes, crash-risk). Invalidation: TTL-based (simple) ya event-driven (jaise `consent.revoked` event pe cache-evict — precise).

**24. Rate limiting kaise implement karte ho?**
Token bucket (tokens fixed rate se refill, bursty traffic ko flexibility) ya sliding-window (precise, thoda zyada compute) algorithm, Redis-backed counter per-user/per-API-key, API Gateway level pe centralize karna better hai duplicate-per-service logic se.

**25. High traffic scenario kaise handle karte ho?**
Horizontal auto-scaling (Kubernetes HPA — CPU/custom-metric based), aggressive caching, database read-replicas, aur queue-based load-leveling (Kafka spike ko buffer karta hai, consumers apni pace se process karte hain bina sudden-load se crash hue).

---

## Section 6 — Observability

**26. Logging strategy microservices me kaise hoti hai?**
Structured (JSON) logs, correlation-ID poore request-lifecycle mein propagate hota hai (headers ke through), centralized aggregation (ELK/Fluentd/CloudWatch) — ek jagah se saari services ke logs search ho sakein.

**27. Distributed tracing kya hota hai?**
Ek unique trace-ID request ke entry pe generate hota hai, har service apna span (ek hop ka time-segment) create karke trace-ID propagate karti hai aage. Tracing tool (Zipkin/Jaeger/OpenTelemetry) saare spans collect karke waterfall-visualization banata hai — exactly dikhta hai request kis-kis service se hoke gaya aur kaha time laga.

**28. Metrics kaise collect karte ho?**
Micrometer se application metrics (latency, throughput, error-rate, custom business-metrics) instrument karte ho, Prometheus/CloudWatch scrape karta hai, Grafana pe dashboard/alerts.

**29. Monitoring tools kaise use karte ho?**
Prometheus+Grafana, CloudWatch, Datadog, New Relic — dashboards (latency percentiles, error-rate) aur alarms (threshold-cross pe notify) ke liye.

**30. Debugging distributed system kaise karte ho?**
Correlation-ID se logs trace karo across services, distributed-tracing se latency-breakdown dekho (kaunsa hop slow tha), metrics se anomaly identify karo (kaunsi service degrade hui pehle).

---

## Section 7 — Deployment & DevOps

**31. Containerization (Docker) ka role kya hai?**
Consistent environment (dev=prod parity — "works on my machine" problem solve), isolation (dependencies conflict nahi karte), portability (kahi bhi run ho sakta hai jaha Docker hai).

**32. Kubernetes ka use kyun hota hai?**
Container orchestration — auto-scaling, self-healing (unhealthy pods automatically replace), rolling-deployments (zero-downtime), service-discovery/load-balancing built-in — ye sab manually manage karna operationally bahut complex hota.

**33. CI/CD pipeline kaise design karte ho?**
Build → automated-test → containerize (Docker image) → security-scan → staging-deploy+verify → production-deploy (rolling/blue-green/canary) — sab automated, code-merge pe trigger hota hai.

**34. Blue-green deployment kya hota hai?**
Do identical environments (blue=current, green=naya), naya version green mein deploy+verify karo, phir traffic instantly blue se green pe switch — rollback bhi instant (wapas blue pe switch).

**35. Canary deployment kya hota hai?**
Naye version ko chhoti % traffic pe roll out karo pehle, metrics monitor karo, gradually % badhao agar sab theek hai — risk exposure gradual, poori userbase ek-saath naye (potentially buggy) version pe nahi jaati.

---

## Section 8 — Real-World Scenarios (MOST IMPORTANT)

**36. Service down ho jaye to system kaise behave karega?**
Circuit breaker turant OPEN ho jaata hai (agar configured hai), calling services fast-fail response dete hain bajaye hang hone ke, fallback/cached-response serve hota hai jaha possible, aur alerts fire hote hain (CloudWatch/PagerDuty). Downstream/dependent services bhi apna graceful-degradation trigger karti hain (jaise us specific feature ko disable karke baaki application chalti rahe).

**37. Database slow ho jaye to kya karoge?**
Connection-pool exhaustion risk hota hai — timeout+circuit-breaker isse application ko protect karte hain (poore thread-pool ko block hone se bachate hain). Immediate mitigation: read-replica pe traffic offload (agar read-heavy issue hai), aur root-cause investigate karo (`EXPLAIN ANALYZE` se slow-query, missing-index, ya connection-pool-tuning issue).

**38. Kafka delay ho jaye to impact kya hoga?**
Consumer-lag badhega, downstream processing delay hoga (jaise notifications late jaayengi), lekin **data loss nahi hota** (Kafka messages persist karta hai retention-period tak) — eventual-consistency ka window bas bada ho jaata hai. Alert lagao consumer-lag metric pe taaki proactively pata chale.

**39. Data inconsistency kaise fix karoge?**
Root-cause identify karo (missed event, race-condition, ya partial-saga-failure), ek reconciliation job chalao jo systems ke beech data compare kare aur correct kare, aur prevention ke liye idempotency/ordering-guarantees improve karo taaki dobara na ho.

**40. 10x traffic aajaye to system kaise scale karega?**
Auto-scaling (HPA) naye pods spin karta hai CPU/custom-metric threshold cross hone pe, caching load reduce karta hai DB pe, database read-replicas extra read-capacity dete hain, aur rate-limiting non-critical traffic ko throttle karta hai taaki critical paths ke liye capacity bachi rahe. **Load-testing pehle se kiya hona chahiye** taaki pata ho system genuinely kitna scale kar sakta hai bina surprises ke.

---

*Part 2 mein: Kafka Fundamentals (Producers/Consumers, Delivery Semantics, Performance, Fault Tolerance, Real-World Usage).*
