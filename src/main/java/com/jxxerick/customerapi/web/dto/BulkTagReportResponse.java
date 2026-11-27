package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Outcome;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Report;

import java.util.List;
import java.util.UUID;

public record BulkTagReportResponse(int total, int updated, int unchanged, int failed, List<Item> items) {

    public static BulkTagReportResponse from(Report report) {
        return new BulkTagReportResponse(report.total(), report.updated(), report.unchanged(), report.failed(),
                report.items().stream()
                        .map(item -> new Item(item.customerId(), item.outcome(), item.message()))
                        .toList());
    }

    public record Item(UUID customerId, Outcome outcome, String message) {
    }
}
