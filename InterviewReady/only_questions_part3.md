# Senior Java Backend Developer — Interview Question Bank (Part 3 of 4)

**Coverage:** Section 9 (Design Patterns), Section 10 (REST APIs), Section 11 (SQL), Section 12 (System Design - Backend)
**Continuing numbering from Part 2** (Q1–100 already covered).
**Legend:** ⭐⭐⭐⭐⭐ = Top priority | 🔥 = Must-prepare

---

## Section 9 — Design Patterns (Spring Boot ke context mein)

**101. ⭐⭐⭐⭐⭐ Singleton pattern Spring khud kaise implement karta hai (default bean scope)? Thread-safe singleton manually kaise likhoge (double-checked locking)?**
Spring ka default bean scope hi **singleton** hai — container startup pe (ya first request pe) ek hi instance banata hai aur usi ko poori application ke liye reuse karta hai. Manual thread-safe singleton (Spring ke bahar):
```java
public class Singleton {
    private static volatile Singleton instance;
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) instance = new Singleton();
            }
        }
        return instance;
    }
}
```
`volatile` isliye zaroori hai kyunki bina uske JVM instruction-reordering kar sakta hai — ek thread ko ek "partially constructed" object dikh sakta hai (object reference assign ho gaya lekin constructor poora complete nahi hua).

**102. Factory pattern vs Abstract Factory — Spring mein `BeanFactory`/`FactoryBean` isi pattern ka real example kaise hai?**
Factory pattern — ek method input ke basis pe alag-alag concrete implementations return karta hai (jaise `PaymentGatewayFactory.get("stripe")` → `StripeGateway` instance). Abstract Factory — factories ka factory, related objects ki poori "family" banata hai. Spring ka `BeanFactory`/`ApplicationContext` khud Factory pattern ka real-world example hai — tum `context.getBean(SomeService.class)` call karte ho, aur container decide karta hai actual konsi implementation return karni hai (configuration/profile ke basis pe).

**103. Builder pattern kab use karoge — Lombok `@Builder` ke internal mechanism se explain karo, aur immutable object banane mein kaise help karta hai?**
Jab constructor mein bahut saare parameters hon (especially optional ones), telescoping-constructor anti-pattern (multiple overloaded constructors) se bachne ke liye Builder use karte ho. Lombok `@Builder` compile-time annotation-processing se ek nested static `Builder` class generate karta hai jisme fluent chained methods hote hain (`Employee.builder().name("x").age(30).build()`) — `build()` call hote hi final immutable object banta hai. Ye readable code deta hai aur immutability maintain karta hai (koi setter expose nahi hota final object pe).

**104. Prototype pattern ka real use case do jaha har baar naya independent object chahiye ho (Spring prototype bean scope se relate karo).**
Jab har baar ek "template" jaisa object chahiye ho jo baad mein independently customize hoga (jaise ek base configuration object jise har request apne hisaab se modify kare bina doosre requests ko affect kiye) — Prototype pattern relevant hai. Spring ka `prototype` bean scope isi concept se directly relate karta hai: singleton ke against, har `getBean()` call pe Spring ek **naya independent instance** return karta hai.

**105. ⭐⭐⭐⭐⭐ Proxy pattern — Spring AOP aur `@Transactional` internally proxy pattern hi use karte hain, explain karo kaise.**
Proxy pattern mein ek "proxy" object real/target object ko wrap karta hai aur uske access ko control karta hai. Spring AOP runtime pe ek proxy object banata hai jo target bean ko wrap karta hai — jab bhi tum us bean ka method call karte ho, actually proxy ka method pehle invoke hota hai (jaha cross-cutting logic — transaction begin/commit, logging, security-check — inject hoti hai), aur phir proxy internally actual target object ka method call karta hai. `@Transactional` isi mechanism se kaam karta hai: proxy method-entry pe transaction start karta hai, actual method run hone dete hai, aur method-exit pe (exception ya success ke basis pe) commit/rollback karta hai.

