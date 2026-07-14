# Senior Java Backend Developer — Interview Question Bank (Part 2 of 4)

**Coverage:** Section 5 (Spring Framework), Section 6 (Spring Boot), Section 7 (Spring Data JPA & Hibernate), Section 8 (Microservices)
**Continuing numbering from Part 1** (Q1–54 already covered).
**Legend:** ⭐⭐⭐⭐⭐ = Top priority | 🔥 = Must-prepare

---

## Section 5 — Spring Framework

**55. ⭐⭐⭐⭐⭐🔥 IOC container aur Dependency Injection ka actual benefit kya hai — testability aur loose coupling ke concrete example se explain karo.**
IOC (Inversion of Control) ka matlab hai objects apni dependencies khud `new` karke nahi banate — container (Spring) unhe create karke inject karta hai. Testability: `PatientService` ko test karte waqt real `ConsentClient` ki jagah ek mock `ConsentClient` constructor se pass kar sakte ho, bina Spring context load kiye bhi — pure unit test fast aur isolated rehta hai. Loose coupling: `PatientService` sirf `ConsentClient` interface pe depend karta hai, actual implementation (REST client, ya baad mein gRPC client) switch ho sakti hai bina `PatientService` ka code chhue.

**56. Constructor injection vs field injection vs setter injection — production code mein kaunsa best practice hai aur kyun?**
Constructor injection best practice hai: fields `final` bana sakte ho (immutability), dependency mandatory hai toh object incomplete state mein ban hi nahi sakta (agar dependency missing hai, compile-time/startup pe hi pata chal jaayega), aur testing mein bina Spring context ke bhi `new PatientService(mockRepo)` se directly test kar sakte ho. Field injection (`@Autowired` seedha field pe) discouraged hai kyunki reflection use hota hai, immutability nahi milti, aur testing ke liye reflection ya Spring `TestContext` chahiye hota hai.

**57. ⭐⭐⭐⭐⭐ Spring bean lifecycle ke saare steps explain karo — instantiation se lekar destruction tak (`@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`).**
Instantiate (constructor call) → dependencies inject ho (property population) → Aware interfaces callback (`BeanNameAware` waghera) → `BeanPostProcessor.postProcessBeforeInitialization()` → `@PostConstruct` method / `InitializingBean.afterPropertiesSet()` → `BeanPostProcessor.postProcessAfterInitialization()` (ye AOP proxies yahi wrap karta hai) → bean ready-to-use → (application shutdown pe) `@PreDestroy` / `DisposableBean.destroy()`.

**58. Bean scopes (`singleton`, `prototype`, `request`, `session`) — singleton bean ke andar prototype bean inject karne mein kya problem aati hai, aur kaise solve karoge?**
Singleton bean container mein sirf ek baar banta hai — agar uske constructor/field mein prototype bean inject kiya, wo prototype bhi sirf ek baar hi resolve hoga us singleton ki lifetime mein (kyunki injection sirf ek baar hota hai singleton creation ke waqt) — ye prototype ka "har baar naya instance" wala expected behavior break kar deta hai. Fix: `ObjectFactory<T>` ya `Provider<T>` inject karo (jo har `.getObject()`/`.get()` call pe naya prototype resolve karega), ya `@Lookup` annotated method use karo jo Spring runtime pe override karke naya instance dega.

**59. ⭐⭐⭐⭐⭐ Circular dependency kya hota hai do beans ke beech, aur Spring ise kaise resolve karta hai (ya kab fail hota hai)?**
Bean A ko construction ke liye Bean B chahiye, aur Bean B ko Bean A chahiye — circular. Constructor injection mein Spring ise resolve NAHI kar sakta (`BeanCurrentlyInCreationException` throw hoti hai) kyunki dono ko pehle poori tarah construct hona zaroori hai ek-dusre ke liye, jo circular impossible hai. Field/setter injection mein singleton scope ke liye Spring "early reference" return kar sakta hai (three-level cache mechanism se, partially-constructed bean ka reference) — technically resolve ho jaata hai lekin ye design smell maana jaata hai; better solution hai circular dependency ko refactor karna (jaise common logic ko ek teesri service mein extract karke dono A aur B usi teesri service pe depend karein).

**60. `@Autowired` by type kaam karta hai — agar same type ke multiple beans hain toh kaise resolve karoge (`@Qualifier`, `@Primary`)?**
Agar same type ke multiple beans exist karte hain aur Spring disambiguate nahi kar paata, `NoUniqueBeanDefinitionException` throw hoti hai. `@Qualifier("specificBeanName")` se explicitly bata do kaunsa bean chahiye. `@Primary` ek bean ko "default choice" mark kar deta hai jab qualifier specify na kiya jaaye.

