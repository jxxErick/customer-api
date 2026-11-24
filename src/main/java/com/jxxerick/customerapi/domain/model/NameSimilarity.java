package com.jxxerick.customerapi.domain.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/** Fuzzy name comparison: Jaro-Winkler over accent-free, lower-case names. */
public final class NameSimilarity {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final double PREFIX_SCALE = 0.1;
    private static final int MAX_PREFIX = 4;

    private NameSimilarity() {
    }

    /** "  José  da Silva " becomes "jose da silva". */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String plain = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        String letters = NON_LETTERS.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return SPACES.matcher(letters).replaceAll(" ").strip();
    }

    /** Best of the plain and token-sorted comparison, so "Silva Maria" matches "Maria Silva". */
    public static double score(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        return Math.max(jaroWinkler(a, b), jaroWinkler(sortTokens(a), sortTokens(b)));
    }

    static double jaroWinkler(String a, String b) {
        double jaro = jaro(a, b);
        int prefix = 0;
        int limit = Math.min(MAX_PREFIX, Math.min(a.length(), b.length()));
        while (prefix < limit && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * PREFIX_SCALE * (1 - jaro);
    }

    static double jaro(String a, String b) {
        if (a.equals(b)) {
            return a.isEmpty() ? 0 : 1;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int window = Math.max(0, Math.max(a.length(), b.length()) / 2 - 1);
        boolean[] matchedA = new boolean[a.length()];
        boolean[] matchedB = new boolean[b.length()];
        int matches = 0;
        for (int i = 0; i < a.length(); i++) {
            int from = Math.max(0, i - window);
            int to = Math.min(b.length() - 1, i + window);
            for (int j = from; j <= to; j++) {
                if (!matchedB[j] && a.charAt(i) == b.charAt(j)) {
                    matchedA[i] = true;
                    matchedB[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0;
        }
        int transpositions = 0;
        for (int i = 0, j = 0; i < a.length(); i++) {
            if (!matchedA[i]) {
                continue;
            }
            while (!matchedB[j]) {
                j++;
            }
            if (a.charAt(i) != b.charAt(j)) {
                transpositions++;
            }
            j++;
        }
        double m = matches;
        return (m / a.length() + m / b.length() + (m - transpositions / 2.0) / m) / 3;
    }

    private static String sortTokens(String name) {
        String[] tokens = name.split(" ");
        Arrays.sort(tokens);
        return String.join(" ", tokens);
    }
}
