package controller;

import dao.EmployeeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AdminEmployeesController - Màn hình Quản lý nhân viên (UC54, chỉ tài khoản ADMIN)
 * GET  /admin/employees?position=... : Danh sách nhân viên (users role ADMIN / STAFF) + thống kê
 * POST /admin/employees action=save   : Thêm / sửa nhân viên
 * POST /admin/employees action=lock | unlock : Khoá (LOCKED) / mở khoá (ACTIVE)
 * Không xoá cứng nhân viên - nghỉ việc thì chuyển status INACTIVE.
 */
@WebServlet(name = "AdminEmployeesController", urlPatterns = {"/admin/employees"})
public class AdminEmployeesController extends HttpServlet {

    /** Vai trò chọn trên form: ADMIN = Quản trị, còn lại là nhân viên (STAFF) theo vị trí làm việc */
    private static final Set<String> ROLE_CHOICES = Set.of("ADMIN", "CASHIER", "BARISTA", "SHIPPER");
    private static final Set<String> POSITIONS = Set.of("MANAGER", "BARISTA", "CASHIER", "SHIPPER");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");
    private static final String[] FORM_FIELDS = {"id", "fullName", "phone", "email", "role", "position",
            "hireDate", "birthday", "status", "address"};
    private static final String SESSION_REFRESHED_MESSAGE =
            "Phiên làm việc đã được làm mới (VD vừa đăng nhập lại). Kiểm tra lại thông tin, nhập lại mật khẩu và bấm lưu lần nữa.";

