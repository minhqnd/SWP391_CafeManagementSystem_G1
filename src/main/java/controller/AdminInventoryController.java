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

        String action = request.getParameter("action");
        if ("adjust".equalsIgnoreCase(action)) {
            try {
                int ingredientId = Integer.parseInt(request.getParameter("ingredientId"));
                String txType = request.getParameter("transactionType"); // ADJUSTMENT or SPOILAGE
                String direction = request.getParameter("direction");   // INCREASE or DECREASE
                String qtyStr = request.getParameter("quantity");
                String reason = request.getParameter("reason");

                if (qtyStr == null || qtyStr.isBlank()) {
                    session.setAttribute("inventoryError", "Vui lòng nhập số lượng điều chỉnh.");
                } else {
                    BigDecimal quantity = new BigDecimal(qtyStr.trim());
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        session.setAttribute("inventoryError", "Số lượng phải lớn hơn 0.");
                    } else {
                        boolean isIncrease = "INCREASE".equalsIgnoreCase(direction) && !"SPOILAGE".equalsIgnoreCase(txType);
                        if (reason == null || reason.trim().length() < 3 || reason.trim().length() > 500) {
                            throw new IllegalArgumentException("Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.");
                        }
                        inventoryDAO.recordStockAdjustment(ingredientId, txType, quantity, isIncrease,
                                "ADJUSTMENT", null, reason.trim());
                        session.setAttribute("inventoryMessage", "Ghi nhận phiếu điều chỉnh tồn kho thành công!");
                    }
                }
            } catch (SQLException | ClassNotFoundException | IllegalArgumentException e) {
                getServletContext().log("Inventory adjustment error", e);
                session.setAttribute("inventoryError", e.getMessage());
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/inventory");
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
