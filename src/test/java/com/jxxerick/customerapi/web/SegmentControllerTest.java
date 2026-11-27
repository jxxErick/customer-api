package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.command.CreateSegmentCommand;
import com.jxxerick.customerapi.application.usecase.CreateSegmentUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteSegmentUseCase;
import com.jxxerick.customerapi.application.usecase.ListSegmentCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.ListSegmentsUseCase;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.SegmentNameInUseException;
import com.jxxerick.customerapi.domain.exception.SegmentNotFoundException;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.SegmentSummary;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SegmentController.class)
class SegmentControllerTest {

    private static final Segment VIPS_SP = Segment.create("VIPs SP",
            new CustomerFilter(null, Set.of(new Tag("vip"), new Tag("atacado")), "SP", null), Fixtures.NOW);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateSegmentUseCase createSegment;
    @MockBean
    private ListSegmentsUseCase listSegments;
    @MockBean
    private ListSegmentCustomersUseCase listSegmentCustomers;
    @MockBean
    private DeleteSegmentUseCase deleteSegment;

    @Test
    void createShouldReturn201WithCriteria() throws Exception {
        when(createSegment.execute(new CreateSegmentCommand("VIPs SP", null, List.of("vip", "atacado"), "sp", null)))
                .thenReturn(VIPS_SP);

        mockMvc.perform(post("/api/segments").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "VIPs SP", "tags": ["vip", "atacado"], "state": "sp"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/segments/" + VIPS_SP.id() + "/customers"))
                .andExpect(jsonPath("$.criteria.tags[0]").value("atacado"))
                .andExpect(jsonPath("$.criteria.tags[1]").value("vip"))
                .andExpect(jsonPath("$.criteria.state").value("SP"));
    }

    @Test
    void createShouldRequireName() throws Exception {
        mockMvc.perform(post("/api/segments").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tags": ["vip"]}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createSegment);
    }

    @Test
    void createShouldReturn400WithoutCriteriaAnd409ForTakenName() throws Exception {
        when(createSegment.execute(any()))
                .thenThrow(new DomainValidationException("Informe ao menos um critério para o segmento"))
                .thenThrow(new SegmentNameInUseException("VIPs"));

        mockMvc.perform(post("/api/segments").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"VIPs\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Informe ao menos um critério para o segmento"));
        mockMvc.perform(post("/api/segments").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"VIPs\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listShouldIncludeLiveCount() throws Exception {
        when(listSegments.execute()).thenReturn(List.of(new SegmentSummary(VIPS_SP, 7)));

        mockMvc.perform(get("/api/segments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("VIPs SP"))
                .andExpect(jsonPath("$[0].customers").value(7));
    }

    @Test
    void customersShouldReturnPage() throws Exception {
        when(listSegmentCustomers.execute(VIPS_SP.id(), 1, 5))
                .thenReturn(new PageResult<>(List.of(Fixtures.customer()), 1, 5, 6));

        mockMvc.perform(get("/api/segments/{id}/customers", VIPS_SP.id()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void deleteShouldReturn204Or404() throws Exception {
        UUID unknown = UUID.randomUUID();
        doThrow(new SegmentNotFoundException(unknown)).when(deleteSegment).execute(unknown);

        mockMvc.perform(delete("/api/segments/{id}", VIPS_SP.id())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/segments/{id}", unknown)).andExpect(status().isNotFound());
        verify(deleteSegment).execute(VIPS_SP.id());
    }
}
