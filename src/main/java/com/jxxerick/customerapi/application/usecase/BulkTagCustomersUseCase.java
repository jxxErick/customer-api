package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.BulkTagCommand;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adds and removes tags on many customers at once. Each customer is saved on its own, like the CSV
 * import, so one failure (not found, tag limit, concurrent edit) never blocks the others.
 */
public class BulkTagCustomersUseCase {

    public static final int MAX_CUSTOMERS = 1000;
    private static final int PAGE_SIZE = 100;

    private final GetSegmentUseCase getSegment;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public BulkTagCustomersUseCase(GetSegmentUseCase getSegment, CustomerRepository customerRepository,
                                   CustomerHistory history, Clock clock) {
        this.getSegment = getSegment;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    public Report execute(BulkTagCommand command) {
        Set<Tag> toAdd = parse(command.add());
        Set<Tag> toRemove = parse(command.remove());
        validateTags(toAdd, toRemove);
        List<UUID> targets = targets(command);

        List<Item> items = new ArrayList<>(targets.size());
        for (UUID id : targets) {
            items.add(apply(id, toAdd, toRemove));
        }
        return Report.of(items);
    }

    private Item apply(UUID id, Set<Tag> toAdd, Set<Tag> toRemove) {
        Optional<Customer> found = customerRepository.findById(id);
        if (found.isEmpty()) {
            return new Item(id, Outcome.NOT_FOUND, "Cliente não encontrado");
        }
        Customer customer = found.get();
        SortedSet<Tag> previous = new TreeSet<>(customer.getTags());
        try {
            if (!customer.changeTags(toAdd, toRemove)) {
                return new Item(id, Outcome.UNCHANGED, null);
            }
            Customer saved = customerRepository.save(customer);
            history.record(CustomerChange.retagged(saved, previous, clock.instant()));
            return new Item(id, Outcome.UPDATED, null);
        } catch (DomainValidationException exception) {
            return new Item(id, Outcome.REJECTED, exception.getMessage());
        } catch (ConcurrentUpdateException exception) {
            return new Item(id, Outcome.CONFLICT, exception.getMessage());
        }
    }

    /** Ids are collected before any change, since retagging moves customers in and out of tag-based segments. */
    private List<UUID> targets(BulkTagCommand command) {
        boolean byIds = !command.customerIds().isEmpty();
        if (byIds == (command.segmentId() != null)) {
            throw new DomainValidationException("Informe a lista de clientes ou um segmento (um dos dois)");
        }
        if (byIds) {
            ensureWithinLimit(command.customerIds().size());
            return List.copyOf(command.customerIds());
        }
        CustomerFilter filter = getSegment.execute(command.segmentId()).filter();
        ensureWithinLimit(customerRepository.countMatching(filter));
        List<UUID> ids = new ArrayList<>();
        PageResult<Customer> page;
        int number = 0;
        do {
            page = customerRepository.findMatching(filter, number++, PAGE_SIZE);
            page.items().forEach(customer -> ids.add(customer.getId()));
        } while (number < page.totalPages() && ids.size() < MAX_CUSTOMERS);
        return ids;
    }

    private static void ensureWithinLimit(long customers) {
        if (customers > MAX_CUSTOMERS) {
            throw new DomainValidationException(
                    "A operação atinge %d clientes; o máximo por vez é %d".formatted(customers, MAX_CUSTOMERS));
        }
    }

    private static void validateTags(Set<Tag> toAdd, Set<Tag> toRemove) {
        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            throw new DomainValidationException("Informe tags para adicionar ou remover");
        }
        Set<Tag> both = new TreeSet<>(toAdd);
        both.retainAll(toRemove);
        if (!both.isEmpty()) {
            throw new DomainValidationException(
                    "A tag %s não pode ser adicionada e removida ao mesmo tempo".formatted(both.iterator().next()));
        }
    }

    /** Fails on the first invalid tag, before touching any customer. */
    private static Set<Tag> parse(Collection<String> raw) {
        return raw.stream().filter(tag -> tag != null && !tag.isBlank()).map(Tag::new).collect(Collectors.toSet());
    }

    public enum Outcome {
        UPDATED,
        UNCHANGED,
        NOT_FOUND,
        REJECTED,
        CONFLICT
    }

    /** {@code message} explains failures and is null otherwise. */
    public record Item(UUID customerId, Outcome outcome, String message) {
    }

    public record Report(int total, int updated, int unchanged, int failed, List<Item> items) {

        static Report of(List<Item> items) {
            int updated = count(items, Outcome.UPDATED);
            int unchanged = count(items, Outcome.UNCHANGED);
            return new Report(items.size(), updated, unchanged, items.size() - updated - unchanged, List.copyOf(items));
        }

        private static int count(List<Item> items, Outcome outcome) {
            return (int) items.stream().filter(item -> item.outcome() == outcome).count();
        }
    }
}
