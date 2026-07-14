# 29LPA Prep — Answers Part 2: Kafka Fundamentals

Source: `29LPA_MIcroservices_Kafka_ScenarioBased.txt` — "KAFKA" section ke saare questions, theory + Hinglish why/how annotation + code snippets ke saath. Part 1 = Microservices Basics, Part 3 = Scenario-Based.

---

## Section 1 — Fundamentals (Deep Understanding)

**1. Apache Kafka kya hai aur kyun use karte hain?**
Kafka ek distributed event-streaming platform hai — publish-subscribe messaging system jo high-throughput, fault-tolerant, aur durable hai (messages disk pe persist hote hain, replay bhi possible hai). Use karte hain jab high-volume event-data multiple consumers ke through process karni ho, ya services ko decouple karna ho async communication se.

**2. Kafka vs traditional messaging systems (RabbitMQ etc.)**
RabbitMQ — traditional broker, message consume hote hi queue se gayab, complex routing (exchanges/bindings), per-message ack. Kafka — messages ek persistent log mein rehte hain retention-period tak (consume hone ke baad bhi), multiple consumer-groups independently apni pace pe replay/read kar sakte hain, bahut high throughput. RabbitMQ classic "task queue" ke liye better, Kafka "event streaming/log" ke liye.

**3. Kafka architecture explain karo (broker, topic, partition)**
**Broker** — ek Kafka server jo data store/serve karta hai (cluster mein multiple brokers). **Topic** — logical category/stream of messages (jaise `patient.created`). **Partition** — topic ko multiple partitions mein split karte hain parallelism ke liye, har partition ek ordered, immutable log hai, alag brokers pe distribute ho sakti hai.

**4. Topic aur partition ka concept kya hai?**
Topic ek "channel" hai events ke liye. Partition us channel ka ek shard hai — messages **partition ke andar** order mein guaranteed hain (globally topic mein nahi), key-based partitioning se related messages (jaise same `patientId`) consistently same partition mein jaate hain (unke beech order maintain hoti hai).

**5. Partitioning ka benefit kya hai?**
Parallelism — multiple consumers (ek consumer-group ke andar) alag-alag partitions parallel process kar sakte hain, throughput badhta hai. Scalability — partitions alag brokers pe distribute ho sakti hain, load balance hota hai cluster ke across.

---

## Section 2 — Producers & Consumers

**6. Producer ka working kaise hota hai?**
Producer message ko ek topic (aur partition-key se implicit/explicit partition) pe publish karta hai. `acks` config decide karta hai kitna acknowledgment chahiye (0/1/all) success maanne se pehle. Producer batching aur compression bhi apply karta hai throughput ke liye.
```java
ProducerRecord<String, String> record = new ProducerRecord<>("patient.created", patientId, eventJson);
kafkaTemplate.send(record);
```

**7. Consumer group kya hota hai?**
Multiple consumer-instances jo ek logical group banate hain (same `group.id`) — Kafka ensure karta hai ek partition ek time pe **ek hi consumer (within group)** padhe, isse parallel-processing hoti hai bina duplicate-processing ke within group. Alag consumer-groups independently poora topic apni pace se padh sakte hain.

**8. Multiple consumers ek hi topic kaise read karte hain?**
Agar alag consumer-groups hain, har group independently poora topic padhta hai (apna khud ka offset track karta hai) — jaise ek group notification bhejta hai, doosra group analytics update karta hai, dono independently same events consume karte hain bina ek-doosre ko affect kiye.

**9. Offset kya hota hai?**
Har message ek partition ke andar ek sequential ID (offset) rakhta hai. Consumer apna "current position" (last-processed offset) per-partition track karta hai — isse pata chalta hai restart ke baad kaha se aage padhna hai.

**10. Auto commit vs manual commit difference**
Auto-commit — periodically (jaise har 5 sec) client khud offset commit kar deta hai, simple lekin risk hai ki message "processed" mark ho jaaye bina actually successfully process hue (beech mein crash), ya duplicate-processing ho. Manual commit — processing **successfully complete hone ke baad** explicitly commit karo, better control, standard production practice.
```java
@KafkaListener(topics = "patient.created")
public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
    process(record.value());
    ack.acknowledge(); // manual commit — sirf success ke baad
}
```

