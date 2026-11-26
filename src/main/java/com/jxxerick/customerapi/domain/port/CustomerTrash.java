package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Deleted customers waiting to be restored or purged. */
public interface CustomerTrash {

    void put(TrashedCustomer trashed);

    Optional<TrashedCustomer> find(UUID customerId);

    /** Most recently deleted first. */
    PageResult<TrashedCustomer> list(int page, int size);

    void remove(UUID customerId);

    /** Removes everything whose purge date is not after {@code now}; returns how many. */
    int purgeDue(Instant now);
}
