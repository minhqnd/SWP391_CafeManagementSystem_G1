package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

class ValidationBoundaryTest {
    private record Case(String name, String expected, Supplier<String> check) { }

    @TestFactory Stream<DynamicTest> exactBoundaryMessages() {
        List<Case> cases = new ArrayList<>();
        for (int size : new int[]{0, 1, 99, 100, 101}) {
            String name = "x".repeat(size);
            String expected = size == 0 ? "Vui lòng nhập họ và tên." : size > 100 ? "Họ và tên tối đa 100 ký tự." : null;
            cases.add(new Case("BF10 name length " + size, expected, () -> AccountValidation.nameError(AccountValidation.text(name))));
        }
        for (int size : new int[]{149, 150, 151}) {
            String email = "a".repeat(size - 10) + "@gmail.com";
            cases.add(new Case("BF10 email length " + size, size > 150 ? "Email tối đa 150 ký tự." : null, () -> AccountValidation.emailError(email)));
        }
        for (String email : new String[]{"x@gmaill.comm", "x@gmail.com.evil", "x@@gmail.com", ".x@gmail.com", "x..y@gmail.com", "x y@gmail.com"})
            cases.add(new Case("BF10 bad email " + email, "Sai định dạng email.", () -> AccountValidation.emailError(email)));
        for (String phone : new String[]{"096599999", "09659999999", "1965999999", "09659a9999", "０９６５９９９９９９"})
            cases.add(new Case("BF10 bad phone " + phone, "Số điện thoại sai định dạng.", () -> AccountValidation.phoneError(AccountValidation.phone(phone))));
        cases.add(new Case("BF10 exact 10 digit phone", null, () -> AccountValidation.phoneError("0965999999")));
        for (String empty : new String[]{null, "", " ", "\u00a0", "\u2003", "\u202f", "\ufeff"}) {
            cases.add(new Case("BF10 empty name " + empty, "Vui lòng nhập họ và tên.", () -> AccountValidation.nameError(AccountValidation.text(empty))));
            cases.add(new Case("BF10 empty email " + empty, "Vui lòng nhập email.", () -> AccountValidation.emailError(AccountValidation.email(empty))));
            cases.add(new Case("BF10 empty phone " + empty, "Vui lòng nhập số điện thoại.", () -> AccountValidation.phoneError(AccountValidation.phone(empty))));
            cases.add(new Case("BF11 empty category " + empty, "Vui lòng nhập tên danh mục.", () -> error(() -> MenuValidation.requiredName(empty, "Tên danh mục", 100))));
            cases.add(new Case("BF12 empty rule " + empty, "Vui lòng nhập mốc đổi.", () -> error(() -> LoyaltyValidation.wholeNumber(empty, "Mốc đổi", 1, 1_000_000))));
        }
        cases.add(new Case("BF10 separator-only phone", "Vui lòng nhập số điện thoại.", () -> AccountValidation.phoneError(AccountValidation.phone("().-"))));
        cases.add(new Case("BF10 formatted phone with Unicode spaces", null, () -> AccountValidation.phoneError(AccountValidation.phone("(096)\u00a0599-9999"))));
        cases.add(new Case("BF10 NUL is invalid, not whitespace", "Họ và tên chứa ký tự không được hỗ trợ.", () -> AccountValidation.nameError(AccountValidation.text("\u0000QA"))));
        for (int size : new int[]{7, 8, 254, 255, 256}) {
            String password = "A1" + "x".repeat(size - 2);
            cases.add(new Case("BF10 password length " + size, size < 8 || size > 255 ? "Mật khẩu phải từ 8 đến 255 ký tự." : null,
                    () -> AccountValidation.passwordError(password, password)));
        }
        for (String password : new String[]{"        ", "\u00a0".repeat(8), "\u2003".repeat(8)})
            cases.add(new Case("BF10 whitespace password", "Mật khẩu không được chỉ có khoảng trắng.", () -> AccountValidation.passwordError(password, password)));
        for (String confirmation : new String[]{null, "", "   ", "\u00a0"})
            cases.add(new Case("BF10 missing confirmation " + confirmation, "Vui lòng nhập mật khẩu xác nhận.", () -> AccountValidation.passwordError("Secret123", confirmation)));
        cases.add(new Case("BF10 mismatched confirmation", "Mật khẩu xác nhận không khớp.", () -> AccountValidation.passwordError("Secret123", "Other123")));
        cases.add(new Case("BF10 letters-only password", "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).", () -> AccountValidation.passwordError("abcdefgh", "abcdefgh")));
        cases.add(new Case("BF10 password with control", "Mật khẩu không được chứa ký tự điều khiển.", () -> AccountValidation.passwordError("Abcd12\u00007", "Abcd12\u00007")));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (String day : new String[]{"1899-12-31", "1900-02-29", "2001-02-29", today.plusDays(1).toString()})
            cases.add(new Case("BF10 invalid birthday " + day, "Ngày sinh phải là ngày có thật, từ năm 1900 đến hôm nay.", () -> AccountValidation.birthdayError(day)));
        for (String day : new String[]{"1900-01-01", "2000-02-29", today.toString()})
            cases.add(new Case("BF10 valid birthday " + day, null, () -> AccountValidation.birthdayError(day)));
        for (int max : new int[]{30, 50, 100, 150}) {
            for (int size : new int[]{max - 1, max, max + 1}) {
                String name = "x".repeat(size);
                cases.add(new Case("BF11 name limit " + max + " length " + size, size > max ? "Tên tối đa " + max + " ký tự." : null,
                        () -> error(() -> MenuValidation.requiredName(name, "Tên", max))));
            }
        }
        for (String value : new String[]{"0", "0.01", "9999999999.98", "9999999999.99", "0".repeat(63) + "1", "\u00a01.50\u00a0"})
            cases.add(new Case("BF11 valid price " + value, null, () -> error(() -> MenuValidation.money(value, "Giá"))));
        String[][] badPrices = {{"", "Vui lòng nhập giá."}, {"-0.01", "Giá không được âm."}, {"10000000000", "Giá không được vượt quá 9.999.999.999,99."},
                {"9999999999.991", "Giá không được vượt quá 9.999.999.999,99."}, {"1.001", "Giá có tối đa 2 chữ số thập phân."},
                {"abc", "Giá phải là số, dùng dấu chấm nếu có phần thập phân."}, {"1e3", "Giá phải là số, dùng dấu chấm nếu có phần thập phân."},
                {"1,5", "Giá phải là số, dùng dấu chấm nếu có phần thập phân."}, {"0".repeat(64) + "1", "Giá tối đa 64 ký tự."}};
        for (String[] entry : badPrices)
            cases.add(new Case("BF11 invalid price " + entry[0], entry[1], () -> error(() -> MenuValidation.money(entry[0], "Giá"))));
        for (String value : new String[]{"0.001", "999999999.998", "999999999.999"})
            cases.add(new Case("BF11 valid quantity " + value, null, () -> error(() -> MenuValidation.quantity(value, "Định lượng"))));
        String[][] badQuantities = {{"0", "Định lượng phải lớn hơn 0."}, {"-0.001", "Định lượng phải lớn hơn 0."},
                {"0.0001", "Định lượng có tối đa 3 chữ số thập phân."}, {"1000000000", "Định lượng không được vượt quá 999.999.999,999."}};
        for (String[] entry : badQuantities)
            cases.add(new Case("BF11 invalid quantity " + entry[0], entry[1], () -> error(() -> MenuValidation.quantity(entry[0], "Định lượng"))));
        for (long[] bounds : new long[][]{{1,100_000_000}, {1,1_000_000}, {0,100_000_000}}) {
            long min = bounds[0], max = bounds[1];
            for (long value : new long[]{min - 1, min, min + 1, max - 1, max, max + 1}) {
                String expected = value < 0 ? "Quy tắc phải là số nguyên từ " + min + " đến " + max + "."
                        : value < min || value > max ? "Quy tắc phải từ " + min + " đến " + max + "." : null;
                cases.add(new Case("BF12 " + min + ".." + max + " value " + value, expected, () -> error(() -> LoyaltyValidation.wholeNumber(Long.toString(value), "Quy tắc", min, max))));
            }
        }
        return cases.stream().map(c -> DynamicTest.dynamicTest(c.name(), () -> assertEquals(c.expected(), c.check().get())));
    }

    private static String error(Runnable run) {
        try { run.run(); return null; }
        catch (IllegalArgumentException e) { return e.getMessage(); }
    }
}
