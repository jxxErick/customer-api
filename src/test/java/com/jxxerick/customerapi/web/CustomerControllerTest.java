package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.CustomerHistoryQuery;
import com.jxxerick.customerapi.application.command.ImportRow;
import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.application.command.MergeCustomersCommand;
import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ExportCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.FindCustomerByCpfUseCase;
import com.jxxerick.customerapi.application.usecase.FindDuplicateCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerHistoryUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerStatsUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.IdempotentCreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.MergeCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.RenameCustomerUseCase;
import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.CustomerMergeConflictException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.IdempotencyKeyReusedException;
import com.jxxerick.customerapi.domain.exception.ImportTooLargeException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.DuplicateCandidate;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.CustomerStats;
import com.jxxerick.customerapi.domain.model.LocationCount;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(CustomerControllerTest.FixedClock.class)
class CustomerControllerTest {

    private static final String VALID_BODY = """
            {"name":"Maria Silva","email":"maria@example.com","zipCode":"01001-000","number":"100","complement":"Apto 12"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateCustomerUseCase createCustomer;
    @MockBean
    private GetCustomerUseCase getCustomer;
    @MockBean
    private ListCustomersUseCase listCustomers;
    @MockBean
    private ChangeCustomerAddressUseCase changeAddress;
    @MockBean
    private RenameCustomerUseCase renameCustomer;
    @MockBean
    private DeleteCustomerUseCase deleteCustomer;
    @MockBean
    private GetCustomerStatsUseCase customerStats;
    @MockBean
    private FindCustomerByCpfUseCase findByCpf;
    @MockBean
    private IdempotentCreateCustomerUseCase idempotentCreate;
    @MockBean
    private ImportCustomersUseCase importCustomers;
    @MockBean
    private GetCustomerHistoryUseCase customerHistory;
    @MockBean
    private ExportCustomersUseCase exportCustomers;
    @MockBean
    private FindDuplicateCustomersUseCase findDuplicates;
    @MockBean
    private MergeCustomersUseCase mergeCustomers;

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void renameShouldReturnUpdatedCustomer() throws Exception {
        Customer customer = Fixtures.customer();
        customer.rename("Maria Souza");
        when(renameCustomer.execute(new RenameCustomerCommand(customer.getId(), "Maria Souza", null)))
                .thenReturn(customer);

        mockMvc.perform(patch("/api/customers/{id}/name", customer.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria Souza\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"));
    }

    @Test
    void renameShouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(patch("/api/customers/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Nome é obrigatório"));
    }

    @Test
    void createShouldReturn201WithLocation() throws Exception {
        Customer customer = Fixtures.customer();
        when(createCustomer.execute(new CreateCustomerCommand(
                "Maria Silva", "maria@example.com", "01001-000", "100", "Apto 12"))).thenReturn(customer);

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/customers/" + customer.getId())))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.address.zipCode").value("01001-000"))
                .andExpect(jsonPath("$.address.city").value("São Paulo"));
    }

    @Test
    void createShouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Nome é obrigatório"))
                .andExpect(jsonPath("$.errors.zipCode").value("CEP é obrigatório"));
    }

    @Test
    void createShouldReturn409WhenEmailInUse() throws Exception {
        when(createCustomer.execute(any())).thenThrow(new EmailAlreadyInUseException("maria@example.com"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado: maria@example.com"));
    }

    @Test
    void createShouldReturn422WhenZipCodeUnknown() throws Exception {
        when(createCustomer.execute(any())).thenThrow(new AddressNotFoundException("01001-000"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createShouldReturn503WhenProviderIsDown() throws Exception {
        when(createCustomer.execute(any())).thenThrow(new AddressProviderUnavailableException(new RuntimeException()));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void listShouldReturnCustomers() throws Exception {
        when(listCustomers.execute(new ListCustomersQuery(null, 0, 20)))
                .thenReturn(new PageResult<>(List.of(Fixtures.customer()), 0, 20, 1));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listShouldPassNameFilterAndPaging() throws Exception {
        when(listCustomers.execute(new ListCustomersQuery("maria", 2, 5)))
                .thenReturn(new PageResult<>(List.of(), 2, 5, 11));

        mockMvc.perform(get("/api/customers").param("name", "maria").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getShouldReturn404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(getCustomer.execute(id)).thenThrow(new CustomerNotFoundException(id));

        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void changeAddressShouldReturnUpdatedCustomer() throws Exception {
        Customer customer = Fixtures.customer();
        when(changeAddress.execute(new ChangeAddressCommand(customer.getId(), "01001000", "7", null, null)))
                .thenReturn(customer);

        mockMvc.perform(put("/api/customers/{id}/address", customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zipCode\":\"01001000\",\"number\":\"7\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()));
    }

    @Test
    void getShouldReturnVersionAsETag() throws Exception {
        Customer customer = Fixtures.storedCustomer(4);
        when(getCustomer.execute(customer.getId())).thenReturn(customer);

        mockMvc.perform(get("/api/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"4\""));
    }

    @Test
    void renameShouldForwardIfMatchVersion() throws Exception {
        Customer customer = Fixtures.storedCustomer(5);
        when(renameCustomer.execute(new RenameCustomerCommand(customer.getId(), "Maria Souza", 4L)))
                .thenReturn(customer);

        mockMvc.perform(patch("/api/customers/{id}/name", customer.getId())
                        .header(HttpHeaders.IF_MATCH, "W/\"4\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria Souza\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"5\""));
    }

    @Test
    void changeAddressShouldReturn412WhenVersionIsOutdated() throws Exception {
        UUID id = UUID.randomUUID();
        when(changeAddress.execute(new ChangeAddressCommand(id, "01001000", "7", null, 1L)))
                .thenThrow(new ConcurrentUpdateException(id));

        mockMvc.perform(put("/api/customers/{id}/address", id)
                        .header(HttpHeaders.IF_MATCH, "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zipCode\":\"01001000\",\"number\":\"7\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void renameShouldReturn400WhenIfMatchIsMalformed() throws Exception {
        mockMvc.perform(patch("/api/customers/{id}/name", UUID.randomUUID())
                        .header(HttpHeaders.IF_MATCH, "abc")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(renameCustomer);
    }

    @Test
    void statsShouldReturnCustomersPerState() throws Exception {
        when(customerStats.execute()).thenReturn(CustomerStats.from(List.of(
                new LocationCount("SP", "São Paulo", 3), new LocationCount("RJ", "Niterói", 1))));

        mockMvc.perform(get("/api/customers/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(4))
                .andExpect(jsonPath("$.states[0].state").value("SP"))
                .andExpect(jsonPath("$.states[0].percentage").value(75.0))
                .andExpect(jsonPath("$.states[1].cities[0].city").value("Niterói"));
    }

    @Test
    void createShouldForwardCpfAndReturnItMasked() throws Exception {
        Customer customer = Customer.create("Maria Silva", new Email("maria@example.com"), new Cpf("52998224725"),
                Fixtures.address(), Fixtures.NOW);
        when(createCustomer.execute(new CreateCustomerCommand(
                "Maria Silva", "maria@example.com", "01001-000", "100", null, "529.982.247-25"))).thenReturn(customer);

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Maria Silva","email":"maria@example.com","zipCode":"01001-000","number":"100",
                         "cpf":"529.982.247-25"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***.982.247-**"));
    }

    @Test
    void createShouldReturn409WhenCpfInUse() throws Exception {
        when(createCustomer.execute(any())).thenThrow(new CpfAlreadyInUseException(new Cpf("52998224725")));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("CPF já cadastrado: ***.982.247-**"));
    }

    @Test
    void lookupShouldFindCustomerByCpfInTheBody() throws Exception {
        Customer customer = Fixtures.storedCustomer(2);
        when(findByCpf.execute("52998224725")).thenReturn(customer);

        mockMvc.perform(post("/api/customers/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""))
                .andExpect(jsonPath("$.id").value(customer.getId().toString()));
    }

    @Test
    void lookupShouldReturn400WhenCpfIsMissing() throws Exception {
        mockMvc.perform(post("/api/customers/lookup").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpf").value("CPF é obrigatório"));
        verifyNoInteractions(findByCpf);
    }

    @Test
    void createWithoutKeyShouldNotTouchTheIdempotencyStore() throws Exception {
        when(createCustomer.execute(any())).thenReturn(Fixtures.customer());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        verifyNoInteractions(idempotentCreate);
    }

    @Test
    void createWithKeyShouldUseIdempotentFlow() throws Exception {
        Customer customer = Fixtures.customer();
        when(idempotentCreate.execute(eq("req-12345678"), any()))
                .thenReturn(new IdempotentCreateCustomerUseCase.Result(customer, false));

        mockMvc.perform(post("/api/customers").header("Idempotency-Key", "req-12345678")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(header().string("Location", endsWith("/api/customers/" + customer.getId())));
        verifyNoInteractions(createCustomer);
    }

    @Test
    void replayShouldBeFlaggedInAHeader() throws Exception {
        Customer customer = Fixtures.storedCustomer(0);
        when(idempotentCreate.execute(eq("req-12345678"), any()))
                .thenReturn(new IdempotentCreateCustomerUseCase.Result(customer, true));

        mockMvc.perform(post("/api/customers").header("Idempotency-Key", "req-12345678")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(customer.getId().toString()));
    }

    @Test
    void reusedKeyShouldReturn422() throws Exception {
        when(idempotentCreate.execute(eq("req-12345678"), any()))
                .thenThrow(new IdempotencyKeyReusedException("req-12345678"));

        mockMvc.perform(post("/api/customers").header("Idempotency-Key", "req-12345678")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void malformedKeyShouldReturn400() throws Exception {
        when(idempotentCreate.execute(eq("x"), any())).thenThrow(new DomainValidationException("chave inválida"));

        mockMvc.perform(post("/api/customers").header("Idempotency-Key", "x")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importShouldReadUtf8CsvBodyAndReturnReport() throws Exception {
        UUID id = UUID.randomUUID();
        List<ImportRow> expected = List.of(new ImportRow(2, new CreateCustomerCommand(
                "João Pereira", "joao@example.com", "20040020", "5", null, null)));
        when(importCustomers.execute(expected)).thenReturn(new ImportCustomersUseCase.Report(1, 1, 0,
                List.of(new ImportCustomersUseCase.RowResult(2, ImportCustomersUseCase.RowStatus.CREATED,
                        "joao@example.com", id, null))));

        mockMvc.perform(post("/api/customers/import").contentType("text/csv")
                        .content("nome;email;cep;numero\nJoão Pereira;joao@example.com;20040020;5\n"
                                .getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("CREATED"))
                .andExpect(jsonPath("$.rows[0].customerId").value(id.toString()));
    }

    @Test
    void importShouldAcceptMultipartUpload() throws Exception {
        when(importCustomers.execute(any())).thenReturn(new ImportCustomersUseCase.Report(0, 0, 0, List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "clientes.csv", "text/csv",
                "name,email,zipCode,number\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/customers/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void importShouldReturn400ForUnreadableCsv() throws Exception {
        mockMvc.perform(post("/api/customers/import").contentType("text/csv").content("name,cpf\nAna,1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cabeçalho sem as colunas obrigatórias: email, zipCode, number"));
        verifyNoInteractions(importCustomers);
    }

    @Test
    void importShouldReturn413WhenFileIsTooBig() throws Exception {
        when(importCustomers.execute(any())).thenThrow(new ImportTooLargeException(1001, 1000));

        mockMvc.perform(post("/api/customers/import").contentType("text/csv")
                        .content("name,email,zipCode,number\n"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void deleteShouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());

        verify(deleteCustomer).execute(id);
    }

    @Test
    void deleteShouldReturn404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CustomerNotFoundException(id)).when(deleteCustomer).execute(id);

        mockMvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void historyShouldReturnChangesPage() throws Exception {
        Customer customer = Fixtures.customer();
        String before = customer.getName();
        customer.rename("Maria Souza");
        CustomerChange renamed = CustomerChange.renamed(customer, before, Fixtures.NOW);
        when(customerHistory.execute(new CustomerHistoryQuery(customer.getId(), 0, 5)))
                .thenReturn(new PageResult<>(List.of(renamed), 0, 5, 2));

        mockMvc.perform(get("/api/customers/{id}/history", customer.getId()).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("RENAMED"))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-01-10T12:00:00Z"))
                .andExpect(jsonPath("$.items[0].changes[0].field").value("name"))
                .andExpect(jsonPath("$.items[0].changes[0].from").value("Maria Silva"))
                .andExpect(jsonPath("$.items[0].changes[0].to").value("Maria Souza"))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void historyShouldReturn404ForUnknownCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerHistory.execute(new CustomerHistoryQuery(id, 0, 20)))
                .thenThrow(new CustomerNotFoundException(id));

        mockMvc.perform(get("/api/customers/{id}/history", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportShouldStreamCsvAttachment() throws Exception {
        Customer customer = Fixtures.customer();
        doAnswer(invocation -> {
            ((Consumer<Customer>) invocation.getArgument(1)).accept(customer);
            return 1L;
        }).when(exportCustomers).execute(isNull(), any());

        MvcResult started = mockMvc.perform(get("/api/customers/export"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"customers-2026-01-10.csv\""))
                .andExpect(content().bytes(("\uFEFFname;email;zipCode;number;complement;street;neighborhood;city;state;createdAt\r\n"
                        + "Maria Silva;maria@example.com;01001-000;100;Apto 12;Praça da Sé;Sé;São Paulo;SP;2026-01-10T12:00:00Z\r\n")
                        .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void exportShouldPassNameFilter() throws Exception {
        MvcResult started = mockMvc.perform(get("/api/customers/export").param("name", "silva"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(started)).andExpect(status().isOk());

        verify(exportCustomers).execute(eq("silva"), any());
    }

    @Test
    void duplicatesShouldUseDefaultMinScore() throws Exception {
        Customer first = Fixtures.customer();
        Customer second = Customer.create("MARIA SILVA", new Email("msilva@example.com"), Fixtures.address(),
                Fixtures.NOW);
        DuplicateCandidate candidate = DuplicateCandidate.evaluate(first, second).orElseThrow();
        when(findDuplicates.execute(FindDuplicateCustomersUseCase.DEFAULT_MIN_SCORE)).thenReturn(List.of(candidate));

        mockMvc.perform(get("/api/customers/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].first.name").value("Maria Silva"))
                .andExpect(jsonPath("$[0].second.name").value("MARIA SILVA"))
                .andExpect(jsonPath("$[0].score").value(1.0))
                .andExpect(jsonPath("$[0].reasons[0]").value("SAME_NAME"))
                .andExpect(jsonPath("$[0].reasons[1]").value("SAME_ZIP_CODE"));
    }

    @Test
    void duplicatesShouldPassMinScore() throws Exception {
        when(findDuplicates.execute(0.95)).thenReturn(List.of());

        mockMvc.perform(get("/api/customers/duplicates").param("minScore", "0.95"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void mergeShouldReturnTargetWithETag() throws Exception {
        Customer target = Fixtures.storedCustomer(3);
        UUID duplicateId = UUID.randomUUID();
        when(mergeCustomers.execute(new MergeCustomersCommand(target.getId(), duplicateId, 2L))).thenReturn(target);

        mockMvc.perform(post("/api/customers/{id}/merge", target.getId())
                        .header(HttpHeaders.IF_MATCH, "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duplicateId\":\"" + duplicateId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"3\""))
                .andExpect(jsonPath("$.id").value(target.getId().toString()));
    }

    @Test
    void mergeShouldReturn400WithoutDuplicateId() throws Exception {
        mockMvc.perform(post("/api/customers/{id}/merge", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.duplicateId").value("Cliente duplicado é obrigatório"));
        verifyNoInteractions(mergeCustomers);
    }

    @Test
    void mergeShouldReturn409ForDifferentCpfs() throws Exception {
        when(mergeCustomers.execute(any())).thenThrow(
                new CustomerMergeConflictException("Os clientes têm CPFs diferentes e não podem ser mesclados"));

        mockMvc.perform(post("/api/customers/{id}/merge", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duplicateId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict());
    }
}
