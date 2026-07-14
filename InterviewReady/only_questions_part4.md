# Senior Java Backend Developer — Interview Question Bank (Part 4 of 4)

**Coverage:** Section 13 (Coding), Section 14 (Project-Based), + Final Revision List
**Continuing numbering from Part 3** (Q1–138 already covered). Total question bank = 158 questions across 14 sections.
**Legend:** ⭐⭐⭐⭐⭐ = Top priority | 🔥 = Must-prepare

---

## Section 13 — Coding (Interview-Level)

**139. ⭐⭐⭐⭐⭐ LRU Cache implement karo bina `LinkedHashMap` use kiye — HashMap + Doubly Linked List se, O(1) get aur put ke saath.**
```java
class LRUCache {
    class Node { int key, value; Node prev, next; }
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(), tail = new Node();
    private final int capacity;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail; tail.prev = head;
    }
    public int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        remove(node); insertAtFront(node);
        return node.value;
    }
    public void put(int key, int value) {
        if (map.containsKey(key)) remove(map.get(key));
        else if (map.size() == capacity) remove(tail.prev); // evict least-recently-used
        Node node = new Node();
        node.key = key; node.value = value;
        insertAtFront(node); map.put(key, node);
    }
    private void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; map.remove(n.key); }
    private void insertAtFront(Node n) { n.next = head.next; n.next.prev = n; head.next = n; n.prev = head; }
}
```
HashMap O(1) lookup deta hai, doubly linked list O(1) mein "recently used ko front pe le jaana" aur "tail se evict karna" allow karta hai.

**140. HashMap ka simplified version khud implement karo — hashing function aur collision handling (chaining) ke saath.**
```java
class SimpleHashMap<K, V> {
    static class Entry<K, V> { K key; V value; Entry<K, V> next; }
    private Entry<K, V>[] buckets = new Entry[16];

    public void put(K key, V value) {
        int idx = Math.abs(key.hashCode()) % buckets.length;
        Entry<K, V> e = buckets[idx];
        while (e != null) {
            if (e.key.equals(key)) { e.value = value; return; }
            e = e.next;
        }
        Entry<K, V> newEntry = new Entry<>();
        newEntry.key = key; newEntry.value = value; newEntry.next = buckets[idx];
        buckets[idx] = newEntry; // collision chaining — head pe insert
    }
    public V get(K key) {
        Entry<K, V> e = buckets[Math.abs(key.hashCode()) % buckets.length];
        while (e != null) { if (e.key.equals(key)) return e.value; e = e.next; }
        return null;
    }
}
```

**141. Java Streams se ek line mein integers ko even/odd group karke count nikaalo (`Collectors.groupingBy` + `Collectors.counting`).**
```java
Map<Boolean, Long> result = numbers.stream()
    .collect(Collectors.groupingBy(n -> n % 2 == 0, Collectors.counting()));
// {false=<odd count>, true=<even count>}
```

**142. String mein sabse pehla non-repeating character dhoondo — Stream-based aur LinkedHashMap-based dono approach likho.**
```java
// Stream approach (readable, O(n^2) worst case due to indexOf/lastIndexOf)
Character result = str.chars().mapToObj(c -> (char) c)
    .filter(c -> str.indexOf(c) == str.lastIndexOf(c))
    .findFirst().orElse(null);

// LinkedHashMap approach — O(n), order preserved
Map<Character, Integer> freq = new LinkedHashMap<>();
for (char c : str.toCharArray()) freq.merge(c, 1, Integer::sum);
for (var e : freq.entrySet()) if (e.getValue() == 1) return e.getKey();
```

**143. ⭐⭐⭐⭐⭐🔥 Producer-Consumer problem code karo `BlockingQueue` use karke — multi-threaded, do threads ke saath.**
```java
BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
Runnable producer = () -> {
    for (int i = 0; i < 100; i++) {
        try { queue.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
};
Runnable consumer = () -> {
    while (true) {
        try { Integer val = queue.take(); process(val); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
    }
};
new Thread(producer).start();
new Thread(consumer).start();
```
`put()`/`take()` automatically block hote hain queue full/empty hone pe — manual `wait()`/`notify()` ki zarurat nahi.

