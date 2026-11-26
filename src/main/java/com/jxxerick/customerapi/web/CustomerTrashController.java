package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.usecase.ListTrashUseCase;
import com.jxxerick.customerapi.application.usecase.PurgeTrashedCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.RestoreCustomerUseCase;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.web.dto.CustomerResponse;
import com.jxxerick.customerapi.web.dto.PageResponse;
import com.jxxerick.customerapi.web.dto.TrashedCustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers/trash")
public class CustomerTrashController {

    private final ListTrashUseCase listTrash;
    private final RestoreCustomerUseCase restoreCustomer;
    private final PurgeTrashedCustomerUseCase purgeCustomer;

    public CustomerTrashController(ListTrashUseCase listTrash, RestoreCustomerUseCase restoreCustomer,
                                   PurgeTrashedCustomerUseCase purgeCustomer) {
        this.listTrash = listTrash;
        this.restoreCustomer = restoreCustomer;
        this.purgeCustomer = purgeCustomer;
    }

    /** Most recently deleted first. */
    @GetMapping
    public PageResponse<TrashedCustomerResponse> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listTrash.execute(page, size).map(TrashedCustomerResponse::from));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<CustomerResponse> restore(@PathVariable UUID id) {
        Customer customer = restoreCustomer.execute(id);
        return ResponseEntity.ok().eTag(ETags.of(customer.getVersion())).body(CustomerResponse.from(customer));
    }

    /** Deletes for good, before the retention period is over. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> purge(@PathVariable UUID id) {
        purgeCustomer.execute(id);
        return ResponseEntity.noContent().build();
    }
}