**61. Component scan kaise kaam karta hai internally — `@ComponentScan` na likhne pe Spring Boot beans kaise dhoondta hai?**
`@ComponentScan` classpath scan karke `@Component`, `@Service`, `@Repository`, `@Controller` (ye sab internally `@Component` hi hain, meta-annotated) waali classes ko dhoondh kar unke bean definitions register karta hai. Spring Boot mein `@SpringBootApplication` khud internally `@ComponentScan` include karta hai jo main application class ke package (aur sub-packages) se scanning start karta hai — isiliye main class ko root package mein rakhna best practice hai, warna sibling/parent packages ke components scan hi nahi honge.

**62. ⭐⭐⭐⭐⭐ AOP (Aspect-Oriented Programming) mein proxy-based implementation kaise kaam karti hai — JDK dynamic proxy vs CGLIB proxy mein farak kya hai?**
Spring AOP runtime pe proxy object banata hai jo actual target object ko wrap karta hai — method call proxy se guzarta hai jaha aspect logic (jaise `@Transactional`, logging) inject hoti hai, phir actual target method call hota hai. JDK dynamic proxy tab use hota hai jab target class kam-se-kam ek interface implement karti hai — proxy usi interface ko implement karke banta hai. CGLIB proxy tab use hota hai jab class koi interface implement nahi karti — CGLIB runtime pe target class ka ek subclass generate karta hai (isi wajah se `final` classes/methods pe CGLIB proxy nahi ban sakta, aur self-invocation — same class ke andar ek method doosre `@Transactional` method ko call kare — proxy ko bypass kar deta hai, ye ek common gotcha hai).

**63. `@Transactional` ke propagation types (`REQUIRED`, `REQUIRES_NEW`, `NESTED`) mein farak kya hai, real scenario ke saath?**
`REQUIRED` (default) — agar caller ke paas already active transaction hai toh usi mein participate karo, warna naya start karo. `REQUIRES_NEW` — hamesha ek bilkul naya independent transaction start karo, existing ko suspend kar do — real scenario: audit-log likhna chahte ho chahe main business transaction rollback ho jaaye, toh audit method `REQUIRES_NEW` rakho. `NESTED` — database savepoint use karta hai; agar inner transaction fail ho toh sirf usi savepoint tak rollback hota hai, outer transaction continue kar sakta hai (agar outer khud fail ho jaaye toh dono rollback honge).

**64. Spring profiles ka use kya hai multi-environment (dev/staging/prod) setup mein, aur profile-specific bean kaise define karoge?**
`application-dev.yml`, `application-prod.yml` jaisi files alag database URLs, log levels, feature flags rakhti hain — `spring.profiles.active=prod` (env var/command-line/config) se decide hota hai kaunsi active hogi. Profile-specific bean: `@Profile("dev")` annotation kisi `@Bean`/`@Component` pe lagao — wo bean sirf tab register hoga jab `dev` profile active ho (jaise dev mein ek mock external-API client, prod mein real client).

---

## Section 6 — Spring Boot

