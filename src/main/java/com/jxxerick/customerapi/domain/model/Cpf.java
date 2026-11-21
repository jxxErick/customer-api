package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

/** Brazilian individual taxpayer id, stored as 11 digits with valid check digits. */
public record Cpf(String value) {

    private static final int LENGTH = 11;

    public Cpf {
        String digits = Validations.requireText(value, "CPF é obrigatório").replaceAll("\\D", "");
        if (digits.length() != LENGTH || digits.chars().distinct().count() == 1 || !hasValidCheckDigits(digits)) {
            throw new DomainValidationException("CPF inválido");
        }
        value = digits;
    }

    /** 529.982.247-25 */
    public String formatted() {
        return value.substring(0, 3) + "." + value.substring(3, 6) + "." + value.substring(6, 9) + "-"
                + value.substring(9);
    }

    /** Hides the first and last digits, as recommended for display (***.982.247-**). */
    public String masked() {
        return "***." + value.substring(3, 6) + "." + value.substring(6, 9) + "-**";
    }

    private static boolean hasValidCheckDigits(String digits) {
        return checkDigit(digits, 9) == digits.charAt(9) - '0' && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    /** Mod 11 over the first {@code length} digits, weights from {@code length + 1} down to 2. */
    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum * 10 % 11;
        return remainder == 10 ? 0 : remainder;
    }

    /** Never prints the full document. */
    @Override
    public String toString() {
        return masked();
    }
}
