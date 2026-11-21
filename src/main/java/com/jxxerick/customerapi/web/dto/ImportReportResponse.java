package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase.Report;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase.RowStatus;

import java.util.List;
import java.util.UUID;

public record ImportReportResponse(int total, int created, int failed, List<Row> rows) {

    public static ImportReportResponse from(Report report) {
        return new ImportReportResponse(report.total(), report.created(), report.failed(), report.rows().stream()
                .map(r -> new Row(r.line(), r.status(), r.email(), r.customerId(), r.error()))
                .toList());
    }

    public record Row(int line, RowStatus status, String email, UUID customerId, String error) {
    }
}
