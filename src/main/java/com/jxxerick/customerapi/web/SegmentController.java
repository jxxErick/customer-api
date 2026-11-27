package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.application.usecase.CreateSegmentUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteSegmentUseCase;
import com.jxxerick.customerapi.application.usecase.ListSegmentCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.ListSegmentsUseCase;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.web.dto.CreateSegmentRequest;
import com.jxxerick.customerapi.web.dto.CustomerResponse;
import com.jxxerick.customerapi.web.dto.PageResponse;
import com.jxxerick.customerapi.web.dto.SegmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Saved filters over the customer base. */
@RestController
@RequestMapping("/api/segments")
public class SegmentController {

    private final CreateSegmentUseCase createSegment;
    private final ListSegmentsUseCase listSegments;
    private final ListSegmentCustomersUseCase listSegmentCustomers;
    private final DeleteSegmentUseCase deleteSegment;

    public SegmentController(CreateSegmentUseCase createSegment, ListSegmentsUseCase listSegments,
                             ListSegmentCustomersUseCase listSegmentCustomers, DeleteSegmentUseCase deleteSegment) {
        this.createSegment = createSegment;
        this.listSegments = listSegments;
        this.listSegmentCustomers = listSegmentCustomers;
        this.deleteSegment = deleteSegment;
    }

    @PostMapping
    public ResponseEntity<SegmentResponse> create(@Valid @RequestBody CreateSegmentRequest request) {
        Segment segment = createSegment.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}/customers")
                .buildAndExpand(segment.id())
                .toUri();
        return ResponseEntity.created(location).body(SegmentResponse.from(segment));
    }

    /** Ordered by name, each with how many customers match it now. */
    @GetMapping
    public List<SegmentResponse> list() {
        return listSegments.execute().stream().map(SegmentResponse::from).toList();
    }

    @GetMapping("/{id}/customers")
    public PageResponse<CustomerResponse> customers(@PathVariable UUID id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listSegmentCustomers.execute(id, page, size).map(CustomerResponse::from));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteSegment.execute(id);
        return ResponseEntity.noContent().build();
    }
}
