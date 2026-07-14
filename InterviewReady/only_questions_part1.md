# Senior Java Backend Developer — Interview Question Bank (Part 1 of 4)

**Coverage:** Section 1 (Core Java), Section 2 (Multithreading & Concurrency), Section 3 (Java 8), Section 4 (Java 17)
**Level:** 6+ years experience — product-based companies (Amazon, Microsoft, Oracle, Walmart, JP Morgan, Rakuten, Goldman Sachs, EPAM, Publicis Sapient, Deloitte)
**Legend:** ⭐⭐⭐⭐⭐ = Top priority | 🔥 = Must-prepare
**Note:** Har question ke saath ab explanation bhi hai — pehle khud answer sochne ki koshish karo, phir explanation se match karo.

---

## Section 1 — Core Java (Deep / Advanced)

**1. ⭐⭐⭐⭐⭐🔥 String immutable kyun banayi gayi hai? Security, string pool caching, aur multithreading — teeno angles se explain karo.**
Security ke liye — DB URL, file path, network connection jaise sensitive values aksar String argument ke roop mein pass hote hain; agar mutable hoti toh koi bhi reference se beech mein value badal sakta tha. String pool caching isliye kaam karti hai kyunki agar String immutable hai toh same literal ko safely multiple jagah reuse kiya ja sakta hai bina ye dar ke ki koi ek jagah change karke doosri jagah bhi asar daal dega. Thread-safety — immutable object naturally thread-safe hota hai kyunki uski state kabhi badalti hi nahi, isliye multiple threads bina synchronization ke bhi safely share kar sakte hain.

**2. ⭐⭐⭐⭐⭐ `new String("abc")` aur String literal `"abc"` mein memory allocation ka farak kya hai? `intern()` method kya karta hai?**
`"abc"` literal String Constant Pool mein check karta hai — agar already exist karta hai toh wahi reference reuse hota hai, warna naya banake pool mein daal diya jaata hai. `new String("abc")` hamesha heap mein ek naya object banata hai, chahe pool mein already same value ho. `intern()` call karne pe object ko pool mein daal diya jaata hai (ya agar already hai toh pool wala reference return hota hai) — isse `new String("abc").intern() == "abc"` true ho jaata hai.

**3. StringBuilder vs StringBuffer — internal implementation aur performance difference kya hai high-concurrency scenario mein?**
StringBuffer ke methods `synchronized` hain (thread-safe lekin lock overhead ki wajah se slower), StringBuilder non-synchronized hai (fast, lekin thread-safe nahi). Practically, string building zyadatar ek single method ke local scope mein hoti hai (single-thread), isliye StringBuilder almost hamesha better choice hai — StringBuffer sirf tab jab genuinely ek StringBuffer object multiple threads ke beech directly share ho raha ho.

**4. ⭐⭐⭐⭐⭐🔥 `equals()` aur `hashCode()` ka contract kya hai? Agar sirf `equals()` override karke `hashCode()` na karo, toh HashMap/HashSet mein exactly kya break hoga?**
Contract: agar do objects `equals()` se equal hain, toh unka `hashCode()` bhi same hona chahiye (reverse zaroori nahi — different objects same hashCode share kar sakte hain, collision). Agar sirf `equals()` override karo, `hashCode()` Object class ka default (memory-address-based) reh jaata hai — do "logically equal" objects alag-alag hashCode denge, isliye HashMap unhe alag buckets mein daal dega. Result: `map.get(logicallyEqualKey)` fail ho jaayega kyunki galat bucket search hoga, aur duplicate "equal" entries HashSet mein add ho jayengi jo theoretically nahi honi chahiye.

**5. ⭐⭐⭐⭐⭐ `==` vs `equals()` — Integer caching (`-128 to 127`) ke context mein deep dive karo. Ye behavior kyun hai?**
`==` reference compare karta hai (objects ke liye), `equals()` value compare karta hai. Java `Integer` ke liye `-128` se `127` tak ke values ko internally cache (`IntegerCache`) karta hai autoboxing ke time — isliye `Integer a=100, b=100;` mein `a==b` true hai (same cached object). Lekin `Integer a=200, b=200;` mein `a==b` false hai kyunki ye range se bahar hai, dono alag naye objects bante hain. Ye optimization hai kyunki chhoti values bahut common hain (loop counters, array indices) — cache reuse se memory/GC pressure kam hoti hai.

**6. Ek immutable class khud se design karo — kaunse steps zaroori hain (final fields, no setters, defensive copy)?**
Class ko `final` banao taaki koi subclass overriding se behavior na badal sake, saare fields `private final` rakho, koi setter method mat likho, sab values constructor mein hi set karo, aur agar koi field mutable object hai (jaise `List` ya `Date`) toh constructor mein aur getter dono jagah defensive copy return karo — direct reference return karoge toh caller usse bahar se modify kar sakta hai aslimutability tootegi.

