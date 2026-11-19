package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.RenameCustomerUseCase;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.web.dto.ChangeAddressRequest;
import com.jxxerick.customerapi.web.dto.CreateCustomerRequest;
import com.jxxerick.customerapi.web.dto.CustomerResponse;
import com.jxxerick.customerapi.web.dto.RenameCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CreateCustomerUseCase createCustomer;
    private final GetCustomerUseCase getCustomer;
    private final ListCustomersUseCase listCustomers;
    private final ChangeCustomerAddressUseCase changeAddress;
    private final RenameCustomerUseCase renameCustomer;
    private final DeleteCustomerUseCase deleteCustomer;

    public CustomerController(CreateCustomerUseCase createCustomer,
                              GetCustomerUseCase getCustomer,
                              ListCustomersUseCase listCustomers,
                              ChangeCustomerAddressUseCase changeAddress,
                              RenameCustomerUseCase renameCustomer,
                              DeleteCustomerUseCase deleteCustomer) {
        this.createCustomer = createCustomer;
        this.getCustomer = getCustomer;
        this.listCustomers = listCustomers;
        this.changeAddress = changeAddress;
        this.renameCustomer = renameCustomer;
        this.deleteCustomer = deleteCustomer;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = createCustomer.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return listCustomers.execute().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return CustomerResponse.from(getCustomer.execute(id));
    }

    @PutMapping("/{id}/address")
    public CustomerResponse changeAddress(@PathVariable UUID id, @Valid @RequestBody ChangeAddressRequest request) {
        return CustomerResponse.from(changeAddress.execute(request.toCommand(id)));
    }

    @PatchMapping("/{id}/name")
    public CustomerResponse rename(@PathVariable UUID id, @Valid @RequestBody RenameCustomerRequest request) {
        return CustomerResponse.from(renameCustomer.execute(request.toCommand(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteCustomer.execute(id);
        return ResponseEntity.noContent().build();
    }
}
