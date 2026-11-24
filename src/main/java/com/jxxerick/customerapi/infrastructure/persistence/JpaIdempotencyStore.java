package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.IdempotencyKey;
import com.jxxerick.customerapi.domain.model.IdempotencyRecord;
import com.jxxerick.customerapi.domain.port.IdempotencyStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Adapter from Spring Data JPA to the {@link IdempotencyStore} port. */
@Repository
class JpaIdempotencyStore implements IdempotencyStore {

    private final SpringDataIdempotencyKeyRepository jpa;

    JpaIdempotencyStore(SpringDataIdempotencyKeyRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        return jpa.findById(key.value())
                .map(e -> new IdempotencyRecord(new IdempotencyKey(e.getKey()), e.getRequestHash(), e.getCustomerId(),
                        e.getCreatedAt()));
    }

    @Override
    public void save(IdempotencyRecord record) {
        jpa.save(new IdempotencyKeyEntity(record.key().value(), record.requestHash(), record.customerId(),
                record.createdAt()));
    }

    @Override
    @Transactional
    public int deleteCreatedBefore(Instant cutoff) {
        return jpa.deleteCreatedBefore(cutoff);
    }
}