**7. ⭐⭐⭐⭐⭐🔥 HashMap internally kaise kaam karta hai — hashing, bucket index calculation, collision handling, aur Java 8 mein treeification (red-black tree) kab trigger hoti hai?**
Key ka `hashCode()` liya jaata hai, phir ek supplemental hash spreading function apply hoti hai (high bits ko low bits ke saath XOR karna, taaki distribution better ho), phir `(capacity-1) & hash` se bucket index milta hai. Collision (same bucket) hone pe entries linked list mein chain hoti hain. Java 8+ mein agar ek bucket mein 8 ya usse zyada nodes ho jayein AND table capacity kam se kam 64 ho, toh us bucket ki linked list red-black tree mein convert ho jaati hai — isse worst-case lookup O(n) se O(log n) ho jaata hai.

**8. ⭐⭐⭐⭐⭐ HashMap resize/rehashing kaise hoti hai? Load factor aur initial capacity ka role kya hai performance mein?**
Default initial capacity 16, default load factor 0.75 — matlab jab entries ki sankhya `capacity * loadFactor` (yaani 12) cross kar jaaye, capacity double ho jaati hai (32) aur saari existing entries naye buckets mein rehash hoti hain (expensive — poora map traverse hota hai). Agar tumhe pehle se pata hai ki map mein roughly kitni entries jayengi, initial capacity usi hisaab se set karna (`new HashMap<>(expectedSize)`) baar-baar resize hone se bachata hai.

**9. ⭐⭐⭐⭐⭐🔥 ConcurrentHashMap thread-safety kaise achieve karta hai — Java 7 (segment locking) vs Java 8 (CAS + synchronized on bucket) mein kya badla?**
Java 7 mein map ko fixed number of "segments" (default 16) mein divide kiya jaata tha, har segment ka apna lock hota tha — matlab max 16 threads truly parallel write kar sakte the (alag segments pe). Java 8 mein segments hata diye gaye; ab granularity per-bucket hai — insert ke time agar bucket empty hai toh CAS (lock-free) operation se insert hota hai, aur sirf tab jab genuinely collision ho (same bucket mein already node hai) tab us bucket ke first node pe `synchronized` block lagta hai. Ye bahut zyada fine-grained hai, high concurrency mein Java 7 se kaafi better perform karta hai.

**10. LinkedHashMap ka use case kya hai — kaise ye insertion/access order maintain karta hai (LRU cache ke liye kaise use hota hai)?**
LinkedHashMap internally ek HashMap hai plus ek doubly linked list jo entries ko order mein connect karta hai — default insertion order, ya constructor mein `accessOrder=true` pass karo toh access order (jo entry hal hi mein access hui wo end mein chali jaati hai). LRU cache banane ke liye: `accessOrder=true` set karo, aur `removeEldestEntry(Map.Entry eldest)` method override karo jo `true` return kare jab `size() > maxCapacity` — HashMap automatically eldest (least-recently-used) entry remove kar dega.

**11. TreeMap kis data structure pe based hai aur uska time complexity kya hai insert/search ke liye?**
Red-Black Tree (self-balancing binary search tree) pe based hai — insert, search, aur delete sab guaranteed O(log n) hain. Custom ordering ke liye constructor mein `Comparator` pass kar sakte ho, warna default `Comparable` (natural ordering) use hoti hai.

**12. ⭐⭐⭐⭐⭐ ArrayList vs LinkedList — internal array vs doubly-linked-list implementation, aur insertion/deletion/random-access ke real trade-offs kya hain?**
ArrayList resizable array hai — random access O(1) (index se direct), lekin beech mein insert/delete O(n) (baaki elements shift karne padte hain). LinkedList doubly-linked nodes hai — theoretically beech mein insert/delete O(1) hai *agar* tumhare paas already reference hai us position ka, lekin random access O(n) (traverse karna padta hai). Practically, LinkedList production mein bahut kam use hota hai kyunki har node ka memory overhead (do extra pointers) aur poor cache locality (nodes memory mein scattered hote hain) ki wajah se real-world performance ArrayList se bhi kharab nikalti hai zyadatar cases mein.

**13. Comparable vs Comparator — multiple fields pe sorting (jaise pehle salary, phir naam) kaise implement karoge `Comparator.comparing().thenComparing()` se?**
`Comparable` class ke andar ek hi natural ordering define karta hai (`compareTo()` method), class khud implement karti hai. `Comparator` bahar se pass hota hai, aur ek class ke multiple alag orderings ban sakte hain. Multi-field sort: `list.sort(Comparator.comparing(Employee::getSalary).thenComparing(Employee::getName))` — pehle salary se compare, agar salary equal ho toh naam se.