---

## Section 3 — Delivery Semantics

**11. At-most-once, At-least-once, Exactly-once kya hote hain?**
**At-most-once** — message ek baar try hota hai, fail ho toh drop (data-loss risk, kabhi duplicate nahi). **At-least-once** — success milne tak retry karte hain, duplicate-processing possible (consumer idempotent hona chahiye). **Exactly-once** — na loss, na duplicate — sabse hard achieve karna, Kafka transactions API se Kafka-ecosystem ke andar possible.

**12. Exactly-once achieve kaise karte ho Kafka me?**
Idempotent producer (duplicate-prevent producer-side, `enable.idempotence=true`) + transactional API (`transactional.id` set karke, producer-send aur consumer-offset-commit dono ek atomic transaction mein) — "read-process-write" pattern ke liye achievable hai within Kafka. External-system side-effects (DB write) ke liye application-level idempotency alag se chahiye hoti hai.

**13. Duplicate messages kaise avoid karte ho?**
Consumer-side idempotency — har event ka unique `eventId`, ek "processed_events" table/set mein check karo pehle se process hua ya nahi, agar haan skip karo. Producer-side — idempotent producer enable karo (network-retry se hone waale duplicates prevent hote hain).

**14. Idempotent producer kya hota hai?**
`enable.idempotence=true` — producer ko ek unique sequence-number milta hai per-partition, broker duplicate sequence-numbers ko automatically reject/dedupe kar deta hai — retry se hone waale duplicates producer-level pe hi prevent ho jaate hain.

**15. Transactional producer kya hota hai?**
Multiple messages (possibly multiple topics/partitions) ko ek atomic transaction mein bhejna — ya sab commit, ya sab abort. "Read-process-write" (ek topic se consume karke doosre mein produce karna) ko exactly-once banane ke liye use hota hai.
```java
producer.initTransactions();
producer.beginTransaction();
producer.send(record1);
producer.send(record2);
producer.commitTransaction(); // atomic — dono ya koi nahi
```

---

## Section 4 — Performance & Scaling

**16. Kafka me high throughput kaise achieve hota hai?**
Partitioning (parallelism), batching (multiple messages ek network-call mein), compression (kam bytes-over-wire), aur `acks` tuning (`acks=1` zyada throughput deta hai `acks=all` se, safety trade-off ke saath).

**17. Partition count kaise decide karte ho?**
`expected-throughput ÷ per-partition-throughput` = minimum partitions, aur consumer-parallelism-need bhi consider karo (ek partition ek time pe ek hi consumer padh sakta hai within group — jitne max parallel consumers chahiye, utne minimum partitions chahiye). Zyada partitions overhead bhi badhate hain (metadata, file-handles) — balance zaroori hai.

**18. Producer batching kaise kaam karta hai?**
Producer messages turant individually nahi bhejta — `linger.ms` (thoda wait karo naye messages accumulate hone ke liye) aur `batch.size` config se multiple messages ek hi network-request mein batch karke bhejta hai — throughput significantly better, thoda latency trade-off hota hai.

**19. Compression ka role kya hai?**
`compression.type` (gzip/snappy/lz4/zstd) — messages compress karke bhejte hain, network-bandwidth aur storage dono kam lagte hain, especially batching ke saath combine hone pe effective. Trade-off: thoda CPU overhead compress/decompress mein.

**20. Consumer lag kya hota hai? kaise monitor karte ho?**
Consumer-lag = `latest-produced-offset − consumer-ka-last-committed-offset` — matlab consumer producer se kitna "peeche" hai. Monitor: Kafka built-in tooling (`kafka-consumer-groups.sh --describe`), ya Prometheus/Grafana se continuous track + alert (threshold cross hone pe).

---

## Section 5 — Fault Tolerance

**21. Replication factor kya hota hai?**
Har partition ki kitni copies (replicas) alag brokers pe maintain hoti hain — `replication-factor=3` matlab 3 copies, ek broker down ho jaaye bhi data doosre replicas se available rehta hai.

**22. Leader-follower model kaise kaam karta hai?**
Har partition ka ek "leader" broker hota hai (saare reads/writes leader se hi hote hain), baaki replicas "followers" hain jo leader se continuously replicate karte hain. Leader down hone pe ek in-sync follower automatically naya leader ban jaata hai.

