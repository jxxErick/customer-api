package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.IdempotencyKey;
import com.jxxerick.customerapi.domain.model.IdempotencyRecord;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaIdempotencyStore.class)
class JpaIdempotencyStoreTest {

    @Autowired
    private JpaIdempotencyStore store;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindRecord() {
        IdempotencyRecord record = record("key-00000001", Fixtures.NOW);

        store.save(record);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.find(new IdempotencyKey("key-00000001"))).contains(record);
        assertThat(store.find(new IdempotencyKey("key-00000002"))).isEmpty();
    }

    @Test
    void shouldReplaceRecordOfTheSameKey() {
        store.save(record("key-00000001", Fixtures.NOW));
        IdempotencyRecord newer = record("key-00000001", Fixtures.NOW.plus(Duration.ofDays(2)));
        store.save(newer);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.find(new IdempotencyKey("key-00000001"))).contains(newer);
    }

    @Test
    void shouldDeleteOnlyRecordsOlderThanTheCutoff() {
        store.save(record("key-old-0001", Fixtures.NOW.minus(Duration.ofHours(30))));
        store.save(record("key-new-0001", Fixtures.NOW.minus(Duration.ofHours(1))));
        entityManager.flush();

        int removed = store.deleteCreatedBefore(Fixtures.NOW.minus(Duration.ofHours(24)));
        entityManager.clear();

        assertThat(removed).isEqualTo(1);
        assertThat(store.find(new IdempotencyKey("key-old-0001"))).isEmpty();
        assertThat(store.find(new IdempotencyKey("key-new-0001"))).isPresent();
    }

    private static IdempotencyRecord record(String key, Instant createdAt) {
        return new IdempotencyRecord(new IdempotencyKey(key), "a".repeat(64), UUID.randomUUID(), createdAt);
    }
}