**144. Array mein duplicate elements dhoondo bina extra space use kiye (in-place approach, sorting ya index-marking).**
```java
Arrays.sort(arr); // O(n log n) time, in-place sort — O(1) extra space
for (int i = 1; i < arr.length; i++)
    if (arr[i] == arr[i - 1]) System.out.println("duplicate: " + arr[i]);
```

**145. Custom thread-safe Singleton class likho double-checked locking ke saath — explain karo `volatile` yaha kyun zaroori hai.**
Code aur explanation Part 3 mein Q101 dekho — short recap: `volatile` isliye zaroori hai kyunki bina uske JVM instruction reordering kar sakta hai, jisse ek thread ko ek "half-constructed" object dikh sakta hai jiska reference already assign ho chuka hai lekin constructor poora execute nahi hua.

**146. Do sorted arrays ko O(m+n) time mein merge karo bina extra array use kiye (in-place merge logic).**
```java
void merge(int[] arr1, int m, int[] arr2, int n) { // arr1 mein end pe extra space hai
    int i = m - 1, j = n - 1, k = m + n - 1;
    while (j >= 0) {
        if (i >= 0 && arr1[i] > arr2[j]) arr1[k--] = arr1[i--];
        else arr1[k--] = arr2[j--];
    }
}
```
Peeche se fill karte ho taaki `arr1` ke abhi-tak-unprocessed elements overwrite na hon.

**147. Nested list (`List<List<Integer>>`) ko flatten karo `flatMap` use karke — actual Stream code likho.**
```java
List<Integer> flat = nestedList.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
```

**148. Ek simple thread-safe rate limiter class design/code karo (token bucket algorithm).**
```java
class TokenBucket {
    private final int capacity;
    private double tokens;
    private final double refillRatePerSecond;
    private long lastRefillTime = System.currentTimeMillis();

    public TokenBucket(int capacity, double refillRatePerSecond) {
        this.capacity = capacity; this.tokens = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }
    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) { tokens -= 1; return true; }
        return false;
    }
    private void refill() {
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - lastRefillTime) / 1000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
        lastRefillTime = now;
    }
}
```

---

## Section 14 — Project-Based Questions

> Ye questions tumhari khud ki experience se answer karne wale hain — neeche jo diya hai wo "answer *structure*" hai (kaise organize karo jawab), fake specific facts nahi. Genuine detail apni taraf se bharo; jo genuinely nahi hua wo mat bolo.

**149. ⭐⭐⭐⭐⭐🔥 Apne project ka architecture explain karo — services, communication (sync/async), aur data flow verbally describe karo bina diagram dekhe.**
Structure: (1) 1-line elevator pitch — project kis problem ko solve karta hai. (2) Core services list karo aur har ek ki ek-line responsibility. (3) Communication pattern — kaunsa sync (REST) hai, kaunsa async (Kafka/queue) hai aur kyun. (4) Ek concrete end-to-end example do (jaise "user X action karta hai → service Y → service Z → response") — abstract explanation se real example zyada convincing lagta hai interviewer ko.

**150. ⭐⭐⭐⭐⭐ Microservices kyun choose kiye monolith ke bajaye tumhare project mein — concrete reasoning do (team structure, domain boundary, independent deployment).**
Generic "scalability ke liye" jaisa textbook answer avoid karo — specific reasoning do jo tumhare actual project se related ho: team structure (alag teams alag domains own karte the?), domain complexity (kuch specific bounded contexts clear the?), ya deployment independence (ek team ko doosri ka wait kiye bina deploy karna padta tha?). Agar monolith bhi kaam kar sakta tha aur microservices "already decided" thi jab tum join hue, honestly bolo ki "ye decision meri joining se pehle ki thi, lekin maine dekha ki X benefit mil raha tha practically."

**151. Sabse bada production issue jo tumne face/debug kiya — identify se resolve tak ka process kya tha?**
Structure (STAR-jaisa): Symptom (kya observe hua) → Investigation (kaunse logs/metrics/tools use kiye) → Root cause (asli wajah kya nikli) → Fix (kya specifically badla) → Validation (kaise confirm kiya ki fix kaam kar gaya) → Lesson (aage kya prevent karne ke liye process/monitoring add kiya). Apna genuine issue use karo chahe chhota lage — depth aur honesty zyada matter karti hai size se.

