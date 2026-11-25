package controller;

import dao.IngredientDAO;
import dao.SupplierDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.Supplier;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminIngredientController", urlPatterns = {"/admin/ingredients"})
public class AdminIngredientController extends HttpServlet {

    // Giới hạn dữ liệu khớp với cấu trúc bảng `ingredients` trong MySQL
    static final int MAX_NAME_LENGTH = 150;
    static final int MAX_UNIT_LENGTH = 20;
    static final BigDecimal MAX_STOCK_VALUE = new BigDecimal("999999999.999");
    static final int MAX_STOCK_SCALE = 3;

    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        if (session.getAttribute("ingredientCsrfToken") == null) {
            session.setAttribute("ingredientCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "ingredientMessage");
        moveFlash(session, request, "ingredientError");

        try {
            String keyword = request.getParameter("q");
            String supplierParam = request.getParameter("supplierId");
            Integer supplierId = (supplierParam != null && !supplierParam.isBlank())
                    ? Integer.parseInt(supplierParam) : null;

            List<Ingredient> ingredients = ingredientDAO.getAllIngredientsWithDetails(keyword, supplierId);
            List<Supplier> suppliers = supplierDAO.getActiveSuppliers();
            int[] stats = ingredientDAO.getIngredientStats();

            request.setAttribute("ingredients", ingredients);
            request.setAttribute("suppliers", suppliers);
            request.setAttribute("totalIngredients", stats[0]);
            request.setAttribute("totalSuppliers", stats[1]);
            request.setAttribute("underMinStockCount", stats[2]);
            request.setAttribute("unusedRecipesCount", stats[3]);
            request.setAttribute("searchKeyword", keyword != null ? keyword : "");
            request.setAttribute("selectedSupplierId", supplierId != null ? supplierId : 0);

            String editId = request.getParameter("editId");
            if (editId != null && !editId.isBlank()) {
                try {
                    Ingredient selected = ingredientDAO.getIngredientById(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedIngredient", selected);
                } catch (NumberFormatException ignored) {
                }
            }

            request.getRequestDispatcher("/admin-ingredients.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin ingredients", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh mục nguyên vật liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("ingredientCsrfToken");
        String formToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(formToken)) {
            session.setAttribute("ingredientError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/ingredients");
            return;
        }

        session.removeAttribute("ingredientMessage");
        session.removeAttribute("ingredientError");
        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "create": {
                    String name = request.getParameter("name");
                    String unit = request.getParameter("unit");
                    String supplierParam = request.getParameter("supplierId");
                    String stockParam = request.getParameter("currentStock");
                    String minStockParam = request.getParameter("minStockLevel");

                    // Kiểm tra tên nguyên liệu và đơn vị tính trước khi xử lý số
                    String error = validateText(name, "Tên nguyên liệu", MAX_NAME_LENGTH);
                    if (error == null) {
                        error = validateText(unit, "Đơn vị tính (ĐVT)", MAX_UNIT_LENGTH);
                    }
                    if (error != null) {
                        session.setAttribute("ingredientError", error);
                        break;
                    }

                    // Kiểm tra nhà cung cấp, ngưỡng tối thiểu và tồn kho ban đầu
                    Integer supplierId = parseSupplierId(supplierParam);
                    BigDecimal minStockLevel = parseStockParam(minStockParam, "Ngưỡng tối thiểu");
                    BigDecimal currentStock = parseStockParam(stockParam, "Tồn kho ban đầu");

                    // Lưu nguyên liệu mới khi toàn bộ dữ liệu hợp lệ
                    Ingredient item = new Ingredient(0, supplierId, name.trim(), unit.trim(), currentStock, minStockLevel);
                    ingredientDAO.insertIngredient(item);
                    session.setAttribute("ingredientMessage", "Thêm nguyên vật liệu mới thành công!");
                    break;
                }
                case "update": {
                    int id = parseIngredientId(request.getParameter("id"));
                    String name = request.getParameter("name");
                    String unit = request.getParameter("unit");
                    String supplierParam = request.getParameter("supplierId");
                    String minStockParam = request.getParameter("minStockLevel");

                    // Kiểm tra tên nguyên liệu và đơn vị tính trước khi xử lý số
                    String error = validateText(name, "Tên nguyên liệu", MAX_NAME_LENGTH);
                    if (error == null) {
                        error = validateText(unit, "Đơn vị tính (ĐVT)", MAX_UNIT_LENGTH);
                    }
                    if (error != null) {
                        session.setAttribute("ingredientError", error);
                        break;
                    }

                    // Kiểm tra nhà cung cấp và ngưỡng tối thiểu (không sửa tồn kho trực tiếp ở đây)
                    Integer supplierId = parseSupplierId(supplierParam);
                    BigDecimal minStockLevel = parseStockParam(minStockParam, "Ngưỡng tối thiểu");

                    Ingredient item = new Ingredient(id, supplierId, name.trim(), unit.trim(), BigDecimal.ZERO, minStockLevel);
                    ingredientDAO.updateIngredient(item);
                    session.setAttribute("ingredientMessage", "Cập nhật nguyên vật liệu thành công!");
                    break;
                }
                case "toggle": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String rawActive = request.getParameter("active");
                    if (!"true".equals(rawActive) && !"false".equals(rawActive)) {
                        throw new NumberFormatException("Invalid active state");
                    }
                    boolean active = Boolean.parseBoolean(rawActive);
                    if (ingredientDAO.updateIngredientActive(id, active)) {
                        session.setAttribute("ingredientMessage", active
                                ? "Đã hiện lại nguyên liệu. Có thể chọn cho công thức và phiếu nhập mới."
                                : "Đã ngừng sử dụng nguyên liệu. Tồn kho, công thức và lịch sử cũ được giữ nguyên.");
                    } else {
                        session.setAttribute("ingredientError", "Không tìm thấy nguyên liệu. Hãy tải lại danh sách.");
                    }
                    break;
                }
                default:
                    session.setAttribute("ingredientError", "Thao tác không được hỗ trợ.");
                    break;
            }
        } catch (NumberFormatException e) {
            getServletContext().log("Ingredient action error", e);
            session.setAttribute("ingredientError", ingredientErrorMessage(e));
        } catch (IllegalArgumentException e) {
            // Thông báo lỗi validate cụ thể từng trường cho người dùng
            session.setAttribute("ingredientError", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Ingredient action error", e);
            session.setAttribute("ingredientError", ingredientErrorMessage(e));
        }

        response.sendRedirect(request.getContextPath() + "/admin/ingredients");
    }

    /**
     * Kiểm tra chuỗi bắt buộc và độ dài tối đa sau khi cắt khoảng trắng đầu/cuối.
     * Trả về thông báo lỗi nếu không hợp lệ, hoặc null nếu hợp lệ.
     */
    static String validateText(String rawValue, String fieldLabel, int maxLength) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return fieldLabel + " không được để trống.";
        }
        if (rawValue.trim().length() > maxLength) {
            return fieldLabel + " không được vượt quá " + maxLength + " ký tự.";
        }
        return null;
    }

    /**
     * Kiểm tra số lượng tồn kho / ngưỡng tối thiểu khớp với kiểu DECIMAL(12,3) trong DB:
     * không âm, tối đa 999,999,999.999 và tối đa 3 chữ số thập phân.
     */
    static String validateStockNumber(BigDecimal value, String fieldLabel) {
        if (value == null) {
            return fieldLabel + " không hợp lệ.";
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return fieldLabel + " không được nhỏ hơn 0.";
        }
        if (value.compareTo(MAX_STOCK_VALUE) > 0) {
            return fieldLabel + " quá lớn (tối đa 999,999,999.999).";
        }
        if (value.stripTrailingZeros().scale() > MAX_STOCK_SCALE) {
            return fieldLabel + " chỉ được có tối đa 3 chữ số thập phân.";
        }
        return null;
    }

    // Đọc và kiểm tra mã nguyên liệu khi cập nhật; ném IllegalArgumentException nếu sai định dạng hoặc <= 0.
    private static int parseIngredientId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ.");
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ.");
        }
    }

    // Đọc và kiểm tra nhà cung cấp (tùy chọn); trả null nếu người dùng chọn "0" hoặc bỏ trống.
    private static Integer parseSupplierId(String supplierParam) {
        if (supplierParam == null || supplierParam.isBlank() || "0".equals(supplierParam.trim())) {
            return null;
        }
        try {
            int supplierId = Integer.parseInt(supplierParam.trim());
            if (supplierId <= 0) {
                throw new IllegalArgumentException("Nhà cung cấp được chọn không hợp lệ.");
            }
            return supplierId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nhà cung cấp được chọn không hợp lệ.");
        }
    }

    // Chuyển chuỗi số tồn kho / ngưỡng tối thiểu sang BigDecimal (rỗng = 0) rồi kiểm tra giới hạn.
    private static BigDecimal parseStockParam(String rawParam, String fieldLabel) {
        if (rawParam == null || rawParam.isBlank()) {
            return BigDecimal.ZERO;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(rawParam.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " phải là một số hợp lệ.");
        }
        String error = validateStockNumber(value, fieldLabel);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return value;
    }

    static String ingredientErrorMessage(Exception error) {
        if (error instanceof SQLException sql) {
            String state = sql.getSQLState();
            if ("45000".equals(state)) {
                return sql.getMessage();
            }
            if ("23000".equals(state)) {
                return "Không thể hoàn tất thao tác vì nguyên liệu còn liên kết với dữ liệu khác. Hãy tải lại danh sách và kiểm tra công thức, lịch sử kho hoặc phiếu nhập hàng.";
            }
            if ("22003".equals(state)) {
                return "Giá trị số nhập vào quá lớn so với giới hạn lưu trữ (tối đa 999,999,999.999).";
            }
            if ("22001".equals(state)) {
                return "Dữ liệu văn bản vượt quá độ dài cho phép (tên tối đa 150 ký tự, đơn vị tính tối đa 20 ký tự).";
            }
            if (sql.getErrorCode() == 3819) {
                return "Dữ liệu không thỏa mãn điều kiện hợp lệ (tồn kho và ngưỡng tối thiểu phải lớn hơn hoặc bằng 0).";
            }
            return "Không thể lưu nguyên liệu. Vui lòng thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.";
        }
        if (error instanceof NumberFormatException) return "Mã nguyên liệu, số lượng hoặc ngưỡng tồn không hợp lệ.";
        return "Không thể cập nhật nguyên liệu. Vui lòng liên hệ quản trị viên.";
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        return true;
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object val = session.getAttribute(key);
        if (val != null) {
            request.setAttribute(key, val);
            session.removeAttribute(key);
        }
    }
}
