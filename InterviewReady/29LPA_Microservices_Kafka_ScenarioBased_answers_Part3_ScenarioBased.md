# 29LPA Prep — Answers Part 3: Scenario-Based

Source: `29LPA_MIcroservices_Kafka_ScenarioBased.txt` — "SCENARIO BASED" section (sabse important part — real interviews mein yahi sabse zyada poocha jaata hai), theory + Hinglish why/how annotation + code snippets ke saath. Part 1 = Microservices Basics, Part 2 = Kafka Fundamentals.

---

## Section 1 — Service Communication Failures

**1. Ek service down ho jaye — system kaise behave karega?**
Circuit breaker (agar configured hai) turant OPEN ho jaata hai → calling services fast-fail response dete hain bajaye hang hone ke → fallback/cached-response serve hota hai jaha possible → alerts fire hote hain (CloudWatch/PagerDuty) → dependent features gracefully degrade hote hain (jaise ek non-critical feature disable ho jaaye, core-flow chalta rahe) → jab service recover kare, HALF_OPEN test-calls se automatically CLOSED wapas ho jaata hai.

**2. Synchronous call fail ho jaye toh fallback kya hoga?**
Resilience4j `@CircuitBreaker`/`@Retry` ke `fallbackMethod` se — raw exception caller ko propagate karne ke bajaye ek default/cached/degraded response return karo.
```java
@CircuitBreaker(name = "consentService", fallbackMethod = "denyByDefault")
public boolean checkConsent(String patientId) { return consentClient.check(patientId); }

public boolean denyByDefault(String patientId, Throwable t) {
    return false; // fail-safe: security-sensitive case mein "deny" default hona chahiye, "allow" nahi
}
```
*Why fail-safe matters:* security-sensitive checks (consent, auth) mein fallback hamesha "restrictive" default hona chahiye — fail-open (accidentally sabko access de dena) bahut bada risk hai.

**3. Kafka unavailable ho jaye toh kya strategy hogi?**
Producer-side: Kafka client internally retry karta hai; agar lambe time tak unavailable hai, **Transactional Outbox pattern** use kar sakte ho (event ko pehle apne hi DB ke ek "outbox" table mein same transaction mein save karo, ek separate poller Kafka wapas aane pe replay kare) — isse event-loss nahi hota bina Kafka ki availability pe hard-depend hue. Consumer-side: client-library automatically reconnect/retry karta hai, processing bas paused rehta hai jab tak broker wapas na aaye — koi data-loss nahi (jab tak retention khatam na ho jaaye).

**4. API timeout ho raha hai — root cause kaise find karoge?**
Distributed-tracing se dekho kaunsa hop/service slow hai. Us downstream-service ke apne metrics check karo (CPU, DB-query-time, GC-pauses). Network-level bhi check karo (DNS-resolution-delay, connection-pool-exhaustion). Logs mein timeout ke exact-time ke around correlate karo — kya us waqt deploy hua tha ya traffic-spike aayi thi.

**5. Retry kab use karoge aur kab avoid karoge?**
**Use:** transient errors (timeout, 5xx, network-blip) pe, aur operation genuinely idempotent-safe ho. **Avoid:** permanent errors (400 bad-request, validation-failure — retry se same result aayega, sirf waste hai), ya non-idempotent operation jisme retry se side-effect duplicate ho sakta hai (bina idempotency-key ke "create order" retry karna).

---

## Section 2 — Kafka Message Handling

**6. Consumer message process karte waqt crash ho gaya — kya hoga?**
Agar manual-commit use ho raha tha aur commit process-complete se pehle nahi hua tha (crash beech mein), offset commit nahi hua — restart/rebalance ke baad **wahi message dobara deliver hoga** (at-least-once behavior). Isiliye consumer idempotent hona chahiye (Part 2, Q13) taaki dobara-processing safe ho.

**7. Duplicate message process ho gaya — kaise handle karoge?**
Idempotent-consumer pattern — `eventId` ek "processed_events" table mein check karo, agar already-processed hai skip karo. Ya agar operation naturally idempotent hai (jaise `SET status='active'` — baar-baar chalao same result), explicit dedup ki zarurat nahi.

