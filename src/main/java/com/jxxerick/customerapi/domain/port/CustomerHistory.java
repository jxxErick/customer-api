package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.PageResult;

import java.util.UUID;

public interface CustomerHistory {

    void record(CustomerChange change);

    /** Newest first. */
    PageResult<CustomerChange> findByCustomer(UUID customerId, int page, int size);
}
