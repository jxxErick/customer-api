package com.jxxerick.customerapi.support;

import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerHistory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** In-memory history that keeps everything recorded, in order. */
public class RecordingHistory implements CustomerHistory {

    private final List<CustomerChange> changes = new ArrayList<>();

    @Override
    public void record(CustomerChange change) {
        changes.add(change);
    }

    @Override
    public PageResult<CustomerChange> findByCustomer(UUID customerId, int page, int size) {
        List<CustomerChange> matching = changes.stream()
                .filter(change -> change.customerId().equals(customerId))
                .sorted(Comparator.comparing(CustomerChange::occurredAt).reversed())
                .toList();
        int from = Math.min(page * size, matching.size());
        return new PageResult<>(matching.subList(from, Math.min(from + size, matching.size())), page, size,
                matching.size());
    }

    public List<CustomerChange> recorded() {
        return List.copyOf(changes);
    }
}
