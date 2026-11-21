package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.IdempotencyKey;
import com.jxxerick.customerapi.domain.model.IdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(IdempotencyKey key);

    /** Inserts or replaces the record of the key. */
    void save(IdempotencyRecord record);

    /** Removes records created before {@code cutoff}; returns how many. */
    int deleteCreatedBefore(Instant cutoff);
}
