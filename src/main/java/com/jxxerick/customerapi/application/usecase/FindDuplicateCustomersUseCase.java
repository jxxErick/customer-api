package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.DuplicateCandidate;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compares every pair of customers and lists the probable duplicates, best match first. */
public class FindDuplicateCustomersUseCase {

    public static final double DEFAULT_MIN_SCORE = 0.85;
    static final int PAGE_SIZE = 500;
    static final int MAX_RESULTS = 200;

    private final CustomerRepository customerRepository;

    public FindDuplicateCustomersUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<DuplicateCandidate> execute(double minScore) {
        if (minScore < 0.5 || minScore > 1) {
            throw new DomainValidationException("minScore deve estar entre 0.5 e 1");
        }
        List<Customer> customers = loadAll();
        List<DuplicateCandidate> candidates = new ArrayList<>();
        // O(n²) on purpose: fine for a few thousand customers and catches typos anywhere in the name
        for (int i = 0; i < customers.size(); i++) {
            for (int j = i + 1; j < customers.size(); j++) {
                DuplicateCandidate.evaluate(customers.get(i), customers.get(j))
                        .filter(candidate -> candidate.score() >= minScore)
                        .ifPresent(candidates::add);
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(DuplicateCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.first().getName()))
                .limit(MAX_RESULTS)
                .toList();
    }

    private List<Customer> loadAll() {
        List<Customer> customers = new ArrayList<>();
        int page = 0;
        PageResult<Customer> result;
        do {
            result = customerRepository.search(null, page++, PAGE_SIZE);
            customers.addAll(result.items());
        } while (page < result.totalPages());
        return customers;
    }
}
