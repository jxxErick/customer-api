package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.UUID;

/** Customers matching the segment right now, ordered by name. */
public class ListSegmentCustomersUseCase {

    private final GetSegmentUseCase getSegment;
    private final CustomerRepository customerRepository;

    public ListSegmentCustomersUseCase(GetSegmentUseCase getSegment, CustomerRepository customerRepository) {
        this.getSegment = getSegment;
        this.customerRepository = customerRepository;
    }

    public PageResult<Customer> execute(UUID segmentId, int page, int size) {
        Segment segment = getSegment.execute(segmentId);
        // reuses the listing's paging limits
        ListCustomersQuery query = new ListCustomersQuery(segment.filter(), page, size);
        return customerRepository.findMatching(query.filter(), query.page(), query.size());
    }
}
