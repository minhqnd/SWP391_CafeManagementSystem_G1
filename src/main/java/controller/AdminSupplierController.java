package controller;

import dao.SupplierDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Supplier;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminSupplierController", urlPatterns = {"/admin/suppliers"})
public class AdminSupplierController extends HttpServlet {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        if (session.getAttribute("supplierCsrfToken") == null) {
            session.setAttribute("supplierCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "supplierMessage");
        moveFlash(session, request, "supplierError");

        try {
            List<Supplier> suppliers = supplierDAO.getAllSuppliers();
            int[] stats = supplierDAO.getSupplierStats();

            request.setAttribute("suppliers", suppliers);
            request.setAttribute("totalSuppliers", stats[0]);
            request.setAttribute("activeSuppliers", stats[1]);
            request.setAttribute("inactiveSuppliers", stats[2]);

            String editId = request.getParameter("editId");
            if (editId != null && !editId.isBlank()) {
                try {
                    Supplier selected = supplierDAO.getSupplierById(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedSupplier", selected);
                } catch (NumberFormatException ignored) {
                }
            }

            request.getRequestDispatcher("/admin-suppliers.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin suppliers", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh sách nhà cung cấp.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("supplierCsrfToken");
        String formToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(formToken)) {
            session.setAttribute("supplierError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/suppliers");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "create": {
                    String name = request.getParameter("name");
                    String phone = request.getParameter("phone");
                    String address = request.getParameter("address");
                    String status = request.getParameter("status");

                    if (name == null || name.trim().isEmpty()) {
                        session.setAttribute("supplierError", "Tên nhà cung cấp không được để trống.");
                    } else {
                        Supplier s = new Supplier(0, name.trim(), phone != null ? phone.trim() : "",
                                address != null ? address.trim() : "",
                                status != null ? status.trim() : "ACTIVE");
                        supplierDAO.insertSupplier(s);
                        session.setAttribute("supplierMessage", "Thêm nhà cung cấp mới thành công!");
                    }
                    break;
                }
                case "update": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String name = request.getParameter("name");
                    String phone = request.getParameter("phone");
                    String address = request.getParameter("address");
                    String status = request.getParameter("status");

                    if (name == null || name.trim().isEmpty()) {
                        session.setAttribute("supplierError", "Tên nhà cung cấp không được để trống.");
                    } else {
                        Supplier s = new Supplier(id, name.trim(), phone != null ? phone.trim() : "",
                                address != null ? address.trim() : "",
                                status != null ? status.trim() : "ACTIVE");
                        supplierDAO.updateSupplier(s);
                        session.setAttribute("supplierMessage", "Cập nhật nhà cung cấp thành công!");
                    }
                    break;
                }
                case "toggle": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String currentStatus = request.getParameter("currentStatus");
                    supplierDAO.toggleStatus(id, currentStatus);
                    session.setAttribute("supplierMessage", "Thay đổi trạng thái nhà cung cấp thành công!");
                    break;
                }
                default:
                    session.setAttribute("supplierError", "Thao tác không được hỗ trợ.");
                    break;
            }
        } catch (SQLException | ClassNotFoundException | NumberFormatException e) {
            getServletContext().log("Supplier action error", e);
            session.setAttribute("supplierError", "Đã xảy ra lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/suppliers");
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
