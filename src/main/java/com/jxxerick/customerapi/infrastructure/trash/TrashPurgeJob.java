package com.jxxerick.customerapi.infrastructure.trash;

import com.jxxerick.customerapi.domain.port.CustomerTrash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Permanently removes trashed customers whose retention is over. */
@Component
public class TrashPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(TrashPurgeJob.class);

    private final CustomerTrash trash;
    private final Clock clock;

    public TrashPurgeJob(CustomerTrash trash, Clock clock) {
        this.trash = trash;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${trash.purge-interval}", initialDelayString = "${trash.purge-interval}")
    public void purgeDue() {
        int removed = trash.purgeDue(clock.instant());
        if (removed > 0) {
            log.info("Purged {} customer(s) from the trash", removed);
        }
    }
}
