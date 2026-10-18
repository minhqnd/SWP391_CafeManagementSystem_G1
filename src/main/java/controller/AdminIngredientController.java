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

                    if (name == null || name.trim().isEmpty() || unit == null || unit.trim().isEmpty()) {
                        session.setAttribute("ingredientError", "Tên nguyên liệu và đơn vị tính là bắt buộc.");
                    } else {
                        Integer supplierId = (supplierParam != null && !supplierParam.isBlank() && !supplierParam.equals("0"))
                                ? Integer.parseInt(supplierParam) : null;
                        BigDecimal currentStock = (stockParam != null && !stockParam.isBlank())
                                ? new BigDecimal(stockParam.trim()) : BigDecimal.ZERO;
                        BigDecimal minStockLevel = (minStockParam != null && !minStockParam.isBlank())
                                ? new BigDecimal(minStockParam.trim()) : BigDecimal.ZERO;

                        Ingredient item = new Ingredient(0, supplierId, name.trim(), unit.trim(), currentStock, minStockLevel);
                        ingredientDAO.insertIngredient(item);
                        session.setAttribute("ingredientMessage", "Thêm nguyên vật liệu mới thành công!");
                    }
                    break;
                }
                case "update": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String name = request.getParameter("name");
                    String unit = request.getParameter("unit");
                    String supplierParam = request.getParameter("supplierId");
                    String minStockParam = request.getParameter("minStockLevel");

                    if (name == null || name.trim().isEmpty() || unit == null || unit.trim().isEmpty()) {
                        session.setAttribute("ingredientError", "Tên nguyên liệu và đơn vị tính là bắt buộc.");
                    } else {
                        Integer supplierId = (supplierParam != null && !supplierParam.isBlank() && !supplierParam.equals("0"))
                                ? Integer.parseInt(supplierParam) : null;
                        BigDecimal minStockLevel = (minStockParam != null && !minStockParam.isBlank())
                                ? new BigDecimal(minStockParam.trim()) : BigDecimal.ZERO;

                        Ingredient item = new Ingredient(id, supplierId, name.trim(), unit.trim(), BigDecimal.ZERO, minStockLevel);
                        ingredientDAO.updateIngredient(item);
                        session.setAttribute("ingredientMessage", "Cập nhật nguyên vật liệu thành công!");
                    }
                    break;
                }
                case "delete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    ingredientDAO.deleteIngredient(id);
                    session.setAttribute("ingredientMessage", "Xóa nguyên vật liệu thành công!");
                    break;
                }
                default:
                    session.setAttribute("ingredientError", "Thao tác không được hỗ trợ.");
                    break;
            }
        } catch (SQLException | ClassNotFoundException | NumberFormatException e) {
            getServletContext().log("Ingredient action error", e);
            session.setAttribute("ingredientError", e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/ingredients");
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
