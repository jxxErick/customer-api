package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateCandidateTest {

    private static final Address OTHER_ADDRESS = new Address(
            new PostalAddress(new ZipCode("20040-002"), "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ"),
            "10", null);

    private static Customer customer(String name, String email, Address address) {
        return Customer.create(name, new Email(email), address, Fixtures.NOW);
    }

    @Test
    void shouldFlagSameNameAndZipCode() {
        Customer first = customer("Maria Silva", "maria@example.com", Fixtures.address());
        Customer second = customer("MARIA SILVA", "msilva@work.com", Fixtures.address());

        DuplicateCandidate candidate = DuplicateCandidate.evaluate(first, second).orElseThrow();

        assertThat(candidate.score()).isEqualTo(1.0);
        assertThat(candidate.reasons()).containsExactly(DuplicateReason.SAME_NAME, DuplicateReason.SAME_ZIP_CODE);
    }

    @Test
    void shouldFlagSimilarNameWithoutBonusWhenNothingElseMatches() {
        Customer first = customer("Maria Silva", "maria@example.com", Fixtures.address());
        Customer second = customer("Maira Silva", "other@example.com", OTHER_ADDRESS);

        DuplicateCandidate candidate = DuplicateCandidate.evaluate(first, second).orElseThrow();

        assertThat(candidate.reasons()).containsExactly(DuplicateReason.SIMILAR_NAME);
        assertThat(candidate.score()).isEqualTo(Math.round(NameSimilarity.score("Maria Silva", "Maira Silva") * 1000)
                / 1000.0);
    }

    @Test
    void shouldFlagSameEmailUserEvenWithDifferentNames() {
        Customer first = customer("Maria Silva", "Maria.Silva+promo@gmail.com", Fixtures.address());
        Customer second = customer("M. S. Costa", "mariasilva@hotmail.com", OTHER_ADDRESS);

        Optional<DuplicateCandidate> candidate = DuplicateCandidate.evaluate(first, second);

        assertThat(candidate).hasValueSatisfying(found ->
                assertThat(found.reasons()).containsExactly(DuplicateReason.SAME_EMAIL_USER));
    }

    @Test
    void shouldIgnoreUnrelatedCustomers() {
        Customer first = customer("Maria Silva", "maria@example.com", Fixtures.address());
        Customer second = customer("João Pereira", "joao@example.com", Fixtures.address());

        assertThat(DuplicateCandidate.evaluate(first, second)).isEmpty();
    }

    @Test
    void shouldExtractEmailUser() {
        assertThat(DuplicateCandidate.emailUser(new Email("Jo.Ao+news@x.com"))).isEqualTo("joao");
    }
}