**106. Adapter pattern ka real use case do (jaise legacy third-party API ko apne interface ke saath integrate karna).**
Jab existing (legacy ya third-party) code ka interface tumhare application ke expected interface se match nahi karta, ek Adapter class beech mein rakh ke translate karte ho. Real example: ek purana SOAP-based hospital records system ko integrate karna hai, lekin tumhara application ek modern `PatientDataProvider` (REST-style) interface expect karta hai — ek `LegacySoapAdapter implements PatientDataProvider` banao jo internally SOAP calls kare lekin bahar se apne application ko standard interface dikhaye, taaki application code ko underlying SOAP complexity ka pata hi na chale.

**107. Decorator pattern vs inheritance — Java IO classes (`BufferedReader(new FileReader())`) isi pattern ka classic example hain, explain karo.**
`new BufferedReader(new FileReader("file.txt"))` — `BufferedReader` `FileReader` ko "decorate" karta hai extra behavior (buffering, jisse read performance improve hoti hai) add karke, bina `FileReader` ki class hierarchy ko modify kiye ya usse inherit kiye. Ye composition-based hai, isliye runtime pe flexibly multiple decorators stack kar sakte ho (jaise `BufferedReader(new InputStreamReader(new FileInputStream(...)))`) — inheritance se aisi flexible combination achieve karna practically impossible ho jaata (har combination ke liye alag subclass banani padti).

**108. Facade pattern — Spring Boot service layer khud ek facade ki tarah kaise kaam karta hai multiple repositories/external calls ke upar?**
Controller layer ek simple `orderService.placeOrder(request)` call karta hai. Andar `OrderService` internally multiple repositories (Order, Inventory), external API calls (Payment Gateway), aur Kafka event publish — sab kuch orchestrate karta hai. Poori complexity ek simple, unified interface (`placeOrder()`) ke peeche chhup jaati hai — controller (client) ko underlying complexity ka pata hi nahi chalta. Yahi Facade pattern hai.

**109. ⭐⭐⭐⭐⭐ Strategy pattern kaise implement karoge Spring mein multiple implementations ke saath (jaise multiple payment gateways) — `@Qualifier` ya Map-based bean injection se?**
Ek common interface `PaymentStrategy` define karo (`processPayment(amount)`), aur har gateway (`StripeStrategy`, `RazorpayStrategy`) usse implement kare, har ek `@Component("stripe")` jaisa named bean ho. Runtime pe select karne ke do tareeke: (1) `@Qualifier("stripe")` se ek specific implementation compile-time inject karo agar fix pata hai, ya (2) `Map<String, PaymentStrategy>` type ka field inject karo — Spring automatically saari implementations ko unke bean-name se keyed ek Map bana ke de deta hai, aur tum runtime pe `strategyMap.get(userSelectedGateway)` se dynamically select kar sakte ho.

**110. Observer pattern — Spring Application Events (`ApplicationEventPublisher`, `@EventListener`) isi pattern pe based hain, explain karo real use case ke saath.**
```java
eventPublisher.publishEvent(new OrderPlacedEvent(orderId));

@EventListener
public void onOrderPlaced(OrderPlacedEvent event) {
    inventoryService.reserveStock(event.getOrderId());
}
```
Publisher (OrderService) ko listeners ka koi direct reference nahi rakhna padta — completely decoupled. Real use case: `OrderPlacedEvent` publish hone pe alag-alag independent listeners (inventory reservation, confirmation email, analytics tracking) apna kaam kar sakte hain bina `OrderService` ko in sabke baare mein jaante hue — naya listener add karna existing code touch kiye bina possible hai.

**111. Chain of Responsibility pattern — Servlet Filter chain ya Spring Security filter chain isi pattern ka example kaise hai?**
Request ek ordered "chain" of filters se sequentially guzarta hai — har filter ko decide karne ka mauka milta hai ki wo request ko process kare aur aage badhaye (`chain.doFilter(request, response)` call karke), ya yahi process ko rok de (jaise authentication filter agar token invalid paaye toh 401 return karke chain ko aage nahi badhne dega). Har filter ko doosre filters ke internal implementation ka pata nahi hota, wo bas apna kaam karke decide karta hai "aage bhejna hai ya rokna hai" — ye exactly Chain of Responsibility pattern hai.

