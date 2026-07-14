# Interview Prep — Hinglish Quick-Recall Blocks

Har block ek chhota chunk hai — padho, samjho, yaad rakho. Full detail wale doc (`Senior_Java_Backend_Interview_Guide.md`) ke saath isko revision ke liye use karo.

Yaad rahe: **infra/cloud decisions kabhi "maine kiya" mat bolna** — hamesha "DevOps/Cloud team ne owned kiya, maine application side se use/consume kiya" bolna.

---

## 🎤 BLOCK 1: Elevator Pitch (ratta maar lo, bolna aana chahiye)

"Maine ek healthcare platform pe kaam kiya jo multiple hospitals ko connect karta tha — taaki patient ka data (records, prescriptions, consent) hospitals ke beech securely share ho sake. Backend Spring Boot microservices mein bana tha — patient service, doctor service, consent service, document exchange — REST se sync communication aur Kafka se async events. Data exchange FHIR standard follow karta tha. Ek AI assistant bhi tha jo RAG use karke Azure OpenAI se real patient data ke basis pe answer deta tha, hallucination avoid karne ke liye grounded tha. Poora system Kubernetes + AWS pe chalta tha, jo DevOps team manage karti thi — main application layer pe focus karta tha: APIs banana, bugs fix karna, performance improve karna."

**Yaad rakhne ka trick:** 5 words — *Hospitals connect, FHIR data, Kafka events, AI grounded, DevOps infra.*

---

## 🏗️ BLOCK 2: Architecture — Kaunse Services Hain

| Service | Kaam |
|---|---|
| Patient Service | patient info, registration |
| Doctor Service | doctor profile, availability |
| Appointment Service | booking, Kafka events publish |
| Consent Service | FHIR Consent — kisko access diya |
| Document Exchange | consent check karke document bhejna |
| Notification Service | Kafka consume karke email/SMS |
| AI Assistant | RAG based Q&A |

**Yaad rakhne ka trick:** *"Patient-Doctor milte hain Appointment mein, Consent se Document jaata hai, Notification batata hai, AI answer deta hai."*

---

## 🔐 BLOCK 3: Auth Flow (5 steps, order yaad rakho)

1. User login → JWT milta hai
2. Gateway token validate karta hai
3. Har service Spring Security se JWT verify karti hai (locally)
4. Role check (`ROLE_DOCTOR` waghera)
5. **Consent check** — sirf role kaafi nahi, specific patient ke liye active consent bhi chahiye

**Yaad rakhne ka trick:** *"Login → Gateway → Service → Role → Consent"* (LGSRC — "Log Sabko Roko Consent se")

---

## 🔄 BLOCK 4: API Flow Example (doctor record maangta hai)

Doctor → Gateway → Document Service → **Consent Service se check** ("active consent hai?") → agar haan, document fetch → Doctor ko response → background mein audit log + Kafka event

**Yaad rakhne ka trick:** Consent check hamesha **data release se pehle** hoti hai, kabhi baad mein nahi.

---

## 📋 BLOCK 5: FHIR — Kyun Use Kiya

Purana tarika: HL7 v2 (pipe-delimited, parse karna mushkil) ya har hospital ka apna format.
FHIR: REST-based standard, JSON resources, sab hospitals same format samajh sakte hain.

**Yaad rakhne ka trick:** *"FHIR = universal language jisse alag alag hospital systems ek dusre se baat kar sakein."*

---

## 📦 BLOCK 6: FHIR Ke 6 Resources (ek-ek line mein)

| Resource | Ek line mein |
|---|---|
| **Patient** | naam, DOB, ID — basic info |
| **Consent** | kisko, kya data, kab tak access diya |
| **DocumentReference** | document ka pointer (discharge summary waghera) |
| **MedicationRequest** | prescription — kya dawai chal rahi hai |
| **Observation** | ek lab result/vital measurement |
| **Encounter** | ek visit/admission ka record |