**14. Checked vs unchecked exceptions — API design karte waqt kab custom checked exception banaoge vs runtime exception?**
Checked exception (extends `Exception`) tab banao jab condition genuinely recoverable ho aur caller ko compile-time pe force karna zaroori ho ki wo explicitly handle kare (jaise `InsufficientFundsException` jaha caller ko definitely react karna chahiye). Unchecked (extends `RuntimeException`) tab jab ye programming/validation error ho jise globally ek jagah handle karna better hai (jaise `@ControllerAdvice` se) — checked exceptions REST API layers mein bahut clutter create karte hain isliye modern Spring Boot apps mostly unchecked exceptions use karti hain.

**15. ⭐⭐⭐⭐⭐ Generics mein type erasure kya hai? Isse kaunsi runtime limitations aati hain (jaise `new T()` na kar paana)?**
Compile time pe generic types check hoti hain, lekin compiler bytecode generate karte waqt saari generic type information "erase" kar deta hai — runtime pe `List<String>` aur `List<Integer>` dono sirf `List` hote hain (backward compatibility ke liye pre-generics code ke saath). Isliye runtime pe `new T()` nahi kar sakte (T ka actual class pata nahi hota), `instanceof List<String>` check nahi kar sakte, aur generic array (`new T[]`) directly nahi bana sakte.

**16. Reflection ka real production use case kya hai (jaise Spring dependency injection ke andar)? Iske performance aur security trade-offs kya hain?**
Spring reflection use karta hai constructors/fields ko runtime pe inspect karke beans automatically wire karne ke liye, Jackson JSON serialization bhi reflection se fields access karta hai. Trade-offs: performance overhead (reflective calls JIT ke liye utna optimize-friendly nahi hote jitna direct calls), aur security risk (private fields/methods bhi access ho sakte hain jo encapsulation todta hai) — isliye application code mein directly reflection likhna avoid karte hain, framework ke through hi indirectly use hota hai.

**17. Shallow copy vs deep copy — `Cloneable` interface ke problems kya hain, aur production code mein cloning se kaise bachte ho?**
Shallow copy sirf top-level primitive/reference fields copy karta hai — agar field khud koi object hai (jaise `List`), dono copies same nested object ko point karte hain (ek badlega toh dusre mein bhi dikhega). Deep copy nested objects ko bhi independently copy karta hai. `Cloneable` interface problematic hai kyunki iske andar koi method hi nahi hai (sirf marker), actual `clone()` method `Object` class mein `protected` hai jo confusing design hai, aur checked `CloneNotSupportedException` handle karna padta hai. Production mein iski jagah copy constructor ya static factory method (`Employee.copyOf(emp)`) use karna cleaner hai.

**18. ⭐⭐⭐⭐⭐ Optional class ka correct use kya hai? `Optional.get()` directly call karna anti-pattern kyun hai?**
Optional ka purpose hai API contract explicitly batana ki "return value absent ho sakti hai" — caller ko force karta hai handle karne ke liye. `.get()` directly call karna anti-pattern hai kyunki agar value absent hai toh `NoSuchElementException` throw hoga — bilkul waisa hi jaise NPE avoid karne ke liye Optional use kiya aur phir bhi crash. Better: `orElse(default)`, `orElseGet(supplier)`, `orElseThrow(customException)`, ya `map()`/`ifPresent()` chain use karo. Optional ko field type ya method parameter ke roop mein use karna bhi discouraged hai — sirf return type ke liye recommended hai.

**19. Enum internally ek class hi hai — enum se Singleton pattern kaise implement karte ho, aur ye approach traditional singleton se zyada safe kyun hai?**
`public enum Singleton { INSTANCE; }` — JVM guarantee karta hai ki enum ki instances sirf ek hi baar banti hain, aur ye guarantee reflection attacks aur serialization/deserialization attacks se bhi safe hai (jo traditional double-checked-locking singleton ko break kar sakte hain). Isiliye Joshua Bloch (Effective Java) enum singleton ko sabse safe approach maante hain.

**20. Custom annotation kaise banate ho? `@Retention` aur `@Target` ka role kya hai, aur Spring mein custom annotation ka ek real use case do.**
`@interface MyAnnotation { String value(); }` se define karte ho. `@Retention(RetentionPolicy.RUNTIME)` batata hai ki annotation compiled class file mein rahega aur reflection se runtime pe read kiya ja sakega (vs `SOURCE`/`CLASS` jo compile-time tak hi rehte hain). `@Target` batata hai kaha apply ho sakta hai (`METHOD`, `FIELD`, `TYPE`, etc). Real example: custom `@RateLimited` annotation banao aur ek AOP `@Aspect` likho jo is annotation wale methods ko intercept karke rate-limiting logic apply kare — bina har method mein manually rate-limit code duplicate kiye.

---