**112. Template Method pattern — `JdbcTemplate`/`RestTemplate` mein ye pattern kaise use hota hai boilerplate reduce karne ke liye?**
Template Method pattern mein parent class ek algorithm ka fixed "skeleton" define karti hai, aur specific customizable steps subclass/callback ke through fill hote hain. `JdbcTemplate` boilerplate steps (connection open karna, exceptions ko Spring ke `DataAccessException` hierarchy mein translate karna, resources properly close karna finally block mein) khud handle karta hai, aur tum sirf variable part provide karte ho (actual SQL query + `RowMapper` callback jo result ko object mein convert kare). Isse repetitive JDBC boilerplate (try-catch-finally, connection management) har jagah likhne se bacha jaata hai.

---

## Section 10 — REST APIs

**113. ⭐⭐⭐⭐⭐ PUT vs PATCH vs POST — idempotency ke perspective se teeno mein farak explain karo real example ke saath.**
**PUT** idempotent hai — same request N baar bhejo, result same rehta hai (poora resource replace karta hai naye data se — jaise `PUT /patients/5` poora patient object replace kar deta hai). **PATCH** partial update karta hai — idempotent hona *chahiye* by convention lekin implementation pe depend karta hai (jaise `PATCH {"increment": 1}` idempotent NAHI hai kyunki har call se value badhegi, jabki `PATCH {"status": "active"}` idempotent hai). **POST** non-idempotent hai by convention — same POST request baar-baar bhejne se typically har baar ek naya resource create ho sakta hai (jaise "create order" API, isliye isko idempotent banane ke liye alag se idempotency-key pattern chahiye).

**114. HTTP status codes — 401 vs 403 mein exact difference kya hai, aur 409 (Conflict) kab use karoge?**
**401 Unauthorized** — matlab request khud ko properly authenticate hi nahi kar paayi (missing, invalid, ya expired token) — system ko pata hi nahi ki caller kaun hai. **403 Forbidden** — system ko pata hai caller kaun hai (authentication successful), lekin us caller ko requested resource/action ka access/permission nahi hai (authorization fail). **409 Conflict** — request syntactically valid hai lekin resource ki current state ke saath conflict kar rahi hai — jaise duplicate email se signup karne ki koshish, ya optimistic-locking version-mismatch (koi doosra already resource update kar chuka hai).

**115. Pagination REST API mein kaise design karoge large dataset ke liye — offset-based vs cursor-based pagination mein trade-off kya hai?**
Offset-based (`?page=2&size=20`, internally `LIMIT 20 OFFSET 20`) implement karna simple hai lekin bade offsets pe performance degrade hoti hai (DB ko skip-hone-wali rows bhi internally scan karni padti hain), aur agar pagination ke beech mein data insert/delete ho jaaye toh pages "shift" ho sakte hain (duplicate ya missing results dikhna). Cursor-based (`?after=lastSeenId&limit=20`) consistent performance deta hai chahe kितna bhi deep paginate karo (index seek use hota hai `WHERE id > lastSeenId ORDER BY id LIMIT 20`), aur concurrent modifications se bhi resilient hai — high-traffic ya infinite-scroll type APIs ke liye better choice hai.

**116. Filtering aur sorting query parameters ke through kaise design karoge taaki API flexible ho lekin over-fetching na ho?**
`GET /patients?status=active&sortBy=lastVisit&order=desc&page=0&size=20` jaisa design flexible hai, lekin **allowed sort/filter fields ko whitelist karo** server-side (arbitrary column names accept karna SQL-injection-adjacent risk aur unindexed-column performance risk dono create karta hai). Zyada filters ek saath combine hone ki possibility ho toh composite indexing strategy pehle se plan karo (Section 11 dekho), aur default `size` limit + max `size` cap lagao taaki client accidentally/maliciously bahut bada dataset na maang sake.

