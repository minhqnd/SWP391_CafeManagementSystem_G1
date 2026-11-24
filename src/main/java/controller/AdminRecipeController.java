package controller;

import dao.IngredientDAO;
import dao.ProductDAO;
import dao.ProductIngredientDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.Product;
import model.ProductIngredient;
import model.RecipeProductSummary;
import model.User;
import utils.MenuValidation;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "AdminRecipeController", urlPatterns = {"/admin/recipes", "/admin/recipe/update"})
public class AdminRecipeController extends HttpServlet {

    private static final int MAX_RECIPE_LINES = 100;

    private final ProductDAO productDAO;
    private final IngredientDAO ingredientDAO;
    private final ProductIngredientDAO productIngredientDAO;

    public AdminRecipeController() {
        this(new ProductDAO(), new IngredientDAO(), new ProductIngredientDAO());
    }

    AdminRecipeController(ProductDAO productDAO, IngredientDAO ingredientDAO,
                          ProductIngredientDAO productIngredientDAO) {
        this.productDAO = productDAO;
        this.ingredientDAO = ingredientDAO;
        this.productIngredientDAO = productIngredientDAO;
    }

    private record RecipeDraft(int productId, List<Map<String, String>> rows) implements Serializable {}

    @Override
    // [CTM02]
    // . Hàm doGet trong AdminRecipeController chuẩn bị trang quản lý công thức. Em gọi ProductDAO lấy món,
    //   IngredientDAO lấy nguyên liệu và giá nhập, ProductIngredientDAO lấy các công thức. Sau đó em tính chi
    //   phí, gom số tổng và đưa dữ liệu về trang để xem hoặc sửa.
    //
    // Tra tiếp: ProductIngredientDAO.java / CTM03.
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        try {
            HttpSession session = request.getSession();
            if (session.getAttribute("recipeCsrfToken") == null) {
                session.setAttribute("recipeCsrfToken", UUID.randomUUID().toString());
            }
            List<Product> products = productDAO.getProductsForAdmin(null, null);

            List<Ingredient> ingredients = ingredientDAO.getAllIngredients();
            // [CTCH02]
            // . Trong doGet của AdminRecipeController, em tìm món được chọn trong danh sách đã đọc rồi lấy
            //   các dòng công thức của món đó. Có đúng món em mới đưa thông tin vào hộp sửa; không tìm thấy
            //   thì báo lỗi.
            //
            // Kết thúc: mở sửa khi hộp công thức của đúng món được hiển thị.
            int selectedProductId = parseOptionalProductId(request.getParameter("productId"));
            Product selectedProduct = findProduct(products, selectedProductId);
            if (selectedProduct == null) {
                if (request.getParameter("productId") != null)
                    request.setAttribute("recipeError", selectedProductId > 0
                            ? "Không tìm thấy sản phẩm cần thiết lập công thức. Sản phẩm có thể đã bị xóa."
                            : "Mã sản phẩm không hợp lệ. Vui lòng mở lại công thức từ danh sách sản phẩm.");
                selectedProductId = 0;
            }

            Map<Integer, List<ProductIngredient>> allRecipeIngredients = productIngredientDAO.getAllProductIngredientsMap();
            List<ProductIngredient> recipeIngredients = selectedProduct == null
                    ? List.of()
                    : allRecipeIngredients.getOrDefault(selectedProductId, List.of());

            Map<Integer, BigDecimal> ingredientUnitCosts = ingredientDAO.getLatestPurchasePrices();

            // [CTG01]
            // . Em lấy lượng nguyên liệu nhân đơn giá rồi cộng thành giá vốn của món. Em giữ riêng phần thiếu
            //   giá và gom kết quả theo từng sản phẩm để trang biết tổng nào chưa tính đủ.
            //
            // Tra cứu: Kết quả được đưa về admin-recipes.jsp / CTM04 và form admin-recipes.jsp / CTL01 qua
            //          cuối hàm mở trang tại CTM02. Cách tính từng dòng của phần tóm tắt nằm ở
            //          RecipeProductSummary.java / CTG02.

            // [SQL11-44] Giá vốn công thức đang mở: Java cộng giá nhập mới nhất nhân định lượng từng dòng.

            BigDecimal recipeCost = BigDecimal.ZERO;
            List<String> missingCostIngredients = new ArrayList<>();
            for (ProductIngredient recipeIngredient : recipeIngredients) {
                BigDecimal unitCost = ingredientUnitCosts.get(recipeIngredient.getIngredientId());
                if (unitCost == null) {
                    missingCostIngredients.add(recipeIngredient.getIngredientName());
                } else {

                    recipeCost = recipeCost.add(unitCost.multiply(BigDecimal.valueOf(recipeIngredient.getQuantityRequired())));
                }
            }
            boolean recipeCostComplete = !recipeIngredients.isEmpty() && missingCostIngredients.isEmpty();
            List<RecipeProductSummary> productSummaries = new ArrayList<>();

            // [SQL11-45] Thẻ Có/Chưa có định mức: Java đếm món có danh sách nguyên liệu rồi lấy tổng món trừ số
            //   đã có.

            int productsWithRecipe = 0;
            int recipeLineCount = 0;
            for (Product product : products) {
                List<ProductIngredient> productIngredients = allRecipeIngredients.getOrDefault(product.getId(), List.of());
                if (!productIngredients.isEmpty()) productsWithRecipe++;
                recipeLineCount += productIngredients.size();

                List<RecipeProductSummary.IngredientLine> costLines = new ArrayList<>();
                // [SQL11-46] Cột Giá vốn trên bảng món: Java tính riêng tổng giá nhập nhân định lượng của từng
                //   sản phẩm.

                BigDecimal productCost = BigDecimal.ZERO;
                boolean productCostComplete = !productIngredients.isEmpty();
                for (ProductIngredient ingredient : productIngredients) {
                    BigDecimal unitCost = ingredientUnitCosts.get(ingredient.getIngredientId());
                    costLines.add(new RecipeProductSummary.IngredientLine(ingredient, unitCost));
                    if (unitCost == null) {
                        productCostComplete = false;
                    } else {
                        productCost = productCost.add(unitCost.multiply(BigDecimal.valueOf(ingredient.getQuantityRequired())));
                    }
                }

                productSummaries.add(new RecipeProductSummary(
                        product, costLines, productCost, productCostComplete));
            }

            request.setAttribute("products", products);
            request.setAttribute("productSummaries", productSummaries);
            request.setAttribute("productsWithRecipe", productsWithRecipe);
            request.setAttribute("productsWithoutRecipe", products.size() - productsWithRecipe);
            request.setAttribute("recipeLineCount", recipeLineCount);
            request.setAttribute("ingredients", ingredients);
            request.setAttribute("ingredientUnitCosts", ingredientUnitCosts);
            request.setAttribute("selectedProduct", selectedProduct);
            request.setAttribute("recipeIngredients", recipeIngredients);
            request.setAttribute("selectedProductId", selectedProductId);
            request.setAttribute("recipeCost", recipeCost);
            request.setAttribute("recipeCostComplete", recipeCostComplete);
            request.setAttribute("recipeHasIngredients", !recipeIngredients.isEmpty());
            request.setAttribute("missingCostIngredients", missingCostIngredients);
            List<Map<String, String>> formRows = new ArrayList<>();
            for (ProductIngredient line : recipeIngredients) {
                formRows.add(Map.of("ingredientId", Integer.toString(line.getIngredientId()),
                        "quantity", BigDecimal.valueOf(line.getQuantityRequired()).stripTrailingZeros().toPlainString()));
            }

            Object draft = session.getAttribute("recipeDraft");
            session.removeAttribute("recipeDraft");
            if (draft instanceof RecipeDraft submitted && submitted.productId() == selectedProductId) {
                formRows = submitted.rows();
            }
            request.setAttribute("recipeFormRows", formRows);
            moveFlash(request, "recipeMessage");
            moveFlash(request, "recipeError");
            request.getRequestDispatcher("/admin-recipes.jsp").forward(request, response);
        } catch (SQLException | ClassNotFoundException | RuntimeException e) {
            getServletContext().log("Failed to load admin recipes", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được công thức lúc này. Vui lòng thử lại sau.");
        }
    }

