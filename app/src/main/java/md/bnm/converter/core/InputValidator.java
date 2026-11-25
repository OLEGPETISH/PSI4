package md.bnm.converter.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Pure Java validator for user inputs (monetary amounts, currency codes, and BNM dates).
 */
public final class InputValidator {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final DateTimeFormatter BNM_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private InputValidator() {
    }

    /**
     * Checks whether the "Convert" button should be enabled in the UI.
     */
    public static boolean isConvertReady(String rawAmount, String fromCurrency, String toCurrency) {
        return rawAmount != null && !rawAmount.trim().isEmpty()
                && fromCurrency != null && !fromCurrency.trim().isEmpty()
                && toCurrency != null && !toCurrency.trim().isEmpty();
    }

    /**
     * Validates and parses a user-entered monetary amount string.
     * Supports both dot ('.') and comma (',') decimal separators.
     */
    public static BigDecimal validateAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            throw new ValidationException("Введите сумму для конвертации (поле не может быть пустым).");
        }

        String normalized = rawAmount.trim().replace(" ", "").replace(',', '.');

        if (normalized.startsWith("-")) {
            try {
                new BigDecimal(normalized);
                throw new ValidationException("Сумма не может быть отрицательной. Введите число больше нуля.");
            } catch (NumberFormatException ignored) {
                throw new ValidationException("Некорректный формат суммы. Введите положительное число (например, 100 или 25.50).");
            }
        }

        BigDecimal value;
        try {
            value = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Некорректное значение суммы «" + rawAmount.trim() + "». Разрешены только цифры и разделитель дробной части.",
                    e
            );
        }

        if (value.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Сумма конвертации должна быть больше нуля (0 не допускается).");
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Сумма не может быть отрицательной.");
        }

        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("Введённая сумма превышает максимально допустимый предел (1 000 000 000 000).");
        }

        return value;
    }

    /**
     * Validates that a currency code is non-empty and consists of 3 Latin letters.
     */
    public static String validateCurrencyCode(String rawCode, String fieldName) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new ValidationException("Не выбрана " + fieldName + ".");
        }
        String code = rawCode.trim().toUpperCase();
        if (!code.matches("^[A-Z]{3}$")) {
            throw new ValidationException("Некорректный код валюты: «" + rawCode + "».");
        }
        return code;
    }

    /**
     * Validates a date string in DD.MM.YYYY format.
     */
    public static LocalDate validateDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            throw new ValidationException("Дата курса не может быть пустой. Используйте формат ДД.ММ.ГГГГ.");
        }
        try {
            return LocalDate.parse(rawDate.trim(), BNM_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Некорректная дата «" + rawDate.trim() + "». Укажите существующую дату в формате ДД.ММ.ГГГГ.",
                    e
            );
        }
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Дата не указана.");
        }
        return date.format(BNM_DATE_FORMATTER);
    }
}