**117. ⭐⭐⭐⭐⭐🔥 API idempotency kaise ensure karte ho non-idempotent operations (jaise "create order") mein — idempotency key pattern explain karo.**
Client ek unique `Idempotency-Key` header bhejta hai request ke saath (typically ek client-generated UUID, jo ek particular logical operation-attempt ke liye consistent rehta hai retries ke across). Server pehle check karta hai ek fast-lookup store (Redis/DB) mein — agar wo key already process ho chuki hai, pehle se computed/stored response wapas return kar do bina operation ko dobara actually execute kiye. Agar key naya hai, operation process karo aur result ko us key ke saath store kar do future retries ke liye. Isse client network-timeout ke baad safely retry kar sakta hai (jaha usse pata nahi ki original request server tak pahunchi thi ya nahi) — bina duplicate order/duplicate charge ke risk ke.

**118. ⭐⭐⭐⭐⭐ JWT-based authentication kaise kaam karta hai — token structure (header, payload, signature), aur token expiry/refresh strategy kya honi chahiye?**
JWT ke teen parts hote hain (dot-separated, base64-encoded): **Header** (signing algorithm info, jaise HS256/RS256), **Payload** (claims — user ID, roles, `exp` expiry timestamp, waghera), **Signature** (Header+Payload ko secret key ya private key se cryptographically sign kiya jaata hai — isse server verify kar sakta hai ki token tamper nahi hua). Expiry/refresh strategy: access token ko **short-lived** rakho (jaise 15 minutes — agar leak bhi ho jaaye toh damage window chhota rahega), aur ek longer-lived **refresh token** (securely stored, jaise httpOnly secure cookie) issue karo jisse naya access token silently generate kiya ja sake bina user ko baar-baar login karwaye.

**119. OAuth2 ke grant types (Authorization Code, Client Credentials) mein kab kaunsa use karoge — machine-to-machine vs user-facing app ke liye?**
**Authorization Code grant** — user-facing applications ke liye (browser-based redirect flow) — user apni credentials seedha third-party application ko nahi deta, balki authorization server (jaise Google/Okta) pe login karta hai aur ek "authorization code" application ko milta hai jo phir token se exchange hota hai — sabse secure user-delegated-access flow hai. **Client Credentials grant** — machine-to-machine communication ke liye jaha koi human user involved hi nahi hai — ek backend service doosri backend service ko apni khud ki identity (client ID + secret) se authenticate karti hai, jaise internal microservice-to-microservice API calls.

**120. Rate limiting kaise implement karoge API Gateway ya application level pe (token bucket vs sliding window algorithm)?**
**Token bucket** — ek bucket mein tokens ek fixed rate se refill hote rehte hain (jaise 10 tokens/second), har incoming request ek token consume karti hai, agar bucket empty hai request reject/queue ho jaati hai — ye bursty traffic ko thodi flexibility deta hai (agar tokens accumulate ho gaye hain idle period mein, ek short burst allow ho sakta hai). **Sliding window** — ek time window (jaise pichle 60 seconds) mein kitni requests aayi count karta hai aur limit enforce karta hai — zyada precise/smooth rate limiting deta hai lekin thoda zyada memory/computation chahiye (fixed-window ke against jo boundary pe "double burst" allow kar sakta hai).

---

## Section 11 — SQL

**121. ⭐⭐⭐⭐⭐ INNER JOIN vs LEFT JOIN vs self-join — real query example do jaha teeno ka use case clearly differ karta ho.**
**INNER JOIN** — sirf un rows ko return karta hai jaha dono tables mein match mila ho: `SELECT * FROM orders o INNER JOIN customers c ON o.customer_id = c.id` — sirf wahi orders jinka koi valid customer hai. **LEFT JOIN** — left table ki saari rows return karta hai, right table se match na milne pe uske columns `NULL` ho jaate hain: `SELECT c.name, o.id FROM customers c LEFT JOIN orders o ON c.id = o.customer_id` — saare customers dikhega, chahe unhone koi order kiya ho ya nahi. **Self-join** — table ko khud se join karna, jaise employee-manager relationship: `SELECT e.name, m.name AS manager FROM employees e JOIN employees m ON e.manager_id = m.id` — manager bhi employee table mein hi hai.

