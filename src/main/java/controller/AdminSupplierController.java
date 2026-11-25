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

    // Giới hạn độ dài khớp với cấu trúc bảng `suppliers` trong DB
    static final int MAX_NAME_LENGTH = 150;
    static final int MAX_PHONE_LENGTH = 20;
    static final int MAX_ADDRESS_LENGTH = 500;

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

        session.removeAttribute("supplierMessage");
        session.removeAttribute("supplierError");

        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "create": {
                    String name = request.getParameter("name");
                    String phone = request.getParameter("phone");
                    String address = request.getParameter("address");
                    String rawStatus = request.getParameter("status");

                    // Kiểm tra từng trường dữ liệu trước khi lưu
                    String error = validateName(name);
                    if (error == null) error = validatePhone(phone);
                    if (error == null) error = validateAddress(address);
                    if (error != null) {
                        session.setAttribute("supplierError", error);
                        break;
                    }

                    String status = parseStatus(rawStatus, true);
                    Supplier s = new Supplier(
                            0,
                            name.trim(),
                            phone != null ? phone.trim() : "",
                            address != null ? address.trim() : "",
                            status
                    );
                    supplierDAO.insertSupplier(s);
                    session.setAttribute("supplierMessage", "Thêm nhà cung cấp mới thành công!");
                    break;
                }
                case "update": {
                    int id = parseSupplierId(request.getParameter("id"));
                    String name = request.getParameter("name");
                    String phone = request.getParameter("phone");
                    String address = request.getParameter("address");
                    String rawStatus = request.getParameter("status");

                    // Kiểm tra từng trường dữ liệu trước khi cập nhật
                    String error = validateName(name);
                    if (error == null) error = validatePhone(phone);
                    if (error == null) error = validateAddress(address);
                    if (error != null) {
                        session.setAttribute("supplierError", error);
                        break;
                    }

                    String status = parseStatus(rawStatus, true);
                    Supplier s = new Supplier(
                            id,
                            name.trim(),
                            phone != null ? phone.trim() : "",
                            address != null ? address.trim() : "",
                            status
                    );
                    if (supplierDAO.updateSupplier(s)) {
                        session.setAttribute("supplierMessage", "Cập nhật nhà cung cấp thành công!");
                    } else {
                        session.setAttribute("supplierError", "Không tìm thấy nhà cung cấp cần cập nhật. Hãy tải lại danh sách.");
                    }
                    break;
                }
                case "toggle": {
                    int id = parseSupplierId(request.getParameter("id"));
                    String currentStatus = parseStatus(request.getParameter("currentStatus"), false);
                    if (supplierDAO.toggleStatus(id, currentStatus)) {
                        session.setAttribute("supplierMessage", "Thay đổi trạng thái nhà cung cấp thành công!");
                    } else {
                        session.setAttribute("supplierError", "Không tìm thấy nhà cung cấp. Hãy tải lại danh sách.");
                    }
                    break;
                }
                default:
                    session.setAttribute("supplierError", "Thao tác không được hỗ trợ.");
                    break;
            }
        } catch (NumberFormatException e) {
            getServletContext().log("Supplier action error", e);
            session.setAttribute("supplierError", supplierErrorMessage(e));
        } catch (IllegalArgumentException e) {
            session.setAttribute("supplierError", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Supplier action error", e);
            session.setAttribute("supplierError", supplierErrorMessage(e));
        }

        response.sendRedirect(request.getContextPath() + "/admin/suppliers");
    }

    // Kiểm tra tên nhà cung cấp bắt buộc, tối đa 150 ký tự.
    static String validateName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "Tên nhà cung cấp không được để trống.";
        }
        if (rawName.trim().length() > MAX_NAME_LENGTH) {
            return "Tên nhà cung cấp không được vượt quá 150 ký tự.";
        }
        return null;
    }

    /**
     * Kiểm tra số điện thoại (không bắt buộc): tối đa 20 ký tự,
     * chỉ cho phép chữ số, khoảng trắng, dấu '.', '-' và '+' ở đầu; có từ 8 đến 15 chữ số.
     */
    static String validatePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String phone = rawPhone.trim();
        if (phone.length() > MAX_PHONE_LENGTH) {
            return "Số điện thoại không được vượt quá 20 ký tự.";
        }
        int digitCount = 0;
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digitCount++;
            } else if (ch == '+' && i == 0) {
                // Cho phép dấu '+' ở đầu số điện thoại quốc tế
            } else if (ch != ' ' && ch != '-' && ch != '.') {
                return "Số điện thoại không hợp lệ (chỉ gồm số, từ 8 đến 15 chữ số).";
            }
        }
        if (digitCount < 8 || digitCount > 15) {
            return "Số điện thoại không hợp lệ (chỉ gồm số, từ 8 đến 15 chữ số).";
        }
        return null;
    }

    // Kiểm tra địa chỉ (không bắt buộc), tối đa 500 ký tự.
    static String validateAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return null;
        }
        if (rawAddress.trim().length() > MAX_ADDRESS_LENGTH) {
            return "Địa chỉ không được vượt quá 500 ký tự.";
        }
        return null;
    }

    // Kiểm tra trạng thái chỉ nhận ACTIVE hoặc INACTIVE.
    static String parseStatus(String rawStatus, boolean defaultActive) {
        if (rawStatus == null || rawStatus.isBlank()) {
            if (defaultActive) {
                return "ACTIVE";
            }
            throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ.");
        }
        String status = rawStatus.trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ.");
        }
        return status;
    }

    // Đọc mã nhà cung cấp, bắt buộc > 0.
    private static int parseSupplierId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("Mã nhà cung cấp không hợp lệ.");
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Mã nhà cung cấp không hợp lệ.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã nhà cung cấp không hợp lệ.");
        }
    }

    // Chuyển lỗi DB / định dạng sang thông báo tiếng Việt an toàn.
    static String supplierErrorMessage(Exception error) {
        if (error instanceof SQLException sql) {
            String state = sql.getSQLState();
            if ("45000".equals(state)) {
                return sql.getMessage();
            }
            if ("22001".equals(state)) {
                return "Dữ liệu nhập vào vượt quá độ dài cho phép (tên tối đa 150 ký tự, số điện thoại tối đa 20 ký tự).";
            }
            if ("23000".equals(state)) {
                return "Không thể hoàn tất thao tác do ràng buộc dữ liệu liên kết với nhà cung cấp.";
            }
            return "Không thể lưu thông tin nhà cung cấp. Vui lòng thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.";
        }
        if (error instanceof NumberFormatException) {
            return "Mã nhà cung cấp không hợp lệ.";
        }
        return "Không thể lưu thông tin nhà cung cấp. Vui lòng liên hệ quản trị viên.";
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
