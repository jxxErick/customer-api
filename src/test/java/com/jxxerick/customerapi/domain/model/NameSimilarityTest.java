package com.jxxerick.customerapi.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NameSimilarityTest {

    @Test
    void shouldNormalizeAccentsCaseAndSpaces() {
        assertThat(NameSimilarity.normalize("  José  da SILVA-Júnior ")).isEqualTo("jose da silva junior");
        assertThat(NameSimilarity.normalize(null)).isEmpty();
    }

    @Test
    void shouldMatchClassicJaroWinklerValues() {
        assertThat(NameSimilarity.jaroWinkler("martha", "marhta")).isCloseTo(0.961, within(0.001));
        assertThat(NameSimilarity.jaroWinkler("dixon", "dicksonx")).isCloseTo(0.813, within(0.001));
    }

    @Test
    void shouldScoreIdenticalNamesAsOne() {
        assertThat(NameSimilarity.score("Maria Silva", "MARIA  SILVA")).isEqualTo(1.0);
    }

    @Test
    void shouldIgnoreTokenOrder() {
        assertThat(NameSimilarity.score("Silva Maria", "Maria Silva")).isEqualTo(1.0);
    }

    @Test
    void shouldScoreTyposHighAndDifferentNamesLow() {
        assertThat(NameSimilarity.score("Maria Silva", "Maira Silva")).isGreaterThan(0.9);
        assertThat(NameSimilarity.score("Maria Silva", "João Pereira")).isLessThan(0.7);
    }

    @Test
    void shouldScoreEmptyNamesAsZero() {
        assertThat(NameSimilarity.score("", "Maria")).isZero();
    }
}
