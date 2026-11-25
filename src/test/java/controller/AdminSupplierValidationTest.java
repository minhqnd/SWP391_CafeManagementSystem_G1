package controller;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class AdminSupplierValidationTest {

    @Test
    void validateNameChecksRequiredAndMaxLength() {
        assertEquals("Tên nhà cung cấp không được để trống.", AdminSupplierController.validateName(null));
        assertEquals("Tên nhà cung cấp không được để trống.", AdminSupplierController.validateName("   "));
        assertEquals("Tên nhà cung cấp không được vượt quá 150 ký tự.",
                AdminSupplierController.validateName("a".repeat(151)));
        assertNull(AdminSupplierController.validateName("Fresh Bean Supplier"));
    }

    @Test
    void validatePhoneAllowsEmptyAndValidFormats() {
        assertNull(AdminSupplierController.validatePhone(null));
        assertNull(AdminSupplierController.validatePhone("   "));
        assertNull(AdminSupplierController.validatePhone("0912345678"));
        assertNull(AdminSupplierController.validatePhone("+84 912-345.678"));

        assertTrue(AdminSupplierController.validatePhone("abc123456").contains("không hợp lệ"));
        assertTrue(AdminSupplierController.validatePhone("09123").contains("không hợp lệ"));
        assertTrue(AdminSupplierController.validatePhone("0".repeat(21)).contains("20 ký tự"));
    }

    @Test
    void validateAddressChecksMaxLength() {
        assertNull(AdminSupplierController.validateAddress(null));
        assertNull(AdminSupplierController.validateAddress("Cau Giay, Hanoi"));
        assertEquals("Địa chỉ không được vượt quá 500 ký tự.",
                AdminSupplierController.validateAddress("a".repeat(501)));
    }

    @Test
    void parseStatusValidatesEnumValues() {
        assertEquals("ACTIVE", AdminSupplierController.parseStatus(null, true));
        assertEquals("INACTIVE", AdminSupplierController.parseStatus(" inactive ", false));
        assertThrows(IllegalArgumentException.class,
                () -> AdminSupplierController.parseStatus("DELETED", true));
        assertThrows(IllegalArgumentException.class,
                () -> AdminSupplierController.parseStatus("", false));
    }

    @Test
    void supplierErrorMessageHidesTechnicalDetails() {
        assertTrue(AdminSupplierController.supplierErrorMessage(new SQLException("Data too long", "22001"))
                .contains("150 ký tự"));
        assertTrue(AdminSupplierController.supplierErrorMessage(new SQLException("FK constraint", "23000"))
                .contains("ràng buộc"));
        assertFalse(AdminSupplierController.supplierErrorMessage(new SQLException("secret SQL detail", "08000"))
                .contains("secret"));
        assertTrue(AdminSupplierController.supplierErrorMessage(new NumberFormatException("bad id"))
                .contains("không hợp lệ"));
    }
}