## Section 2 — Java Multithreading & Concurrency

**21. ⭐⭐⭐⭐⭐🔥 Thread lifecycle ke saare states explain karo (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED) aur transitions kab hoti hain?**
NEW — thread object bana lekin `start()` nahi bulaya. RUNNABLE — `start()` ke baad, ya toh actually chal raha hai ya CPU scheduler ka wait kar raha hai. BLOCKED — kisi `synchronized` block/method mein enter karne ke liye monitor lock ka wait kar raha hai. WAITING — `wait()`, `join()` (bina timeout) call kiya, indefinitely wait karega jab tak koi doosra thread notify/interrupt na kare. TIMED_WAITING — `sleep(ms)`, `wait(timeout)`, `join(timeout)` — fixed time ke liye wait. TERMINATED — `run()` method complete ho gaya ya exception se bahar nikal gaya.

**22. Runnable vs Callable — Callable ko `ExecutorService` ke saath kaise use karte ho, aur `Future.get()` blocking kyun hota hai?**
`Runnable.run()` kuch return nahi karta aur checked exception throw nahi kar sakta. `Callable<T>.call()` ek value return karta hai aur checked exception throw kar sakta hai. `executorService.submit(callable)` turant ek `Future<T>` return karta hai (task background mein chal raha hota hai), aur `future.get()` call karne pe calling thread block ho jaata hai jab tak task complete na ho — kyunki result abhi available nahi hai, JVM ke paas kaam aage badhane ka koi aur tarika nahi except wait karna.

**23. ⭐⭐⭐⭐⭐ CompletableFuture kya problem solve karta hai jo plain Future nahi kar sakta? `thenApply`, `thenCompose`, aur `thenCombine` mein farak batao.**
Plain Future mein result ke liye manually block/poll karna padta hai, aur callbacks chain nahi kar sakte. CompletableFuture non-blocking, composable async programming allow karta hai. `thenApply(fn)` — result ko transform karta hai (jaise `map`, sync). `thenCompose(fn)` — jab callback khud ek naya `CompletableFuture` return kare (jaise `flatMap`, nested futures ko flatten karta hai). `thenCombine(other, fn)` — do independent CompletableFutures ka result combine karta hai jab dono complete ho jaayein.

**24. ⭐⭐⭐⭐⭐ `volatile` keyword exactly kya guarantee karta hai — visibility ya atomicity? `volatile` aur `synchronized` mein kya farak hai?**
`volatile` sirf **visibility** guarantee karta hai — ek thread ka write turant CPU cache se main memory mein flush hota hai aur doosre threads ko turant dikhta hai. Ye **atomicity** guarantee nahi karta — `count++` jaisa compound operation volatile hone pe bhi thread-safe nahi hai kyunki ye read-modify-write teen alag steps hain jinke beech interleaving ho sakti hai. `synchronized` dono visibility aur atomicity (mutual exclusion) guarantee karta hai, lekin blocking hai aur zyada overhead laata hai.

**25. Atomic classes (`AtomicInteger`, `AtomicReference`) internally kaise kaam karte hain — CAS (Compare-And-Swap) operation explain karo.**
CAS ek hardware-level atomic instruction hai jo teen values leta hai: memory location, expected current value, aur naya value. Agar location ki actual value expected value ke barabar hai, tabhi naye value se update karta hai (atomically), warna kuch nahi karta aur caller ko batata hai fail hua — jisse caller retry kar sakta hai (spin loop). Isse lock-free thread-safe operations possible hote hain bina `synchronized` ke blocking overhead ke.

**26. ⭐⭐⭐⭐⭐🔥 `synchronized` keyword vs `ReentrantLock` — kab `ReentrantLock` use karoge jab `synchronized` already available hai?**
`ReentrantLock` extra flexibility deta hai jo `synchronized` nahi de sakta: `tryLock()` (non-blocking attempt, agar lock available nahi toh turant false return kar do), timeout ke saath lock lena (`tryLock(time, unit)`), fairness policy (FIFO order mein threads ko lock milna, starvation avoid karna), aur `lockInterruptibly()` (waiting thread ko interrupt kiya ja sakta hai). `synchronized` simpler hai aur JVM automatically release kar deta hai even exception aane pe — `ReentrantLock` mein manually `finally` block mein `unlock()` call karna zaroori hai, warna lock leak ho jaayega.

**27. ReadWriteLock kis scenario mein use karoge jaha `ReentrantLock` kaafi nahi hai?**
Jab reads bahut zyada hon aur writes rare hon (jaise ek in-memory cache), `ReadWriteLock` multiple readers ko simultaneously parallel read karne deta hai (read lock shared hota hai — koi ek-doosre ko block nahi karta), lekin writer ko exclusive access chahiye (write lock exclusive hai, jab tak koi writer lock le, koi aur reader/writer nahi chal sakta). Normal `ReentrantLock` mein readers bhi ek-doosre ko block karte, jo unnecessary contention aur throughput loss create karta hai.