**Yaad rakhne ka trick:** *"PC-DMOE"* — Patient, Consent, Document, Medication, Observation, Encounter.

---

## 🏥 BLOCK 7: Cross-Hospital Document Sharing Flow

1. Patient consent deta hai portal pe
2. Hospital B patient dhoondta hai
3. System check karta hai: active consent hai Hospital B ke liye?
4. Haan → Hospital A ke FHIR endpoint se document fetch
5. Doctor ko FHIR Bundle return hota hai
6. Consent revoke → turant access band

**Yaad rakhne ka trick:** *"Consent revoke = turant lock, kal se nahi, abhi se."*

---

## 📨 BLOCK 8: Kafka — Core Concepts (rapid fire)

- **Producer/Consumer:** jo bhejta hai / jo sunta hai
- **Topic example:** `consent.revoked`, `appointment.booked`, `document.shared`
- **Retry + DLQ:** fail hone pe retry, phir bhi fail toh Dead Letter Topic mein bhej do (drop mat karo)
- **Consumer Group:** multiple pods, ek partition ek hi consumer padhta hai — no duplicate
- **Ordering:** `patientId` se partition key — ek patient ke events order mein hi aayenge
- **Idempotency:** duplicate message aaye toh bhi dobara process na ho — `eventId` check karo pehle

**Yaad rakhne ka trick:** *"Order patient-wise, duplicate se bacho eventId se, fail ho toh DLQ mein daalo."*

---

## 🤖 BLOCK 9: RAG (AI Assistant) — Poora Flow 5 Steps Mein

1. **Ingestion + Chunking** — document ko chhote pieces mein todna
2. **Embedding** — har chunk ko vector (numbers) mein convert karna (Azure OpenAI embedding model)
3. **Storage** — pgvector (Postgres ke andar hi) mein store karna
4. **Similarity Search** — query ka vector banao, sabse relevant chunks dhoondo
5. **Prompt + Generate** — retrieved context + question ko Azure OpenAI ko bhejo, low temperature pe answer lo, citation ke saath

**Yaad rakhne ka trick:** *"Chunk → Embed → Store → Search → Answer"* (CESSA — yaad rakhne ke liye "Chess-A")

---

## ❓ BLOCK 10: RAG vs Fine-Tuning (Why RAG?)

| Point | RAG jeetta hai kyun |
|---|---|
| Freshness | naya document aaya toh bas embed karo, retrain nahi karna |
| Traceability | source dikha sakte ho, fine-tuned model nahi bata sakta kahan se aaya |
| Data isolation | patient data model ke weights mein bake nahi hota, sirf retrieval time pe use hota hai |
| Cost | naya document embed karna sasta, model retrain karna mehenga |

**Yaad rakhne ka trick:** *"RAG = fresh + traceable + safe + cheap."*

---

## 🚫 BLOCK 11: Hallucination Kaise Roka (5 tarike)

1. Sirf retrieved context se answer do, apni knowledge se nahi
2. Prompt mein guardrail — "pata na ho toh 'nahi pata' bolo"
3. Low temperature — random/creative answer kam
4. Har answer ke saath source citation
5. Confidence threshold — agar relevant kuch mila hi nahi, toh "insufficient info" bolo

**Yaad rakhne ka trick:** *"Context-only, Guardrail, Low-temp, Citation, Threshold"* — **CGLCT**

---

## 🎯 BLOCK 12: Agentic AI Hai Ya Nahi? (Ye Interview Mein Zaroor Poochenge)

**Honest answer: NAHI, ye Agentic AI nahi hai — ye RAG hai.**

Agentic AI ka matlab: LLM khud decide karta hai kaunsa tool kab use karna hai, multi-step planning karta hai.
Yaha jo hai: ek **fixed pipeline** — embed → retrieve → prompt → answer. Sequence hardcoded hai, model decide nahi karta.

Agar routing hai (jaise: vector search use karu ya seedha FHIR API call karu) — wo bhi simple rule-based code hai, LLM khud nahi choose kar raha.

