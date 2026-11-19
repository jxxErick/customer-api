package com.jxxerick.customerapi.application.command;

/** Optional name filter plus zero-based paging. */
public record ListCustomersQuery(String name, int page, int size) {

    public static final int MAX_SIZE = 100;

    public ListCustomersQuery {
        name = name == null || name.isBlank() ? null : name.strip();
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }
}
