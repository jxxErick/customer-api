package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

interface SpringDataIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query("delete from IdempotencyKeyEntity k where k.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