**Yaad rakhne ka trick:** *"Fixed pipeline = RAG. Model khud decide kare next step kya = Agentic. Humara fixed hai."*

---

## 📖 BLOCK 13: Production Stories — Sirf Practice Ke Liye (Sach Bolna Zaroori)

⚠️ Ye 4 stories **template hain, verified fact nahi**. Sirf structure/depth samajhne ke liye. Agar tumhara khud ka real chhota experience hai, wahi use karo — jhooti detail follow-up questions mein pakdi jaayegi.

Har story ka structure yaad rakho:
**Symptom → Investigation (logs/metrics) → Root Cause → Fix → Deploy → Validation → Lesson**

Quick examples (practice ke liye):
1. **Performance:** slow search → missing index → trigram index add kiya → 2.5s se 180ms
2. **Bug:** duplicate notification → idempotency missing → eventId check add kiya
3. **API optimization:** slow availability lookup → Redis cache add kiya → 450ms se 90ms
4. **Incident:** consent service crash → missing config → config fix + fail-fast validation add kiya

**Yaad rakhne ka trick:** *"S-I-R-F-D-V-L"* — Symptom, Investigation, Root cause, Fix, Deploy, Validate, Lesson.

---

## ☁️ BLOCK 14: AWS — Backend Developer Angle Se (6 Services, 1-Line Each)

| Service | Backend dev angle |
|---|---|
| **EC2** | indirect — EKS nodes isi pe chalte hain, direct control nahi tha |
| **S3** | document binaries store karne ke liye, IAM role se access, pre-signed URLs |
| **RDS Postgres** | managed DB, HikariCP pooling, TLS connection, credentials secrets manager se |
| **IAM** | IRSA se pod ko scoped role milta hai — hardcoded keys kabhi nahi |
| **CloudWatch** | structured logs + metrics, PHI kabhi log mein nahi likhna |
| **EKS + ECR** | Dockerfile likha, health checks add kiye, graceful shutdown handle kiya |

**Golden rule sabke liye:** *"Maine use kiya aur apni service configure ki, infra provision DevOps ne kiya."*

**Yaad rakhne ka trick:** *"E-S-R-I-C-E"* — EC2, S3, RDS, IAM, CloudWatch, EKS.

---

## 🗣️ BLOCK 15: Rapid-Fire Q&A (Hinglish Mein Bole Jaane Wale Jawab)

**Q: Microservices kyun, monolith kyun nahi?**
A: Har domain (patient/consent/document) alag rate se change hota hai aur alag security-sensitivity hai — isliye independently deploy/scale karna better tha.

**Q: At-least-once delivery ka matlab kya hai aur aapne kaise handle kiya?**
A: Message dobara bhi aa sakta hai — isliye consumer idempotent banaya, eventId check karke.

**Q: Consent revoke ho jaye toh kya hota hai turant?**
A: Har request pe fresh check hota hai, cached nahi rehta — revoke hote hi agli request fail ho jaati hai.

**Q: RAG mein doosre patient ka data leak kaise rukta hai?**
A: Similarity search khud patientId aur consent scope se pehle hi filter karta hai — cross-patient retrieval possible hi nahi.

**Q: Aapne infra design kiya?**
A: Nahi — DevOps/Cloud team ne infra design/provision kiya, maine application side se use kiya, Dockerfile/health-checks/config apne service ke liye likhe.

---

## ✅ Final Revision Checklist (interview se pehle)

- [ ] Elevator pitch bina ruke bol sakta hoon
- [ ] 6 FHIR resources ek saans mein bata sakta hoon
- [ ] Kafka ordering + idempotency ka reason bata sakta hoon
- [ ] RAG ka 5-step flow bol sakta hoon
- [ ] Agentic AI wala honest answer clear hai
- [ ] Kam se kam 1 real story (chhoti hi sahi) confidently bata sakta hoon
- [ ] AWS ke 6 services mein "maine use kiya" vs "DevOps ne banaya" ka farak clear hai