**28. CountDownLatch vs CyclicBarrier — dono ek jaisa lagte hain, real difference kya hai use-case ke terms mein?**
`CountDownLatch` **ek-baar-use** hota hai (reset nahi ho sakta) — N events complete hone ka wait karta hai, jaise main thread N background services ke start hone ka wait kare `latch.await()` se, aur har service `latch.countDown()` call kare start hone pe. `CyclicBarrier` **reusable** hai — N threads ek common synchronization point pe pahunchne ka wait karte hain aur saath mein aage badhte hain (jaise multi-phase computation jaha sab threads phase-1 complete karke phase-2 mein saath enter karein), barrier automatically reset ho jaata hai next round ke liye.

**29. Semaphore ka real-world use case do (jaise limited DB connections ko control karna).**
`Semaphore` fixed number of "permits" maintain karta hai. Agar tumhare paas sirf 5 concurrent DB connections allowed hain, `new Semaphore(5)` banao — har thread DB call karne se pehle `acquire()` karega (permit le kar), agar sab 5 permits already li ja chuki hain toh naya thread wait karega, aur kaam ke baad `release()` karega taaki agla thread aage badh sake. Isse ek limited resource pe concurrent access control hota hai.

**30. ⭐⭐⭐⭐⭐ ExecutorService ke different thread pool types (`FixedThreadPool`, `CachedThreadPool`, `ScheduledThreadPool`) kab use karoge?**
`FixedThreadPool(n)` — fixed N threads hamesha maintain, extra tasks queue mein wait karte hain — predictable, steady load ke liye achha. `CachedThreadPool` — threads dynamically create/reuse hote hain, idle threads 60 sec baad terminate ho jaate hain — short-lived, bursty, unpredictable load ke liye, lekin risk hai ki bahut zyada threads ban sakte hain unbounded scenario mein (resource exhaustion). `ScheduledThreadPool` — periodic ya delayed tasks (jaise cron-job jaisa recurring health-check) ke liye.

**31. ⭐⭐⭐⭐⭐🔥 ThreadPoolExecutor ke core parameters (`corePoolSize`, `maxPoolSize`, `queueCapacity`, `RejectedExecutionHandler`) production mein kaise tune karoge high-throughput API ke liye?**
`corePoolSize` — normally maintain hone wale threads (idle rehne par bhi zinda). `maxPoolSize` — burst capacity, jab queue bhi full ho jaaye tab tak naye threads is limit tak banaye jaate hain. `queueCapacity` — kitne pending tasks wait kar sakte hain jab core threads busy hain. Tuning: CPU-bound tasks ke liye pool size ≈ number of CPU cores (zyada threads context-switching overhead badhayenge), IO-bound tasks ke liye zyada threads (kyunki threads mostly wait kar rahe hote hain). **Bounded queue use karo** (unbounded se `OutOfMemoryError` risk hai agar tasks producer se fast queue ho jaayein). `RejectedExecutionHandler` (jaise `CallerRunsPolicy`) zaroor define karo jab pool aur queue dono full ho jaayein, taaki system crash hone ke bajaye gracefully degrade kare.

**32. ForkJoinPool kya hai aur work-stealing algorithm kaise kaam karta hai? Parallel streams internally isi ka use karti hain — explain karo.**
`ForkJoinPool` divide-and-conquer style tasks ke liye design hua hai — bada task recursively chhote independent subtasks mein split (`fork`) hota hai aur results combine (`join`) hote hain. Work-stealing: har worker thread ka apna deque (double-ended queue) hota hai tasks ka; normal cases mein thread apne deque ke ek end se kaam leta hai, lekin jab uska kaam khatam ho jaata hai, wo kisi doosre busy thread ke deque ke doosre end se task "steal" kar leta hai — isse load naturally balance ho jaata hai bina ek central shared queue ke contention ke. Parallel streams internally default common `ForkJoinPool` (size = CPU cores) use karti hain.

**33. ⭐⭐⭐⭐⭐ Deadlock, livelock, aur starvation mein farak kya hai? Deadlock ko production mein kaise detect/prevent karoge (thread dump analysis)?**
**Deadlock** — do ya zyada threads ek-dusre ke paas ka resource circularly wait kar rahe hain, koi bhi aage nahi badh sakta (permanently frozen). **Livelock** — threads active/running hain, state change kar rahe hain, lekin actual progress nahi ho raha (jaise dono ek-dusre ko politely raasta dene ki koshish mein baar-baar move karte rehte hain). **Starvation** — ek thread ko resource kabhi milta hi nahi kyunki doosre (higher priority ya zyada aggressive) threads hamesha aage nikal jaate hain. Detection: production mein `jstack <pid>` se thread dump lo — JVM khud "Found one Java-level deadlock" jaisa message dega exact thread names aur locks ke saath. Prevention: hamesha consistent lock-ordering follow karo (saare threads locks ko same order mein acquire karein), aur jaha ho sake `tryLock(timeout)` use karo indefinite block hone se bachne ke liye.