**23. Broker down ho jaye to kya hota hai?**
Us broker ke leader-partitions ke liye naya leader-election hota hai (ISR mein se), clients automatically naye leader ko discover karke redirect ho jaate hain — thodi der (failover time) unavailability ke alawa system bina data-loss ke continue karta hai (agar replication-factor>1 aur ISR maintained thi).

**24. Data loss kaise prevent karte ho?**
Replication-factor `>=3`, `acks=all` (producer sirf tab success maane jab saare in-sync-replicas confirm karein), `min.insync.replicas` config (minimum kitne replicas write-confirm karein before success) — sab combine karke durability ensure karte hain.

**25. ISR (In-Sync Replicas) kya hota hai?**
Wo replicas jo leader ke saath fully caught-up/synced hain (bina significant lag ke). Sirf ISR mein se hi naya leader elect ho sakta hai — isse guarantee milta hai naya leader ka data outdated nahi hai.

---

## Section 6 — Real-World Usage

**26. Kafka ko microservices me kaise use karte ho?**
Event-driven communication ke liye — services domain-events publish karti hain (`order.placed`), interested services subscribe karke apna kaam karti hain bina direct coupling ke — pehle bhi healthcare-guide mein isi pattern ko `consent.granted`/`document.shared` events ke saath detail kiya tha.

**27. Event-driven architecture kaise design karte ho?**
Domain-events identify karo (state-changes jo doosri services ko matter karte hain), topics design karo (per-domain-event-type), producers/consumers define karo, aur schema (JSON-schema/Avro, versioning-strategy) plan karo backward-compatibility maintain karne ke liye.

**28. Message ordering kaise maintain karte ho?**
Same partition-key use karo related messages ke liye (jaise `patientId`) — Kafka guarantee karta hai same-partition messages order mein hi consume honge. Cross-partition ordering guarantee nahi hoti.

**29. Retry mechanism kaise implement karte ho?**
Consumer processing fail ho toh — immediate retry (thoda backoff ke saath), aur max-attempts ke baad DLQ mein bhej do. **Retry-topic pattern**: alag "retry" topic banao jisme delayed-retry messages jaayein, alag delay-tiers ke liye multiple retry-topics bhi ban sakte hain.

**30. Dead Letter Queue (DLQ) kya hota hai? kaise use karte ho?**
Jab message N retries ke baad bhi process nahi ho paata, ek separate "dead letter" topic mein bhej dete hain (drop nahi karte) — taaki koi manually investigate kar sake, aur main-topic ka processing block na ho ek "poison message" ki wajah se.
```java
@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2))
@KafkaListener(topics = "patient.created")
public void consume(String message) { ... }

@DltHandler
public void handleDlt(String message) {
    log.error("Message sent to DLT after retries: {}", message);
}
```

---

## 💣 Interviewer Killer Questions

**"Agar consumer slow ho jaye to kya hoga?"**
Consumer-lag badhta rahega. Agar bahut zyada slow ho gaya (consumer `max.poll.interval.ms` exceed kar de), Kafka usse "dead" maan ke partition kisi doosre consumer ko reassign kar deta hai (rebalance trigger) — data-loss nahi hota (Kafka retain karta hai retention-period tak), lekin processing-delay badhta hai. Fix: consumer scale-out karo (zyada instances, agar partitions available hain), ya processing-logic optimize karo.

**"Kafka me message lost ho sakta hai?"**
Haan, agar sahi configuration na ho — jaise `acks=0`/`acks=1` ke saath leader-failure ho jaaye replication se pehle, ya `min.insync.replicas` low set ho. Sahi config (`acks=all`, replication-factor≥3, `min.insync.replicas≥2`) se loss-risk almost-zero kiya ja sakta hai, lekin "impossible" bolna galat answer hai — distributed systems mein absolute-guarantees rare hoti hain, tumhe trade-offs explicitly samajhne chahiye (yahi senior-level answer interviewer expect karta hai).