**8. Message lost ho gaya — kaise detect karoge?**
Producer-side — delivery-confirmation na mile (`acks` timeout/failure) toh producer-logs mein error dikhega, is par metric/alert lagao. Consumer-side — agar expected downstream-effect nahi hua (jaise notification kabhi nahi gayi), ek **reconciliation-job** se detect karo (source-of-truth ke saath periodically compare karke).

**9. Out-of-order messages aaye — kya karoge?**
Agar same partition-key consistently use ho raha tha, order guaranteed honi chahiye thi within-partition — phir bhi out-of-order dikhe toh check karo partition-key logic mein bug toh nahi. Application-level defense: events mein timestamp/version rakho, consumer khud reconcile kare (jaise "sirf newer-version wala update apply karo, agar already newer state hai toh purana event ignore karo").

**10. Consumer lag continuously badh raha hai — fix kaise karoge?**
Investigate karo: per-message processing-time kitna hai (slow downstream external-call?), consumer-count vs partition-count (scale-out possible hai kya), aur agar processing-logic mein hi bottleneck hai (jaise synchronous per-message external-API-call), usse optimize/parallelize/batch karo.

---

## Section 3 — Data Consistency (MOST IMPORTANT)

**11. Multiple services me data inconsistent ho gaya — kaise fix karoge?**
Pehle root-cause identify karo (missed event? race-condition? partial saga-failure?). Immediate: ek reconciliation-job chalao jo source-of-truth se compare karke correct kare. Long-term prevention: idempotency, proper event-ordering, aur robust saga-compensations taaki dobara na ho.

**12. Distributed transaction fail ho gaya — rollback kaise hoga?**
Saga pattern mein — jo steps already complete ho chuke the, unke **compensating actions** (undo-operations) reverse-order mein trigger hote hain (jaise agar payment-charge ho chuka tha lekin order-confirm fail hua, payment ko refund karne wala compensating-action chalega).

**13. Saga pattern implement karna ho toh kaise karoge?**
Steps define karo aur har step ke liye ek compensating-action, orchestrator (ya choreography-events) se sequence manage karo, aur failure-handling explicit design karo (kaunsa step fail hone pe kaunse pichle steps undo karne hain — order matter karta hai, reverse-sequence mein undo hote hain).
```java
try {
    inventoryService.reserve(orderId);
    paymentService.charge(orderId);
    orderService.confirm(orderId);
} catch (PaymentFailedException e) {
    inventoryService.release(orderId); // compensating action — sirf jo already ho chuka tha wo undo
}
```

**14. Partial failure me system ka behavior kya hoga?**
Kuch steps succeed ho gaye, kuch nahi — system "inconsistent intermediate state" mein temporarily rehta hai (eventual-consistency accept karni padti hai). Ya toh automatic-compensation (saga) chalega, ya agar automated-recovery possible nahi hai, monitoring/alert se manual-intervention trigger hoga.

**15. Eventually consistent system me correctness kaise ensure karte ho?**
Idempotent operations (dobara-apply safe), event-ordering guarantees jaha zaroori ho, version/timestamp-based conflict-resolution (last-write-wins ya explicit merge-logic), aur periodic reconciliation-jobs jo consistency verify/fix karte hain — "eventually correct" ka matlab hai system khud-se converge ho jaaye correct-state pe given-enough-time, bina external-intervention ke normal-cases mein.

---

## Section 4 — Scaling Scenarios

**16. Traffic suddenly 10x ho gaya — kya steps loge?**
Auto-scaling (HPA) turant naye pods spin kare, aggressive caching load DB se hatati hai, database read-replicas extra read-capacity dete hain, rate-limiting non-critical traffic ko throttle karta hai. **Pehle se load-testing** hona chahiye taaki actual-capacity-limits pata hon surprise-mein nahi.

