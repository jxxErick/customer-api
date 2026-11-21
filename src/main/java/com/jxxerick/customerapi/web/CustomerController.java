package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.FindCustomerByCpfUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerStatsUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.IdempotentCreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.RenameCustomerUseCase;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.web.dto.ChangeAddressRequest;
import com.jxxerick.customerapi.web.dto.CpfLookupRequest;
import com.jxxerick.customerapi.web.dto.CreateCustomerRequest;
import com.jxxerick.customerapi.web.dto.CustomerResponse;
import com.jxxerick.customerapi.web.dto.CustomerStatsResponse;
import com.jxxerick.customerapi.web.dto.PageResponse;
import com.jxxerick.customerapi.web.dto.RenameCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final CreateCustomerUseCase createCustomer;
    private final GetCustomerUseCase getCustomer;
    private final ListCustomersUseCase listCustomers;
    private final ChangeCustomerAddressUseCase changeAddress;
    private final RenameCustomerUseCase renameCustomer;
    private final DeleteCustomerUseCase deleteCustomer;
    private final GetCustomerStatsUseCase customerStats;
    private final FindCustomerByCpfUseCase findByCpf;
    private final IdempotentCreateCustomerUseCase idempotentCreate;

    public CustomerController(CreateCustomerUseCase createCustomer,
                              GetCustomerUseCase getCustomer,
                              ListCustomersUseCase listCustomers,
                              ChangeCustomerAddressUseCase changeAddress,
                              RenameCustomerUseCase renameCustomer,
                              DeleteCustomerUseCase deleteCustomer,
                              GetCustomerStatsUseCase customerStats,
                              FindCustomerByCpfUseCase findByCpf,
                              IdempotentCreateCustomerUseCase idempotentCreate) {
        this.createCustomer = createCustomer;
        this.getCustomer = getCustomer;
        this.listCustomers = listCustomers;
        this.changeAddress = changeAddress;
        this.renameCustomer = renameCustomer;
        this.deleteCustomer = deleteCustomer;
        this.customerStats = customerStats;
        this.findByCpf = findByCpf;
        this.idempotentCreate = idempotentCreate;
    }

    /** With an Idempotency-Key, a retry returns the customer created the first time. */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer;
        boolean replayed = false;
        if (idempotencyKey == null) {
            customer = createCustomer.execute(request.toCommand());
        } else {
            IdempotentCreateCustomerUseCase.Result result =
                    idempotentCreate.execute(idempotencyKey, request.toCommand());
            customer = result.customer();
            replayed = result.replayed();
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
        ResponseEntity.BodyBuilder builder = ResponseEntity.created(location);
        if (replayed) {
            builder.header(IDEMPOTENT_REPLAYED, "true");
        }
        return withETag(builder, customer);
    }

    @GetMapping
    public PageResponse<CustomerResponse> list(@RequestParam(required = false) String name,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listCustomers.execute(new ListCustomersQuery(name, page, size))
                .map(CustomerResponse::from));
    }

    @GetMapping("/stats")
    public CustomerStatsResponse stats() {
        return CustomerStatsResponse.from(customerStats.execute());
    }

    /** POST keeps the CPF out of the URL. */
    @PostMapping("/lookup")
    public ResponseEntity<CustomerResponse> lookupByCpf(@Valid @RequestBody CpfLookupRequest request) {
        return withETag(ResponseEntity.ok(), findByCpf.execute(request.cpf()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return withETag(ResponseEntity.ok(), getCustomer.execute(id));
    }

    @PutMapping("/{id}/address")
    public ResponseEntity<CustomerResponse> changeAddress(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ChangeAddressRequest request) {
        Customer customer = changeAddress.execute(request.toCommand(id, ETags.parseIfMatch(ifMatch)));
        return withETag(ResponseEntity.ok(), customer);
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<CustomerResponse> rename(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody RenameCustomerRequest request) {
        Customer customer = renameCustomer.execute(request.toCommand(id, ETags.parseIfMatch(ifMatch)));
        return withETag(ResponseEntity.ok(), customer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteCustomer.execute(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<CustomerResponse> withETag(ResponseEntity.BodyBuilder builder, Customer customer) {
        if (customer.getVersion() != null) {
            builder.eTag(ETags.of(customer.getVersion()));
        }
        return builder.body(CustomerResponse.from(customer));
    }
}
