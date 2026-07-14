package com.healthcare.ai.repository;

import com.healthcare.ai.entity.PatientContextEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PatientContextEmbeddingRepository extends JpaRepository<PatientContextEmbedding, UUID> {

    @Query(value = "SELECT chunk_text FROM ai.patient_context_embeddings " +
            "WHERE patient_id = :patientId AND consent_id = :consentId " +
            "ORDER BY embedding <-> cast(:queryEmbedding as vector) LIMIT :limit",
            nativeQuery = true)
    List<String> findSimilarChunks(@Param("patientId") String patientId,
                                   @Param("consentId") String consentId,
                                   @Param("queryEmbedding") String queryEmbedding,
                                   @Param("limit") int limit);
}
