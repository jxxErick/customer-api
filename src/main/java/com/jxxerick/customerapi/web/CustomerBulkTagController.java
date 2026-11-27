package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase;
import com.jxxerick.customerapi.web.dto.BulkTagReportResponse;
import com.jxxerick.customerapi.web.dto.BulkTagRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tag changes over many customers; kept apart so the single-customer controller stays focused. */
@RestController
@RequestMapping("/api/customers/tags/bulk")
public class CustomerBulkTagController {

    private final BulkTagCustomersUseCase bulkTag;

    public CustomerBulkTagController(BulkTagCustomersUseCase bulkTag) {
        this.bulkTag = bulkTag;
    }

    /** Always 200 with one line per customer; only request-level problems become errors. */
    @PostMapping
    public BulkTagReportResponse apply(@RequestBody BulkTagRequest request) {
        return BulkTagReportResponse.from(bulkTag.execute(request.toCommand()));
    }
}
