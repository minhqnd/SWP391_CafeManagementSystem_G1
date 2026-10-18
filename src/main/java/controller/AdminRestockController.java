package controller;

import dao.IngredientDAO;
import dao.RestockRequestDAO;
import dao.SupplierDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Ingredient;
import model.RestockRequest;
import model.RestockRequest.RestockItem;
import model.Supplier;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet(name = "AdminRestockController", urlPatterns = {"/admin/restock-requests"})
public class AdminRestockController extends HttpServlet {

    private final RestockRequestDAO restockRequestDAO = new RestockRequestDAO();
    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        if (session.getAttribute("restockCsrfToken") == null) {
            session.setAttribute("restockCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "restockMessage");
        moveFlash(session, request, "restockError");

        try {
            List<Ingredient> lowStockList = restockRequestDAO.getLowStockIngredients();
            List<RestockRequest> draftRequests = restockRequestDAO.getRestockRequests("DRAFT");
            List<Supplier> activeSuppliers = supplierDAO.getActiveSuppliers();
            List<Ingredient> allIngredients = ingredientDAO.getAllIngredientsWithDetails(null, null);
            Map<String, Object> stats = restockRequestDAO.getRestockStats();

            request.setAttribute("lowStockList", lowStockList);
            request.setAttribute("draftRequests", draftRequests);
            request.setAttribute("activeSuppliers", activeSuppliers);
            request.setAttribute("allIngredients", allIngredients);
            request.setAttribute("stats", stats);

            request.getRequestDispatcher("/admin-restock.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin restock requests", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh sách đề xuất nhập hàng.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("restockCsrfToken");
        String formToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(formToken)) {
            session.setAttribute("restockError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/restock-requests");
            return;
        }

        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "create": {
                    int supplierId = Integer.parseInt(request.getParameter("supplierId"));
                    int ingredientId = Integer.parseInt(request.getParameter("ingredientId"));
                    String qtyStr = request.getParameter("quantity");
                    String priceStr = request.getParameter("unitPrice");

                    if (qtyStr == null || qtyStr.isBlank() || priceStr == null || priceStr.isBlank()) {
                        session.setAttribute("restockError", "Vui lòng nhập đầy đủ số lượng và đơn giá dự kiến.");
                    } else {
                        BigDecimal qty = new BigDecimal(qtyStr.trim());
                        BigDecimal price = new BigDecimal(priceStr.trim());

                        List<RestockItem> items = new ArrayList<>();
                        items.add(new RestockItem(ingredientId, "", "", qty, price));

                        restockRequestDAO.createRestockRequest(supplierId, user.getId(), items);
                        session.setAttribute("restockMessage", "Tạo đề xuất nhập hàng thành công!");
                    }
                    break;
                }
                case "approve": {
                    int poId = Integer.parseInt(request.getParameter("id"));
                    restockRequestDAO.updateStatus(poId, "ORDERED");
                    session.setAttribute("restockMessage", "Đã duyệt đề xuất PO-" + String.format("%04d", poId) + " thành công!");
                    break;
                }
                case "reject": {
                    int poId = Integer.parseInt(request.getParameter("id"));
                    restockRequestDAO.updateStatus(poId, "CANCELLED");
                    session.setAttribute("restockMessage", "Đã từ chối đề xuất PO-" + String.format("%04d", poId) + "!");
                    break;
                }
                default:
                    session.setAttribute("restockError", "Thao tác không được hỗ trợ.");
                    break;
            }
        } catch (SQLException | ClassNotFoundException | NumberFormatException e) {
            getServletContext().log("Restock action error", e);
            session.setAttribute("restockError", e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/restock-requests");
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
