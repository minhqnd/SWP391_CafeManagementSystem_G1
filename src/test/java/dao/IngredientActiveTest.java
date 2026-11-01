package dao;

import model.Ingredient;
import model.ProductIngredient;
import model.PurchaseOrder;
import model.PurchaseOrderItem;
import model.RestockRequest.RestockItem;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IngredientActiveTest {
    private boolean active;
    private boolean exists = true;
    private boolean oldRecipeLine;
    private int commits;
    private int rollbacks;
    private final List<String> writes = new ArrayList<>();
    private final List<Object> bindings = new ArrayList<>();

    private Connection connection() {
        return mock(Connection.class, (p,m,a) -> switch (m.getName()) {
            case "prepareStatement" -> statement((String) a[0]);
            case "commit" -> { commits++; yield null; }
            case "rollback" -> { rollbacks++; yield null; }
            default -> null;
        });
    }
    private PreparedStatement statement(String sql) {
        return mock(PreparedStatement.class, (p,m,a) -> {
            if (m.getName().startsWith("set") && a != null && a.length == 2) bindings.add(a[1]);
            if (m.getName().equals("executeQuery")) {
                boolean row = sql.contains("FROM product_ingredients") ? oldRecipeLine : exists;
                boolean[] first = {true};
                return mock(ResultSet.class, (rp,rm,ra) -> switch (rm.getName()) {
                    case "next" -> { boolean next = first[0] && row; first[0] = false; yield next; }
                    case "getBoolean" -> active;
                    case "getInt" -> 8;
                    default -> null;
                });
            }
            if (m.getName().equals("executeUpdate")) { writes.add(sql); return exists ? 1 : 0; }
            if (m.getName().equals("executeBatch")) { writes.add(sql); return new int[]{1}; }
            return null;
        });
    }
    private IngredientDAO dao() {
        return new IngredientDAO() { @Override protected Connection getConnection() { return connection(); } };
    }

    @Test void deactivateOnlyUpdatesFlagNotStockOrHistory() throws Exception {
        assertTrue(dao().updateIngredientActive(8, false));
        assertEquals(List.of("UPDATE ingredients SET is_active = ? WHERE id = ?"), writes);
        assertEquals(List.of(false, 8), bindings);
    }
    @Test void reactivateUsesSameRow() throws Exception {
        assertTrue(dao().updateIngredientActive(8, true));
        assertEquals(List.of(true, 8), bindings);
    }
    @Test void missingIngredientDoesNotReportSuccess() throws Exception {
        exists = false;
        assertFalse(dao().updateIngredientActive(8, false));
    }
    @Test void invalidIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> dao().updateIngredientActive(0, false));
        assertTrue(writes.isEmpty());
    }
    @Test void inactiveOrMissingCannotBeSelectedForNewWork() {
        assertEquals("45000", assertThrows(SQLException.class,
                () -> IngredientDAO.requireActive(connection(), 8)).getSQLState());
        active = true; exists = false;
        assertThrows(SQLException.class, () -> IngredientDAO.requireActive(connection(), 8));
    }
    @Test void activeIngredientIsAccepted() throws Exception {
        active = true;
        IngredientDAO.requireActive(connection(), 8);
        assertTrue(writes.isEmpty());
    }
    @Test void existingInactiveRecipeCanBeSavedWithoutDroppingItsLines() throws Exception {
        oldRecipeLine = true;
        recipes().saveRecipe(13, List.of(new ProductIngredient(13,8,150,"Lemon","piece")));
        assertEquals(1, commits);
        assertEquals(0, rollbacks);
        assertEquals(2, writes.size());
    }
    @Test void cannotAddInactiveIngredientToNewRecipe() {
        assertThrows(SQLException.class, () -> recipes().saveRecipe(13, List.of(new ProductIngredient(13,8,150,"Lemon","piece"))));
        assertTrue(writes.isEmpty());
        assertEquals(1, rollbacks);
    }
    private ProductIngredientDAO recipes() {
        return new ProductIngredientDAO() { @Override protected Connection getConnection() { return connection(); } };
    }
    @Test void cannotCreatePurchaseForInactiveIngredient() {
        PurchaseOrderDAO orders = new PurchaseOrderDAO() { @Override protected Connection getConnection() { return connection(); } };
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setIngredientId(8); item.setQuantity(BigDecimal.ONE); item.setUnitPrice(BigDecimal.TEN);
        assertThrows(SQLException.class, () -> orders.create(new PurchaseOrder(), List.of(item)));
        assertTrue(writes.isEmpty());
        assertEquals(1, rollbacks);
    }
    @Test void cannotCreateRestockForInactiveIngredient() {
        RestockRequestDAO requests = new RestockRequestDAO() { @Override protected Connection getConnection() { return connection(); } };
        assertThrows(SQLException.class, () -> requests.createRestockRequest(1,"DRAFT","test","",
                List.of(new RestockItem(8,"Lemon","piece",BigDecimal.ONE))));
        assertTrue(writes.isEmpty());
        assertEquals(1, rollbacks);
    }
    @Test void activePickerFiltersInactiveButAdminListIsUnchanged() {
        Ingredient available = new Ingredient(1,"Milk","ml");
        Ingredient hidden = new Ingredient(8,"Lemon","piece"); hidden.setActive(false);
        IngredientDAO dao = new IngredientDAO() {
            @Override public List<Ingredient> getAllIngredientsWithDetails(String q,Integer supplier) {
                return List.of(available,hidden);
            }
        };
        assertEquals(List.of(available), dao.getActiveIngredientsWithDetails());
        assertEquals(2, dao.getAllIngredientsWithDetails(null,null).size());
    }
    @Test void viewOnlyOffersEditWithoutToggleOrDelete() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/admin-ingredients.jsp"));
        assertTrue(jsp.contains("title=\"Chỉnh sửa\""));
        assertFalse(jsp.contains("name=\"action\" value=\"toggle\""));
        assertFalse(jsp.contains("visibility_off"));
        assertFalse(jsp.contains("name=\"action\" value=\"delete\""));
    }
    private static <T> T mock(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
