package com.jxxerick.customerapi.domain.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Two customers that are probably the same person, with a 0..1 score and why. */
public record DuplicateCandidate(Customer first, Customer second, double score, Set<DuplicateReason> reasons) {

    static final double NAME_THRESHOLD = 0.85;
    static final double SAME_ZIP_BONUS = 0.05;
    static final double SAME_EMAIL_USER_BONUS = 0.10;

    public DuplicateCandidate {
        reasons = reasons.isEmpty() ? EnumSet.noneOf(DuplicateReason.class) : EnumSet.copyOf(reasons);
    }

    /** Empty when nothing besides chance links the two customers. */
    public static Optional<DuplicateCandidate> evaluate(Customer first, Customer second) {
        Set<DuplicateReason> reasons = EnumSet.noneOf(DuplicateReason.class);
        double nameScore = NameSimilarity.score(first.getName(), second.getName());
        if (NameSimilarity.normalize(first.getName()).equals(NameSimilarity.normalize(second.getName()))) {
            reasons.add(DuplicateReason.SAME_NAME);
        } else if (nameScore >= NAME_THRESHOLD) {
            reasons.add(DuplicateReason.SIMILAR_NAME);
        }
        boolean sameEmailUser = emailUser(first.getEmail()).equals(emailUser(second.getEmail()));
        if (reasons.isEmpty() && !sameEmailUser) {
            return Optional.empty();
        }
        double score = nameScore;
        if (sameEmailUser) {
            reasons.add(DuplicateReason.SAME_EMAIL_USER);
            score += SAME_EMAIL_USER_BONUS;
        }
        if (first.getAddress().zipCode().equals(second.getAddress().zipCode())) {
            reasons.add(DuplicateReason.SAME_ZIP_CODE);
            score += SAME_ZIP_BONUS;
        }
        double rounded = Math.round(Math.min(1, score) * 1000) / 1000.0;
        return Optional.of(new DuplicateCandidate(first, second, rounded, reasons));
    }

    /** "Maria.Silva+promo@x.com" and "mariasilva@y.com" share the user "mariasilva". */
    static String emailUser(Email email) {
        String local = email.value().substring(0, email.value().indexOf('@'));
        int tag = local.indexOf('+');
        if (tag >= 0) {
            local = local.substring(0, tag);
        }
        return local.replace(".", "").toLowerCase(Locale.ROOT);
    }
}
