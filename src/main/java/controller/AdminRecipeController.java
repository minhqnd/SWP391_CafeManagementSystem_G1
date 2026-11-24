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

    // [SUA11-07] Luyện sửa: Một công thức chỉ được tối đa 5 dòng nguyên liệu thay vì 100.
    // . Đổi MAX_RECIPE_LINES và kiểm tra rows.length trong admin-form-validation.js; lời báo server dùng hằng
    //   số này.
    // . Phần giữ bản nhập dở dùng MAX_RECIPE_LINES+1 để còn hiện dòng vượt giới hạn cho người dùng sửa; kiểm
    //   tra lại sau khi đổi.
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
    // . Khi mở Quản lý công thức, doGet trong AdminRecipeController.java đọc sản phẩm qua ProductDAO, nguyên
    //   liệu và giá nhập qua IngredientDAO, công thức qua ProductIngredientDAO. Em đưa các dữ liệu này về
    //   admin-recipes.jsp để hiện danh sách món, công thức và giá vốn.
    // ... Trong AdminRecipeController.java, đọc tiếp thân doGet ngay bên dưới: đọc các danh sách, tính chi phí rồi
    //     đặt dữ liệu trước khi forward tới admin-recipes.jsp.
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
            // . Ở doGet trong AdminRecipeController.java, em nhận productId của món vừa bấm. Em gọi findProduct (tìm
            //   đúng món trong danh sách đã đọc) ngay trong controller, truyền danh sách sản phẩm và mã này để chọn
            //   đúng món rồi điền công thức vào hộp chỉnh sửa.
            // ... Khi findProduct đã chọn được món, doGet đưa selectedProduct sang admin-recipes.jsp; đọc tiếp tại
            //     admin-recipes.jsp, tìm [CTCH04] để thấy món của hộp chỉnh sửa.
            int selectedProductId = parseOptionalProductId(request.getParameter("productId"));
            Product selectedProduct = findProduct(products, selectedProductId);
            if (selectedProduct == null) {
                if (request.getParameter("productId") != null)
                    request.setAttribute("recipeError", selectedProductId > 0
                            ? "Không tìm thấy sản phẩm cần thiết lập công thức. Sản phẩm có thể đã bị xóa."
                            : "Mã sản phẩm không hợp lệ. Vui lòng mở lại công thức từ danh sách sản phẩm.");
                selectedProductId = 0;
            }

            // . Để mở đúng các dòng công thức, ở doGet trong AdminRecipeController.java em gọi
            //   getAllProductIngredientsMap (đọc công thức và nhóm theo mã sản phẩm) trong
            //   ProductIngredientDAO.java. Em dùng dữ liệu theo mã món để dựng form và đếm sản phẩm có định mức.
            // ... Đọc tiếp tại ProductIngredientDAO.java, tìm [CTM03].
            Map<Integer, List<ProductIngredient>> allRecipeIngredients = productIngredientDAO.getAllProductIngredientsMap();
            List<ProductIngredient> recipeIngredients = selectedProduct == null
                    ? List.of()
                    : allRecipeIngredients.getOrDefault(selectedProductId, List.of());

            // [CTTONG04]
            // . Để tính giá vốn, ở doGet trong AdminRecipeController.java em gọi getLatestPurchasePrices (đọc giá
            //   nhập đã nhận gần nhất) trong IngredientDAO.java. Hàm trả về đơn giá nhập gần nhất của từng nguyên
            //   liệu để em nhân với lượng dùng trong công thức.
            // ... Đọc tiếp tại IngredientDAO.java, tìm [CTTONG05].
            Map<Integer, BigDecimal> ingredientUnitCosts = ingredientDAO.getLatestPurchasePrices();

            // [CTTONG03]
            // . Trong doGet của AdminRecipeController.java, em lấy từng lượng nguyên liệu nhân với giá nhập rồi cộng
            //   lại thành recipeCost. Em đưa tổng chi phí và danh sách nguyên liệu chưa có giá về admin-recipes.jsp
            //   để người dùng biết giá vốn đang được tính từ đâu.
            // ... Trong AdminRecipeController.java, tiếp tục doGet tới chỗ đặt recipeCost, recipeCostComplete và
            //     missingCostIngredients; admin-recipes.jsp dùng các giá trị này hiện chi phí hoặc phần còn thiếu.

            // [SQL11-44] Giá vốn công thức đang mở: Java cộng giá nhập mới nhất nhân định lượng từng dòng.
            // . Nguồn là getAllProductIngredientsMap và getLatestPurchasePrices; thiếu giá nhập thì đánh dấu
            //   chưa đủ dữ liệu.

            // [SUA11-08] Luyện sửa: Giá vốn công thức đang mở cộng thêm 2000đ bao bì.
            // . Cộng một lần sau vòng lặp tính nguyên liệu, không cộng 2000 vào từng dòng.
            // . Cần thống nhất với productCost của từng món bên dưới và recalculateCost trong
            //   admin-recipes.jsp; đây là chi phí, không phải giá bán.
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
            // [CTTS01]
            // . Trong doGet của AdminRecipeController.java, em đối chiếu danh sách sản phẩm với công thức từ
            //   ProductIngredientDAO.getAllProductIngredientsMap (đọc công thức và nhóm theo mã sản phẩm). Em đếm
            //   món có công thức, chưa có công thức và tổng số dòng rồi đưa các số về admin-recipes.jsp để hiện
            //   thống kê.
            // ... Đọc tiếp tại admin-recipes.jsp, tìm [CTM04].

            // [SQL11-45] Thẻ Có/Chưa có định mức: Java đếm món có danh sách nguyên liệu rồi lấy tổng món trừ số
            //   đã có.
            // . Không có câu COUNT riêng trong SQL cho hai thẻ này; nguồn là danh sách sản phẩm và bản nhóm
            //   công thức.
            int productsWithRecipe = 0;
            int recipeLineCount = 0;
            for (Product product : products) {
                List<ProductIngredient> productIngredients = allRecipeIngredients.getOrDefault(product.getId(), List.of());
                if (!productIngredients.isEmpty()) productsWithRecipe++;
                recipeLineCount += productIngredients.size();

                // [CTGT02]
                // . Với từng món trong doGet, em đọc công thức và lấy đơn giá trong ingredientUnitCosts, nhân định lượng
                //   rồi cộng thành productCost. Em tạo IngredientLine cho từng nguyên liệu và RecipeProductSummary cho
                //   cả món, đưa productSummaries về admin-recipes.jsp để hiện giá vốn, chi phí từng dòng và tình trạng
                //   đã có hoặc chưa có công thức.
                // ... Đọc tiếp tại RecipeProductSummary.java, tìm [CTGT03].
                List<RecipeProductSummary.IngredientLine> costLines = new ArrayList<>();
                // [SQL11-46] Cột Giá vốn trên bảng món: Java tính riêng tổng giá nhập nhân định lượng của từng
                //   sản phẩm.
                // . Không lấy giá bán làm giá vốn; món thiếu giá nhập được đánh dấu thay vì kết luận chi phí
                //   bằng 0.

                // [SUA11-09] Luyện sửa: Giá vốn trên từng thẻ món cộng thêm một khoản cố định.
                // . Nếu yêu cầu thêm phí cho mỗi phần thì cộng một lần sau khi tính xong nguyên liệu của món
                //   đó.
                // . Giữ giống recipeCost của công thức đang mở và phần xem trước; vẫn phân biệt món chưa có
                //   công thức hoặc thiếu giá nhập.
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
    // [CTL02]
    // . Khi nhận form tại doPost trong AdminRecipeController.java, em lấy mã sản phẩm rồi gọi
    //   ProductDAO.getProductForAdmin để kiểm tra món còn tồn tại. Nếu có món, em tiếp tục kiểm tra từng
    //   dòng công thức; không tìm thấy thì báo lỗi và dừng lưu.
    // ... Đọc tiếp tại AdminRecipeController.java, tìm [CTL03].
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

            // . Trước khi lưu công thức, em gọi getProductForAdmin (đọc một sản phẩm để quản lý) trong
            //   ProductDAO.java, truyền mã sản phẩm vừa nhận. Nếu không có món thì báo lỗi và dừng; có món mới tiếp
            //   tục kiểm tra các dòng nguyên liệu.
            // ... Đọc tiếp tại ProductDAO.java, tìm [CTCH03].
            Product product = productDAO.getProductForAdmin(productId);
            if (product == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật công thức.");

            // [CTL03]
            // . Sau khi xác nhận được sản phẩm, em gọi parseIngredients ngay trong AdminRecipeController.java để
            //   kiểm tra từng dòng công thức. Em truyền mã món, danh sách mã nguyên liệu và danh sách định lượng,
            //   rồi nhận lại các dòng hợp lệ để chuyển sang bước lưu.
            // ... Đọc tiếp tại AdminRecipeController.java, tìm [CTL04].
            List<ProductIngredient> recipeIngredients = parseIngredients(
                    productId,
                    request.getParameterValues("ingredientId"),
                    request.getParameterValues("quantity"));
            // [CTL05]
            // . Khi nhận được danh sách hợp lệ, em gọi saveRecipe trong ProductIngredientDAO.java để lưu công thức.
            //   Em truyền mã sản phẩm cùng danh sách nguyên liệu và định lượng, giúp hàm thay công thức cho đúng món
            //   đang sửa.
            // ... Đọc tiếp tại ProductIngredientDAO.java, tìm [CTL06].
            productIngredientDAO.saveRecipe(productId, recipeIngredients);
            saved = true;
            // [CTL07]
            // . saveRecipe ghi xong và trả về AdminRecipeController.doPost. Em đặt thông báo thành công rồi mở
            //   lại trang; doGet đọc công thức vừa lưu, tính lại giá vốn và chuyển dữ liệu sang
            //   admin-recipes.jsp để hiển thị.
            // ... Đọc tiếp tại admin-recipes.jsp, tìm [CTL08].

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
            // . Nếu lưu công thức bị lỗi, em gọi submittedRows (giữ các dòng công thức vừa nhập) trong
            //   AdminRecipeController.java, truyền hai danh sách nguyên liệu và định lượng để giữ các dòng vừa nhập
            //   vào recipeDraft. doGet mở lại đúng món với dữ liệu đó cho người dùng sửa tiếp.
            session.setAttribute("recipeDraft", new RecipeDraft(productId, submittedRows(
                    request.getParameterValues("ingredientId"), request.getParameterValues("quantity"))));
            redirect += "?productId=" + productId;
        }

        // . Cuối doPost trong AdminRecipeController.java, em gọi sendRedirect (chuyển sang đường dẫn khác) để mở
        //   lại /admin/recipes. Khi cần giữ hộp sửa sau lỗi, em kèm productId; doGet sẽ đọc dữ liệu và đưa về
        //   admin-recipes.jsp để hiển thị kết quả.
        response.sendRedirect(redirect);
    }

    // [CTL09]
    // . Khi lưu lỗi và đã có mã món, doPost gọi submittedRows để giữ mã nguyên liệu và định lượng theo đúng
    //   từng dòng, kể cả dòng trống hoặc bị trùng. Em giữ tối đa 101 dòng trong recipeDraft để người dùng sửa
    //   tiếp.
    // . doGet dùng bản này dựng lại các ô trong hộp sửa công thức. Các dòng nhập dở chưa được ghi vào
    //   product_ingredients, nên công thức đang lưu vẫn được giữ.
    // ... Quay lại doPost trong AdminRecipeController.java sau submittedRows để giữ recipeDraft và thêm
    //   productId vào đường dẫn; doGet mở lại hộp sửa để kết thúc lần lưu lỗi.
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
    // . Hàm parseIngredients trong AdminRecipeController.java nhận mã món, các mã nguyên liệu và định lượng. Em
    //   kiểm tra công thức có nguyên liệu, không quá 100 dòng, các dòng đã nhập phải đủ thông tin và không
    //   trùng nguyên liệu.
    // . Với từng định lượng, em gọi MenuValidation.quantity để kiểm tra số dương, tối đa 999.999.999,999 và
    //   không quá ba chữ số thập phân. Đúng hết thì trả danh sách về doPost để lưu; sai thì báo lỗi để doPost
    //   giữ form.
    // ... Đọc tiếp tại AdminRecipeController.java, tìm [CTL05].
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

    // [CTQ01]
    // . Trước khi mở hoặc lưu công thức, doGet và doPost gọi isAdmin (kiểm tra tài khoản Admin còn hoạt
    //   động) trong AdminRecipeController.java. Hàm kiểm tra tài khoản là Admin còn hoạt động; đúng thì tiếp
    //   tục, sai thì trả lỗi và dừng yêu cầu.
    // ... Quay lại đầu doGet hoặc doPost trong AdminRecipeController.java sau isAdmin; đúng thì tiếp tục đọc hoặc
    //     lưu công thức, sai thì đã trả lỗi và kết thúc yêu cầu.
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