**34. Race condition ka ek real code example do jaha `i++` thread-safe nahi hai, aur fix kaise karoge.**
```java
int count = 0;
// do threads simultaneously ye call karein:
count++;  // read count, add 1, write back — 3 separate steps
```
Agar do threads exact same time pe `count++` karein, dono "purani" value read kar sakte hain (jaise dono 5 read karte hain), dono apne apne 6 calculate karke write karte hain — final value 6 hoti hai jabki 7 honi chahiye thi (ek update "lost" ho gaya). Fix: `synchronized` block use karo, ya `AtomicInteger.incrementAndGet()`, ya `ReentrantLock`.

**35. Producer-Consumer problem `BlockingQueue` se kaise solve karte ho? `wait()`/`notify()` se implement karne se kya farak hai?**
```java
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
// Producer:
queue.put(task); // agar queue full hai, automatically block ho jaata hai
// Consumer:
Task t = queue.take(); // agar queue empty hai, automatically block ho jaata hai
```
`BlockingQueue` internally `wait()`/`notify()` (ya locks/conditions) khud handle karta hai — manually implement karne mein spurious wakeups (thread bina actual notify ke wake ho jaana) aur missed signals (notify call ho gaya jab koi wait hi nahi kar raha tha) jaise subtle bugs handle karne padte hain. `BlockingQueue` ye saari complexity abstract kar deta hai, isliye production code mein manual `wait()`/`notify()` almost kabhi nahi likha jaata.

**36. Thread-safe collections (`CopyOnWriteArrayList`, `ConcurrentLinkedQueue`) kab use karoge normal synchronized collections ke bajaye?**
`CopyOnWriteArrayList` tab use karo jab reads bahut zyada hon aur writes rare hon — har write pe pura underlying array copy hota hai (expensive write), lekin reads bina kisi lock ke fast hote hain (snapshot iterate karte hain, `ConcurrentModificationException` bhi nahi aati). `Collections.synchronizedList()` har single operation pe lock leta hai — high-read scenario mein zyada contention create karta hai compared to CopyOnWrite.

---

## Section 3 — Java 8

**37. ⭐⭐⭐⭐⭐ Lambda expressions functional interfaces ke saath kaise kaam karte hain? `@FunctionalInterface` ka role kya hai?**
Lambda ek functional interface (jisme exactly ek abstract method ho) ka concise anonymous implementation deta hai — `(x, y) -> x + y` uss single abstract method ka body ban jaata hai. `@FunctionalInterface` annotation compile-time check karti hai ki interface mein galti se doosra abstract method add na ho jaaye (jo lambda syntax ko ambiguous bana deta).

**38. Built-in functional interfaces (`Function`, `Predicate`, `Supplier`, `Consumer`, `BiFunction`) ka real use case do har ek ka.**
`Function<T,R>` — input leke transform karke output do (jaise `String -> Integer` parsing). `Predicate<T>` — input leke boolean return karo (filter condition, jaise `x -> x > 18`). `Supplier<T>` — bina input ke value do (lazy value generation, jaise `() -> new ExpensiveObject()`). `Consumer<T>` — input leke kuch return na karo, side-effect (jaise `x -> System.out.println(x)` ya DB save). `BiFunction<T,U,R>` — do inputs leke ek output.

**39. ⭐⭐⭐⭐⭐🔥 Stream API mein intermediate vs terminal operations mein farak kya hai? Streams lazy kyun hote hain — example se prove karo.**
Intermediate operations (`map`, `filter`, `sorted`) **lazy** hote hain — call karne pe turant execute nahi hote, sirf ek pipeline define karte hain. Terminal operation (`collect`, `forEach`, `count`, `findFirst`) call hote hi poori pipeline actually execute hoti hai, element-by-element (batch mein nahi). Proof: `Stream.of(1,2,3).map(x -> { System.out.println("mapping "+x); return x*2; })` — is line ko run karo bina terminal operation ke, kuch print nahi hoga, kyunki koi terminal operation nahi bulaya.

**40. `map()` vs `flatMap()` — jab tumhare paas `List<List<String>>` ho toh flatMap kyun zaroori hai?**
`map()` ek element ko ek doosre element mein transform karta hai (1-to-1 mapping) — agar `List<List<String>>` pe `map(x -> x)` karoge, result `Stream<List<String>>` hi rahega. `flatMap()` tab chahiye jab har element khud ek collection ho aur tumhe unko ek single flat stream mein "unwrap" karke combine karna ho — `list.stream().flatMap(List::stream)` se `Stream<String>` milta hai jisme saare inner lists ke elements ek saath flat ho jaate hain.