**152. ⭐⭐⭐⭐⭐ Ek performance optimization story batao — before/after metrics ke saath, exactly kya identify aur fix kiya?**
Same STAR structure, specifically before/after number ke saath. Agar exact number yaad nahi hai, approximate range bolna theek hai ("kaafi improvement tha, exact number yaad nahi lekin noticeable tha") — bina invented precise number ke jo follow-up mein defend na kar sako.

**153. Memory leak production mein kaise investigate karoge — heap dump analysis ka process kya hai (VisualVM/Eclipse MAT jaise tools)?**
Process: production se heap dump lo (`jmap -dump:live,format=b,file=heap.hprof <pid>`), VisualVM ya Eclipse MAT mein load karo, **"dominator tree"** dekho — kaunsa object sabse zyada retained memory occupy kar raha hai. Behtar hai do dumps lo (kuch time gap ke saath) aur compare karo ki kaunse objects **grow** ho rahe hain time ke saath. Common culprits: static collections jo continuously grow karte hain bina cleanup ke, unclosed resources (connections/streams), event-listener leaks (registered but kabhi unregister nahi hue), `ThreadLocal` variables jo cleanup nahi hui thread-pool reuse ke context mein.

**154. Thread dump analysis kaise karte ho — deadlock ya thread contention kaise identify karoge usse?**
`jstack <pid>` se thread dump lo (production mein bina restart kiye). **BLOCKED** state wale threads dhoondo — unke "waiting to lock <address>" info se pata chalta hai kaunsa lock chahiye, aur "locked <address>" se pata chalta hai kaun hold kar raha hai. Agar genuine deadlock hai, JVM khud explicitly "Found one Java-level deadlock" section print kar deta hai dump mein saare involved threads/locks ke saath — manually trace karne ki zarurat nahi padti deadlock ke case mein.

**155. GC tuning kab zaroori padi — kaunsa GC algorithm use kiya aur kaunse parameters tune kiye?**
Process: pehle GC logs enable karo (`-Xlog:gc*:file=gc.log`), pause-time aur frequency pattern analyze karo. Agar frequent full-GCs ho rahe hain, heap size ya young/old generation ratio tune karna pad sakta hai. Agar latency-sensitive application hai (jaise real-time APIs), G1GC (default balanced) se ZGC (ultra-low-pause) consider karo bade heaps ke liye. Application-level bhi dekho — unnecessary object churn (jaise loop ke andar String concatenation `+` use karna instead of StringBuilder) GC pressure badhata hai independent of GC tuning.

**156. Database bottleneck kaise identify aur resolve kiya — slow query se lekar fix tak ka process batao.**
Process: slow-query log ya APM tool (jaise New Relic/Datadog) se slow endpoints identify karo, us endpoint ki actual query nikaalo aur `EXPLAIN ANALYZE` chalao (Q126/Q128 dekho) root cause dhoondhne ke liye (missing index, N+1 query, bad join order). Connection pool metrics bhi check karo — kabhi symptom "slow query" nahi balki "connection pool exhaustion" hota hai (queries khud fast hain lekin connection milne ka wait ho raha hai).

**157. Tumhare system ki scaling strategy kya thi — horizontal vs vertical scaling, aur stateless service design ka role kya tha isme?**
Horizontal scaling (zyada instances/pods add karna) stateless services ke liye straightforward hai — condition ye hai ki koi bhi session/state application server ke andar store na ho (sab kuch DB/Redis/S3 mein bahar), taaki load balancer kisi bhi instance ko koi bhi request bhej sake bina "sticky session" ke. Vertical scaling (bade/powerful instance) short-term simpler hai lekin ek hardware limit hai, aur single-point-of-failure risk horizontal se zyada hai (agar wo ek bada instance down ho jaaye, poora capacity chala jaata hai).

**158. CI/CD pipeline aur deployment strategy (blue-green/canary/rolling) apne project ke context mein explain karo.**
**Blue-green**: do identical production environments maintain karo, naya version "green" environment mein deploy karo, verify karo, phir traffic ko instantly "blue" se "green" pe switch kar do — rollback bhi instant hai (wapas blue pe switch). **Canary**: naye version ko chhoti percentage traffic pe roll out karo pehle, metrics monitor karo, gradually percentage badhao agar sab theek hai — risk exposure gradual hota hai. **Rolling** (Kubernetes ka default): ek-ek pod karke purane version ko naye se replace karo, readiness probes ensure karte hain ki naya pod healthy hai traffic milne se pehle. Apne project mein jo genuinely use hua wahi bolo — zyadatar Kubernetes-based setups mein rolling deployment default hota hai jab tak explicitly blue-green/canary setup na kiya gaya ho.

