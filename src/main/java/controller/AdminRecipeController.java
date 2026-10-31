package controller;

import dao.IngredientDAO;
import dao.ProductDAO;
import dao.ProductIngredientDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.Product;
import model.ProductIngredient;
import model.RecipeProductSummary;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AdminRecipeController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final ProductIngredientDAO productIngredientDAO = new ProductIngredientDAO();

    @Override
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
            int selectedProductId = parseOptionalProductId(request.getParameter("productId"));
            Product selectedProduct = findProduct(products, selectedProductId);
            if (selectedProduct == null) {
                selectedProductId = 0;
            }

            Map<Integer, List<ProductIngredient>> allRecipeIngredients = productIngredientDAO.getAllProductIngredientsMap();
            List<ProductIngredient> recipeIngredients = selectedProduct == null
                    ? List.of()
                    : allRecipeIngredients.getOrDefault(selectedProductId, List.of());
            Map<Integer, BigDecimal> ingredientUnitCosts = ingredientDAO.getLatestPurchasePrices();
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
            int productsWithRecipe = 0;
            int recipeLineCount = 0;
            for (Product product : products) {
                List<ProductIngredient> productIngredients = allRecipeIngredients.getOrDefault(product.getId(), List.of());
                if (!productIngredients.isEmpty()) productsWithRecipe++;
                recipeLineCount += productIngredients.size();

                List<RecipeProductSummary.IngredientLine> costLines = new ArrayList<>();
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
            moveFlash(request, "recipeMessage");
            moveFlash(request, "recipeError");
            request.getRequestDispatcher("/admin-recipes.jsp").forward(request, response);
        } catch (SQLException | ClassNotFoundException | RuntimeException e) {
            getServletContext().log("Failed to load admin recipes", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được công thức. Hãy kiểm tra kết nối database và dữ liệu sản phẩm/nguyên liệu.");
        }
    }

    @Override
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
            session.setAttribute("recipeMessage", "Đã lưu công thức cho món “" + product.getName() + "”.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("recipeError", e.getMessage());
        } catch (Exception e) {
            getServletContext().log("Failed to save admin recipe", e);
            session.setAttribute("recipeError", "Không lưu được công thức. Hãy kiểm tra sản phẩm, nguyên liệu và kết nối database.");
        }

        String redirect = request.getContextPath() + "/admin/recipes";
        if (productId > 0) redirect += "?productId=" + productId;
        response.sendRedirect(redirect);
    }

    private List<ProductIngredient> parseIngredients(int productId, String[] ingredientIds, String[] quantities) {
        List<ProductIngredient> result = new ArrayList<>();
        if (ingredientIds == null && quantities == null) return result;
        if (ingredientIds == null || quantities == null || ingredientIds.length != quantities.length) {
            throw new IllegalArgumentException("Kiểm tra lại danh sách nguyên liệu và định lượng.");
        }

        Set<Integer> seenIngredientIds = new HashSet<>();
        for (int i = 0; i < ingredientIds.length; i++) {
            String rawId = ingredientIds[i] == null ? "" : ingredientIds[i].trim();
            String rawQuantity = quantities[i] == null ? "" : quantities[i].trim();
            if (rawId.isEmpty() && rawQuantity.isEmpty()) continue;
            if (rawId.isEmpty() || rawQuantity.isEmpty()) {
                throw new IllegalArgumentException("Mỗi dòng nguyên liệu cần có cả nguyên liệu và định lượng.");
            }

            int ingredientId;
            BigDecimal quantity;
            try {
                ingredientId = Integer.parseInt(rawId);
                quantity = new BigDecimal(rawQuantity);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nguyên liệu hoặc định lượng không đúng định dạng.");
            }
            if (ingredientId <= 0) throw new IllegalArgumentException("Nguyên liệu không hợp lệ.");
            if (quantity.signum() <= 0
                    || quantity.stripTrailingZeros().scale() > 3
                    || quantity.compareTo(new BigDecimal("999999999.999")) > 0) {
                throw new IllegalArgumentException("Định lượng phải lớn hơn 0 và có tối đa 3 chữ số thập phân.");
            }
            if (!seenIngredientIds.add(ingredientId)) {
                throw new IllegalArgumentException("Không chọn trùng một nguyên liệu trong cùng công thức.");
            }
            result.add(new ProductIngredient(productId, ingredientId, quantity.doubleValue(), null, null));
        }
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
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý công thức sản phẩm.");
            return false;
        }
        return true;
    }
}
