# Spring AI + OpenAI Interview Guide (Part 1 of 2) — Trimmed to Most-Important Only

**Profile:** Senior Java Spring Boot Backend Developer, 6 yrs backend + 1.5-2 yrs LLM integration (Spring AI + OpenAI)
**Coverage:** Section 1 (Spring AI), Section 2 (OpenAI APIs), Section 3 (RAG & AI Search), Section 4 (Spring Boot Integration)
**Note:** Ye ab sirf un questions tak limited hai jo ek "AI person" interview mein genuinely sabse zyada poochhe jaate hain — poori exhaustive list nahi, sirf high-yield core.

---

## Section 1 — Spring AI (6 questions)

### 1.1 ChatClient vs ChatModel
**Q: ChatClient kya hai aur ChatModel se kaise differ karta hai?**

`ChatClient` Spring AI ka high-level, fluent API hai — builder pattern se banta hai (`ChatClient.builder(chatModel).build()`), aur usme prompt-building, advisor-chaining, aur response-mapping (jaise `.call().entity(MyDto.class)`) built-in hoti hai. `ChatModel` low-level interface hai jo actual provider (OpenAI/Azure/Anthropic) se raw request/response handle karta hai — `ChatClient` internally isi ko wrap karta hai. Production mein zyadatar `ChatClient` use karte hain kyunki boilerplate kam karta hai.

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultSystem("You are a helpful assistant for a healthcare platform.")
    .build();

String response = chatClient.prompt().user("What are the symptoms of dehydration?")
    .call().content();
```

**Interviewer keeps digging:**
- *"Why dono expose kiye Spring ne?"* → Separation of concerns — low-level flexibility (ChatModel) aur high-level ergonomics (ChatClient) alag use-cases ke liye.
- *"What if structured object chahiye response mein?"* → `.call().entity(MyRecord.class)` — Spring AI internally JSON-schema/structured-output mechanism use karta hai.

**Common mistake:** har request pe naya `ChatClient` builder se banana. **Best practice:** ek singleton `ChatClient` bean banao, defaults bean-level pe set karo.

### 1.2 PromptTemplate + System vs User Prompt
**Q: PromptTemplate kya problem solve karta hai, aur System prompt vs User prompt mein architecturally kya farak hai?**

`PromptTemplate` string-templating deta hai (`{variable}` placeholders) taaki dynamic values (user input, retrieved context) safely prompt-structure mein inject ho. **System prompt** model ka role/behavior/constraints define karta hai ("tum healthcare assistant ho, sirf provided context se answer do") — developer-controlled. **User prompt** end-user ka actual input hai — **untrusted**. Security angle se system aur user content ko strictly separate roles mein rakhna zaroori hai (prompt injection risk kam karne ke liye — Part 2, Section 5).

```java
PromptTemplate template = new PromptTemplate("""
    Answer the question using only the provided context.
    Context: {context}
    Question: {question}
    """);
Prompt prompt = template.create(Map.of("context", retrievedContext, "question", userQuestion));
```

**Follow-up:** *"Prompt injection se system prompt ko architecturally kaise protect karte ho?"* → User input ko kabhi system-message mein concatenate mat karo, sirf user-message role mein rakho.

### 1.3 Conversation Memory
**Q: Multi-turn conversation mein memory kaise maintain karte ho — kya poori history har baar model ko bhejte ho?**

Spring AI `ChatMemory` abstraction deta hai (`InMemoryChatMemory`, ya custom Redis/DB-backed) jo history ko `conversationId` ke against store karta hai. `MessageChatMemoryAdvisor` ise `ChatClient` ke saath automatically wire karta hai. Poori history bhejna token-cost aur context-window dono ke liye problematic hai lambi conversations mein — production mein **sliding window** (last N turns) ya **periodic summarization** use karte hain.

**Interviewer keeps digging:**
- *"Context window exceed ho jaaye toh?"* → Sliding window ya summarization approach.
- *"Memory production mein kaha store karte ho — in-memory theek hai?"* → In-memory sirf single-instance/dev ke liye theek hai; multi-instance production deployment mein Redis/DB-backed `ChatMemory` chahiye taaki koi bhi instance conversation continue kar sake (stateless service design).

### 1.4 Advisors
**Q: Advisor pattern Spring AI mein kya solve karta hai — real example do.**

Advisor ek interceptor/middleware pattern hai jo `ChatClient` ki request/response pipeline mein cross-cutting concerns inject karta hai — Spring MVC Interceptor/AOP jaisa hi concept. Built-in: `MessageChatMemoryAdvisor` (history inject), `QuestionAnswerAdvisor` (RAG — vector-store se context automatically fetch/inject), `SimpleLoggerAdvisor` (logging).

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new MessageChatMemoryAdvisor(chatMemory),
        new QuestionAnswerAdvisor(vectorStore)
    )
    .build();
```

**Follow-up:** *"Multiple advisors ka order matter karta hai?"* → Haan — advisors ek chain mein execute hote hain, order decide karta hai RAG-context-injection pehle ho ya memory-injection — galat order se prompt malformed ban sakta hai.

### 1.5 VectorStore + Basic RAG
**Q: VectorStore abstraction kya karta hai, aur ek basic RAG pipeline Spring AI mein kaise banate ho?**

`VectorStore` interface (`PgVectorStore`, etc.) documents ko unke embeddings ke saath store karta hai, `similaritySearch(SearchRequest)` query-embedding banake top-k closest matches return karta hai (`topK`, `similarityThreshold`, metadata-filters ke saath).

```java
// 1. Retrieve
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.query(userQuestion).withTopK(5).withSimilarityThreshold(0.7));
String context = docs.stream().map(Document::getContent).collect(Collectors.joining("\n"));

// 2. Augment + Generate
String prompt = "Answer using only this context: %s\nQuestion: %s".formatted(context, userQuestion);
String answer = chatClient.prompt().user(prompt).call().content();
```
Production mein `QuestionAnswerAdvisor` isi flow ko automatically handle karta hai.

**Follow-up:** *"similarityThreshold kyun zaroori hai?"* → Agar genuinely relevant document store mein hai hi nahi, similarity-search phir bhi "top-k closest" return karega. Threshold weak/irrelevant matches reject karta hai — hallucination-risk kam hota hai.

### 1.6 Streaming Responses
**Q: Streaming response Spring AI mein kaise implement karte ho, frontend tak SSE se kaise pahunchate ho?**