**122. Indexing internally B-tree structure pe based hoti hai — composite index mein column order kyun matter karta hai (leftmost prefix rule)?**
Composite index (multiple columns pe ek hi index, jaise `(status, created_at)`) internally ek B-tree hai jo columns ke sequence mein sorted hai — pehle `status` se sort, phir usi status ke andar `created_at` se sort. **Leftmost prefix rule**: query us index ko efficiently tabhi use kar sakti hai jab query ka WHERE clause index ke leftmost column(s) se start ho — `WHERE status=? AND created_at=?` ya sirf `WHERE status=?` dono index use kar sakti hain, lekin `WHERE created_at=?` (bina status ke) index use nahi kar payegi kyunki wo B-tree `created_at` ke basis pe globally sorted nahi hai, sirf har status-group ke andar sorted hai — bilkul waise jaise ek dictionary mein tum "pehle letter" ke bina beech ke kisi letter se directly search nahi kar sakte.

**123. Normalization (1NF, 2NF, 3NF) vs denormalization — high-read system mein kab jaan-bujhkar denormalize karoge?**
Normalization data redundancy khatam karta hai (har fact ek hi jagah store hota hai), update anomalies avoid karta hai, lekin reads ke liye zyada JOINs lagte hain jo especially high-read-volume systems mein performance cost bante hain. High-read system mein jaan-bujhkar denormalize karte hain — kuch redundant columns rakhte hain (jaise `orders` table mein `customer_name` bhi directly store kar lena, bina har baar `customers` table join kiye) taaki read queries fast rahein — trade-off ye hai ki `customer_name` update hone pe usse saari denormalized copies mein bhi sync karna padega (write complexity thodi badh jaati hai read-performance ke badle mein).

**124. ⭐⭐⭐⭐⭐🔥 Transaction isolation levels (Read Uncommitted, Read Committed, Repeatable Read, Serializable) — dirty read, non-repeatable read, aur phantom read ka real example do.**
**Read Uncommitted** (sabse loose) — dirty read possible hai: T1 ne ek row update ki lekin abhi commit nahi kiya, T2 wo uncommitted value padh sakta hai — agar T1 rollback ho jaaye, T2 ne ek aisi value use kar li jo kabhi actually exist hi nahi hui. **Read Committed** — dirty read prevent karta hai (sirf committed data dikhta hai), lekin non-repeatable read possible hai: T1 ek row do baar padhta hai same transaction ke andar, lekin beech mein T2 ne wo row update+commit kar diya, toh T1 ko dono baar alag value milegi. **Repeatable Read** — non-repeatable read prevent karta hai, lekin phantom read possible hai: T1 ek range-query do baar chalata hai (jaise `WHERE age > 30`), beech mein T2 ne matching criteria wali ek nayi row insert+commit kar di, toh T1 ke doosre query mein ek extra ("phantom") row dikh sakti hai. **Serializable** (sabse strict) — phantom read bhi prevent karta hai, transactions ko effectively sequential jaisa behave karwata hai, lekin sabse zyada locking/performance cost aata hai.

**125. Deadlock database level pe kaise hota hai, aur application code mein kaise avoid/handle karte ho (consistent lock ordering)?**
Do transactions circularly ek-doosre ka wait karte hain — T1 ne row-A lock kiya hai aur row-B ka wait kar raha hai (jo T2 ke paas lock hai), aur T2 ne row-B lock kiya hai aur row-A ka wait kar raha hai (jo T1 ke paas hai) — dono permanently stuck. Database engine khud periodically deadlocks detect karta hai aur ek transaction ko forcefully abort/rollback kar deta hai ("deadlock victim") taaki doosra aage badh sake. Application-level prevention: **consistent lock ordering** follow karo — hamesha same predictable order mein rows access/lock karo saari transactions mein (jaise "hamesha lower primary-key wali row pehle lock karo"), isse circular-wait situation ban hi nahi sakti.

