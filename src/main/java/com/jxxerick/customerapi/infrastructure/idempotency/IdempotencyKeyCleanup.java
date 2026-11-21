package com.jxxerick.customerapi.infrastructure.idempotency;

import com.jxxerick.customerapi.domain.port.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Keeps the idempotency table small by purging keys past their TTL. */
@Component
public class IdempotencyKeyCleanup {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyCleanup.class);

    private final IdempotencyStore store;
    private final IdempotencyProperties properties;
    private final Clock clock;

    public IdempotencyKeyCleanup(IdempotencyStore store, IdempotencyProperties properties, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${idempotency.cleanup-interval}",
            initialDelayString = "${idempotency.cleanup-interval}")
    public void purgeExpired() {
        int removed = store.deleteCreatedBefore(clock.instant().minus(properties.ttl()));
        if (removed > 0) {
            log.info("Removed {} expired idempotency key(s)", removed);
        }
    }
}
