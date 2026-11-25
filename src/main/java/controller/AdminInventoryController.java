package controller;

import dao.IngredientDAO;
import dao.InventoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.InventoryTransaction;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminInventoryController", urlPatterns = {"/admin/inventory"})
public class AdminInventoryController extends HttpServlet {

    // Giới hạn dữ liệu khớp với cột DECIMAL(12,3) và reason VARCHAR(500) trong DB
    static final BigDecimal MAX_STOCK_VALUE = new BigDecimal("999999999.999");
    static final int MAX_STOCK_SCALE = 3;
    static final int MIN_REASON_LENGTH = 3;
    static final int MAX_REASON_LENGTH = 500;

    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final IngredientDAO ingredientDAO = new IngredientDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        if (session.getAttribute("inventoryCsrfToken") == null) {
            session.setAttribute("inventoryCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "inventoryMessage");
        moveFlash(session, request, "inventoryError");

        try {
            String statusFilter = request.getParameter("status");
            String txTypeFilter = request.getParameter("txType");

            List<Ingredient> stockList = inventoryDAO.getInventoryStockList(statusFilter);
            List<InventoryTransaction> transactions = inventoryDAO.getRecentTransactions(20, txTypeFilter);
            List<Ingredient> allIngredients = ingredientDAO.getAllIngredientsWithDetails(null, null);

            BigDecimal estimatedValue = inventoryDAO.getTotalInventoryEstimatedValue();
            int totalTransactions = inventoryDAO.getTotalTransactionCount();
            int[] ingredientStats = ingredientDAO.getIngredientStats();

            request.setAttribute("stockList", stockList);
            request.setAttribute("transactions", transactions);
            request.setAttribute("allIngredients", allIngredients);
            request.setAttribute("estimatedValue", estimatedValue);
            request.setAttribute("totalIngredients", ingredientStats[0]);
            request.setAttribute("underMinStockCount", ingredientStats[2]);
            request.setAttribute("totalTransactions", totalTransactions);
            request.setAttribute("selectedStatus", statusFilter != null ? statusFilter : "all");
            request.setAttribute("selectedTxType", txTypeFilter != null ? txTypeFilter : "all");

            request.getRequestDispatcher("/admin-inventory.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin inventory", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được thông tin tồn kho và lịch sử xuất nhập.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("inventoryCsrfToken");
        String formToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(formToken)) {
            session.setAttribute("inventoryError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/inventory");
            return;
        }

        session.removeAttribute("inventoryMessage");
        session.removeAttribute("inventoryError");

        String action = request.getParameter("action");
        if ("adjust".equalsIgnoreCase(action)) {
            try {
                // Kiểm tra và chuẩn hóa từng trường nhập từ form
                int ingredientId = parseIngredientId(request.getParameter("ingredientId"));
                String txType = parseTransactionType(request.getParameter("transactionType"));
                boolean isIncrease = parseDirection(txType, request.getParameter("direction"));
                BigDecimal quantity = parseQuantityParam(request.getParameter("quantity"));

                String reason = request.getParameter("reason");
                String reasonError = validateReason(reason);
                if (reasonError != null) {
                    session.setAttribute("inventoryError", reasonError);
                } else {
                    boolean updated = inventoryDAO.recordStockAdjustment(
                            ingredientId, txType, quantity, isIncrease, "ADJUSTMENT", null, reason.trim());
                    if (updated) {
                        session.setAttribute("inventoryMessage", "Ghi nhận phiếu điều chỉnh tồn kho thành công!");
                    } else {
                        session.setAttribute("inventoryError", "Không tìm thấy nguyên liệu cần điều chỉnh. Hãy tải lại trang.");
                    }
                }
            } catch (NumberFormatException e) {
                getServletContext().log("Inventory adjustment error", e);
                session.setAttribute("inventoryError", inventoryErrorMessage(e));
            } catch (IllegalArgumentException e) {
                // Trả thông báo lỗi validate cụ thể từng trường cho người dùng
                session.setAttribute("inventoryError", e.getMessage());
            } catch (SQLException | ClassNotFoundException e) {
                getServletContext().log("Inventory adjustment error", e);
                session.setAttribute("inventoryError", inventoryErrorMessage(e));
            }
        } else {
            session.setAttribute("inventoryError", "Thao tác không được hỗ trợ.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/inventory");
    }

    /**
     * Kiểm tra số lượng điều chỉnh khớp với DECIMAL(12,3):
     * phải lớn hơn 0, tối đa 999,999,999.999 và tối đa 3 chữ số thập phân.
     */
    static String validateQuantity(BigDecimal value) {
        if (value == null) {
            return "Số lượng thay đổi không hợp lệ.";
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return "Số lượng thay đổi phải lớn hơn 0.";
        }
        if (value.compareTo(MAX_STOCK_VALUE) > 0) {
            return "Số lượng thay đổi quá lớn (tối đa 999,999,999.999).";
        }
        if (value.stripTrailingZeros().scale() > MAX_STOCK_SCALE) {
            return "Số lượng thay đổi chỉ được có tối đa 3 chữ số thập phân.";
        }
        return null;
    }

    // Kiểm tra lý do / ghi chú bắt buộc từ 3 đến 500 ký tự.
    static String validateReason(String rawReason) {
        if (rawReason == null) {
            return "Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.";
        }
        int len = rawReason.trim().length();
        if (len < MIN_REASON_LENGTH || len > MAX_REASON_LENGTH) {
            return "Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.";
        }
        return null;
    }

    // Chỉ chấp nhận 2 loại điều chỉnh trên form: ADJUSTMENT hoặc SPOILAGE.
    static String parseTransactionType(String rawType) {
        if (rawType == null) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }
        String type = rawType.trim().toUpperCase();
        if (!"ADJUSTMENT".equals(type) && !"SPOILAGE".equals(type)) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }
        return type;
    }

    // Với hao hụt (SPOILAGE) luôn trừ kho; với cân kho (ADJUSTMENT) chỉ nhận INCREASE hoặc DECREASE.
    static boolean parseDirection(String txType, String rawDirection) {
        if ("SPOILAGE".equals(txType)) {
            return false;
        }
        String dir = rawDirection != null ? rawDirection.trim() : "";
        if ("INCREASE".equalsIgnoreCase(dir)) {
            return true;
        }
        if ("DECREASE".equalsIgnoreCase(dir)) {
            return false;
        }
        throw new IllegalArgumentException("Hướng điều chỉnh không hợp lệ.");
    }

    // Đọc mã nguyên liệu được chọn; bắt buộc là số nguyên > 0.
    private static int parseIngredientId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn nguyên liệu cần điều chỉnh hợp lệ.");
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Vui lòng chọn nguyên liệu cần điều chỉnh hợp lệ.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Vui lòng chọn nguyên liệu cần điều chỉnh hợp lệ.");
        }
    }

    // Đọc chuỗi số lượng điều chỉnh và kiểm tra điều kiện hợp lệ.
    private static BigDecimal parseQuantityParam(String rawQty) {
        if (rawQty == null || rawQty.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số lượng điều chỉnh.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(rawQty.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số lượng thay đổi phải là một số hợp lệ.");
        }
        String error = validateQuantity(value);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return value;
    }

    // Chuyển lỗi DB / định dạng sang thông báo tiếng Việt an toàn, không lộ chi tiết kỹ thuật.
    static String inventoryErrorMessage(Exception error) {
        if (error instanceof SQLException sql) {
            String state = sql.getSQLState();
            if ("45000".equals(state)) {
                return sql.getMessage();
            }
            if ("22003".equals(state)) {
                return "Giá trị số lượng hoặc tồn kho sau điều chỉnh vượt quá giới hạn lưu trữ (tối đa 999,999,999.999).";
            }
            if ("22001".equals(state)) {
                return "Lý do / ghi chú vượt quá độ dài tối đa 500 ký tự.";
            }
            if (sql.getErrorCode() == 3819) {
                return "Dữ liệu điều chỉnh không thỏa mãn điều kiện hợp lệ (số lượng phải lớn hơn 0 và tồn kho không được âm).";
            }
            return "Không thể ghi nhận phiếu điều chỉnh tồn kho. Vui lòng thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.";
        }
        if (error instanceof NumberFormatException) {
            return "Mã nguyên liệu hoặc số lượng điều chỉnh không hợp lệ.";
        }
        return "Không thể ghi nhận phiếu điều chỉnh tồn kho. Vui lòng liên hệ quản trị viên.";
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