**126. Query optimization ke liye `EXPLAIN`/`EXPLAIN ANALYZE` kaise padhte ho — full table scan identify karke index kaise add karoge?**
`EXPLAIN ANALYZE` query ka actual execution plan dikhata hai — dhyaan se dekho **"Seq Scan"** (sequential/full table scan — agar table bada hai toh ye red flag hai) vs **"Index Scan"/"Index Only Scan"** (index efficiently use ho rahi hai — achha sign). Estimated rows/cost vs actual rows/time bhi compare karo — bade gap ka matlab table statistics stale ho sakte hain (`ANALYZE tablename` chalao freshen karne ke liye). Full table scan identify hone pe usually fix hota hai — WHERE clause ya JOIN condition mein use ho rahe columns pe index add karna.

**127. Stored procedures ka use kab justified hai microservices architecture mein, aur kab avoid karna chahiye (business logic app layer mein rakhna better kyun hai)?**
Microservices architecture mein stored procedures mein business logic embed karna generally **avoid** kiya jaata hai kyunki: version control/testing/CI-CD application-code ke saath consistent nahi rehta (DB scripts alag lifecycle mein hote hain), database vendor lock-in badh jaata hai, aur ye microservices ke core principle ke against jaata hai (service boundaries clear rahein, logic portable/testable ho application layer mein). Justified hona possible hai sirf pure bulk data-manipulation ya heavy reporting-type operations ke liye jaha network round-trip avoid karna genuinely critical performance factor ho, business-logic ke liye nahi.

**128. Execution plan mein "cost" aur "rows" estimate ka matlab kya hota hai, aur query slow hone ka root cause kaise trace karoge?**
"Cost" ek relative arbitrary unit hai (actual milliseconds nahi) jo query planner estimate karta hai — kitna "expensive" (disk I/O + CPU) ye step hoga, typically `startup_cost..total_cost` format mein dikhta hai. "Rows" estimate hai ki ye step kitni rows return karega. Agar estimated rows aur `EXPLAIN ANALYZE` ki actual rows mein bahut bada farak ho, matlab table statistics stale hain (`ANALYZE` chalao). Slow query trace karne ka process: `EXPLAIN ANALYZE` chalao, dekho kaunsa individual step sabse zyada time/cost consume kar raha hai (usually deepest nested loop ya seq scan) — wahi root cause hota hai, aur fix usually ek missing index ya suboptimal join order hota hai.

---

## Section 12 — System Design (Backend-Focused)

**129. ⭐⭐⭐⭐⭐🔥 URL Shortener design karo — unique short code generation strategy (base62 encoding vs hash-based), database schema, aur high-read scalability kaise achieve karoge?**
Unique short code: **Base62 encoding** — ek auto-incrementing DB ID (jaise `12345`) ko base62 (a-z, A-Z, 0-9 = 62 characters) mein encode karo, chhota alphanumeric string milta hai jo by-construction collision-free hai (kyunki underlying ID unique hai). Alternative: **hash-based** — long URL ka MD5/SHA hash lekar uska prefix use karo, lekin isme collision-check logic explicitly add karni padti hai. Schema: `short_code (PK)`, `long_url`, `created_at`, `expiry_date`, `click_count`. High-read scalability: chunki reads (redirect requests) writes (URL creation) se bahut zyada hote hain, aggressive **Redis caching** karo (`short_code → long_url` mapping) taaki zyadatar redirects DB tak jaayein hi na, aur read-replicas add karo agar cache-miss traffic bhi zyada ho.