**17. Kafka partitions kam pad gaye — kya karoge?**
Existing-topic pe partitions increase kar sakte ho (Kafka increase allow karta hai, decrease nahi) — lekin isse existing key-to-partition mapping change ho jaati hai (kuch keys ke liye ordering-guarantee temporarily break ho sakti hai during-transition). Isliye planned/low-traffic-window mein karo, aur future-growth anticipate karke shuru mein hi generous partition-count rakhna better practice hai.

**18. Ek consumer slow hai — kaise scale karoge?**
Agar partitions available hain (`consumer-count < partition-count`), naye consumer-instances add karo same group mein — Kafka automatically partitions redistribute kar dega. Agar already `consumer-count = partition-count`, pehle partitions badhao phir consumers add karo.

**19. Database bottleneck ban gaya — solution kya hai?**
Read-replicas (read-heavy load offload), caching (Redis, repeated-reads DB tak na jaayein), query-optimization (indexing, N+1-fix), connection-pool-tuning, aur agar genuinely write-throughput ka issue hai, sharding/partitioning-strategy consider karo (ye bigger architectural change hai).

**20. High latency under load — kaise debug karoge?**
Distributed-tracing se identify karo load ke dauraan kaunsa hop slow hai. Resource-metrics check karo (CPU/memory/GC-pauses per-service). Connection-pool-exhaustion check karo. Staging mein load-test replicate karo taaki safely profile kar sako bina production impact kiye.

---

## Section 5 — Real Event-Driven Use Cases

**21. Appointment booking event flow design karo**
```
AppointmentBookingRequested (user books)
   → Appointment Service validates slot-availability
   → AppointmentConfirmed / AppointmentRejected event published
        → Notification Service (email/SMS bhejta hai)
        → Calendar Service (slot ko block karta hai)
        → Analytics Service (metrics update karta hai)
```
Saare downstream-consumers async, independently, parallel react karte hain `AppointmentConfirmed` event pe — Appointment Service ko unka pata bhi nahi hota (decoupled).

**22. Payment success event miss ho gaya — kaise handle karoge?**
Isiliye Kafka jaisa durable-log use karte hain (RabbitMQ jaisa transient-queue ke against) — agar consumer down tha jab event publish hua, event topic mein **persist rehta hai**, consumer wapas online hoke apne last-committed-offset se aage padhega — automatically "catch up" ho jaata hai. Event genuinely "miss" nahi hota jab tak retention-period expire na ho jaaye. Edge-case-safety-net: ek reconciliation-job jo payment-gateway ke source-of-truth se periodically compare kare.

**23. Notification service delay kar raha hai — impact kya hoga?**
Core business-flow (jaise appointment-booking) **unaffected** rehta hai kyunki notification async/decoupled hai — sirf user ko notification thodi der se milegi. Impact user-experience tak limited hai, business-critical-data/state affect nahi hoti. Is service ka consumer-lag monitor karo taaki delay-severity turant pata chale.

**24. Same event multiple baar trigger ho raha hai — fix kaise karoge?**
Producer-side bug ho sakta hai (jaise retry-logic bina idempotent-producer ke duplicate-send kar rahi ho), ya consumer-side redelivery normal at-least-once-behavior hai. Fix: producer mein idempotent-producer enable karo, aur consumer mein idempotency-check (`eventId`-dedup) — dono layers pe protection rakhna best practice hai.

**25. Event schema change ho gaya — backward compatibility kaise maintain karoge?**
Schema-registry use karo (jaise Confluent Schema Registry, Avro/Protobuf ke saath) jo compatibility-rules enforce kare. Naye fields **ADD** karo optional/default-value ke saath — existing-fields kabhi remove/rename mat karo. Consumers ko naye-field-absence gracefully handle karna chahiye (deployment ke dauraan old-producers abhi bhi purane-schema bhej rahe honge). Versioning-strategy (topic-name mein version, ya explicit schema-version-field) bhi common approach hai.

---

## Section 6 — Fault Tolerance & Recovery

