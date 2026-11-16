package controller;

import dao.ShopSettingDAO;
import model.ShopSettingEntry;
import model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminSettingsControllerTest {
    static class FakeDAO extends ShopSettingDAO {
        final Map<String, String> values = new LinkedHashMap<>(Map.of(
                "points_step", "10", "delivery_fee", "15000", "payment_bank_name", "MBBank",
                "payment_account_no", "12345", "shift_morning", "08:00-12:00", "other_setting", "old"));
        Map<String, String> saved;
        boolean fail;
        @Override public List<ShopSettingEntry> findAll() {
            List<ShopSettingEntry> entries = new ArrayList<>();
            values.forEach((key, value) -> entries.add(new ShopSettingEntry(key, value, null)));
            return entries;
        }
        @Override public boolean updateAll(Map<String, String> changes) {
            saved = new LinkedHashMap<>(changes);
            if (fail) return false;
            values.putAll(changes);
            return true;
        }
    }

    private static BaristaControllerTest.Exchange exchange(FakeDAO dao) {
        BaristaControllerTest.Exchange x = new BaristaControllerTest.Exchange();
        User admin = new User();
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        x.sessionValues.put("user", admin);
        x.sessionValues.put("settingsCsrfToken", "settings-test-token");
        x.params.put("csrfToken", "settings-test-token");
        dao.values.forEach((key, value) -> x.params.put("setting_" + key, value));
        return x;
    }

    @Test void onePostSavesAllGroupsAndGetDisplaysSavedValues() throws Exception {
        FakeDAO dao = new FakeDAO();
        AdminSettingsController controller = new AdminSettingsController(dao);
        BaristaControllerTest.Exchange post = exchange(dao);
        post.params.put("setting_points_step", "20");
        post.params.put("setting_delivery_fee", "20000");
        post.params.put("setting_payment_bank_name", "VCB");
        post.params.put("setting_shift_morning", "09:00-13:00");
        post.params.put("setting_other_setting", "new");
        controller.doPost(post.request, post.response);
        assertEquals("/cafe/admin/settings", post.redirected);
        assertEquals(5, dao.saved.size());
        BaristaControllerTest.Exchange get = exchange(dao);
        controller.doGet(get.request, get.response);
        assertEquals("/admin-settings.jsp", get.forwarded);
        assertEquals(dao.values, get.attributes.get("form"));
        Map<?, ?> groups = (Map<?, ?>) get.attributes.get("settingGroups");
        assertEquals(List.of("Dùng điểm Aura trừ vào đơn", "Giao hàng & Freeship", "Thông tin ngân hàng", "Ca làm việc", "Cài đặt khác"), new ArrayList<>(groups.keySet()));
    }

    @Test void validationAndDatabaseErrorsPreserveEverySubmittedValueWithoutPartialSave() throws Exception {
        FakeDAO dao = new FakeDAO();
        AdminSettingsController controller = new AdminSettingsController(dao);
        BaristaControllerTest.Exchange invalid = exchange(dao);
        invalid.params.put("setting_points_step", "0");
        invalid.params.put("setting_payment_bank_name", "New bank");
        invalid.params.put("setting_shift_morning", "25:00-12:00");
        controller.doPost(invalid.request, invalid.response);
        assertNull(dao.saved);
        assertEquals("/admin-settings.jsp", invalid.forwarded);
        Map<?, ?> form = (Map<?, ?>) invalid.attributes.get("form");
        assertEquals("New bank", form.get("payment_bank_name"));
        assertEquals("25:00-12:00", form.get("shift_morning"));
        assertEquals("MBBank", dao.values.get("payment_bank_name"));

        BaristaControllerTest.Exchange missing = exchange(dao);
        missing.params.remove("setting_payment_account_no");
        controller.doPost(missing.request, missing.response);
        assertNull(dao.saved);
        assertNotNull(missing.attributes.get("settingsError"));

        dao.fail = true;
        BaristaControllerTest.Exchange failed = exchange(dao);
        failed.params.put("setting_payment_bank_name", "New bank");
        controller.doPost(failed.request, failed.response);
        assertEquals("New bank", ((Map<?, ?>) failed.attributes.get("form")).get("payment_bank_name"));
        assertEquals("MBBank", dao.values.get("payment_bank_name"));
        assertNotNull(failed.attributes.get("settingsError"));
    }

    @Test void missingCsrfCannotSaveSettings() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaControllerTest.Exchange post = exchange(dao);
        post.params.remove("csrfToken");
        new AdminSettingsController(dao).doPost(post.request, post.response);
        assertEquals(403, post.status);
        assertNull(dao.saved);
        assertNull(post.redirected);
        assertNull(post.forwarded);
    }

    @Test void validatesExistingNumericAndShiftRules() {
        assertNull(AdminSettingsController.validate("delivery_minutes", "5"));
        assertNotNull(AdminSettingsController.validate("delivery_minutes", "301"));
        assertNull(AdminSettingsController.validate("shift_evening", "18:00-23:30"));
        assertNotNull(AdminSettingsController.validate("shift_evening", "18:00-24:00"));
        assertNotNull(AdminSettingsController.validate("payment_bank_name", " "));
    }
}
