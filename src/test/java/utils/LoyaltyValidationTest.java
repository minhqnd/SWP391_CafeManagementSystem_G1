package utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoyaltyValidationTest {
    @Test void acceptsExactBoundaries() {
        assertEquals(1, parse("1", 1, 1_000_000));
        assertEquals(1_000_000, parse("1000000", 1, 1_000_000));
        assertEquals(999_999, parse("999999", 1, 1_000_000));
        assertEquals(2, parse("2", 1, 1_000_000));
        assertEquals(0, parse("0", 0, 100_000_000));
        assertEquals(100_000_000, parse("100000000", 0, 100_000_000));
        assertEquals(99_999_999, parse("99999999", 0, 100_000_000));
        assertEquals(1, parse("1", 0, 100_000_000));
        assertEquals(Integer.MAX_VALUE, parse("2147483647", 1, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE - 1, parse("2147483646", 1, Integer.MAX_VALUE));
        assertEquals(10, parse(" 10 ", 1, 1_000_000));
    }

    @Test void rejectsMalformedAndOutOfRangeBeforeAnyNarrowing() {
        String[] invalid = {null, "", " ", "0", "-1", "-4294967295", "-9223372036854775808",
                "1000001", "2147483647", "2147483648", "4294967297", "9223372036854775807",
                "9223372036854775808", "999999999999999999999999", "1.0", "1e3", "NaN", "Infinity",
                "+1", "1a", "1 0", "１", "<script>"};
        for (String value : invalid) {
            assertThrows(IllegalArgumentException.class, () -> parse(value, 1, 1_000_000), String.valueOf(value));
        }
        assertThrows(IllegalArgumentException.class, () -> parse("100000001", 0, 100_000_000));
        assertThrows(IllegalArgumentException.class, () -> parse("2147483648", 1, Integer.MAX_VALUE));
    }

    private long parse(String raw, long min, long max) {
        return LoyaltyValidation.wholeNumber(raw, "Test", min, max);
    }
}