**26. Kafka broker crash ho gaya — system kaise recover karega?**
Leader-election automatically hota hai ISR mein se, clients naye-leader ko discover karke redirect ho jaate hain — thodi der ki unavailability ke alawa system continue karta hai bina data-loss ke. Crashed-broker jab wapas aata hai, wo follower ban ke leader se catch-up karta hai.

**27. Service restart hone ke baad state kaise restore karoge?**
Agar service stateless hai (state DB/cache mein bahar), restart trivial hai — naya instance startup pe zaroori config/connections establish karke turant serve karna shuru kar sakta hai. Kafka-consumer apna last-committed-offset se automatically resume karta hai. Agar koi in-memory local-cache tha, wo cold-start hoga — warm-up-period accept karo ya distributed-cache (Redis) use karo.

**28. Poison message (invalid data) aaya — kya karoge?**
Consumer process karne ki koshish karega, deserialization/validation fail hogi, kuch retries ke baad DLQ mein bhej do — poison-message ki wajah se poora consumer/partition-processing block nahi hona chahiye. DLQ mein gaye messages manually investigate karo, aur data-quality issue ko upstream-source pe fix karo taaki repeat na ho.

**29. Dead Letter Queue ka design kaise karoge?**
Alag topic (`<original-topic>.DLT`), original-message + failure-metadata (error-reason, retry-count, timestamp, original topic/partition/offset) store karo debugging-ease ke liye. Ek alerting/monitoring rakho is DLQ-topic pe (naya-message-aana matlab investigate-karo), aur ek admin process/UI jo DLQ-messages ko review + fix ke baad manually-reprocess kar sake.

**30. Retry strategy design karo (backoff etc.)**
Exponential-backoff (1s, 2s, 4s, 8s...) + **jitter** (random-variation, taaki multiple-failed-consumers same-time pe simultaneously retry na karein — "thundering herd" avoid), max-attempts cap (jaise 3-5), uske baad DLQ. Kafka mein **retry-topic pattern** common hai — alag delay-tier topics (`retry-1s`, `retry-1m`, `retry-1h`).

---

## Section 7 — Production Debugging

**31. Logs me kuch nahi mil raha — issue kaise debug karoge?**
Correlation-ID se cross-check karo — kya request genuinely us service tak pahunchi thi (upstream-logs, load-balancer/gateway-logs check karo). Log-level check karo (kahi DEBUG-level production mein disabled toh nahi jo helpful details deta). Metrics/traces se alternative-signal dhoondo agar logs insufficient hain.

**32. Distributed tracing kaise use karoge?**
Trace-ID request-entry pe generate/propagate hota hai, har service apna span create karta hai, tracing-backend (Jaeger/Zipkin/OpenTelemetry) waterfall-view deta hai — exactly dikhta hai kaunsa hop kitna time le raha tha aur kaha fail hua.

**33. High CPU usage ka reason kaise find karoge?**
Thread-dump lo (`jstack <pid>`), dekho kaunse threads busy hain aur kya kar rahe hain (tight-loop? excessive-GC?). Production-safe profiler (async-profiler, VisualVM) attach karo, flame-graph dekho kaunsa method-call sabse zyada CPU-time le raha hai. Recent-deploy ke saath correlate karo — kya naya code-change hai jo isse trigger kar raha hai.

**34. Memory leak kaise detect karoge microservices me?**
Heap-dump lo (`jmap -dump:live,format=b,file=heap.hprof <pid>`), VisualVM/Eclipse-MAT mein dominator-tree dekho, do-dumps compare karke growing-objects identify karo. Common culprits: unbounded-caches, unclosed-resources, listener-leaks, `ThreadLocal` not cleaned (thread-pool-reuse ke context mein).

**35. End-to-end latency kaise measure karoge (API → Kafka → consumer)?**
Timestamp embed karo message mein jab API-request aayi thi (producer-side). Consumer jab process kare, current-time se subtract karke total-latency calculate karo. Distributed-tracing se full-breakdown milta hai — separate spans: "API-handling-time" + "Kafka-publish-to-consume-gap" + "consumer-processing-time" — clearly pata chalta hai kaha zyada time laga.