**130. ⭐⭐⭐⭐⭐ Notification service design karo jo email/SMS/push teeno support kare — retry, dead-letter queue, aur rate-limiting per user kaise design karoge?**
Ek common `NotificationChannel` interface (Strategy pattern — Q109 dekho) banao jisse `EmailChannel`, `SmsChannel`, `PushChannel` implement karein. Service ek Kafka consumer hai jo domain events (jaise `appointment.booked`) consume karke relevant channel(s) ko trigger karta hai. **Retry**: failed sends ko exponential backoff ke saath retry karo (2s, 4s, 8s...). **Dead-letter queue**: agar N retries ke baad bhi fail ho raha hai, message ko DLQ mein bhej do — automatically drop mat karo, taaki koi manually investigate kar sake (jaise permanently-invalid phone number). **Rate-limiting per user**: Redis mein per-`userId` ek token-bucket counter maintain karo, taaki ek user ko ek hi event-type ke multiple duplicate/spam notifications na chale jaayein.

**131. Payment service design karo — idempotency, distributed transaction handling, aur double-charging kaise prevent karoge?**
**Idempotency**: client-generated idempotency key pattern (Q117/Q98 dekho) — same payment-attempt-key dobara aane pe purana result return karo, dobara charge mat karo. **Distributed transaction handling**: Saga pattern (Q94 dekho) — steps: reserve funds → charge payment gateway → confirm order → publish `payment.completed` event; agar koi step fail ho, pehle-complete-hue steps ke liye compensating actions chalao (jaise "release reserved funds"). **Double-charging prevention**: idempotency key + database-level unique constraint on the transaction-reference-ID column (isse even agar application-logic mein race-condition ho jaaye, DB khud duplicate insert reject kar dega) + payment-gateway-call se pehle status-check ("already charged?") karo.

**132. ⭐⭐⭐⭐⭐ Order Management system design karo — order state machine, inventory reservation, aur concurrent order placement (race condition) kaise handle karoge?**
**Order state machine**: explicit valid states aur transitions define karo — `CREATED → CONFIRMED → SHIPPED → DELIVERED`, ya `CREATED → CANCELLED` — invalid transitions (jaise `DELIVERED → CREATED`) code-level reject karo. **Inventory reservation**: order place hote hi stock ko turant permanently deduct mat karo, pehle temporarily "reserve" karo (ek separate `reserved_quantity` column, ya time-boxed reservation record) jab tak payment confirm na ho jaaye — agar payment fail/timeout ho jaaye, reservation automatically release ho jaaye. **Concurrent order placement race condition**: agar do users simultaneously last available item order karne ki koshish karein, optimistic locking (`@Version` on inventory row) ya pessimistic row-lock (`SELECT FOR UPDATE`) use karo taaki dono orders simultaneously "success" na dekh sakein — ek ko explicitly conflict/out-of-stock response milna chahiye.

**133. Inventory system design karo jaha concurrent stock deduction ho — optimistic locking ya distributed lock (Redis) mein kya choose karoge aur kyun?**
Agar conflict rare/moderate hai (normal traffic pattern), **optimistic locking** (`@Version` field, retry-on-conflict) achha hai — koi actual lock hold nahi hota, throughput high rehta hai. Agar bahut zyada high contention hai ek hi popular item pe (jaise flash-sale scenario jaha hazaaron users same second mein same item order kar rahe hain), optimistic locking mein bahut saari failed-retries honge (wasteful) — us case mein **distributed lock (Redis `SETNX`/Redlock)** ya database-level pessimistic lock (`SELECT FOR UPDATE`) better hai kyunki requests naturally ek queue ki tarah serialize ho jaati hain bajaye baar-baar conflict-and-retry karne ke.

**134. Hotel booking system design karo — double booking prevent karne ke liye kya approach loge (locking vs database constraint)?**
Sabse reliable approach hai **database-level unique constraint** — jaise `UNIQUE(room_id, booking_date)` composite constraint (ya date-range overlap check constraint agar multi-day bookings hain). Agar do concurrent requests same room ko same date ke liye book karne ki koshish karein, dono INSERT try karenge, lekin DB khud automatically dusri insert ko constraint-violation error se reject kar dega — ye race condition ke against ek absolute guarantee deta hai jo sirf application-level locking (jo bugs/missed-edge-cases se bypass ho sakta hai) se nahi milti. Application-level locking/checks extra defense-in-depth ke liye add kar sakte ho, lekin DB constraint hi ultimate safety-net honi chahiye.

