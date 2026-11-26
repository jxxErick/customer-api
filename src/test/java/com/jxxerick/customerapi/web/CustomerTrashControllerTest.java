package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.usecase.ListTrashUseCase;
import com.jxxerick.customerapi.application.usecase.PurgeTrashedCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.RestoreCustomerUseCase;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.TrashedCustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerTrashController.class)
class CustomerTrashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListTrashUseCase listTrash;
    @MockBean
    private RestoreCustomerUseCase restoreCustomer;
    @MockBean
    private PurgeTrashedCustomerUseCase purgeCustomer;

    @Test
    void listShouldReturnTrashedCustomersWithDates() throws Exception {
        Customer customer = Fixtures.customer();
        TrashedCustomer trashed = TrashedCustomer.of(customer, Fixtures.NOW, Duration.ofDays(30));
        when(listTrash.execute(0, 20)).thenReturn(new PageResult<>(List.of(trashed), 0, 20, 1));

        mockMvc.perform(get("/api/customers/trash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].customer.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.items[0].customer.name").value("Maria Silva"))
                .andExpect(jsonPath("$.items[0].deletedAt").value("2026-01-10T12:00:00Z"))
                .andExpect(jsonPath("$.items[0].purgeAt").value("2026-02-09T12:00:00Z"));
    }

    @Test
    void restoreShouldReturnCustomerWithFreshETag() throws Exception {
        Customer restored = Fixtures.storedCustomer(0);
        when(restoreCustomer.execute(restored.getId())).thenReturn(restored);

        mockMvc.perform(post("/api/customers/trash/{id}/restore", restored.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(restored.getId().toString()));
    }

    @Test
    void restoreShouldReturn404WhenNotInTrash() throws Exception {
        UUID id = UUID.randomUUID();
        when(restoreCustomer.execute(id)).thenThrow(new TrashedCustomerNotFoundException(id));

        mockMvc.perform(post("/api/customers/trash/{id}/restore", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não está na lixeira: " + id));
    }

    @Test
    void restoreShouldReturn409WhenEmailIsTaken() throws Exception {
        UUID id = UUID.randomUUID();
        when(restoreCustomer.execute(id)).thenThrow(new EmailAlreadyInUseException("maria@example.com"));

        mockMvc.perform(post("/api/customers/trash/{id}/restore", id)).andExpect(status().isConflict());
    }

    @Test
    void purgeShouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/trash/{id}", id)).andExpect(status().isNoContent());

        verify(purgeCustomer).execute(id);
    }

    @Test
    void purgeShouldReturn404WhenNotInTrash() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TrashedCustomerNotFoundException(id)).when(purgeCustomer).execute(id);

        mockMvc.perform(delete("/api/customers/trash/{id}", id)).andExpect(status().isNotFound());
    }
}