---

*Poora 3-part series complete: Part 1 (Microservices Basics), Part 2 (Kafka Fundamentals), Part 3 (Scenario-Based — is file). Scenario-based sections interviewer ke liye sabse important hain kyunki ye directly test karte hain ki tumne theory ko real production-thinking mein translate kiya hai ya nahi.*
---------------------------------------------------------------------------------========================================

ACKS = ALL (-1) — Kafka Interview Notes

Replication Example

Replication Factor = 3

Broker1 -> Leader Broker2 -> Follower Broker3 -> Follower

Producer always sends messages to the Leader. Followers replicate data
from the Leader.

What is ISR?

ISR = In-Sync Replicas

These are replicas that are fully synchronized with the Leader.

If a follower becomes slow and falls behind, Kafka removes it from the
ISR.

Example:

Leader -> Broker1 Follower -> Broker2 Follower -> Broker3

If Broker3 is out of sync:

Current ISR: - Broker1 (Leader) - Broker2

Broker3 is not waited on for acknowledgements.

acks = 0

Producer sends the message and does not wait for any acknowledgement.

Pros: - Fastest

Cons: - Message may be lost if the leader crashes immediately.

acks = 1

Producer waits only for the Leader.

Flow: Producer -> Leader -> ACK

Followers may not have replicated the message yet.

If the leader crashes before replication, the message may be lost.

acks = all (or -1)

Producer sends the message to the Leader.

Leader writes the message.

Leader replicates the message to all In-Sync Replicas (ISR).

Only after every ISR replica acknowledges, the Leader sends ACK back to
the Producer.

Pros: - Highest durability - Safest option

Cons: - Slightly higher latency

Important

acks=all waits only for ALL IN-SYNC REPLICAS (ISR).

It does NOT wait for replicas that are already out of sync.

ACK Comparison

acks=0 - Waits for: Nobody - Speed: Fastest - Durability: Lowest

acks=1 - Waits for: Leader only - Speed: Fast - Durability: Medium

acks=all (-1) - Waits for: All In-Sync Replicas (ISR) - Speed: Slowest -
Durability: Highest

Interview Answer

‘acks=all (or acks=-1) means the producer considers a message successful
only after the leader and all in-sync replicas (ISR) have successfully
persisted it. This provides the highest durability because another ISR
replica can become the leader without losing the message if the current
leader fails. The trade-off is slightly higher latency.’

Interview Follow-up

Does acks=all guarantee 100% delivery?

No.

It greatly improves durability, but complete reliability also depends
on: - min.insync.replicas - Producer retries - Idempotent Producer
(enable.idempotence=true)


==========================================================================================================================================
Kafka Interview Notes (Q7-Q10)

7. Duplicate Message Processing — How do you handle it?

Problem: Kafka provides at-least-once delivery by default, so the same
message may be delivered more than once.

Solution 1: Idempotent Consumer (Most Common)

Every event contains a unique eventId.

Flow:

Receive Event | Check processed_events table | Already Processed? | |
Yes No Skip Process Event | Save eventId in processed_events

Example:

eventId = EVT-1001

If EVT-1001 already exists in processed_events, ignore the message.

This prevents duplicate processing.

Solution 2: Naturally Idempotent Operations

Example:

SET status=‘ACTIVE’

Running this statement multiple times produces the same final result.

No explicit deduplication is required.

Interview Answer: Use the Idempotent Consumer pattern. Store eventId in
a processed_events table or use naturally idempotent operations so
duplicate messages do not produce duplicate business effects.

------------------------------------------------------------------------

8. Message Lost — How do you detect it?

Producer Side

Producer sends a message.

If ACK is not received (timeout or failure):

-   Producer logs an error
-   Retry may happen
-   Monitor producer failures
-   Configure alerts

Consumer Side

Example:

OrderCreated event should trigger Notification.

If notification never happens:

Order DB = Created Notification DB = Missing

Run a reconciliation job periodically.