    @Override
    // [CTL03]
    // . Khi nhận form công thức, doPost trong AdminRecipeController lấy mã món và các dòng vừa gửi rồi
    //   kiểm tra món còn tồn tại. Em gọi parseIngredients ngay trong class này để ghép nguyên liệu với
    //   định lượng, kiểm tra từng dòng và tạo danh sách dùng cho bước lưu.
    //
    // Tra tiếp: CTL04.
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String expectedToken = (String) session.getAttribute("recipeCsrfToken");
        if (expectedToken == null || !expectedToken.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Phiên làm việc đã hết hạn. Tải lại trang rồi thử lại.");
            return;
        }
        int productId = 0;
        boolean saved = false;
        session.removeAttribute("recipeError");
        session.removeAttribute("recipeMessage");
        session.removeAttribute("recipeDraft");
        try {
            if (!"save".equals(request.getParameter("action"))) {
                throw new IllegalArgumentException("Thao tác công thức không hợp lệ.");
            }

            productId = parseRequiredProductId(request.getParameter("productId"));

            Product product = productDAO.getProductForAdmin(productId);
            if (product == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật công thức.");

            List<ProductIngredient> recipeIngredients = parseIngredients(
                    productId,
                    request.getParameterValues("ingredientId"),
                    request.getParameterValues("quantity"));

            productIngredientDAO.saveRecipe(productId, recipeIngredients);
            saved = true;
            // [CTL06]
            // . Sau khi saveRecipe hoàn tất, em đặt thông báo rồi mở lại danh sách để doGet đọc công thức mới
            //   và tính lại giá vốn. Nếu lưu lỗi, em giữ các dòng đã nhập và mở lại hộp của đúng món. Kết quả
            //   được đưa về admin-recipes.jsp để người dùng xem hoặc sửa tiếp.
            //
            // Tra tiếp: admin-recipes.jsp / CTL07.

            session.setAttribute("recipeMessage", "Đã lưu công thức cho món “" + product.getName() + "”.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("recipeError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Failed to save admin recipe", e);
            session.setAttribute("recipeError", "45000".equals(e.getSQLState())
                    ? "Có nguyên liệu không còn tồn tại hoặc đã ngừng sử dụng. Vui lòng chọn lại nguyên liệu đang hoạt động."
                    : "Không lưu được công thức lúc này. Vui lòng thử lại sau.");
        } catch (Exception e) {
            getServletContext().log("Failed to save admin recipe", e);
            session.setAttribute("recipeError", "Không lưu được công thức lúc này. Vui lòng thử lại sau.");
        }

        String redirect = request.getContextPath() + "/admin/recipes";

        if (!saved && productId > 0) {

            session.setAttribute("recipeDraft", new RecipeDraft(productId, submittedRows(
                    request.getParameterValues("ingredientId"), request.getParameterValues("quantity"))));
            redirect += "?productId=" + productId;
        }

        response.sendRedirect(redirect);
    }

    // Giữ các dòng công thức đang nhập để mở lại đúng món sau lỗi lưu.
    private List<Map<String, String>> submittedRows(String[] ingredientIds, String[] quantities) {
        List<Map<String, String>> rows = new ArrayList<>();
        int length = Math.max(ingredientIds == null ? 0 : ingredientIds.length,
                quantities == null ? 0 : quantities.length);
        for (int i = 0; i < Math.min(length, MAX_RECIPE_LINES + 1); i++) {
            String id = ingredientIds != null && i < ingredientIds.length ? ingredientIds[i] : null;
            String quantity = quantities != null && i < quantities.length ? quantities[i] : null;
            rows.add(Map.of("ingredientId", id == null ? "" : id,
                    "quantity", quantity == null ? "" : quantity));
        }
        return rows;
    }

    // [CTL04]
    // . Trong parseIngredients, em ghép từng nguyên liệu với định lượng, kiểm tra đủ thông tin, không trùng
    //   và lượng dùng hợp lệ. Danh sách đúng được trả về doPost; từ đây em gọi saveRecipe trong
    //   ProductIngredientDAO để lưu công thức cho món.
    //
    // Tra tiếp: ProductIngredientDAO.java / CTL05.
    private List<ProductIngredient> parseIngredients(int productId, String[] ingredientIds, String[] quantities) {

        List<ProductIngredient> result = new ArrayList<>();
        if (ingredientIds == null && quantities == null) {
            throw new IllegalArgumentException("Công thức phải có ít nhất một nguyên liệu.");
        }
        if (ingredientIds == null || quantities == null || ingredientIds.length != quantities.length) {
            throw new IllegalArgumentException("Kiểm tra lại danh sách nguyên liệu và định lượng.");
        }
        if (ingredientIds.length > MAX_RECIPE_LINES) {
            throw new IllegalArgumentException("Không được thêm quá " + MAX_RECIPE_LINES + " dòng nguyên liệu.");
        }

        Set<Integer> seenIngredientIds = new HashSet<>();
        for (int i = 0; i < ingredientIds.length; i++) {
            String rawId = utils.FormInput.trim(ingredientIds[i]);
            String rawQuantity = utils.FormInput.trim(quantities[i]);
            if (rawId.isEmpty() && rawQuantity.isEmpty()) continue;
            String rowLabel = "Dòng " + (i + 1) + ": ";
            if (rawId.isEmpty()) throw new IllegalArgumentException(rowLabel + "vui lòng chọn nguyên liệu.");
            if (rawQuantity.isEmpty()) throw new IllegalArgumentException(rowLabel + "vui lòng nhập định lượng.");

            int ingredientId;
            BigDecimal quantity;
            try {
                if (!rawId.matches("[0-9]{1,10}")) throw new NumberFormatException();
                ingredientId = Integer.parseInt(rawId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(rowLabel + "nguyên liệu không hợp lệ. Vui lòng chọn lại.");
            }
            if (ingredientId <= 0) throw new IllegalArgumentException(rowLabel + "nguyên liệu không hợp lệ. Vui lòng chọn lại.");
            quantity = MenuValidation.quantity(rawQuantity, "Định lượng dòng " + (i + 1));
            if (!seenIngredientIds.add(ingredientId)) {
                throw new IllegalArgumentException(rowLabel + "nguyên liệu bị chọn trùng với dòng trước. Mỗi nguyên liệu chỉ chọn một lần.");
            }
            result.add(new ProductIngredient(productId, ingredientId, quantity.doubleValue(), null, null));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Công thức phải có ít nhất một nguyên liệu.");
        return result;
    }

    private Product findProduct(List<Product> products, int id) {
        if (id <= 0) return null;
        return products.stream().filter(product -> product.getId() == id).findFirst().orElse(null);
    }

    private int parseOptionalProductId(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            int id = Integer.parseInt(value.trim());
            return id > 0 ? id : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int parseRequiredProductId(String value) {
        int id = parseOptionalProductId(value);
        if (id <= 0) throw new IllegalArgumentException("Hãy chọn sản phẩm cần thiết lập công thức.");
        return id;
    }

    private void moveFlash(HttpServletRequest request, String name) {
        HttpSession session = request.getSession();
        Object value = session.getAttribute(name);
        if (value != null) {
            request.setAttribute(name, value);
            session.removeAttribute(name);
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý công thức sản phẩm.");
            return false;
        }
        return true;
    }
}