**"Exactly-once practical hai ya theory?"**
Kafka-to-Kafka flows (read-process-write within Kafka ecosystem) ke liye exactly-once practically achievable hai transactions API se. Lekin jab external-system side-effects involve hote hain (Kafka-consume-karke-DB-write, ya external-API-call), true exactly-once impossible hai (do independent systems ko ek atomic transaction mein guarantee nahi kar sakte generally) — practical approach hai **"at-least-once delivery + idempotent processing"**, jo effectively exactly-once ka result deta hai bina genuine distributed-transaction complexity ke.

---

*Part 3 mein: Scenario-Based questions (Service Communication Failures, Kafka Message Handling, Data Consistency, Scaling Scenarios, Real Event-Driven Use Cases, Fault Tolerance & Recovery, Production Debugging).*
======================================================================================================================

Kafka Interview Notes (Important)

1. Why is Offset maintained per Consumer Group and per Partition?

Suppose a topic has 4 partitions.

P0 P1 P2 P3

Consumer Group = notification-group

Initially:

Consumer1 ├── P0 ├── P1 ├── P2 └── P3

Messages:

P0 Offset0 -> M1 Offset1 -> M2 Offset2 -> M3 Offset3 -> M4

P1 Offset0 -> M5 Offset1 -> M6 Offset2 -> M7

After processing:

P0 committed offset = 4 P1 committed offset = 3 P2 committed offset = 10
P3 committed offset = 2

These offsets DO NOT belong to Consumer1.

Kafka stores:

Consumer Group = notification-group

P0 -> 4 P1 -> 3 P2 -> 10 P3 -> 2

inside the internal topic:

__consumer_offsets

Reason:

If Consumer1 crashes, Kafka performs a rebalance.

Consumer2 may now receive P0.

Kafka checks:

notification-group P0 -> committed offset = 4

Consumer2 starts from Offset 4 (next unread message).

If offsets were stored inside Consumer1, Kafka would not know where to
resume after the crash.

Rule:

Offset = Per Partition + Per Consumer Group

NOT per consumer instance.

------------------------------------------------------------------------

2. Multiple Consumer Groups

Topic

OrderCreated

notification-group

P0 Offset = 100

analytics-group

P0 Offset = 40

Both consume the same topic independently because each group maintains
its own offsets.

------------------------------------------------------------------------

3. 10 Partitions and 3 Consumers

Topic

P0 P1 P2 P3 P4 P5 P6 P7 P8 P9

Consumer1

P0 P1 P2 P3

Consumer2

P4 P5 P6

Consumer3

P7 P8 P9

Each partition is assigned to only ONE consumer within the same group.

A consumer can own multiple partitions.

Maximum parallelism = Number of partitions.

------------------------------------------------------------------------

4. Consumer Crash

Before

Consumer1 -> P0 P1 P2 P3

Consumer2 -> P4 P5 P6

Consumer3 -> P7 P8 P9

Consumer2 crashes.

Kafka rebalances.

Consumer1 -> P0 P1 P2 P3 P4

Consumer3 -> P5 P6 P7 P8 P9

New consumer resumes from the last committed offset.

------------------------------------------------------------------------

5. Producer ACKs

acks=0

Producer sends the message and does not wait for any acknowledgement.

Fastest.

Risk: Message may be lost if broker fails immediately.

Use when occasional loss is acceptable.

------------------------------------------------------------------------

acks=1

Leader broker writes the message and sends acknowledgement.

Producer receives success after leader write.

If leader crashes before followers replicate, the message can still be
lost.

Good balance between performance and durability.

------------------------------------------------------------------------

acks=all (or -1)

Leader waits until all in-sync replicas (ISR) acknowledge.

Only then producer gets success.

Safest option.

Highest durability.

Slightly higher latency.

Used for payments, banking, orders, etc.

------------------------------------------------------------------------

ACK Comparison

acks=0 Fastest Lowest durability

acks=1 Medium speed Medium durability

acks=all Highest durability Highest latency

------------------------------------------------------------------------

Interview One-liners

• Offset is maintained per partition per consumer group. • A consumer
instance is temporary; consumer groups own the progress. • Each
partition is consumed by only one consumer in a group. • Different
consumer groups read the same topic independently. • Maximum parallelism
equals the number of partitions. • acks=0 = no acknowledgement • acks=1
= leader acknowledgement • acks=all = all in-sync replicas acknowledge
