package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.IdempotencyKeyReusedException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.IdempotencyKey;
import com.jxxerick.customerapi.domain.model.IdempotencyRecord;
import com.jxxerick.customerapi.domain.port.IdempotencyStore;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentCreateCustomerUseCaseTest {

    private static final String KEY = "6f1c2a9e-key-0001";
    private static final Duration TTL = Duration.ofHours(24);

    @Mock
    private CreateCustomerUseCase createCustomer;

    @Mock
    private GetCustomerUseCase getCustomer;

    private final InMemoryStore store = new InMemoryStore();
    private final CreateCustomerCommand command =
            new CreateCustomerCommand("Maria Silva", "maria@example.com", "01001-000", "100", null);

    private IdempotentCreateCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = withClockAt(Fixtures.NOW);
    }

    @Test
    void shouldCreateOnFirstCallAndRememberTheKey() {
        Customer customer = Fixtures.customer();
        when(createCustomer.execute(command)).thenReturn(customer);

        IdempotentCreateCustomerUseCase.Result result = useCase.execute(KEY, command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.customer()).isSameAs(customer);
        assertThat(store.find(new IdempotencyKey(KEY))).get()
                .extracting(IdempotencyRecord::customerId).isEqualTo(customer.getId());
    }

    @Test
    void shouldReplayTheSameCustomerOnRetry() {
        Customer customer = Fixtures.customer();
        when(createCustomer.execute(command)).thenReturn(customer);
        when(getCustomer.execute(customer.getId())).thenReturn(customer);
        useCase.execute(KEY, command);

        IdempotentCreateCustomerUseCase.Result retry = withClockAt(Fixtures.NOW.plusSeconds(30)).execute(KEY, command);

        assertThat(retry.replayed()).isTrue();
        assertThat(retry.customer().getId()).isEqualTo(customer.getId());
        verify(createCustomer, times(1)).execute(any());
    }

    @Test
    void shouldRejectTheSameKeyWithADifferentBody() {
        when(createCustomer.execute(command)).thenReturn(Fixtures.customer());
        useCase.execute(KEY, command);

        CreateCustomerCommand other = new CreateCustomerCommand("Maria Silva", "maria@example.com", "01001-000",
                "200", null);

        assertThatThrownBy(() -> useCase.execute(KEY, other))
                .isInstanceOf(IdempotencyKeyReusedException.class)
                .hasMessageContaining(KEY);
        verify(createCustomer, times(1)).execute(any());
    }

    @Test
    void shouldCreateAgainOnceTheKeyExpired() {
        when(createCustomer.execute(command)).thenReturn(Fixtures.customer(), Fixtures.customer());
        useCase.execute(KEY, command);

        IdempotentCreateCustomerUseCase.Result later = withClockAt(Fixtures.NOW.plus(TTL)).execute(KEY, command);

        assertThat(later.replayed()).isFalse();
        verify(createCustomer, times(2)).execute(command);
    }

    @Test
    void shouldValidateTheKeyBeforeDoingAnything() {
        assertThatThrownBy(() -> useCase.execute("short", command)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> useCase.execute("has spaces in it", command))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(createCustomer, getCustomer);
    }

    @Test
    void fingerprintShouldDistinguishMissingFromEmptyFields() {
        String withNull = IdempotentCreateCustomerUseCase.fingerprint(command);
        String withEmpty = IdempotentCreateCustomerUseCase.fingerprint(
                new CreateCustomerCommand("Maria Silva", "maria@example.com", "01001-000", "100", ""));

        assertThat(withNull).hasSize(64).isEqualTo(IdempotentCreateCustomerUseCase.fingerprint(command));
        assertThat(withNull).isNotEqualTo(withEmpty);
    }

    private IdempotentCreateCustomerUseCase withClockAt(Instant instant) {
        return new IdempotentCreateCustomerUseCase(createCustomer, getCustomer, store,
                Clock.fixed(instant, ZoneOffset.UTC), TTL);
    }

    /** Simple map-backed store, enough for the use case rules. */
    private static final class InMemoryStore implements IdempotencyStore {
        private final Map<IdempotencyKey, IdempotencyRecord> records = new HashMap<>();

        @Override
        public Optional<IdempotencyRecord> find(IdempotencyKey key) {
            return Optional.ofNullable(records.get(key));
        }

        @Override
        public void save(IdempotencyRecord record) {
            records.put(record.key(), record);
        }

        @Override
        public int deleteCreatedBefore(Instant cutoff) {
            int before = records.size();
            records.values().removeIf(r -> r.createdAt().isBefore(cutoff));
            return before - records.size();
        }
    }
}
