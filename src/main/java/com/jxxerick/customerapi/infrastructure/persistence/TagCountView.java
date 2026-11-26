package com.jxxerick.customerapi.infrastructure.persistence;

/** Projection of the tag group-by query in {@link SpringDataCustomerRepository}. */
interface TagCountView {

    String getTag();

    long getCustomers();
}
