package com.healthcare.consent.repository;

import com.healthcare.consent.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {
    Optional<Consent> findByIdAndStatus(UUID id, Consent.Status status);
}