**135. Ride booking system (Uber-jaisa) ka high-level design karo — driver-rider matching aur real-time location update kaise scale karoge?**
**Driver-rider matching**: geospatial indexing use karo (geohashing, ya PostGIS/Redis GEO commands) taaki "nearby available drivers" ko efficiently query kiya ja sake bina saare drivers ko linearly scan kiye. **Real-time location update**: drivers periodically (jaise har 3-5 seconds) apni location publish karte hain lightweight persistent connection (WebSocket) ya pub-sub (Kafka/Redis pub-sub) ke through. Har second/frequently DB mein write karna scale nahi karega (bahut zyada write-throughput), isliye hot-path current-location data ko **in-memory store** (Redis GEO) mein rakhte hain jo fast reads/writes handle kar sake, aur periodically (kam frequency pe) durable DB mein snapshot/persist karte hain agar historical tracking chahiye.

**136. Chat application design karo — message delivery guarantee (at-least-once), read receipts, aur online-presence kaise design karoge?**
**Message delivery (at-least-once)**: message ek durable queue/broker (jaise Kafka) mein persist hota hai jab tak recipient client explicitly acknowledge na kare ki usne receive kar liya — agar ACK na mile (client offline/crash), message queued rehta hai aur reconnect pe redeliver hota hai. **Read receipts**: jab recipient message ko actually "open"/view kare, ek lightweight separate event (`message.read`) publish hota hai jo sender ke client ko update karta hai (blue-tick jaisa UI). **Online-presence**: WebSocket connection-based heartbeat mechanism — client periodically ping bhejta hai, ek presence service last-seen timestamp track karta hai; agar heartbeat ek threshold time tak na aaye (connection drop, jaise mobile network switch), user automatically "offline" mark ho jaata hai timeout-based logic se.

**137. ⭐⭐⭐⭐⭐ Centralized logging system design karo microservices ke liye — log aggregation, correlation ID propagation, aur storage/retention strategy kya hogi?**
Har microservice **structured (JSON) logs** generate kare jisme ek **correlation/trace ID** (Q100 dekho) included ho jo poore request-lifecycle mein saari services ke across propagate hota hai (headers ke through). Ek lightweight log-shipper agent (jaise Fluentd/Filebeat, har node pe running) logs ko collect karke ek centralized aggregation pipeline (Logstash/Fluentd) ke through ek searchable storage/analytics system (Elasticsearch/CloudWatch Logs) mein bhejta hai. **Retention strategy**: recent logs (jaise pichle 30 din) "hot" fast-searchable storage mein rakho (debugging ke liye), purane logs "cold"/archival storage (jaise S3 Glacier) mein move kar do — healthcare jaisa regulated domain mein compliance ke liye lambe samay tak retain karna pad sakta hai, lekin cost-optimize karke tiered storage se.

**138. Cache design karo high-traffic read API ke liye — cache-aside vs write-through vs write-behind mein kab kaunsa pattern use karoge, aur cache invalidation kaise handle karoge?**
**Cache-aside** (sabse common) — application khud pehle cache check karta hai, cache-miss hone pe DB se fetch karke phir cache populate karta hai — simple, aur sirf actually-requested data cache hoti hai. **Write-through** — har write cache aur DB dono mein synchronously hoti hai — consistency better hai (cache kabhi stale nahi hota) lekin writes thodi slower ho jaati hain. **Write-behind** — write pehle sirf cache mein hoti hai (fast response), DB mein baad mein async/batched likha jaata hai — writes bahut fast hain lekin agar cache crash ho jaaye commit se pehle, data-loss risk hai. Cache invalidation: TTL-based (simple, thodi staleness acceptable hai use-case mein) ya **event-driven invalidation** (jab underlying data actually change ho, explicitly us cache-key ko evict/update karo) — jaise humare healthcare project mein `consent.revoked` Kafka event pe related cached access-flags ko turant invalidate kiya jaata tha, taaki stale-permission ka risk na ho.

---

*Part 4 mein: Coding Questions, Project-Based Questions, aur Final Revision List (30 questions).*
