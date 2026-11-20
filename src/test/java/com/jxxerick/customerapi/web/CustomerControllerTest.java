package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerStatsUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.RenameCustomerUseCase;
import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerStats;
import com.jxxerick.customerapi.domain.model.LocationCount;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
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
}