**65. ⭐⭐⭐⭐⭐🔥 Spring Boot auto-configuration internally kaise kaam karta hai — `@EnableAutoConfiguration` aur `spring.factories`/`AutoConfiguration.imports` ka role kya hai?**
`@SpringBootApplication` mein `@EnableAutoConfiguration` included hota hai, jo `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file (naye Spring Boot versions mein; pehle `spring.factories` mein tha) mein listed saari candidate `@Configuration` classes ko load karta hai. Har candidate configuration `@ConditionalOnClass` (kya specific library classpath pe hai), `@ConditionalOnMissingBean` (kya user ne khud already ek bean define kiya hai — agar haan toh auto-config apply nahi hoga), `@ConditionalOnProperty` jaisi conditions ke through decide karta hai ki actually apply hona chahiye ya nahi. Isi wajah se "convention over configuration" kaam karta hai — bas `spring-boot-starter-web` add karo, DispatcherServlet/Jackson/embedded-Tomcat automatically configure ho jaate hain.

**66. Starter dependencies (jaise `spring-boot-starter-web`) actually kya karti hain — inke peeche mechanism kya hai?**
Starter khud koi Java code nahi hai, ye ek "dependency descriptor" (POM/BOM) hai jo transitively saari zaroori compatible-version libraries pull karta hai — `spring-boot-starter-web` apne saath Spring MVC, embedded Tomcat, Jackson (JSON), aur Validation library laata hai, sab ek tested-compatible version combination mein — manually version-matching (dependency hell) ki zarurat khatam ho jaati hai.

**67. ⭐⭐⭐⭐⭐ Embedded server (Tomcat/Jetty/Undertow) Spring Boot mein kaise kaam karta hai — external WAR deployment se ye kaise alag hai?**
Spring Boot application executable JAR hoti hai jisme server (default Tomcat) already classpath mein embedded hota hai — `main()` method application context start karta hai aur embedded server ko programmatically start kar deta hai usi JVM process ke andar. `java -jar app.jar` se seedha application chal jaati hai. Traditional approach mein ek separate application server (Tomcat/WebLogic/JBoss) pehle se installed hona padta tha, aur application ko WAR file ke roop mein package karke uss server mein deploy karna padta tha — deployment aur server management alag concerns the.

**68. `application.properties` vs `application.yml` vs `@ConfigurationProperties` — type-safe configuration kaise banate ho complex nested properties ke liye?**
`.properties` flat key-value hai (`app.datasource.url=...`), `.yml` hierarchical/nested structure allow karta hai jo readable hota hai deeply nested config ke liye. Complex nested config ke liye:
```java
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceConfig {
    private String url;
    private int maxPoolSize;
    // getters/setters
}
```
Isse Spring automatically yml ke nested structure ko is class ke fields se bind kar deta hai — compile-time type safety aur IDE auto-complete milta hai, jo individual `@Value("${app.datasource.url}")` se scattered inject karne se kaafi better hai bulk config ke liye.

**69. Externalized configuration ka precedence order kya hai (command-line args, env variables, application.yml, defaults)?**
Roughly highest se lowest priority: command-line arguments (`--server.port=8081`) → `SPRING_APPLICATION_JSON` environment variable → JNDI/servlet init params → OS environment variables → profile-specific `application-{profile}.yml` → base `application.yml` → `@PropertySource` annotated files → hardcoded defaults in code. Matlab agar same property multiple jagah defined hai, upar wala jeetega.

**70. ⭐⭐⭐⭐⭐ Spring Boot Actuator production mein kaise use karte ho — custom health indicator kaise likhoge downstream dependency check karne ke liye?**
`/actuator/health` Kubernetes readiness/liveness probes ke liye standard endpoint hai, `/actuator/metrics` aur `/actuator/prometheus` monitoring dashboards ke liye. Custom health indicator:
```java
@Component
public class ConsentServiceHealthIndicator implements HealthIndicator {
    public Health health() {
        try {
            consentClient.ping();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```
Isse `/actuator/health` overall status downstream dependency ki health ko bhi reflect karega.

**71. Global exception handling `@ControllerAdvice` + `@ExceptionHandler` se kaise implement karte ho, aur consistent error response structure kaise design karoge?**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse(404, ex.getMessage(), Instant.now()));
    }
}
```
Isse har controller mein try-catch duplicate karne ki zarurat nahi padti — ek centralized jagah se saari exceptions consistent `{status, message, timestamp, path}` jaisi structure mein convert hoti hain client ke liye.

**72. Bean validation (`@Valid`, `@NotNull`, custom `@Constraint`) request DTO pe kaise apply karte ho, aur validation error ko client-friendly response mein kaise convert karoge?**
DTO fields pe `@NotNull`, `@Size(min=1,max=100)`, `@Email` jaise annotations lagao, controller method parameter pe `@Valid` lagao (`@RequestBody @Valid PatientDto dto`). Agar validation fail ho, `MethodArgumentNotValidException` throw hoti hai jo `@ControllerAdvice` mein catch karke field-wise error list (`{field: "email", message: "must be valid email"}`) response mein convert karte ho. Custom validation: `@Constraint(validatedBy = MyValidator.class)` se apna annotation + `ConstraintValidator` implementation likh sakte ho.

**73. ⭐⭐⭐⭐⭐ Filter vs Interceptor — dono request ko intercept karte hain, real difference kya hai aur kab kaunsa use karoge?**
Filter Servlet spec ka part hai — Spring MVC context se bhi bahar kaam karta hai, `DispatcherServlet` tak request pahunchne se pehle/baad chalta hai, sirf raw `HttpServletRequest`/`Response` ka access hota hai. Interceptor Spring-MVC-specific hai — `DispatcherServlet` ke andar chalta hai, controller/handler method ke context ka access milta hai (`preHandle`, `postHandle`, `afterCompletion`). Low-level, framework-agnostic concerns (jaise raw authentication token extraction, CORS) Filter mein; business/handler-aware cross-cutting logic (jaise "kaunsa controller method call ho raha hai" jaanke logging) Interceptor mein.

**74. Spring Security ka basic filter chain flow kya hai — ek request authentication se authorization tak kaise guzarta hai?**
Request `SecurityFilterChain` (filters ki ordered list) se guzarta hai — authentication filter (jaise custom JWT filter) token extract/validate karta hai aur agar valid hai `Authentication` object `SecurityContextHolder` mein set kar deta hai. Uske baad authorization filter role/permission checks karta hai (`@PreAuthorize`, URL-pattern-based rules) — agar authenticate/authorize dono pass ho toh request controller tak pahunchti hai, warna 401 (unauthenticated) ya 403 (authenticated but not authorized) return hota hai.

---

## Section 7 — Spring Data JPA & Hibernate

**75. ⭐⭐⭐⭐⭐🔥 Entity lifecycle states (Transient, Persistent, Detached, Removed) explain karo — entity kab kis state mein hota hai?**
**Transient** — abhi-abhi `new Entity()` se bana object, persistence context se koi relation nahi, DB se koi link nahi. **Persistent (Managed)** — `entityManager.persist()` ke baad, ab persistence context isko track kar raha hai, koi bhi field change automatically DB mein sync hoga next flush pe (dirty checking). **Detached** — persistence context/session close ho gaya (jaise transaction end) ya explicitly `detach()` kiya — object exist karta hai memory mein lekin ab uske changes automatically track/sync nahi hote. **Removed** — `entityManager.remove()` call hua hai, agli flush/commit pe corresponding DB row delete ho jaayegi.

**76. ⭐⭐⭐⭐⭐ Persistence context kya hai aur dirty checking kaise kaam karti hai bina explicit `save()` call kiye bhi update ho jaana?**
Persistence context ek "first-level cache" hai jo current transaction/session ke andar saari managed entities track karta hai (unki original loaded state ka snapshot bhi rakhta hai). Dirty checking: transaction commit ya explicit flush ke time, Hibernate automatically har managed entity ki current in-memory state ko uski original snapshot state se compare karta hai — agar difference mila, khud UPDATE SQL generate karke bhej deta hai. Isiliye `@Transactional` method ke andar sirf `entity.setStatus("ACTIVE")` call karna kaafi hai — explicit `repository.save()` call ki zarurat nahi hai agar entity already managed hai.

**77. Lazy vs Eager loading — `LazyInitializationException` kab aati hai aur production mein isse kaise avoid karte ho?**
Lazy loading — association tabhi actual DB query se fetch hoti hai jab access ki jaaye (pehle proxy object return hota hai). Eager — parent fetch hote hi association bhi turant fetch ho jaati hai. `LazyInitializationException` tab aati hai jab persistence context (session/transaction) already close ho chuka ho aur uske baad koi lazy association access karne ki koshish karo (common case: entity controller layer tak pahunchi, jaha `@Transactional` service layer already khatam ho chuka hai, aur JSON serialization ke time Jackson lazy field access karta hai). Avoid: query mein hi `JOIN FETCH` use karo zaroori associations ke liye, `@EntityGraph` annotation use karo, ya DTO projection return karo (entity ko directly controller tak mat le jao).

**78. ⭐⭐⭐⭐⭐🔥 N+1 query problem kya hai — real example do, aur `JOIN FETCH`/`@EntityGraph`/batch fetching se kaise solve karoge?**
```java
List<Order> orders = orderRepository.findAll(); // 1 query
for (Order o : orders) {
    System.out.println(o.getCustomer().getName()); // har order ke liye alag query — N queries
}
```
Total 1+N queries chal jaati hain jabki 1 hi kaafi thi. Fix 1 — JPQL mein `JOIN FETCH`: `SELECT o FROM Order o JOIN FETCH o.customer` (ek hi query mein sab kuch fetch). Fix 2 — `@EntityGraph(attributePaths = "customer")` repository method pe. Fix 3 — `@BatchSize(size = 20)` association pe, jisse individual N queries ki jagah Hibernate multiple parents ke children ek `WHERE id IN (...)` query mein batch fetch kar leta hai.

**79. Cascade types (`PERSIST`, `MERGE`, `REMOVE`, `ALL`) — parent-child relationship mein galat cascade use karne se kya production issue ho sakta hai?**
`PERSIST` — parent save hone pe child bhi auto-save. `MERGE` — parent update hone pe child bhi auto-update. `REMOVE` — parent delete hone pe child bhi auto-delete. `ALL` — sab. Real production risk: agar `CascadeType.REMOVE` galti se ek **shared** child entity pe laga do (jaise ek `Address` jo multiple `Patient` records reference karte hain), toh ek Patient delete karne pe accidentally us shared Address ko bhi delete kar dega — jisse doosre Patients ka data silently corrupt/broken ho jaayega. Isiliye cascade REMOVE sirf true "ownership" (composition, jaise Order → OrderItems) relationships pe hi lagana chahiye, shared references pe kabhi nahi.

**80. ⭐⭐⭐⭐⭐ Optimistic locking vs pessimistic locking — high-concurrency update scenario (jaise inventory count) mein kaunsa use karoge aur kyun?**
Optimistic — assume karo conflict rare hoga; `@Version` field se track karte ho, koi actual DB lock nahi liya jaata, agar concurrent update conflict ho jaaye toh commit ke time `OptimisticLockException` throw hoti hai (application ko retry karna padta hai). Ye high-throughput, low-actual-conflict scenarios ke liye better hai (jaise typical CRUD) — DB lock ka overhead nahi hota. Pessimistic — turant DB-level row lock le lo (`SELECT ... FOR UPDATE`), doosre transactions wait karte hain. High-conflict scenarios mein (jaise inventory stock deduction jaha bahut saare concurrent writes ek hi row pe ho rahe hon aur "lost update" bahut costly ho — negative stock jaana), pessimistic zyada safe hai kyunki retry-logic complex nahi karni padti.

**81. `@Version` field se optimistic locking kaise implement hoti hai, aur `OptimisticLockException` aane pe application level pe kaise handle karte ho?**
```java
@Entity
class Inventory {
    @Version
    private Long version;
    private int stockCount;
}
```
Hibernate har UPDATE query mein automatically `WHERE id=? AND version=?` add karta hai, aur success pe version increment karta hai. Agar UPDATE se 0 rows affect hui (kyunki version already kisi aur transaction ne badal diya tha), Hibernate `OptimisticLockException` throw karta hai. Application level: `@ControllerAdvice` mein catch karke user-friendly "someone else updated this, please refresh and retry" message do, ya automatic retry-with-backoff logic implement karo transient conflicts ke liye.

**82. JPQL vs native query vs Criteria API — complex dynamic filtering (jaise search with 5 optional filters) ke liye kaunsa best approach hai?**
JPQL entity-object-oriented aur database-agnostic hai, simple-to-medium static queries ke liye achha. Native query raw SQL hai — database-specific features (jaise Postgres `pg_trgm` functions) chahiye ho tabhi. Criteria API type-safe, programmatic query building deta hai — dynamic filtering (jaise search API jisme naam, status, date-range sab optional hain aur user ne jo diya usi ka predicate add karna hai) ke liye best fit hai, kyunki runtime pe conditionally `Predicate` list build kar sakte ho bina error-prone string concatenation ke.

**83. First-level cache vs second-level cache — dono kis scope pe kaam karte hain, aur second-level cache (jaise Ehcache/Redis) kab enable karoge?**
First-level cache persistence-context/session scope pe hota hai — default automatically enabled, sirf ek transaction ke andar hi valid hai (transaction khatam, cache bhi gaya). Second-level cache `SessionFactory`/`EntityManagerFactory` scope pe hota hai — poori application ke across-session/across-request valid rehta hai, explicitly enable karna padta hai (jaise Ehcache/Redis provider ke saath). Enable karo jab same read-heavy, infrequently-changing data (jaise reference/lookup tables — hospital list, medication codes) baar-baar alag-alag requests/sessions mein query ho rahi ho — DB round-trips significantly kam ho jaate hain.

**84. Fetch types ka default behavior kya hai `@OneToMany`, `@ManyToOne`, `@ManyToMany` mein, aur inhe override karna kab zaroori hota hai?**
`@OneToMany` aur `@ManyToMany` default **LAZY** hote hain. `@ManyToOne` aur `@OneToOne` default **EAGER** hote hain. Override zaroori hota hai jab default behavior N+1 problem create kar raha ho, ya jab EAGER association unnecessarily bahut zyada data fetch kar rahi ho jo zyadatar use hi nahi hoti — jaise `@ManyToOne` field ko explicitly `fetch = FetchType.LAZY` karna agar wo related entity heavy hai aur rarely access hoti hai.

**85. `@Transactional` ke andar exception aane pe rollback kab hota hai aur kab nahi (checked vs unchecked exception ka role)?**
Default Spring behavior: unchecked exceptions (`RuntimeException` aur uske subclasses, jaise `IllegalStateException`) pe **automatically rollback** hoti hai. Checked exceptions (`Exception` ke direct subclasses jo `RuntimeException` nahi hain) pe by default rollback **nahi** hoti — transaction commit ho jaata hai chahe exception throw hui ho. Ye behavior override karne ke liye `@Transactional(rollbackFor = Exception.class)` explicitly likhna padta hai agar checked exception pe bhi rollback chahiye.

**86. Hibernate internally query kaise execute karta hai — session, transaction, aur flush mode ka lifecycle briefly explain karo.**
`Session` (Hibernate) / `EntityManager` (JPA) ek Persistence Context maintain karta hai jisme transaction ke dauraan hone waale changes turant DB ko nahi bheje jaate — ye internally queue/track hote rehte hain. "Flush" ke time (jo commit se pehle automatically hota hai, ya explicit `flush()` call pe, ya kisi query execute hone se theek pehle auto-flush mode mein) actual batch SQL statements DB ko bheje jaate hain. Isse Hibernate ko batching/optimization ka mauka milta hai (multiple changes ek saath bhejna) bajaye har single setter call pe turant DB hit karne ke.

---

## Section 8 — Microservices

**87. ⭐⭐⭐⭐⭐ Monolith se microservices split karne ka decision kaise loge — kaunse factors (team size, domain boundary, scaling need) consider karoge?**
Factors: team size aur structure (Conway's Law — system architecture aksar team communication structure ko reflect karta hai, alag teams alag services independently own kar sakein toh microservices sense banate hain), domain boundaries clear hain kya (DDD ke "bounded contexts" identify ho sakte hain kya), independent scaling need (kya kuch specific parts baaki se bahut zyada traffic lete hain), aur independent deployment cadence chahiye (ek team apni service deploy kare bina doosri teams ko block kiye). Chhoti team ya simple/unclear domain ke liye monolith often behtar starting point hota hai — microservices operational complexity (network calls, distributed debugging, eventual consistency) add karte hain jo sirf genuinely bade, complex, multi-team systems mein justify hoti hai.

**88. ⭐⭐⭐⭐⭐ Service discovery kya problem solve karta hai (Eureka/Consul/Kubernetes-native) — client-side vs server-side discovery mein farak kya hai?**
Dynamic environment mein instances scale-up/down, restart, IP change hoti rehti hai — hardcoded URLs maintain karna impossible hai. **Client-side discovery** (jaise Netflix Eureka + Ribbon) — client khud registry se available healthy instances ki list leta hai aur load-balancing decision khud (client-side) karta hai. **Server-side discovery** (jaise Kubernetes `Service` object ya AWS ALB) — client sirf ek fixed/stable endpoint ko call karta hai, aur load balancer/proxy layer actual routing aur instance-selection decide karta hai — client ko underlying instances ka pata hi nahi chalta. Kubernetes-native architectures mostly server-side discovery use karti hain (simpler client code).

**89. API Gateway ke responsibilities kya hain — routing, auth, rate limiting ke alawa aur kya handle karta hai?**
Path-based routing (`/patients/*` → Patient Service), authentication/authorization (single entry point pe hi token validate kar do, downstream services simplify ho jaate hain), rate limiting/throttling, request/response transformation (protocol translation, header manipulation), response aggregation (multiple backend calls ka result ek response mein combine karna client ke liye), aur cross-cutting logging/metrics collection ek centralized jagah se.

**90. ⭐⭐⭐⭐⭐🔥 Circuit breaker pattern (Resilience4j) kaise kaam karta hai — CLOSED, OPEN, HALF_OPEN states explain karo real scenario ke saath.**
**CLOSED** — normal operation, calls downstream service ko jaati hain, failures count ho rahi hoti hain (rolling window mein). Agar failure rate ek configured threshold cross kar jaaye (jaise 50% calls fail ho rahi hain), state **OPEN** ho jaati hai — is state mein naye calls turant fail ho jaate hain (fast-fail) bina actually downstream ko call kiye, taaki struggling downstream service ko aur load na mile aur calling service ka apna thread pool bhi exhaust na ho waiting mein. Ek configured waiting period ke baad state **HALF_OPEN** ho jaati hai — limited number of "test" calls jaane diye jaate hain check karne ke liye ki downstream recover ho gaya kya; agar successful, wapas CLOSED, agar phir bhi fail, wapas OPEN. Real scenario: Document Exchange Service, Consent Service ko call karta hai — agar Consent Service down/slow hai, circuit breaker turant OPEN ho jaayega aur Document Exchange fast-fail response degi bajaye timeout tak wait karne ke, isse poora system responsive rehta hai.

**91. Retry pattern implement karte waqt kaunse pitfalls avoid karoge (jaise retry storm, non-idempotent operation pe retry)?**
Retry storm — agar downstream already overloaded/struggling hai aur saare clients same time pe retry karne lagein, load aur badh jaata hai jo cascading failure trigger kar sakta hai — **exponential backoff + jitter** (random delay variation) use karo taaki retries synchronized na ho. Non-idempotent operations pe retry karna dangerous hai — jaise "create order" API retry karne se duplicate order ban sakta hai agar pehla call actually server pe succeed ho gaya tha lekin response client tak nahi pahuncha (network issue) — sirf genuinely idempotent operations (GET, ya idempotency-key wale POST) pe hi safely retry karna chahiye.

**92. Bulkhead pattern kya isolate karta hai — ek slow downstream service poore system ko down hone se kaise bachaya jaata hai?**
Ship ke watertight compartments jaisa concept — agar ek compartment mein paani bhar jaaye, poora ship nahi doobta kyunki compartments isolated hain. Software mein: agar ek downstream dependency (jaise AI Assistant Service) slow ho jaaye, uske calls ke liye ek **separate, limited thread pool/connection pool** allocate karo — isse wo slow dependency poore application ka shared thread pool exhaust nahi kar payegi, aur baaki unrelated functionality (jaise Patient Search) unaffected rehti hai.

**93. Config Server ka use kya hai centralized configuration ke liye, aur runtime pe config refresh kaise hota hai bina restart kiye?**
Config Server (jaise Spring Cloud Config) saari microservices ki configuration ek centralized, version-controlled jagah (jaise ek Git repo) se serve karta hai — har service apna config yahan se fetch karta hai startup pe. Runtime refresh: relevant beans pe `@RefreshScope` lagao, aur jab config change ho, `/actuator/refresh` endpoint hit karo (ya event-bus-driven auto-refresh setup karo) — bina application restart kiye naya config value pick ho jaata hai un beans mein.

**94. ⭐⭐⭐⭐⭐🔥 Distributed transactions microservices mein kyun problematic hain (2PC ka overhead), aur Saga pattern (choreography vs orchestration) kaise solve karta hai?**
2PC (Two-Phase Commit) mein ek central coordinator saare participant services ko locks hold karke rakhta hai jab tak sab "commit-ready" confirm na kar dein — ye microservices mein impractical hai: tight coupling create hoti hai, locks lambe time tak hold hone se availability/throughput girta hai, aur agar koi participant temporarily unavailable ho jaaye poora transaction stuck ho jaata hai. **Saga pattern** ek badi transaction ko chhoti independent local transactions ki series mein todta hai, har step apna kaam complete karke agla step trigger karta hai — agar beech mein koi step fail ho, pehle complete hue steps ke liye "compensating actions" (jaise reservation cancel karna) chalaye jaate hain. **Choreography** — koi central coordinator nahi, har service apna kaam karke ek event publish karta hai jisse agla service subscribe karke apna kaam karta hai (decoupled, lekin poora flow track karna mushkil hota hai bade sagas mein). **Orchestration** — ek central orchestrator service explicitly decide karta hai kaunsa step kab chalega aur failure pe kaunsi compensating action trigger hogi (flow track karna easier hai, lekin orchestrator ek coordination point ban jaata hai).

**95. ⭐⭐⭐⭐⭐ Event-driven architecture mein eventual consistency ka trade-off kya hai strong consistency ke against?**
Strong consistency — har read turant latest write reflect karta hai across saare services/replicas, lekin ye achieve karne ke liye availability/latency ka cost aata hai (CAP theorem — network partition ke time consistency prioritize karna matlab kuch requests reject/wait karni padti hain). Eventual consistency — temporary inconsistency allow karta hai (kuch milliseconds/seconds ke liye alag services ka data thoda "stale" ho sakta hai jab tak async events process nahi ho jaate), badle mein better availability aur lower latency milti hai. Real trade-off decision: ek notification thodi der se deliver ho jaaye toh acceptable hai (eventual consistency theek hai), lekin payment ya consent-check jaisi cheezein strong consistency maangti hain.

**96. Kafka integration — producer acknowledgment (`acks=0/1/all`), consumer offset commit strategy, aur exactly-once vs at-least-once delivery explain karo.**
`acks=0` — producer kisi acknowledgment ka wait hi nahi karta (fastest, lekin data-loss risk agar broker fail ho jaaye message receive hone se pehle). `acks=1` — sirf partition leader broker se acknowledgment (moderate safety, agar leader crash ho jaaye replication se pehle, data-loss possible). `acks=all` — leader + saare in-sync replicas se acknowledgment (safest, thoda zyada latency). Consumer offset commit: auto-commit (simple but risk hai ki message crash ke time twice process ho ya miss ho jaaye depending on timing) vs manual commit (processing successfully complete hone ke baad explicitly commit karo — better control, standard production practice). At-least-once (default typical setup — duplicates possible hain, consumer ko idempotent design karna padta hai) vs exactly-once (Kafka transactions API se achievable, lekin zyada complexity/overhead ke saath).

**97. RabbitMQ vs Kafka — kab kaunsa choose karoge (message queue vs event streaming use case)?**
RabbitMQ ek traditional message broker hai — complex routing capabilities (exchanges, bindings, routing keys), per-message acknowledgment/priority queues support karta hai — jab tumhe classic "task queue" chahiye (ek message ek consumer process kare aur queue se gayab ho jaaye) tab better fit hai. Kafka ek event streaming/log platform hai — bahut high throughput handle karta hai, messages ek persistent log mein store rehte hain (retention period tak replay bhi kar sakte ho), aur multiple independent consumer groups apni apni pace pe same data padh sakte hain — jab tumhe event sourcing, high-volume streaming, ya multiple independent consumers ek hi event stream se chahiye tab Kafka better hai.

**98. ⭐⭐⭐⭐⭐ Idempotency kaise implement karte ho REST API mein jaha client retry kar sakta hai (jaise payment API)?**
Client ek unique `Idempotency-Key` header bhejta hai request ke saath (usually client-generated UUID, ek particular logical operation ke liye consistent rehta hai retries ke across). Server us key ko ek fast-lookup store (Redis/DB) mein check karta hai — agar wo key already process ho chuki hai, purana stored response wapas return kar do bina operation dobara actually kiye. Agar naya key hai, operation process karo aur result ko us key ke saath store karo future retries ke liye. Isse client safely retry kar sakta hai (jaise network timeout ke baad, jaha usse pata nahi ki original request server tak pahunchi ya nahi) bina duplicate side-effect (jaise double payment charge) ke risk ke.

**99. CAP theorem practically kya matlab rakhta hai microservices design mein — ek real example do jaha tumne consistency vs availability trade-off socha ho.**
CAP theorem: network partition (jo distributed systems mein kabhi bhi ho sakta hai — service unreachable, network delay) hone pe tumhe Consistency aur Availability mein se compromise karna padta hai (Partition tolerance non-negotiable hai distributed system mein). Real example: agar Consent Service temporarily unreachable ho jaaye, ek approach hai "sab data-access requests reject/block karo jab tak Consent Service wapas na aa jaaye" — ye Consistency prioritize karta hai (kabhi bhi stale/wrong consent-decision pe data release nahi hoga). Doosra approach: "last-known cached consent state use karke request serve karo" — ye Availability prioritize karta hai lekin thoda staleness risk leta hai. Healthcare consent jaisa sensitive case mein, Consistency prioritize karna generally safer choice hai — galat consent-decision pe patient data leak hone ka risk availability se zyada costly hai.

**100. Distributed tracing (OpenTelemetry/Zipkin) kaise ek request ka poora path track karta hai multiple services ke beech — correlation/trace ID kaise propagate hota hai?**
Jab request sabse pehli service mein enter karta hai (jaise API Gateway), ek unique **trace ID** generate hota hai (ya agar upstream se already aaya hai toh wahi continue karte hain). Har service jo is request ko handle karti hai apna ek **span** create karti hai (trace ke andar ek segment, apni start/end time ke saath) aur trace ID (plus current span ID as "parent" ke liye) ko HTTP headers (ya Kafka message headers async ke liye) mein aage propagate karti hai next call ke saath. Ek tracing backend (Zipkin/Jaeger, OpenTelemetry Collector) saare services se ye spans collect karke ek waterfall-style visualization banata hai — jisse exactly dikh jaata hai ki request kis-kis service se hoke gaya, kaunse order mein, aur har hop mein kitna time laga — multi-service production issues debug karne ke liye invaluable tool hai.

---

*Part 3 mein: Design Patterns, REST APIs, SQL, System Design.*
