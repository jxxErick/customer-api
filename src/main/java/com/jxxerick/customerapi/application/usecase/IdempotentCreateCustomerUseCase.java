package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.domain.exception.IdempotencyKeyReusedException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.IdempotencyKey;
import com.jxxerick.customerapi.domain.model.IdempotencyRecord;
import com.jxxerick.customerapi.domain.port.IdempotencyStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Creates a customer at most once per Idempotency-Key: a retry with the same body gets the
 * original customer back, and the same key with a different body is rejected.
 */
public class IdempotentCreateCustomerUseCase {

    private final CreateCustomerUseCase createCustomer;
    private final GetCustomerUseCase getCustomer;
    private final IdempotencyStore store;
    private final Clock clock;
    private final Duration ttl;

    public IdempotentCreateCustomerUseCase(CreateCustomerUseCase createCustomer, GetCustomerUseCase getCustomer,
                                           IdempotencyStore store, Clock clock, Duration ttl) {
        this.createCustomer = createCustomer;
        this.getCustomer = getCustomer;
        this.store = store;
        this.clock = clock;
        this.ttl = ttl;
    }

    public Result execute(String rawKey, CreateCustomerCommand command) {
        IdempotencyKey key = new IdempotencyKey(rawKey);
        String hash = fingerprint(command);
        Instant now = clock.instant();

        Optional<IdempotencyRecord> previous = store.find(key).filter(r -> !r.isExpired(now, ttl));
        if (previous.isPresent()) {
            if (!previous.get().matches(hash)) {
                throw new IdempotencyKeyReusedException(key.value());
            }
            return new Result(getCustomer.execute(previous.get().customerId()), true);
        }

        Customer customer = createCustomer.execute(command);
        store.save(new IdempotencyRecord(key, hash, customer.getId(), now));
        return new Result(customer, false);
    }

    /** SHA-256 of every field, separated by a char that can't appear in JSON text input. */
    static String fingerprint(CreateCustomerCommand command) {
        String canonical = String.join("\u0000", Stream.of(command.name(), command.email(), command.zipCode(),
                        command.number(), command.complement(), command.cpf())
                .map(value -> Objects.toString(value, "\u0001"))
                .toList());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** {@code replayed} is true when the customer came from an earlier request. */
    public record Result(Customer customer, boolean replayed) {
    }
}
