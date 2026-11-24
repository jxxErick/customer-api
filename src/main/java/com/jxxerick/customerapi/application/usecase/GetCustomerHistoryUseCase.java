package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CustomerHistoryQuery;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerHistory;

/** Works for deleted customers too: every customer has at least its CREATED entry. */
public class GetCustomerHistoryUseCase {

    private final CustomerHistory history;

    public GetCustomerHistoryUseCase(CustomerHistory history) {
        this.history = history;
    }

    public PageResult<CustomerChange> execute(CustomerHistoryQuery query) {
        PageResult<CustomerChange> result = history.findByCustomer(query.customerId(), query.page(), query.size());
        if (result.totalItems() == 0) {
            throw new CustomerNotFoundException(query.customerId());
        }
        return result;
    }
}
