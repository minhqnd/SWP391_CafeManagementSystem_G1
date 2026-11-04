package controller;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AdminRestockViewTest {
    @Test void purchasesLoadsSharedAdminLayoutAndPreservesActions() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/admin-purchases.jsp"));
        assertTrue(jsp.contains("href=\"${ctx}/assets/css/admin-schedule.css\""));
        assertTrue(jsp.contains("family=Material+Symbols+Outlined"));
        assertTrue(jsp.contains("family=Plus+Jakarta+Sans"));
        assertTrue(jsp.contains("<body class=\"admin-body\">"));
        assertTrue(jsp.contains("<main class=\"admin-main-wrapper\">"));
        assertTrue(jsp.contains("/components/admin-sidebar.jsp"));
        assertFalse(jsp.contains("margin-left:190px"));
        for (String action : new String[]{"create", "order", "cancel", "close", "edit"}) {
            assertTrue(jsp.contains("value=\"" + action + "\""));
        }
    }

    @Test void restockLoadsSharedAdminLayoutAndIconFont() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/admin-restock.jsp"));
        assertTrue(jsp.contains("href=\"${ctx}/assets/css/admin-schedule.css\""));
        assertTrue(jsp.contains("family=Material+Symbols+Outlined"));
        assertTrue(jsp.contains("family=Plus+Jakarta+Sans"));
        assertTrue(jsp.contains("<body class=\"admin-body\">"));
        assertTrue(jsp.contains("admin-main-wrapper"));
        assertTrue(jsp.contains("/components/admin-sidebar.jsp"));
        assertFalse(jsp.contains("margin-left:190px"));
        assertTrue(jsp.contains("name=\"csrfToken\""));
    }
}
