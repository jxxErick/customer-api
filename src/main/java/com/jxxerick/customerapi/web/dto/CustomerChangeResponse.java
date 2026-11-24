package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.FieldChange;

import java.time.Instant;
import java.util.List;

public record CustomerChangeResponse(ChangeType type, Instant occurredAt, List<FieldChangeResponse> changes) {

    public static CustomerChangeResponse from(CustomerChange change) {
        return new CustomerChangeResponse(change.type(), change.occurredAt(),
                change.fields().stream().map(FieldChangeResponse::from).toList());
    }

    public record FieldChangeResponse(String field, String from, String to) {

        static FieldChangeResponse from(FieldChange change) {
            return new FieldChangeResponse(change.field(), change.previous(), change.current());
        }
    }
}
