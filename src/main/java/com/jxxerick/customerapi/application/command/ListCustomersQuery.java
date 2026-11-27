package com.jxxerick.customerapi.application.command;

import com.jxxerick.customerapi.domain.model.CustomerFilter;

import java.util.Collection;

/** Listing criteria plus zero-based paging. */
public record ListCustomersQuery(CustomerFilter filter, int page, int size) {

    public static final int MAX_SIZE = 100;

    public ListCustomersQuery {
        filter = filter == null ? CustomerFilter.ALL : filter;
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }

    public ListCustomersQuery(String name, int page, int size) {
        this(CustomerFilter.byName(name), page, size);
    }

    public static ListCustomersQuery of(String name, Collection<String> rawTags, int page, int size) {
        return of(name, rawTags, null, null, page, size);
    }

    public static ListCustomersQuery of(String name, Collection<String> rawTags, String state, String city,
                                        int page, int size) {
        return new ListCustomersQuery(CustomerFilter.of(name, rawTags, state, city), page, size);
    }
}
