package com.jxxerick.customerapi.infrastructure.idempotency;

import com.jxxerick.customerapi.domain.port.IdempotencyStore;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IdempotencyKeyCleanupTest {

    @Test
    void shouldPurgeKeysOlderThanTheTtl() {
        IdempotencyStore store = mock(IdempotencyStore.class);
        IdempotencyKeyCleanup cleanup = new IdempotencyKeyCleanup(store,
                new IdempotencyProperties(Duration.ofHours(24), Duration.ofHours(1)),
                Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));

        cleanup.purgeExpired();

        verify(store).deleteCreatedBefore(Fixtures.NOW.minus(Duration.ofHours(24)));
    }
}
