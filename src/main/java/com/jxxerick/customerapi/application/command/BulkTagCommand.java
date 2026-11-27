package com.jxxerick.customerapi.application.command;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Tags to add and remove on either an explicit set of customers or every customer of a segment. */
public record BulkTagCommand(Set<UUID> customerIds, UUID segmentId, List<String> add, List<String> remove) {

    public BulkTagCommand {
        customerIds = customerIds == null ? Set.of() : customerIds;
        add = add == null ? List.of() : List.copyOf(add);
        remove = remove == null ? List.of() : List.copyOf(remove);
    }

    /** Repeated or null ids are dropped; the rest keep the order they were sent in. */
    public static BulkTagCommand of(Collection<UUID> ids, UUID segmentId, List<String> add, List<String> remove) {
        Set<UUID> ordered = new LinkedHashSet<>();
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(ordered::add);
        }
        return new BulkTagCommand(ordered, segmentId, add, remove);
    }

    public static BulkTagCommand forCustomers(Collection<UUID> ids, List<String> add, List<String> remove) {
        return of(ids, null, add, remove);
    }

    public static BulkTagCommand forSegment(UUID segmentId, List<String> add, List<String> remove) {
        return of(null, segmentId, add, remove);
    }
}
