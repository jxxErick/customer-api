package com.jxxerick.customerapi.application.command;

import java.util.UUID;

/** Zero-based paging over one customer's history. */
public record CustomerHistoryQuery(UUID customerId, int page, int size) {

    public static final int MAX_SIZE = 100;

    public CustomerHistoryQuery {
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }
}
