package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.TrashedCustomer;

import java.time.Instant;

public record TrashedCustomerResponse(CustomerResponse customer, Instant deletedAt, Instant purgeAt) {

    public static TrashedCustomerResponse from(TrashedCustomer trashed) {
        return new TrashedCustomerResponse(CustomerResponse.from(trashed.customer()), trashed.deletedAt(),
                trashed.purgeAt());
    }
}
