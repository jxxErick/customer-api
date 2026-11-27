package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.BulkTagCommand;

import java.util.List;
import java.util.UUID;

/** Either {@code customerIds} or {@code segmentId}; at least one tag to add or remove. */
public record BulkTagRequest(List<UUID> customerIds, UUID segmentId, List<String> add, List<String> remove) {

    public BulkTagCommand toCommand() {
        return BulkTagCommand.of(customerIds, segmentId, add, remove);
    }
}