`.stream().content()` ek `Flux<String>` (Project Reactor) return karta hai jisme tokens incrementally aate hain — poore response ka wait nahi karna padta, perceived latency kam hoti hai.

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String question) {
    return chatClient.prompt().user(question).stream().content();
}
```

**Follow-up:** *"Streaming mein beech mein model call fail ho jaaye toh error handling kaise?"* → `Flux` ke `onErrorResume`/`onErrorReturn` operators se graceful error-event bhejo bajaye connection abruptly drop hone ke.

---

## Section 2 — OpenAI APIs (6 questions)

### 2.1 Which APIs used and why
**Q: Tumne kaunse specific OpenAI APIs use kiye, aur kyun?**

Typically do primary: **Chat Completions API** (ya newer **Responses API**, stateful/agentic workflows ke liye better — tools/function-calling ko server-side seamlessly handle karta hai) conversational Q&A ke liye, aur **Embeddings API** documents ko vectorize karke pgvector mein store karne ke liye.

**Follow-up:** *"Chat Completions aur Responses API mein practical farak?"* → Responses API stateful hai (`previous_response_id` se continue kar sakte ho bina poori history resend kiye) aur built-in tools support karta hai; Chat Completions stateless hai, har call mein poori history khud bhejni padti hai.

### 2.2 Function/Tool Calling
**Q: Function calling kaise implement kiya — `@Tool` annotation se?**

Java method ko `@Tool(description = "...")` se mark karte ho — model jab decide karta hai tool-call zaroori hai, structured request return karta hai (function name + JSON arguments), Spring AI automatically method invoke karta hai, result model ko wapas jaata hai continuation ke liye.

```java
@Tool(description = "Get current medication list for a patient by patient ID")
public List<Medication> getMedications(String patientId) {
    return medicationRepository.findByPatientId(patientId);
}
```

**Interviewer keeps digging:**
- *"Model galat parameters ke saath tool call kare toh?"* → Method ke andar validation ho, error ko context ke roop mein model ko wapas de sakte ho.
- *"Security angle se tool-calling mein kya dhyaan?"* → Sirf explicitly-whitelisted, safe operations wale tools expose karo, tool ke andar bhi authorization check rakho — model ka decision blindly trust mat karo.

### 2.3 Structured JSON Output
**Q: Structured JSON output kaise force karte ho — kya prompt mein "return JSON" likhna kaafi hai?**

Nahi — sirf prompt-instruction reliable nahi hai. Better: OpenAI ka `response_format: {type: "json_schema", ...}` (structured outputs) use karo. Spring AI mein `.call().entity(MyRecord.class)` ye abstract kar deta hai.

**Follow-up:** *"Model phir bhi schema-violate kare toh fallback?"* → Try-catch around deserialization, retry-with-clarification logic, aur logging taaki pattern track ho sake.

### 2.4 Model Selection Strategy
**Q: GPT-4o/GPT-4.1/reasoning-class models mein kab kaunsa — cost/latency/quality trade-off kaise decide karte ho?**

Simple, high-volume tasks ke liye chhota/cheaper model (mini variants). Complex reasoning/multi-step orchestration ke liye bada model justify hota hai. Production mein aksar **tiered approach** — chhota/fast model pehle try, sirf complex/ambiguous cases bade model ko route hote hain.

**Follow-up:** *"Model version deprecate ho jaaye, risk kaise mitigate?"* → Model name config mein externalize karo, regression-test suite rakho.

### 2.5 Resilience — Rate Limit, Timeout, Retry, Fallback
**Q: OpenAI slow/unavailable ho jaaye, application kaise gracefully handle kare?**

**Rate limiting** — client-side internal rate-limiter (Resilience4j) rakho. **Timeout** — WebClient/RestClient pe explicit connect/read timeout. **Retry** — transient errors pe exponential backoff, sirf idempotent-safe operations pe. **Fallback** — Circuit breaker ke saath combine karo, consistently fail/slow ho toh circuit OPEN ho aur fallback response do.

**Follow-up:** *"Retry aur idempotency ka relation?"* → Chat-completion calls generally retry-safe hain (no side-effect), lekin tool-calling se koi side-effect (DB write) hua ho toh us part ko idempotent banana zaroori hai.

### 2.6 Token Counting & Cost Calculation
**Q: Token usage aur cost kaise track/calculate karte ho production mein?**

Response ke `usage` object (`prompt_tokens`, `completion_tokens`) se capture karke Micrometer → CloudWatch mein log karo, per-user/per-feature breakdown ke saath. Cost = `total_tokens × per-token-price` (input/output pricing alag). Per-user quota bhi implement karo taaki ek user/feature poora budget na kha jaaye.

---

## Section 3 — RAG & AI Search (5 questions)

### 3.1 What is RAG
**Q: RAG kya hai aur kis problem ko solve karta hai jo pure LLM nahi kar sakta?**

RAG — response-generation se pehle relevant information ek external knowledge-store se retrieve karke model ke context mein inject karte hain, taaki model apni training-time knowledge ke bajaye specific/current/verifiable data se answer de. Solve karta hai: LLM ki knowledge training-cutoff tak limited hai, aur bina grounding ke model hallucinate kar sakta hai.

### 3.2 Chunking + Overlap
**Q: Chunking strategy kaise design ki — chunk size aur overlap kaise decide karte ho?**

Chunk size typically 200-500 tokens (bahut chhota = fragmented context, bahut bada = precision girti hai). Natural boundaries (paragraphs) respect karo. **Overlap** (10-20%) consecutive chunks ke beech rakho — agar important info exactly boundary pe split ho jaaye, dono adjacent chunks mein partially present rahe, retrieval-miss risk kam hota hai.

### 3.3 Similarity Search — Cosine Similarity
**Q: Similarity search internally distance-metric kaise compute karta hai — cosine similarity kyun common hai?**

Embeddings high-dimensional vectors hain, similarity typically **cosine similarity** se measure hoti hai — angle between vectors (na ki magnitude), jo text-length variation ke against zyada robust hai. Bade datasets mein brute-force compare slow hai, isliye ANN indexes (HNSW) use hote hain.

### 3.4 pgvector vs Elasticsearch
**Q: pgvector kya hai, aur Elasticsearch ke bajaye pgvector kyun (ya kab nahi)?**

pgvector ek PostgreSQL extension hai jo vector data-type + similarity-search operators add karta hai directly Postgres mein — existing relational data ke saath vectors ko **same database, same transaction** mein combine kar sakte ho. Why: agar data already Postgres mein hai, ek naya separate system maintain karne se bachte ho. Elasticsearch better hai jab scale bahut bada ho (billions of vectors) ya already text-search (BM25) chahiye hybrid-search ke liye.

**Follow-up:** *"pgvector ki limitation bade scale pe?"* → Index build/query latency dedicated vector-DBs (Pinecone/Weaviate) ke comparison mein degrade ho sakti hai tens-of-millions+ vectors pe.

### 3.5 End-to-end search flow
**Q: Poora search flow describe karo — user query se final ranked results tak.**

User query → query embed (same model jo indexing mein use hua) → metadata-filters apply → vector store similarity search (top-k + threshold) → results LLM ko context ke roop mein bhejo final-answer-generation ke liye.

---

## Section 4 — Spring Boot Integration (3 questions)

### 4.1 Complete request flow
**Q: Ek AI-query request ka poora flow describe karo controller se model tak aur wapas.**

Controller → auth/authz → validation → service layer `ChatClient` use karta hai (advisors ke saath) → HTTP call OpenAI ko → response process/format → controller return (sync ya streaming). Async side-effects (usage-logging, audit) Kafka event se decouple hote hain.

### 4.2 Kafka document indexing
**Q: Document indexing pipeline Kafka se kaise design ki — kyun async/event-driven?**

Naya document upload hone pe embedding-generation + vector-store-insert slow-ish operation hai — isliye synchronously upload-response-path mein nahi karte. `document.ingested` Kafka event publish, ek separate AI Indexing Service consume karke text-extract → chunk → embed → vector-store insert — sab async. Isse upload-API fast response deti hai, aur fail hone pe Kafka retry/DLQ se safely retry hota hai.

### 4.3 Async vs Sync API calls
**Q: OpenAI calls sync vs async — kab kaunsa design karoge?**

Sync (blocking) simple hai lekin OpenAI-call (1-5+ sec) ke dauraan servlet-thread block hota hai — high-concurrency mein thread-pool exhaust ho sakta hai. Async/reactive (WebFlux) thread block nahi karta call ke dauraan — high-throughput AI-heavy apps mein better.

---

*Part 2 mein: Security, Performance & Cost, Production Scenarios — same tarike se trimmed, sirf most-important.*


===========================================================================================
================================================================================
  AI IN HEALTHCARE — COMPLETE INTERVIEW PREP (6 YRS JAVA BACKEND)
  Semantic Search · pgvector · OpenAI · RAG · Prompt Engineering
  Hinglish + Stories + Code Snippets + Advanced Concepts
================================================================================

> Yeh file specifically tumhare EPAM healthcare AI project pe based hai.
> Basic se Advanced — story se samjho, code se confirm karo.

================================================================================
SECTION 1: THE BIG PICTURE — TUMHARA AI SYSTEM KAISE KAAM KARTA HAI
================================================================================

STORY: Patient Query System ka Poora Flow

Imagine karo — Prashant naam ka patient hai. Woh apni mobile app pe type karta hai:
  "Doctor ne last visit pe kya blood pressure medication diya tha?"

Ab tumhara system kya karta hai?

STEP 1 — Question ek "embedding" mein convert hota hai
         (1536 numbers ka array — semantic meaning capture karta hai)

STEP 2 — pgvector uss embedding se patient ke saare medical records mein
         search karta hai — "kaunse records is question se MOST SIMILAR hain?"
         Top 3 results: Blood pressure records, last prescription

STEP 3 — Yeh 3 records "context" ke roop mein GPT-4 ko diya jaata hai
         with a carefully crafted prompt

STEP 4 — GPT-4 respond karta hai:
         "Your last visit on 15 Jan 2025, Dr. Sharma prescribed Amlodipine
          5mg once daily for hypertension."

STEP 5 — Response patient ko stream hota hai (typing effect — fast perception)

YEH POORA PATTERN = RAG (Retrieval Augmented Generation)

================================================================================
SECTION 2: EMBEDDINGS — KYA HOTE HAIN?
================================================================================

Q1. Embedding kya hota hai? Story se samjhao

STORY: Colors ko numbers se represent karo
- Red   = [255, 0, 0]
- Pink  = [255, 192, 203]  <- Red ke CLOSE (similar color)
- Blue  = [0, 0, 255]      <- Red se DOOR (different color)

YAHI TEXT KE SAATH:
- "Hypertension"            = [0.12, -0.45, 0.78, ...] (1536 numbers)
- "High blood pressure"     = [0.11, -0.44, 0.77, ...] <- Almost SAME! (same meaning)
- "Pizza recipe"            = [-0.89, 0.23, -0.56, ...] <- Completely DIFFERENT

Embedding = Text ko numbers mein convert karo jahan
            SIMILAR MEANING  →  numbers close hain
            DIFFERENT MEANING →  numbers door hain

TECHNICALLY:
- Vector = ordered list of numbers (dimensions)
- Embedding = High-dimensional space mein text ki "position"
- OpenAI text-embedding-ada-002     → 1536 dimensions
- OpenAI text-embedding-3-small     → 1536 dimensions (newer, 5x cheaper)
- OpenAI text-embedding-3-large     → 3072 dimensions (more accurate)

// ============================================================
// JAVA CODE: Embedding generate karna
// ============================================================

@Service
public class EmbeddingService {

    private final OpenAiService openAiService;

    public EmbeddingService(@Value("${openai.api.key}") String apiKey) {
        // Timeout set karo — embedding API slow ho sakti hai kabhi kabhi
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
    }

    /**
     * Kisi bhi text ko 1536-dimensional vector mein convert karo
     * Yeh vector text ka "semantic fingerprint" hai
     */
    public float[] embed(String text) {
        // Text preprocess karo
        String cleanedText = text.trim().replaceAll("\\s+", " ");

        // OpenAI request banao
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-3-small")  // Cost-effective, good quality
            .input(List.of(cleanedText))
            .build();

        // API call — network call hai
        var result = openAiService.createEmbeddings(request);

        // Response se vector nikalo
        List<Double> embedding = result.getData().get(0).getEmbedding();

        // Double -> float conversion (pgvector float4 use karta hai)
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return vector; // 1536 floats
    }

    /**
     * BATCH EMBEDDING — Multiple texts ek saath
     * Efficient: Ek API call mein N texts → less latency + less cost
     */
    public List<float[]> embedBatch(List<String> texts) {
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-3-small")
            .input(texts)  // List of texts ek saath
            .build();

        return openAiService.createEmbeddings(request)
            .getData().stream()
            .map(e -> toFloatArray(e.getEmbedding()))
            .collect(Collectors.toList());
    }
}

INTERVIEW TIP:
"Maine text-embedding-3-small use kiya — ada-002 se 5x cheaper aur better quality.
Batch embedding use kiya indexing ke time — 100 records ek API call mein embed karo."

================================================================================
SECTION 3: COSINE SIMILARITY — VECTORS COMPARE KAISE KARTE HAIN
================================================================================

Q2. Cosine Similarity vs Euclidean Distance — Healthcare mein kaunsa aur kyun?

WHY COSINE FOR TEXT (NOT EUCLIDEAN):
- Euclidean = Physical distance between points
  Problem: Long document vs short document — lengths different → unfair!
- Cosine = Angle between vectors — LENGTH-INDEPENDENT
  "BP high hai" and "Patient experiencing elevated blood pressure consistently"
  → Same meaning, different lengths → Cosine correctly says SIMILAR ✅

// Cosine similarity formula (understanding ke liye):
// cos(angle) = (A . B) / (|A| x |B|)
// Range: 0.0 (no relation) to 1.0 (identical)

// pgvector mein operators:
//   <=>   = cosine DISTANCE    (0 = same, 1 = opposite) — USE THIS for text
//   <->   = euclidean distance  (0 = same, higher = more different)
//   <#>   = negative inner product

================================================================================
SECTION 4: pgvector — DATABASE SETUP + QUERIES
================================================================================

Q3. pgvector kya hai? Kab use karo?

pgvector = PostgreSQL extension jo vector operations enable karta hai
No separate vector database needed — existing PostgreSQL hi use karo!

ALTERNATIVES comparison:
| Tool      | Type           | Best For                         |
|-----------|----------------|----------------------------------|
| pgvector  | PostgreSQL     | Already using Postgres, moderate |
| Pinecone  | Managed cloud  | Large scale, no infra            |
| Weaviate  | Open source    | Complex filtering + vectors      |
| Qdrant    | Open source    | High performance, production     |
| ChromaDB  | Open source    | Prototyping, local dev           |

// ============================================================
// SETUP — Flyway migration file: V1__create_embeddings.sql
// ============================================================

-- pgvector extension enable karo (DBA karta hai ya migration)
CREATE EXTENSION IF NOT EXISTS vector;

-- Table create karo
CREATE TABLE medical_record_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id  VARCHAR(50)  NOT NULL,
    record_type VARCHAR(50)  NOT NULL,   -- LAB_RESULT, PRESCRIPTION, DIAGNOSIS
    record_date DATE         NOT NULL,
    content     TEXT         NOT NULL,   -- Original text (display ke liye)
    embedding   VECTOR(1536) NOT NULL,   -- pgvector column! 1536 dimensions
    created_at  TIMESTAMP    DEFAULT NOW(),
    metadata    JSONB                    -- Extra: doctor_id, icd_codes etc.
);

-- Index 1: Patient filter ke liye (B-tree index)
CREATE INDEX idx_emb_patient ON medical_record_embeddings(patient_id);

-- Index 2: Vector search ke liye (IVFFlat — approximate nearest neighbor)
CREATE INDEX idx_emb_ivfflat
    ON medical_record_embeddings
    USING ivfflat (embedding vector_cosine_ops)  -- Cosine similarity
    WITH (lists = 100);
-- lists = sqrt(total_rows) rule of thumb
-- 10K rows -> lists=100, 100K rows -> lists=316

-- Index 3: HNSW (newer, better recall, more memory — production preferred)
CREATE INDEX idx_emb_hnsw
    ON medical_record_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
-- m = max connections per node (16 is standard)
-- ef_construction = build accuracy (64 is standard)

// ============================================================
// QUERIES — pgvector SQL
// ============================================================

-- BASIC SIMILARITY SEARCH:
-- <=> = cosine DISTANCE (low = similar)
-- 1 - distance = similarity score (high = similar)
SELECT
    id, record_type, record_date, content,
    1 - (embedding <=> '[0.12, -0.45, 0.78, ...]'::vector) AS similarity_score
FROM medical_record_embeddings
WHERE patient_id = 'PAT-123'
ORDER BY embedding <=> '[0.12, -0.45, 0.78, ...]'::vector  -- closest first
LIMIT 5;

-- WITH THRESHOLD (garbage results exclude karo):
SELECT id, content, record_type,
       1 - (embedding <=> '[...]'::vector) AS similarity
FROM medical_record_embeddings
WHERE patient_id = 'PAT-123'
  AND 1 - (embedding <=> '[...]'::vector) > 0.75  -- 75%+ similar only
ORDER BY embedding <=> '[...]'::vector
LIMIT 10;

-- HYBRID FILTER + VECTOR (date + type + similarity):
SELECT id, content, record_date
FROM medical_record_embeddings
WHERE patient_id   = 'PAT-123'
  AND record_type  = 'PRESCRIPTION'
  AND record_date >= CURRENT_DATE - INTERVAL '6 months'
ORDER BY embedding <=> '[...]'::vector
LIMIT 3;

// ============================================================
// JAVA: Spring Data JPA Repository
// ============================================================

@Repository
public interface MedicalRecordEmbeddingRepository
        extends JpaRepository<MedicalRecordEmbedding, UUID> {

    /**
     * Semantic similarity search for a specific patient
     * Returns top-N most relevant records
     */
    @Query(value = """
        SELECT id, patient_id, record_type, record_date, content,
               1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity_score
        FROM medical_record_embeddings
        WHERE patient_id = :patientId
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<SimilarRecord> findSimilarByPatient(
        @Param("patientId") String patientId,
        @Param("queryEmbedding") float[] queryEmbedding,
        @Param("limit") int limit
    );

    /**
     * WITH THRESHOLD — Low-quality matches exclude karo
     * Healthcare mein important: garbage answer nahi chahiye
     */
    @Query(value = """
        SELECT id, patient_id, record_type, record_date, content,
               1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity_score
        FROM medical_record_embeddings
        WHERE patient_id = :patientId
          AND 1 - (embedding <=> CAST(:queryEmbedding AS vector)) > :threshold
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<SimilarRecord> findSimilarWithThreshold(
        @Param("patientId") String patientId,
        @Param("queryEmbedding") float[] queryEmbedding,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );
}

// Projection interface — sirf zaruri fields fetch
public interface SimilarRecord {
    UUID   getId();
    String getRecordType();
    LocalDate getRecordDate();
    String getContent();
    Double getSimilarityScore();
}

// Entity with pgvector column:
@Entity
@Table(name = "medical_record_embeddings")
public class MedicalRecordEmbedding {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "record_type")
    private String recordType;  // LAB_RESULT, PRESCRIPTION, DIAGNOSIS

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;  // Original human-readable text

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;  // 1536 floats — pgvector stores this

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;  // JSON for extra info
}

SCENARIO QUESTION: "IVFFlat vs HNSW — kab kaunsa?"
IVFFlat: Fast build, less memory, moderate recall — small/medium datasets
HNSW: Slow build, more memory, best recall — large production datasets (>100K)
"Maine initially IVFFlat use kiya (50K records). 200K pe cross karne ke baad HNSW pe migrate kiya."

================================================================================
SECTION 5: RAG PIPELINE — COMPLETE IMPLEMENTATION
================================================================================

Q4. RAG (Retrieval Augmented Generation) explain karo

STORY: Doctor aur Medical File analogy
WITHOUT RAG:
  Patient: "Mera blood sugar normal hai?"
  GPT-4: "Based on general medical standards..." (HALLUCINATION — no real data!)

WITH RAG:
  Patient: "Mera blood sugar normal hai?"
  System: DB se patient ke blood test records fetch karo → Context banao
  GPT-4 + Context: "Your last blood test (Jan 15, 2025) showed fasting glucose
                    at 126 mg/dL which is above the normal range of 70-100 mg/dL.
                    Please consult your doctor."  <- ACCURATE, SOURCE-BASED!

WHY RAG > FINE-TUNING for healthcare:
| Aspect          | Fine-tuning              | RAG                          |
|-----------------|--------------------------|------------------------------|
| Patient-specific| No (general knowledge)   | Yes (real-time fetch)        |
| Data freshness  | Training date tak        | Always fresh                 |
| Privacy         | Data model mein bake     | Data DB mein secure rehta    |
| Cost            | Very expensive ($$$)     | Inference cost only          |
| Updates         | Retrain karna padega     | DB update karo — done        |

// ============================================================
// COMPLETE RAG PIPELINE SERVICE
// ============================================================

@Service
public class RAGPipelineService {

    @Autowired private EmbeddingService embeddingService;
    @Autowired private MedicalRecordEmbeddingRepository vectorRepo;
    @Autowired private OpenAiService openAiService;

    private static final double SIMILARITY_THRESHOLD = 0.72; // 72% minimum
    private static final int MAX_CONTEXT_CHARS = 6000;        // Token budget

    /**
     * Main RAG method — Patient ka question answer karo
     * Flow: Question → Embed → Search → Context → Prompt → LLM → Answer
     */
    public RAGResponse answerPatientQuery(String patientId, String question) {

        // STEP 1: Question embedding generate karo
        float[] questionEmbedding = embeddingService.embed(question);

        // STEP 2: pgvector se relevant records dhundo
        List<SimilarRecord> similarRecords = vectorRepo.findSimilarWithThreshold(
            patientId, questionEmbedding, SIMILARITY_THRESHOLD, 5);

        // Koi relevant record nahi mila?
        if (similarRecords.isEmpty()) {
            return RAGResponse.noDataFound();
        }

        // STEP 3: Context build karo from retrieved records
        String context = buildContext(similarRecords);

        // STEP 4: Prompt build karo (System + User messages)
        List<ChatMessage> messages = buildPromptMessages(question, context);

        // STEP 5: OpenAI GPT-4 call karo
        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
            .model("gpt-4-turbo-preview")
            .messages(messages)
            .maxTokens(600)     // Response length limit karo
            .temperature(0.2)   // Low = factual, less creative
            .topP(0.9)          // Standard nucleus sampling
            .build();

        ChatCompletionResult result = openAiService.createChatCompletion(chatRequest);
        String answer = result.getChoices().get(0).getMessage().getContent();

        // STEP 6: Response with metadata return karo
        return RAGResponse.builder()
            .answer(answer)
            .confidence(similarRecords.get(0).getSimilarityScore())
            .sourcesUsed(similarRecords.size())
            .build();
    }

    /**
     * Records ko LLM ke liye readable format mein convert karo
     * Token limit ke andar raho!
     */
    private String buildContext(List<SimilarRecord> records) {
        StringBuilder ctx = new StringBuilder();
        for (SimilarRecord r : records) {
            String entry = String.format("=== %s (Date: %s, Match: %.0f%%) ===\n%s\n\n",
                r.getRecordType(), r.getRecordDate(),
                r.getSimilarityScore() * 100, r.getContent());

            if (ctx.length() + entry.length() > MAX_CONTEXT_CHARS) break; // Token budget
            ctx.append(entry);
        }
        return ctx.toString();
    }

    /**
     * Prompt messages — System behavior define + User question inject
     */
    private List<ChatMessage> buildPromptMessages(String question, String context) {

        // SYSTEM PROMPT — LLM ka role, rules, constraints
        String systemPrompt = """
            You are a helpful healthcare assistant for patients.
            You have access to the patient's medical records below.

            STRICT RULES:
            1. Answer ONLY from the provided context — never make up information
            2. If not in context → say: "I don't have that in your records"
            3. Never diagnose conditions or recommend medications
            4. Always suggest consulting a doctor for medical decisions
            5. Be empathetic, clear, concise
            6. Cite the date and record type when mentioning specific values
            """;

        // USER PROMPT — Context + Question inject karo
        String userPrompt = String.format("""
            PATIENT MEDICAL RECORDS:
            ──────────────────────────
            %s
            ──────────────────────────

            PATIENT QUESTION: %s

            Answer strictly based on the records above.
            """, context, question);

        return List.of(
            new ChatMessage("system", systemPrompt),
            new ChatMessage("user", userPrompt)
        );
    }
}

================================================================================
SECTION 6: CHUNKING — DOCUMENTS KO PIECES MEIN TORNA
================================================================================

Q5. Chunking kya hota hai? Healthcare mein kaise use kiya?

STORY — Library analogy:
500-page medical book = ek embedding → information blend ho jaati hai
500-page book → chapters → pages → paragraphs = better search!

TYPES OF CHUNKING:

// Fixed Size Chunking (simple):
// "patient has diab..." — sentence beech mein cut — BAD for medical

// Semantic / Structured Chunking (BEST for healthcare):
// Medical records already structured — sections preserve karo

@Service
public class MedicalRecordChunker {

    /**
     * Lab report ko meaningful chunks mein toro
     * Each test = separate chunk (specific questions easy dhundhe jaayein)
     */
    public List<MedicalChunk> chunkLabReport(LabReport report) {
        List<MedicalChunk> chunks = new ArrayList<>();

        // CHUNK 1: Overall summary — report ka context
        chunks.add(new MedicalChunk("LAB_SUMMARY",
            String.format("Lab report dated %s. Ordered by Dr. %s. Summary: %s",
                report.getDate(), report.getOrderedBy(), report.getSummary()),
            report.getPatientId()
        ));

        // CHUNK 2-N: Har test separately — specific test dhundna easy!
        for (LabTest test : report.getTests()) {
            String chunk = String.format(
                "Lab Test: %s | Value: %s %s | Normal Range: %s | Status: %s | Date: %s | Notes: %s",
                test.getTestName(),   // "Hemoglobin"
                test.getValue(),      // "11.2"
                test.getUnit(),       // "g/dL"
                test.getNormalRange(),// "12.0-17.5 g/dL"
                test.getStatus(),     // "LOW" / "NORMAL" / "HIGH"
                report.getDate(),
                test.getNotes()
            );
            chunks.add(new MedicalChunk("LAB_RESULT", chunk, report.getPatientId()));
        }
        return chunks;
    }

    /**
     * Prescription ko chunk karo
     */
    public List<MedicalChunk> chunkPrescription(Prescription rx) {
        List<MedicalChunk> chunks = new ArrayList<>();

        // Header chunk — overall context
        chunks.add(new MedicalChunk("PRESCRIPTION_HEADER",
            String.format("Prescription by Dr. %s on %s for %s",
                rx.getDoctorName(), rx.getDate(), rx.getDiagnosis()),
            rx.getPatientId()
        ));

        // Each medication separately
        for (Medication med : rx.getMedications()) {
            chunks.add(new MedicalChunk("MEDICATION",
                String.format("Medicine: %s | Dose: %s | Frequency: %s | Duration: %s | Instructions: %s | For: %s | Date: %s",
                    med.getName(), med.getDosage(), med.getFrequency(),
                    med.getDuration(), med.getInstructions(), rx.getDiagnosis(), rx.getDate()),
                rx.getPatientId()
            ));
        }
        return chunks;
    }
}

// Indexing Service — Kafka event se trigger, batch embed + save:
@Service
public class RecordIndexingService {

    @KafkaListener(topics = "lab-report-created")
    public void indexLabReport(LabReportCreatedEvent event) {
        LabReport report = labReportRepo.findById(event.getReportId()).orElseThrow();
        List<MedicalChunk> chunks = chunker.chunkLabReport(report);

        // BATCH EMBED — ek API call mein sab (cost + latency save)
        List<String> texts = chunks.stream().map(MedicalChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        List<MedicalRecordEmbedding> records = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            records.add(MedicalRecordEmbedding.builder()
                .patientId(chunks.get(i).getPatientId())
                .recordType(chunks.get(i).getType())
                .content(chunks.get(i).getContent())
                .embedding(embeddings.get(i))  // Corresponding embedding
                .recordDate(report.getDate())
                .build());
        }
        repo.saveAll(records); // Batch insert — one DB round trip
    }
}

================================================================================
SECTION 7: PROMPT ENGINEERING — ADVANCED PATTERNS
================================================================================

Q6. Prompt Engineering — Kya kya patterns use kiye?

// ============================================================
// PATTERN 1: ROLE + RULES + CONTEXT + QUERY (Healthcare standard)
// ============================================================
String systemPrompt = """
    You are a compassionate healthcare assistant.

    YOUR ROLE: Help patients understand their medical records.
    Explain medical terms simply. Be factual and empathetic.

    STRICT BOUNDARIES:
    - NEVER diagnose conditions
    - NEVER recommend medications
    - If not in records → "I don't have that information"
    - Always end: "Please consult your doctor for medical advice"
    """;

// ============================================================
// PATTERN 2: CHAIN OF THOUGHT — Complex questions
// LLM ko step-by-step sochne ke liye force karo
// ============================================================
String chainOfThoughtPrompt = """
    Think step by step before answering:
    Step 1: What exactly is being asked?
    Step 2: What relevant data is in the records?
    Step 3: Connect and analyze the information.
    Step 4: Formulate a safe, clear answer.

    Show brief reasoning, then give the final answer.
    """;

// ============================================================
// PATTERN 3: FEW-SHOT PROMPTING
// Examples deke better format/quality achieve karo
// ============================================================
String fewShotSystem = """
    You are a healthcare assistant. Follow these examples:

    Q: "When was my last blood test?"
    A: "Your most recent blood test was January 15, 2025 ordered by Dr. Sharma.
        Hemoglobin: 11.2 g/dL (below normal 12.0-17.5). Discuss with your doctor."

    Q: "What medicines am I taking?"
    A: "Based on your records (Feb 10, 2025 prescription):
        1. Amlodipine 5mg — once daily (hypertension)
        2. Metformin 500mg — twice daily with meals
        Please verify these are still current with your doctor."

    Follow this format.
    """;

// ============================================================
// TEMPERATURE + OTHER PARAMS — Healthcare settings
// ============================================================

// TEMPERATURE (0.0 to 2.0) — creativity vs determinism
// 0.0 = Same input → Always same output (too rigid)
// 0.1 = Our choice — slightly varied phrasing, factually same (BEST for healthcare)
// 0.7 = Default — creative, varied
// 1.5+ = Very creative (poems, stories — NOT for medical!)

// TOP-P (0.0 to 1.0) — token selection range
// 0.9 = Top 90% probable tokens consider karo (standard)

// MAX TOKENS — Response length limit
// 1 token ≈ 4 chars / 0.75 words
// maxTokens = 600 → ~450 words (good for medical summaries)

ChatCompletionRequest request = ChatCompletionRequest.builder()
    .model("gpt-4-turbo-preview")
    .messages(messages)
    .temperature(0.1)      // Low — factual, consistent
    .topP(0.9)             // Standard
    .maxTokens(600)        // Cost control + concise answers
    .frequencyPenalty(0.3) // Repetition reduce karo
    .build();

================================================================================
SECTION 8: STREAMING RESPONSES
================================================================================

Q7. Streaming kya hota hai? Kaise implement kiya?

WITHOUT STREAMING: 5-8 sec wait → poora text ek saath
WITH STREAMING:    300ms → pehla word → words aate rehte hain (typing effect)

// Server-Sent Events (SSE) — Spring Boot + Reactor

@RestController
@RequestMapping("/api/ai")
public class AIQueryController {

    @GetMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamQuery(
            @RequestParam String patientId,
            @RequestParam String question) {

        return streamingService.streamAnswer(patientId, question)
            .map(token -> ServerSentEvent.<String>builder()
                .data(token)       // Har token ek SSE event
                .build())
            .concatWith(Flux.just(ServerSentEvent.<String>builder()
                .event("done").data("[DONE]").build())); // Stream end signal
    }
}

@Service
public class StreamingRAGService {

    public Flux<String> streamAnswer(String patientId, String question) {
        return Flux.create(emitter -> {
            // Retrieval (same as non-streaming)
            float[] embedding = embeddingService.embed(question);
            List<SimilarRecord> records = vectorRepo.findSimilarWithThreshold(
                patientId, embedding, 0.72, 5);

            if (records.isEmpty()) {
                emitter.next("I don't have specific records for this question.");
                emitter.complete();
                return;
            }

            List<ChatMessage> messages = buildMessages(question, records);

            // STREAMING API CALL — tokens ek ek karke aate hain
            openAiService.streamChatCompletion(
                ChatCompletionRequest.builder()
                    .model("gpt-4-turbo-preview")
                    .messages(messages)
                    .temperature(0.1)
                    .stream(true)   // KEY: streaming enable!
                    .build()
            ).subscribe(
                chunk -> {
                    String token = chunk.getChoices().get(0).getMessage().getContent();
                    if (token != null) emitter.next(token); // Client ko bhejo
                },
                emitter::error,   // Error handle
                emitter::complete // Stream done
            );
        });
    }
}

================================================================================
SECTION 9: HALLUCINATION PREVENTION
================================================================================

Q8. Hallucination kya hota hai? Healthcare mein kaise prevent kiya?

STORY — Dangerous scenario:
Patient: "Kya mujhe diabetes hai?"
BAD GPT response (no context): "Based on your blood sugar of 126 mg/dL..."
WHERE DID 126 COME FROM? — Records nahi dekhe, MADE UP! DANGEROUS!

// PREVENTION TECHNIQUES:

// TECHNIQUE 1: Prompt mein explicit rules
"CRITICAL: Answer ONLY from provided records.
 If not in records — say exactly: 'I don't have that information.'
 Do NOT use general medical knowledge to fill gaps."

// TECHNIQUE 2: Confidence-based response handling
@Service
public class HallucinationSafetyService {

    private static final double HIGH_CONFIDENCE   = 0.85;
    private static final double MEDIUM_CONFIDENCE = 0.72;
    private static final double LOW_CONFIDENCE    = 0.60;

    public SafeRAGResponse safeAnswer(String patientId, String question) {
        float[] emb = embeddingService.embed(question);
        List<SimilarRecord> records = vectorRepo.findSimilarWithThreshold(
            patientId, emb, LOW_CONFIDENCE, 5);

        double bestScore = records.isEmpty() ? 0.0
            : records.get(0).getSimilarityScore();

        // No relevant records — refuse to answer
        if (records.isEmpty() || bestScore < LOW_CONFIDENCE) {
            return SafeRAGResponse.noData(
                "I don't have relevant records to answer this. Please consult your doctor.");
        }

        // Low confidence — answer with strong disclaimer
        if (bestScore < MEDIUM_CONFIDENCE) {
            return SafeRAGResponse.lowConfidence(
                generateAnswer(records, question),
                "Match confidence is low. Please verify with your doctor."
            );
        }

        // High confidence — normal response
        return SafeRAGResponse.high(
            generateAnswer(records, question),
            "Always consult your doctor for medical decisions."
        );
    }
}

// TECHNIQUE 3: Response validation — dangerous phrases detect karo
@Service
public class ResponseValidator {

    private static final List<String> DANGEROUS_PHRASES = List.of(
        "you should take",
        "i recommend taking",
        "increase your dosage",
        "stop taking your medication",
        "you are diagnosed with"
    );

    public ValidationResult validate(String llmResponse) {
        for (String phrase : DANGEROUS_PHRASES) {
            if (llmResponse.toLowerCase().contains(phrase)) {
                return ValidationResult.invalid(
                    "Response contains dangerous medical advice.",
                    "I cannot provide specific medical recommendations. " +
                    "Please consult your doctor for personalized advice."
                );
            }
        }
        return ValidationResult.valid(llmResponse);
    }
}

================================================================================
SECTION 10: CACHING FOR AI PERFORMANCE
================================================================================

Q9. AI System mein caching kaise kiya?

// LEVEL 1: Embedding cache — same text = same vector
@Service
public class CachedEmbeddingService {

    @Cacheable(value = "embeddings",
               key = "T(org.apache.commons.codec.digest.DigestUtils).md5Hex(#text)")
    public float[] embed(String text) {
        return embeddingService.embed(text); // Sirf pehli baar API call
    }

    // Startup pe common queries pre-warm karo
    @PostConstruct
    public void prewarm() {
        List<String> common = List.of(
            "What medications am I taking?",
            "When was my last blood test?",
            "What is my blood pressure history?"
        );
        embeddingService.embedBatch(common); // Pre-compute embeddings
        log.info("Pre-warmed {} common query embeddings", common.size());
    }
}

// LEVEL 2: Result cache — same question + same patient = same context
@Cacheable(value = "ragResults",
           key = "#patientId + '_' + T(o.a.c.c.d.DigestUtils).md5Hex(#question)")
public List<SimilarRecord> findRelevantRecords(String patientId, String question) {
    float[] emb = embeddingService.embed(question); // Cached embedding
    return vectorRepo.findSimilarWithThreshold(patientId, emb, 0.72, 5);
}

// New record aaya → cache invalidate karo
@CacheEvict(value = "ragResults", allEntries = true)
@KafkaListener(topics = "medical-record-created")
public void invalidateOnNewRecord(MedicalRecordCreatedEvent event) {
    log.info("RAG cache invalidated for patient: {}", event.getPatientId());
}

// application.yml:
// spring.cache.type=redis
// spring.cache.redis.time-to-live=3600000   # 1 hour TTL

================================================================================
SECTION 11: COST OPTIMIZATION
================================================================================

Q10. OpenAI Cost Control kaise kiya?

// STRATEGY 1: Smart model selection
@Service
public class ModelSelector {
    public String selectModel(String question, double bestSimilarity) {
        // Simple factual query + high confidence → cheaper model
        if (isSimpleQuery(question) && bestSimilarity > 0.90) {
            return "gpt-3.5-turbo";  // 20x cheaper than GPT-4!
        }
        return "gpt-4-turbo-preview"; // Complex reasoning
    }

    private boolean isSimpleQuery(String q) {
        return q.contains("when") || q.contains("what date") || q.contains("last visit");
    }
}

// STRATEGY 2: Context compression — token count kam karo
private String compressContext(List<SimilarRecord> records) {
    return records.stream()
        .map(r -> String.format("%s|%s|%s",
            r.getRecordType().substring(0, 3), // "LAB" not "LAB_RESULT"
            r.getRecordDate(),
            summarize(r.getContent()))) // Full text → key points
        .collect(Collectors.joining("\n"));
    // ~70% token reduction
}

// STRATEGY 3: Token usage tracking
@Component
public class CostTracker {
    private final Counter tokenCounter;

    public CostTracker(MeterRegistry registry) {
        this.tokenCounter = Counter.builder("openai.tokens.used")
            .tag("model", "gpt4").register(registry);
    }

    public void track(ChatCompletionResult result) {
        tokenCounter.increment(result.getUsage().getTotalTokens());
        // Prometheus → Grafana → Monthly cost dashboard
    }
}

================================================================================
SECTION 12: ADVANCED — FUNCTION CALLING
================================================================================

Q11. Function Calling kya hota hai?

STORY: Normal: Patient asks → AI answers from context
       Function Calling: Patient asks → AI calls your APIs → AI answers with live data

// LLM ko tools define karo:
ChatFunction getAppointments = ChatFunction.builder()
    .name("get_upcoming_appointments")
    .description("Fetch patient's upcoming medical appointments from the system")
    .parameters(ChatFunctionParameters.builder()
        .type("object")
        .properties(Map.of(
            "days_ahead", Map.of(
                "type", "integer",
                "description", "How many days ahead to look (default 30)"
            )
        ))
        .build())
    .build();

// First call — LLM decide karta hai function call karna hai ya nahi
ChatCompletionRequest first = ChatCompletionRequest.builder()
    .model("gpt-4-turbo-preview")
    .messages(List.of(new ChatMessage("user", "When is my next appointment?")))
    .functions(List.of(getAppointments))
    .functionCall("auto")  // LLM decide kare
    .build();

ChatCompletionResult result1 = openAiService.createChatCompletion(first);
ChatMessage assistantMsg = result1.getChoices().get(0).getMessage();

if (assistantMsg.getFunctionCall() != null) {
    // LLM ne function call karna decide kiya
    String funcName = assistantMsg.getFunctionCall().getName();
    String funcResult = executeFunction(funcName, patientId); // Actual data fetch

    // Second call — function result ke saath
    ChatCompletionResult final = openAiService.createChatCompletion(
        ChatCompletionRequest.builder()
            .model("gpt-4-turbo-preview")
            .messages(List.of(
                new ChatMessage("user", "When is my next appointment?"),
                assistantMsg,  // LLM ka function call request
                new ChatMessage("function", funcResult, funcName) // Actual data
            )).build()
    );
    return final.getChoices().get(0).getMessage().getContent(); // Final answer
}

================================================================================
SECTION 13: SCENARIO BASED QUESTIONS
================================================================================

SCENARIO 1: "AI System ne galat medical info diya"
ANSWER:
"Healthcare mein yeh critical hai. Maine safeguards implement kiye:
1. Similarity threshold 0.72 — low confidence pe answer refuse karo
2. Dangerous phrases validator — 'you should take' jaise phrases block
3. Always disclaimer — 'consult your doctor'
4. If incident hota: specific response flag karo, AI temporarily disable,
   patient ko notify karo. Phir investigate: kaunsa chunk retrieve hua?
   Similarity score kya tha? Prompt issue tha ya retrieval? Fix karo,
   monitoring add karo."

SCENARIO 2: "pgvector search slow hai 1M records pe"
ANSWER:
"Steps:
1. EXPLAIN ANALYZE run karo — index use ho raha hai?
2. IVFFlat → HNSW migrate karo (better at scale)
3. Partition table by patient_id — search space smaller
4. Cache hot queries in Redis — same embedding baar baar search mat karo
5. Read replica pe vector search run karo — primary pe writes"

SCENARIO 3: "OpenAI API down hai — kya fallback hai?"
ANSWER:
"Circuit breaker lagaya hai:
1. OpenAI down → Ollama + Llama-3 (local LLM, privacy ke liye bhi better)
2. Local LLM bhi unavailable → Simplified: vector search results directly show karo
3. Graceful degradation message — 'AI temporarily unavailable, showing records directly'
4. Retry with exponential backoff for transient failures"

================================================================================
QUICK CHEAT SHEET — AI INTERVIEW
================================================================================

TERM                 → SIMPLE EXPLANATION
-------------------    -------------------------------------------------------
Embedding           → Text ko numbers mein convert karo (semantic fingerprint)
Vector              → Numbers ka ordered list (position in semantic space)
Cosine similarity   → Angle between vectors (text similarity, length-independent)
pgvector            → PostgreSQL extension for vector operations
RAG                 → Retrieve relevant docs + feed to LLM = accurate answers
Chunking            → Document ko meaningful pieces mein toro
IVFFlat             → Approximate nearest neighbor (moderate speed/accuracy)
HNSW                → Better ANN, higher recall, more memory (production)
Temperature 0.1     → Factual, deterministic (healthcare standard)
Hallucination       → LLM making up facts not in training/context
Few-shot            → Examples deke LLM ko guide karo
Chain-of-thought    → Step-by-step reasoning force karo
Function calling    → LLM apne APIs call kar sakta hai
Streaming           → Tokens ek ek karke bhejo (typing effect)
Context window      → LLM ek baar mein kitna process kar sakta hai
Token               → ~4 chars / LLM ki basic processing unit
RAG vs Fine-tuning  → RAG=fresh data; Fine-tuning=static baked knowledge

================================================================================
END — AI SECTION | EPAM Healthcare Project
================================================================================