    // Giống quy tắc SĐT của màn đăng ký; SĐT là tên đăng nhập
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{9,11}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_ADDRESS_LENGTH = 500;

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "employeeMessage");
        CartController.moveFlashMessage(session, request, "employeeError");
        showPage(request, response, request.getParameter("position"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        String expected = (String) session.getAttribute("employeeCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            // Token cũ (VD đăng xuất / đăng nhập lại khi trang còn mở): không lưu gì,
            // hiển thị lại form với dữ liệu đã nhập và token mới thay vì trang lỗi 403
            if ("save".equals(action)) {
                request.setAttribute("employeeForm", readForm(request));
                request.setAttribute("employeeFormError", SESSION_REFRESHED_MESSAGE);
                showPage(request, response, request.getParameter("returnPosition"));
            } else {
                session.setAttribute("employeeError", "Phiên làm việc đã được làm mới. Vui lòng thao tác lại.");
                redirectToPage(request, response);
            }
            return;
        }

        if ("save".equals(action)) {
            save(request, response, session);
            return;
        }

        try {
            if ("lock".equals(action) || "unlock".equals(action)) {
                changeStatus(request, session, "lock".equals(action));
            } else {
                session.setAttribute("employeeError", "Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("employeeError", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Employee status update failed", e);
            session.setAttribute("employeeError", "Không thể lưu thay đổi vào database. Vui lòng thử lại.");
        }
        redirectToPage(request, response);
    }

    /** Thêm / sửa nhân viên. Lỗi dữ liệu thì hiển thị lại trang, mở lại modal với dữ liệu đã nhập. */
    private void save(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws ServletException, IOException {
        Map<String, String> form = readForm(request);

        try {
            User me = (User) session.getAttribute("user");
            User employee = validateEmployee(form, me);
            String password = validatePassword(request.getParameter("password"), employee.getId() == 0);

            if (employee.getId() == 0) {
                employeeDAO.createEmployee(employee, password);
                session.setAttribute("employeeMessage", "Đã thêm nhân viên mới!");
            } else {
                employeeDAO.updateEmployee(employee, password);
                if (employee.getId() == me.getId()) {
                    refreshSessionUser(me, employee);
                }
                session.setAttribute("employeeMessage", "Cập nhật nhân viên thành công!");
            }
            redirectToPage(request, response);
            return;
        } catch (IllegalArgumentException e) {
            request.setAttribute("employeeFormError", e.getMessage());
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                // Trùng khoá UNIQUE phone (2 thao tác chạy song song vượt qua bước kiểm tra trước)
                request.setAttribute("employeeFormError", "Số điện thoại đã được dùng cho tài khoản khác.");
            } else {
                getServletContext().log("Employee save failed", e);
                request.setAttribute("employeeFormError", "Không thể lưu nhân viên vào database. Vui lòng thử lại.");
            }
        } catch (ClassNotFoundException e) {
            getServletContext().log("Employee save failed", e);
            request.setAttribute("employeeFormError", "Không thể kết nối database. Vui lòng thử lại.");
        }

        // Không bao giờ trả mật khẩu về giao diện
        request.setAttribute("employeeForm", form);
        showPage(request, response, request.getParameter("returnPosition"));
    }

    /** Dữ liệu form đã cắt khoảng trắng (không gồm mật khẩu) */
    private Map<String, String> readForm(HttpServletRequest request) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String field : FORM_FIELDS) {
            String value = request.getParameter(field);
            form.put(field, value == null ? "" : value.trim());
        }
        return form;
    }

    private User validateEmployee(Map<String, String> form, User me) throws SQLException, ClassNotFoundException {
        int id = parseId(form.get("id"), true);
        if (id > 0 && employeeDAO.getEmployeeById(id) == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên cần sửa.");
        }

        String fullName = form.get("fullName");
        if (fullName.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập họ và tên.");
        if (fullName.length() > 120) throw new IllegalArgumentException("Họ và tên tối đa 120 ký tự.");

        String phone = form.get("phone");
        if (phone.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số điện thoại.");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (9-11 chữ số, VD: 0912345678).");
        }
        if (employeeDAO.phoneExists(phone, id)) {
            throw new IllegalArgumentException("Số điện thoại đã được dùng cho tài khoản khác.");
        }

        String email = form.get("email");
        if (!email.isEmpty()) {
            if (email.length() > 150 || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Email không đúng định dạng (VD: ten@auracafe.vn).");
            }
            if (employeeDAO.emailExists(email, id)) {
                throw new IllegalArgumentException("Email đã được dùng cho tài khoản khác.");
            }
        }

        // Một ô Vai trò: Quản trị -> role ADMIN (position MANAGER); Thu ngân / Pha chế / Giao hàng -> role STAFF
        String roleChoice = requireAllowed(form.get("role"), ROLE_CHOICES, "Vai trò không hợp lệ.");
        String role = "ADMIN".equals(roleChoice) ? "ADMIN" : "STAFF";
        String position = "ADMIN".equals(roleChoice) ? "MANAGER" : roleChoice;
        String status = requireAllowed(form.get("status"), STATUSES, "Trạng thái không hợp lệ.");
        String hireDate = parseOptionalDate(form.get("hireDate"), "Ngày vào làm không hợp lệ.");
        String birthday = parseOptionalDate(form.get("birthday"), "Ngày sinh không hợp lệ.");
        if (birthday != null && LocalDate.parse(birthday).isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
        }

        String address = form.get("address");
        if (address.length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Địa chỉ tối đa " + MAX_ADDRESS_LENGTH + " ký tự.");
        }

        // Admin đang đăng nhập không được tự hạ quyền / tự khoá chính mình
        if (id > 0 && id == me.getId()) {
            if (!"ADMIN".equals(role)) {
                throw new IllegalArgumentException("Bạn không thể tự hạ quyền tài khoản đang đăng nhập.");
            }
            if (!"ACTIVE".equals(status)) {
                throw new IllegalArgumentException("Bạn không thể tự khóa hoặc ngừng hoạt động tài khoản đang đăng nhập.");
            }
        }

        User employee = new User(id, fullName, phone, email.isEmpty() ? null : email, role, position, status,
                birthday, address.isEmpty() ? null : address);
        employee.setHireDate(hireDate);
        return employee;
    }

    /**
     * Mật khẩu bắt buộc khi thêm mới; khi sửa bỏ trống = giữ nguyên (trả về null).
     * Cắt khoảng trắng 2 đầu giống UserDAO.login() để tài khoản đăng nhập được bằng đúng mật khẩu vừa đặt.
     */
    private String validatePassword(String rawPassword, boolean isNew) {
        String password = rawPassword == null ? "" : rawPassword.trim();
        if (password.isEmpty()) {
            if (isNew) throw new IllegalArgumentException("Vui lòng nhập mật khẩu cho tài khoản mới.");
            return null;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Mật khẩu phải từ " + MIN_PASSWORD_LENGTH + " ký tự trở lên.");
        }
        if (password.length() > 255) throw new IllegalArgumentException("Mật khẩu tối đa 255 ký tự.");
        return password;
    }

    private void changeStatus(HttpServletRequest request, HttpSession session, boolean lock)
            throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        User employee = employeeDAO.getEmployeeById(id);
        if (employee == null) throw new IllegalArgumentException("Không tìm thấy nhân viên.");

        User me = (User) session.getAttribute("user");
        if (lock && id == me.getId()) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản đang đăng nhập.");
        }

        employeeDAO.updateStatus(id, lock ? "LOCKED" : "ACTIVE");
        session.setAttribute("employeeMessage", employee.getFullName() + (lock ? ": Đã khóa." : ": Hoạt động."));
    }

    /** rawPositionFilter: bộ lọc chức vụ (GET ?position=..., POST returnPosition) */
    private void showPage(HttpServletRequest request, HttpServletResponse response, String rawPositionFilter)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (session.getAttribute("employeeCsrfToken") == null) {
            session.setAttribute("employeeCsrfToken", UUID.randomUUID().toString());
        }

        String position = normalizePositionFilter(rawPositionFilter);
        try {
            request.setAttribute("employees", employeeDAO.getEmployees(position));
            request.setAttribute("employeeStats", employeeDAO.getStats());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load employees", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh sách nhân viên. Hãy kiểm tra kết nối cơ sở dữ liệu.");
            return;
        }
        request.setAttribute("positionFilter", position == null ? "ALL" : position);
        request.setAttribute("today", LocalDate.now().toString());
        request.getRequestDispatcher("/admin-employees.jsp").forward(request, response);
    }

    /** PRG: quay lại danh sách, giữ bộ lọc chức vụ đang chọn (form gửi kèm returnPosition) */
    private void redirectToPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String position = normalizePositionFilter(request.getParameter("returnPosition"));
        String url = request.getContextPath() + "/admin/employees";
        if (position != null) {
            url += "?position=" + URLEncoder.encode(position, StandardCharsets.UTF_8);
        }
        response.sendRedirect(url);
    }

    /** Admin vừa sửa chính mình: cập nhật thông tin hiển thị trong session (vai trò / trạng thái không đổi) */
    private void refreshSessionUser(User me, User saved) {
        me.setFullName(saved.getFullName());
        me.setPhone(saved.getPhone());
        me.setEmail(saved.getEmail());
        me.setPosition(saved.getPosition());
        me.setBirthday(saved.getBirthday());
        me.setAddress(saved.getAddress());
    }

    private String normalizePositionFilter(String rawPosition) {
        return rawPosition != null && POSITIONS.contains(rawPosition) ? rawPosition : null;
    }

    private String requireAllowed(String value, Set<String> allowed, String errorMessage) {
        if (value == null || !allowed.contains(value)) throw new IllegalArgumentException(errorMessage);
        return value;
    }

    /** Ngày dạng YYYY-MM-DD (input type="date"); rỗng thì trả về null để lưu NULL */
    private String parseOptionalDate(String rawDate, String errorMessage) {
        if (rawDate.isEmpty()) return null;
        try {
            return LocalDate.parse(rawDate).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private int parseId(String value, boolean allowZero) {
        try {
            int id = Integer.parseInt(value == null || value.isEmpty() ? (allowZero ? "0" : "") : value.trim());
            if (id < (allowZero ? 0 : 1)) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ tài khoản Admin được quản lý nhân viên.");
            return false;
        }
        return true;
    }
}
