package com.jxxerick.customerapi.support;

import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerTrash;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryTrash implements CustomerTrash {

    private final Map<UUID, TrashedCustomer> entries = new LinkedHashMap<>();

    @Override
    public void put(TrashedCustomer trashed) {
        entries.put(trashed.customer().getId(), trashed);
    }

    @Override
    public Optional<TrashedCustomer> find(UUID customerId) {
        return Optional.ofNullable(entries.get(customerId));
    }

    @Override
    public PageResult<TrashedCustomer> list(int page, int size) {
        List<TrashedCustomer> sorted = entries.values().stream()
                .sorted(Comparator.comparing(TrashedCustomer::deletedAt).reversed())
                .toList();
        int from = Math.min(page * size, sorted.size());
        return new PageResult<>(sorted.subList(from, Math.min(from + size, sorted.size())), page, size,
                sorted.size());
    }

    @Override
    public void remove(UUID customerId) {
        entries.remove(customerId);
    }

    @Override
    public int purgeDue(Instant now) {
        int before = entries.size();
        entries.values().removeIf(trashed -> !trashed.purgeAt().isAfter(now));
        return before - entries.size();
    }

    public int size() {
        return entries.size();
    }
}
