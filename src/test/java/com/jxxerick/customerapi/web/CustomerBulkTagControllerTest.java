package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.command.BulkTagCommand;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Item;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Outcome;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Report;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.SegmentNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerBulkTagController.class)
class CustomerBulkTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BulkTagCustomersUseCase bulkTag;

    @Test
    void shouldReturnReportPerCustomer() throws Exception {
        UUID updated = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(bulkTag.execute(BulkTagCommand.forCustomers(List.of(updated, missing), List.of("vip"), List.of())))
                .thenReturn(new Report(2, 1, 0, 1, List.of(new Item(updated, Outcome.UPDATED, null),
                        new Item(missing, Outcome.NOT_FOUND, "Cliente não encontrado"))));

        mockMvc.perform(post("/api/customers/tags/bulk").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerIds": ["%s", "%s"], "add": ["vip"]}
                                """.formatted(updated, missing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.items[1].outcome").value("NOT_FOUND"))
                .andExpect(jsonPath("$.items[1].message").value("Cliente não encontrado"));
    }

    @Test
    void shouldReturn400ForInvalidRequestAnd404ForUnknownSegment() throws Exception {
        UUID segment = UUID.randomUUID();
        when(bulkTag.execute(BulkTagCommand.forSegment(segment, List.of(), List.of("vip"))))
                .thenThrow(new SegmentNotFoundException(segment));
        when(bulkTag.execute(BulkTagCommand.forCustomers(List.of(), List.of("vip"), List.of())))
                .thenThrow(new DomainValidationException("Informe a lista de clientes ou um segmento (um dos dois)"));

        mockMvc.perform(post("/api/customers/tags/bulk").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\": \"%s\", \"remove\": [\"vip\"]}".formatted(segment)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/customers/tags/bulk").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"add\": [\"vip\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Informe a lista de clientes ou um segmento (um dos dois)"));
    }
}