---

## Final Revision List — 30 Questions Jo Interview Mein Sabse Zyada Aane Ki Sambhavna Hai

*Ye list poore 158-question bank se curated hai — agar time kam hai toh sirf ye 30 zaroor revise karo. Full explanation ke liye original question number pe jao (bracket mein diya hai, jis file mein hai wahi likha hai).*

1. **(Part1, Q1)** String immutable kyun hai — security, string pool, thread-safety teeno angle se.
2. **(Part1, Q4)** `equals()`/`hashCode()` contract — sirf `equals()` override karne se HashMap mein kya toota.
3. **(Part1, Q7)** HashMap internally kaise kaam karta hai — hashing, collision, treeification.
4. **(Part1, Q9)** ConcurrentHashMap Java 7 vs Java 8 mein thread-safety kaise achieve karta hai.
5. **(Part1, Q21)** Thread lifecycle ke saare states aur transitions.
6. **(Part1, Q26)** `synchronized` vs `ReentrantLock` — kab kaunsa.
7. **(Part1, Q31)** ThreadPoolExecutor ke parameters production mein kaise tune karoge.
8. **(Part1, Q33)** Deadlock vs livelock vs starvation — thread dump se deadlock kaise detect karoge.
9. **(Part1, Q39)** Streams lazy kyun hain — intermediate vs terminal operations.
10. **(Part2, Q55)** IOC/DI ka real benefit — testability aur loose coupling se.
11. **(Part2, Q62)** AOP proxy-based implementation — JDK dynamic proxy vs CGLIB.
12. **(Part2, Q65)** Spring Boot auto-configuration internally kaise kaam karta hai.
13. **(Part2, Q75)** Entity lifecycle states — Transient/Persistent/Detached/Removed.
14. **(Part2, Q78)** N+1 query problem — example aur `JOIN FETCH`/`@EntityGraph` se fix.
15. **(Part2, Q80)** Optimistic vs pessimistic locking — kab kaunsa.
16. **(Part2, Q90)** Circuit breaker states (CLOSED/OPEN/HALF_OPEN) real scenario ke saath.
17. **(Part2, Q94)** Distributed transactions kyun problematic — Saga pattern (choreography vs orchestration).
18. **(Part2, Q98)** REST API idempotency kaise implement karoge (jaise payment API).
19. **(Part3, Q105)** Spring AOP aur `@Transactional` proxy pattern se kaise related hain.
20. **(Part3, Q117)** Idempotency key pattern non-idempotent operations ke liye.
21. **(Part3, Q118)** JWT token structure aur refresh strategy.
22. **(Part3, Q124)** Transaction isolation levels — dirty read/non-repeatable read/phantom read.
23. **(Part3, Q129)** URL Shortener design — unique code generation aur scalability.
24. **(Part3, Q132)** Order Management system — concurrent order placement race condition kaise handle.
25. **(Part3, Q137)** Centralized logging system design — correlation ID propagation.
26. **(Part2, Q88)** Service discovery — client-side vs server-side.
27. **(Part4, Q143)** Producer-Consumer `BlockingQueue` se code karke dikhao.
28. **(Part4, Q149)** Apna project architecture verbally explain karo.
29. **(Part4, Q150)** Microservices kyun choose kiye monolith ke bajaye.
30. **(Part4, Q152)** Performance optimization story — before/after metrics ke saath.

---

## Quick Stats

- **Total questions across all 4 files:** 158 (ab saath mein Hinglish explanations bhi)
- **Sections covered:** 14 (Core Java → Project-Based)
- **⭐⭐⭐⭐⭐ high-priority questions:** 64
- **🔥 must-prepare questions:** 24
- **Files:** `only_questions_part1.md` (Core Java, Concurrency, Java 8, Java 17) → `only_questions_part2.md` (Spring, Spring Boot, JPA/Hibernate, Microservices) → `only_questions_part3.md` (Design Patterns, REST, SQL, System Design) → `only_questions_part4.md` (Coding, Project-Based, Revision List — is file)
- **Note:** Section 14 ke answers "structure guides" hain, tumhari khud ki genuine experience se bharne ke liye — invented specifics interview follow-up mein pakdi jaati hain.
