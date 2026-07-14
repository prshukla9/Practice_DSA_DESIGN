CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS patient;
CREATE SCHEMA IF NOT EXISTS consent;
CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE IF NOT EXISTS ai.patient_context_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id VARCHAR(64) NOT NULL,
    consent_id VARCHAR(64) NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1536),
    source_resource VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_embeddings_patient_consent
    ON ai.patient_context_embeddings (patient_id, consent_id);

CREATE INDEX IF NOT EXISTS idx_embeddings_vector
    ON ai.patient_context_embeddings USING ivfflat (embedding vector_cosine_ops);
