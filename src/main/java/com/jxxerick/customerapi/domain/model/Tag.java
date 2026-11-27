package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Free label such as "vip" or "zona-sul": lowercase, no accents, words joined by hyphens. */
public record Tag(String value) implements Comparable<Tag> {

    public static final int MAX_LENGTH = 30;

    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s_]+");

    public Tag {
        Validations.requireNonNull(value, "Tag é obrigatória");
        String normalized = normalize(value);
        if (normalized.length() > MAX_LENGTH || !VALID.matcher(normalized).matches()) {
            throw new DomainValidationException("Tag inválida: " + value.strip());
        }
        value = normalized;
    }

    /** "  Zona Sul " and "zona_sul" both become "zona-sul"; "Área VIP" becomes "area-vip". */
    private static String normalize(String raw) {
        String withoutAccents = MARKS.matcher(Normalizer.normalize(raw.strip(), Normalizer.Form.NFD)).replaceAll("");
        return SEPARATORS.matcher(withoutAccents.toLowerCase()).replaceAll("-");
    }

    @Override
    public int compareTo(Tag other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