**41. ⭐⭐⭐⭐⭐ `reduce()` operation ke teeno overloaded versions explain karo — identity, accumulator, combiner ka role kya hai parallel stream mein?**
`reduce(BinaryOperator)` — sirf accumulator function, empty stream pe `Optional.empty()` return hota hai. `reduce(identity, accumulator)` — identity ek starting/default value deta hai (jaise sum ke liye 0), empty stream pe identity hi return hoti hai. `reduce(identity, accumulator, combiner)` — parallel streams ke liye zaroori hai kyunki data multiple threads mein split hoke process hoti hai, har thread apna partial result nikalta hai accumulator se, aur phir combiner batata hai ki alag-alag threads ke partial results ko final result mein kaise combine kiya jaaye.

**42. ⭐⭐⭐⭐⭐ `Collectors.groupingBy()` se multi-level grouping kaise karte ho (jaise department ke andar designation se group karna)?**
```java
Map<String, Map<String, List<Employee>>> result = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept,
             Collectors.groupingBy(Employee::getDesignation)));
```
Outer `groupingBy` department se group karta hai, inner `groupingBy` (downstream collector ke roop mein) us group ke andar phir designation se group karta hai — result ek nested Map banti hai.

**43. `Collectors.partitioningBy()` aur `groupingBy()` mein kab kaunsa use karoge?**
`partitioningBy` sirf ek boolean `Predicate` leta hai aur hamesha exactly 2 groups deta hai — `true` key aur `false` key, chahe ek group empty hi kyun na ho (jaise "adults" aur "minors"). `groupingBy` arbitrary key-extracting function leta hai aur jitni bhi distinct keys hon utne groups bana sakta hai. Simple binary split ke liye `partitioningBy` zyada expressive hai, multi-category grouping ke liye `groupingBy`.

**44. Method references (`Class::method`) ke 4 types kya hain, aur lambda se readability kaise improve hoti hai?**
1) Static method reference — `Math::sqrt`. 2) Instance method of a particular object — `myList::add`. 3) Instance method of an arbitrary object of a particular type — `String::toUpperCase` (jaha object khud parameter ban jaata hai). 4) Constructor reference — `ArrayList::new`. Ye lambda `x -> ClassName.method(x)` jaisi cheezein short-circuit kar dete hain, code zyada declarative aur readable ban jaata hai.

**45. Default methods interface mein kyun add kiye gaye Java 8 mein? Diamond problem kaise resolve hota hai multiple interfaces mein same default method ho toh?**
Default methods add kiye gaye taaki existing interfaces (jaise `Collection`) mein naye methods (jaise `stream()`) add kiye ja sakein bina un saari classes ko todhe jo pehle se us interface ko implement kar rahi thi (backward compatibility). Diamond problem: agar ek class do interfaces implement karti hai jinme same-signature ka default method hai, compiler force karta hai ki class explicitly override kare — us override ke andar specific interface ka default method call kar sakte ho `InterfaceName.super.methodName()` syntax se.

**46. Parallel streams internally kaise kaam karti hain (ForkJoinPool common pool)? Kab parallel stream use nahi karni chahiye?**
Parallel streams internally default common `ForkJoinPool` use karti hain (size = `Runtime.availableProcessors()`). Avoid karo jab: data size chhota ho (parallelization overhead hi benefit se zyada ho jaata hai), operation order-dependent ho (jaise `forEach` mein order guarantee nahi hoti parallel mein), shared mutable state involved ho (thread-safety issues create hoge), ya operations IO-bound hon (ForkJoinPool CPU-bound work ke liye tuned hai, blocking IO calls poore common pool ko starve kar sakte hain — jo doosre unrelated parallel streams ko bhi affect karega).

**Output-based practice (khud trace karo):**
- `Stream.of(1,2,3).map(x -> x*2).filter(x -> x>2).findFirst()` — Answer: `4`. Lazy + short-circuit evaluation ki wajah se element-by-element process hota hai: 1 → map → 2 → filter(2>2 false, reject). 2 → map → 4 → filter(4>2 true) → `findFirst` turant 4 return kar deta hai — element 3 kabhi process hi nahi hota.
- `list.stream().peek(System.out::println).count()` — implementation-dependent hai: agar stream ka size pehle se known hai (jaise `ArrayList`-backed stream bina filter ke), JVM `count()` ko optimize karke bina actual traversal ke size return kar sakta hai, is case mein `peek` execute hi nahi hoga. Yahi wajah hai `peek` ko sirf debugging ke liye use karo, business logic ke liye nahi.

---

## Section 4 — Java 17