Compare source-of-truth with downstream systems.

Retry or repair missing records.

Interview Answer: Monitor producer acknowledgement failures and retries.
On the consumer side, detect missing business outcomes using
reconciliation jobs that compare source-of-truth data with downstream
systems.


=============================================================================================================================
Kafka Message Loss Detection Notes

Q. Message lost ho gaya - Producer side aur Consumer side se kaise
detect karoge?

====================================================== 1. Producer Side
======================================================

Producer | v Kafka | ACK ?

If ACK is received: - Message published successfully.

If ACK is NOT received: - Producer logs an error. - Retry (if
configured). - TimeoutException / NetworkException may occur.

Monitor: - Failed Publish Count - Retry Count - ACK Timeout - Producer
Error Rate

Example: ERROR Failed to publish OrderCreated event.

====================================================== 2. Consumer Side
======================================================

A consumer cannot directly know that a message was never produced.

Instead, it detects missing business outcomes.

Example:

Order DB: 101 -> CREATED 102 -> CREATED 103 -> CREATED

Notification DB: 101 -> SENT 103 -> SENT

Order 102 is missing.

This indicates that downstream processing did not happen.

====================================================== 3. Reconciliation
Job ======================================================

Compare:

Orders DB vs Notification / Payment / Inventory DB

If mismatch found: - Retry - Republish Event - Repair missing data

====================================================== 4. Monitoring
======================================================

Producer: - ACK Timeout - Retry Count - Failed Publish Count

Consumer: - Consumer Lag - DLQ - Processing Failures - Missing Business
Records

====================================================== Interview Answer
======================================================

Producer: Monitor delivery acknowledgements. If ACK is not received
within timeout or an error occurs, log the failure, retry, and alert
using producer metrics.

Consumer: A consumer cannot detect that a producer never published a
message. Instead, detect missing business effects using reconciliation
jobs that compare source-of-truth data with downstream systems.

Important Interview Line:

A consumer can detect that a message was not processed, but it cannot
directly detect that a message was never produced.

=============================================================================================================================
------------------------------------------------------------------------

9. Out-of-Order Messages — What will you do?

Kafka guarantees ordering only within a partition.

If events for the same business entity go to different partitions,
ordering may break.

Correct Approach

Use a consistent partition key.

Example:

partitionKey = orderId

Then:

OrderCreated PaymentCompleted OrderShipped

All go to the same partition.

Application-Level Protection

Include: - version or - timestamp

Consumer logic:

If incoming event version is older than current state, ignore it.

Example:

Current Version = 5

Incoming Version = 3

Ignore the older event.

Interview Answer: Use the same partition key for related events so Kafka
preserves ordering within the partition. Also include version or
timestamp fields and ignore stale events at the application level.

------------------------------------------------------------------------

10. Consumer Lag Increasing Continuously — How do you fix it?

Consumer Lag

Lag = Latest Offset - Committed Offset

Possible Causes

1.  Slow processing

Example: Every message calls an external API taking 2 seconds.

Solution: - Optimize logic - Batch processing - Parallel processing -
Async processing

2.  Too few consumers

10 partitions Only 2 consumers

Solution: Increase consumers (up to number of partitions).

3.  Downstream bottleneck

Database Redis External APIs

Find slow dependency and optimize.

4.  Large traffic spike

Kafka buffers messages.

Scale consumers horizontally.

Monitor

-   Consumer Lag
-   Processing Time
-   Error Rate
-   CPU
-   Memory

Interview Answer: First identify the bottleneck by measuring processing
time and downstream dependencies. Then scale consumers if partitions
allow, optimize processing using batching or parallelism, and
continuously monitor consumer lag and latency.

------------------------------------------------------------------------

Quick Revision

• Duplicate Message → Idempotent Consumer • Lost Message → ACK
monitoring + Reconciliation Job • Out-of-Order → Same Partition Key +
Version/Timestamp • Consumer Lag → Scale Consumers + Optimize
Processing + Monitor

==========================================================================================================================================