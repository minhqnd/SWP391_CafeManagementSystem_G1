package controller;

import dao.CategoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Category;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminCategoriesController", urlPatterns = {"/admin/categories"})
public class AdminCategoriesController extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("categoryCsrfToken") == null) {
            session.setAttribute("categoryCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "categoryMessage");
        moveFlash(session, request, "categoryError");
        try {
            List<Category> categories = categoryDAO.getAllCategoriesWithCount();
            request.setAttribute("categories", categories);
            request.setAttribute("categoryTotal", categories.size());
            request.setAttribute("categoryActive", categories.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            request.setAttribute("categoryInactive", categories.stream().filter(c -> "INACTIVE".equals(c.getStatus())).count());
            request.setAttribute("productTotal", categoryDAO.getProductCount());

            String editId = request.getParameter("editId");
            if (editId != null && !editId.isBlank()) {
                try {
                    Category selected = categoryDAO.getCategoryById(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedCategory", selected);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed selection and show a blank form.
                }
            }
            request.getRequestDispatcher("/admin-categories.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin categories", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh mục. Hãy kiểm tra kết nối cơ sở dữ liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String expected = (String) session.getAttribute("categoryCsrfToken");
        String submitted = request.getParameter("csrfToken");
        if (expected == null || !expected.equals(submitted)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn. Tải lại trang rồi thử lại.");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("save".equals(action)) {
                save(request, session);
            } else if ("toggle".equals(action)) {
                toggle(request, session);
            } else if ("delete".equals(action)) {
                delete(request, session);
            } else {
                session.setAttribute("categoryError", "Thao tác không hợp lệ.");
            }
        } catch (SQLException e) {
            getServletContext().log("Category database operation failed", e);
            if ("23000".equals(e.getSQLState())) {
                session.setAttribute("categoryError", "Tên danh mục đã tồn tại hoặc danh mục đang được sản phẩm sử dụng.");
            } else {
                session.setAttribute("categoryError", "Không thể lưu thay đổi vào database. Vui lòng kiểm tra database.");
            }
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            session.setAttribute("categoryError", e.getMessage() == null ? "Dữ liệu danh mục không hợp lệ." : e.getMessage());
        } catch (RuntimeException e) {
            getServletContext().log("Category operation failed", e);
            session.setAttribute("categoryError", "Không thể hoàn tất thao tác danh mục.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/categories");
    }

    private void save(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), true);
        String name = trimToNull(request.getParameter("name"));
        String description = trimToNull(request.getParameter("description"));
        String imageUrl = trimToNull(request.getParameter("imageUrl"));
        String status = request.getParameter("status");

        if (name == null || name.length() > 100) throw new IllegalArgumentException("Tên danh mục bắt buộc và tối đa 100 ký tự.");
        if (imageUrl != null && imageUrl.length() > 500) throw new IllegalArgumentException("Đường dẫn ảnh tối đa 500 ký tự.");
        if (imageUrl != null && !(imageUrl.startsWith("https://") || imageUrl.startsWith("http://"))) {
            throw new IllegalArgumentException("Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (categoryDAO.categoryNameExists(name, id)) throw new IllegalArgumentException("Tên danh mục đã tồn tại.");

        Category category = new Category(id, name, status, description, imageUrl, 0);
        if (!categoryDAO.saveCategory(category)) throw new IllegalArgumentException("Không tìm thấy danh mục cần cập nhật.");
        session.setAttribute("categoryMessage", id > 0 ? "Đã cập nhật danh mục '" + name + "'." : "Đã tạo danh mục '" + name + "'.");
    }

    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        String status = request.getParameter("status");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (!categoryDAO.updateCategoryStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        session.setAttribute("categoryMessage", "Đã cập nhật trạng thái danh mục.");
    }

    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        Category category = categoryDAO.getCategoryById(id);
        if (category == null) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        if (categoryDAO.getProductCountByCategory(id) > 0) {
            throw new IllegalArgumentException("Không thể xóa danh mục đang có sản phẩm. Hãy chuyển sang Tạm ẩn.");
        }
        if (!categoryDAO.deleteCategory(id)) throw new IllegalArgumentException("Không thể xóa danh mục.");
        session.setAttribute("categoryMessage", "Đã xóa danh mục '" + category.getName() + "'.");
    }

    private int parseId(String value, boolean allowZero) {
        try {
            int id = Integer.parseInt(value == null ? (allowZero ? "0" : "") : value);
            if (id < (allowZero ? 0 : 1)) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã danh mục không hợp lệ.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String name) {
        Object message = session.getAttribute(name);
        if (message != null) {
            request.setAttribute(name, message);
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ tài khoản Admin được quản lý danh mục.");
            return false;
        }
        return true;
    }
}