**47. ⭐⭐⭐⭐⭐ Records kya problem solve karte hain jo normal POJO/Lombok `@Data` nahi karta? Records immutable kyun hote hain by design?**
Records boilerplate (constructor, getters, `equals()`, `hashCode()`, `toString()`) automatically compiler-level generate karte hain immutable data-carrier classes ke liye — `record Point(int x, int y) {}` bas ek line. Lombok `@Data` bhi similar boilerplate generate karta hai lekin annotation-processing (build-tool dependent) se, jabki records language feature hain (native JVM support, IDE/tooling better samajhta hai). Immutable by design kyunki fields implicitly `private final` hote hain aur koi setter generate nahi hota — DTOs/value objects ke liye ideal hai jaha accidental mutation bugs create kar sakti hai.

**48. Record mein custom validation kaise add karoge (compact constructor)?**
```java
public record Point(int x, int y) {
    public Point {  // compact constructor — parameter list repeat nahi karni
        if (x < 0 || y < 0) throw new IllegalArgumentException("negative not allowed");
    }
}
```
Compact constructor mein field assignment automatically end mein ho jaati hai, tum sirf validation/normalization logic likhte ho.

**49. ⭐⭐⭐⭐⭐ Sealed classes kya hain aur inheritance hierarchy ko restrict karke kya real benefit milta hai — pattern matching ke saath relation kya hai?**
`sealed class Shape permits Circle, Square, Triangle` — sirf explicitly permitted classes hi `Shape` extend kar sakti hain, koi unknown/unexpected subclass ban hi nahi sakta. Benefit: pattern matching ke saath combine hone pe (`switch` expression) compiler exhaustiveness check kar sakta hai — agar tumne saare permitted types handle kar liye hain switch mein, `default` branch ki zarurat nahi rehti, aur agar future mein koi naya permitted type add karo, compiler turant batayega ki switch statements update karne zaroori hain — safer hierarchy modeling.

**50. Pattern matching for `instanceof` — pehle vs Java 17/21 ke syntax mein kya farak hai, aur casting boilerplate kaise kam hoti hai?**
Pehle: `if (obj instanceof String) { String s = (String) obj; ... }` — manual explicit cast. Java 16+ (preview Java 14 se): `if (obj instanceof String s) { ... }` — agar check pass ho jaaye toh `s` automatically cast hoke bind ho jaata hai usi scope mein, manual casting boilerplate poori tarah hat jaati hai.

**51. ⭐⭐⭐⭐⭐ Switch expressions (arrow syntax, `yield`) traditional switch-case se kaise better hain — fall-through bug kaise avoid hota hai?**
Traditional switch mein `break` bhool jaana ek common bug source tha — agar `break` nahi likha toh execution agle case mein "fall through" kar jaata tha accidentally. Arrow syntax (`case MONDAY -> System.out.println("start");`) mein fall-through hota hi nahi — har case independent hai. Agar case ke andar multi-statement block chahiye aur value return karni ho switch expression se, `yield` keyword use karte ho: `case MONDAY -> { doSomething(); yield result; }`.

**52. Text blocks (`"""`) ka real use case do (jaise JSON/SQL query readability).**
```java
String query = """
    SELECT * FROM patients
    WHERE status = 'active'
    AND hospital_id = ?
    """;
```
Multi-line SQL/JSON strings ko bina `\n` aur `+` concatenation aur bina `\"` escaping ke likh sakte ho — readability aur maintainability dono improve hoti hai especially embedded queries ya JSON templates ke liye.

**53. Java 17 mein helpful NullPointerExceptions kya extra information dete hain jo pehle nahi milti thi debugging mein?**
Java 14+ mein introduce hua (Java 15+ default enabled) — pehle NPE sirf generic message deta tha "NullPointerException" bina context ke, tumhe khud dhoondhna padta tha chain mein kaunsa part null tha. Ab agar `a.getB().getC()` fail ho, exact message milta hai: "Cannot invoke `getC()` because the return value of `a.getB()` is null" — debugging bahut fast ho jaati hai especially long method chains mein.

**54. G1GC vs ZGC — Java 17 mein kaunsa low-latency applications ke liye better hai aur kyun?**
G1GC (default garbage collector) region-based hai aur throughput/latency ke beech balance rakhta hai — zyadatar general-purpose applications ke liye achha hai. ZGC ultra-low-latency ke liye design hua hai (pause times consistently sub-millisecond range mein, chahe heap size kitna bhi bada ho — multi-GB se TB tak) — real-time/latency-sensitive applications ke liye better hai, trade-off ye hai ki ZGC thoda zyada CPU overhead consume karta hai pause time itna kam rakhne ke liye.

---

*Part 2 mein: Spring Framework, Spring Boot, Spring Data JPA & Hibernate, Microservices.*
