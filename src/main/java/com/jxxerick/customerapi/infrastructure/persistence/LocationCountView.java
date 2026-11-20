package com.jxxerick.customerapi.infrastructure.persistence;

/** Projection of the group-by query in {@link SpringDataCustomerRepository}. */
interface LocationCountView {

    String getState();

    String getCity();

    long getCustomers();
}
