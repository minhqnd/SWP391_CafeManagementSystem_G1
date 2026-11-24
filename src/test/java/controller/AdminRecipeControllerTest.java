package controller;

import dao.IngredientDAO;
import dao.ProductDAO;
import dao.ProductIngredientDAO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.Product;
import model.ProductIngredient;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminRecipeControllerTest {
    private final Map<String, Object> sessionValues = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> parameters = new HashMap<>();
    private String redirect;
    private String forward;
    private int responseError;
    private int saves;
    private boolean failSave;
    private Throwable loadFailure;
    private AdminRecipeController controller;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        sessionValues.put("user", admin);
        sessionValues.put("recipeCsrfToken", "test-csrf");
        parameters.put("action", new String[]{"save"});
        parameters.put("csrfToken", new String[]{"test-csrf"});
        parameters.put("productId", new String[]{"13"});
        Product product = new Product();
        product.setId(13);
        product.setName("QA Latte");
        Product other = new Product();
        other.setId(14);
        other.setName("Other product");
        ProductDAO products = new ProductDAO() {
            @Override public Product getProductForAdmin(int id) { return id == 13 ? product : other; }
            @Override public List<Product> getProductsForAdmin(Integer category, String keyword) {
                return List.of(product, other);
            }
        };
        IngredientDAO ingredients = new IngredientDAO() {
            @Override public List<Ingredient> getAllIngredients() {
                return List.of(new Ingredient(1, "Fresh milk", "ml"));
            }
            @Override public Map<Integer, BigDecimal> getLatestPurchasePrices() {
                return Map.of(1, new BigDecimal("150"));
            }
        };
        ProductIngredientDAO recipes = new ProductIngredientDAO() {
            @Override public Map<Integer, List<ProductIngredient>> getAllProductIngredientsMap() {
                return Map.of(13, List.of(new ProductIngredient(13, 1, 150, "Fresh milk", "ml")));
            }
            @Override public void saveRecipe(int productId, List<ProductIngredient> rows) throws Exception {
                saves++;
                if (failSave) throw new Exception("test database failure");
            }
        };
        ServletContext context = proxy(ServletContext.class, (p, m, args) -> {
            if (m.getName().equals("log") && args.length > 1
                    && args[0].toString().contains("load admin recipes")) loadFailure = (Throwable) args[1];
            return null;
        });
        controller = new AdminRecipeController(products, ingredients, recipes) {
            @Override public ServletContext getServletContext() { return context; }
        };
    }

    @Test
    void invalidSelectionKeepsSubmittedBlankInsteadOfRestoringOldMilk() throws Exception {
        submit(new String[]{""}, new String[]{"150"});
        assertEquals(0, saves);
        assertEquals("/cafe/admin/recipes?productId=13", redirect);
        load();
        assertNotNull(attributes.get("recipeError"));
        assertEquals(List.of(Map.of("ingredientId", "", "quantity", "150")), attributes.get("recipeFormRows"));
        assertEquals("/admin-recipes.jsp", forward);
        assertFalse(sessionValues.containsKey("recipeDraft"));
    }

    @Test
    void duplicateIngredientsAndBothQuantitiesSurviveRedirect() throws Exception {
        submit(new String[]{"1", "1"}, new String[]{"200", "50"});
        load();
        assertEquals(0, saves);
        assertEquals(2, formRows().size());
        assertEquals("200", formRows().get(0).get("quantity"));
        assertEquals("50", formRows().get(1).get("quantity"));
        assertTrue(attributes.get("recipeError").toString().contains("trùng"));
    }

    @Test
    void invalidQuantityIsRetainedAndNeverSaved() throws Exception {
        for (String quantity : List.of("0", "-1", "1.0001", "")) {
            submit(new String[]{"1"}, new String[]{quantity});
            load();
            assertEquals(quantity, formRows().get(0).get("quantity"));
            assertNotNull(attributes.get("recipeError"));
        }
        assertEquals(0, saves);
    }

    @Test
    void successfulSaveClosesModalAndShowsSuccessOnList() throws Exception {
        submit(new String[]{"1"}, new String[]{"150"});
        assertEquals(1, saves);
        assertEquals("/cafe/admin/recipes", redirect);
        assertFalse(sessionValues.containsKey("recipeDraft"));
        parameters.remove("productId");
        load();
        assertNotNull(attributes.get("recipeMessage"));
        assertNull(attributes.get("selectedProduct"));
        assertNull(attributes.get("recipeError"));
    }

    @Test
    void databaseFailureAlsoKeepsTheDraft() throws Exception {
        failSave = true;
        submit(new String[]{"1"}, new String[]{"225.125"});
        load();
        assertEquals("225.125", formRows().get(0).get("quantity"));
        assertNotNull(attributes.get("recipeError"));
        assertNull(attributes.get("recipeMessage"));
    }

    @Test
    void nextFreshOpenUsesStoredRecipeAfterOneTimeDraftWasConsumed() throws Exception {
        submit(new String[]{"1"}, new String[]{"0"});
        load();
        assertEquals("0", formRows().get(0).get("quantity"));
        load();
        assertEquals("150", formRows().get(0).get("quantity"));
        assertNull(attributes.get("recipeError"));
    }

    @Test
    void draftCannotLeakIntoAnotherProductsForm() throws Exception {
        submit(new String[]{"1"}, new String[]{"0"});
        parameters.put("productId", new String[]{"14"});
        load();
        assertTrue(formRows().isEmpty());
        assertFalse(sessionValues.containsKey("recipeDraft"));
    }

    @Test
    void missingCsrfDoesNotSaveOrCreateDraft() throws Exception {
        parameters.remove("csrfToken");
        submit(new String[]{"1"}, new String[]{"150"});
        assertEquals(403, responseError);
        assertEquals(0, saves);
        assertFalse(sessionValues.containsKey("recipeDraft"));
    }

    @Test
    void jspDisplaysErrorInsideModalAndEscapesRawQuantity() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/admin-recipes.jsp"));
        assertTrue(jsp.indexOf("id=\"recipeFormError\"") > jsp.indexOf("class=\"recipe-modal-body\""));
        assertTrue(jsp.contains("not empty recipeError and empty selectedProduct"));
        assertTrue(jsp.contains("${fn:escapeXml(recipeIngredient.quantity)}"));
        assertTrue(jsp.contains("document.getElementById('recipeFormError')?.focus()"));
    }

    private void submit(String[] ids, String[] quantities) throws Exception {
        attributes.clear();
        parameters.put("ingredientId", ids);
        parameters.put("quantity", quantities);
        controller.doPost(request(), response());
    }

    @Test void recipeRowLimitsAccept99And100ButReject101WithExactMessage() throws Exception {
        for (int count : new int[]{99, 100, 101}) {
            String[] ids = java.util.stream.IntStream.rangeClosed(1, count).mapToObj(Integer::toString).toArray(String[]::new);
            String[] amounts = new String[count]; java.util.Arrays.fill(amounts, "0.001");
            int before = saves; submit(ids, amounts);
            assertEquals(before + (count <= 100 ? 1 : 0), saves);
            if (count > 100) assertEquals("Không được thêm quá 100 dòng nguyên liệu.", sessionValues.get("recipeError"));
        }
    }

    @Test void quantityAtMaximumSavesAndOneStepAboveKeepsTheDraftAndExactError() throws Exception {
        submit(new String[]{"1"}, new String[]{"999999999.999"}); assertEquals(1, saves);
        submit(new String[]{"1"}, new String[]{"1000000000"}); assertEquals(1, saves);
        assertEquals("Định lượng dòng 1 không được vượt quá 999.999.999,999.", sessionValues.get("recipeError"));
        load(); assertEquals("1000000000", formRows().get(0).get("quantity"));
    }

    @Test void blankQuantityAndDuplicateIngredientReportTheActualRow() throws Exception {
        submit(new String[]{"1", "2"}, new String[]{"1", "\u00a0"});
        assertEquals("Dòng 2: vui lòng nhập định lượng.", sessionValues.get("recipeError"));
        submit(new String[]{"1", "1"}, new String[]{"1", "2"});
        assertEquals("Dòng 2: nguyên liệu bị chọn trùng với dòng trước. Mỗi nguyên liệu chỉ chọn một lần.", sessionValues.get("recipeError"));
        assertEquals(0, saves);
    }

    private void load() throws Exception {
        attributes.clear();
        controller.doGet(request(), response());
        if (loadFailure != null) throw new AssertionError("Recipe GET failed", loadFailure);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> formRows() {
        return (List<Map<String, String>>) attributes.get("recipeFormRows");
    }

    private HttpServletRequest request() {
        HttpSession session = proxy(HttpSession.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getAttribute": return sessionValues.get(a[0]);
                case "setAttribute": sessionValues.put((String) a[0], a[1]); return null;
                case "removeAttribute": sessionValues.remove(a[0]); return null;
                default: return null;
            }
        });
        return proxy(HttpServletRequest.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getSession": return session;
                case "getContextPath": return "/cafe";
                case "getParameter":
                    String[] value = parameters.get(a[0]);
                    return value == null || value.length == 0 ? null : value[0];
                case "getParameterValues": return parameters.get(a[0]);
                case "setAttribute": attributes.put((String) a[0], a[1]); return null;
                case "getRequestDispatcher":
                    String path = (String) a[0];
                    return proxy(RequestDispatcher.class, (dispatcher, method, args) -> { forward = path; return null; });
                default: return null;
            }
        });
    }

    private HttpServletResponse response() {
        return proxy(HttpServletResponse.class, (p, m, a) -> {
            if (m.getName().equals("sendRedirect")) redirect = (String) a[0];
            if (m.getName().equals("sendError")) responseError = (int) a[0];
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
