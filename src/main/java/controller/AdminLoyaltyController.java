package controller;

import dao.LoyaltyTransactionDAO;
import dao.ShopSettingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShopSettings;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

// BF12: JSP gửi GET (xem/tìm khách) hoặc POST (lưu quy tắc/cộng/trừ) tới URL này.
@WebServlet(name = "AdminLoyaltyController", urlPatterns = {"/admin/loyalty"})
public class AdminLoyaltyController extends HttpServlet {
    private final LoyaltyTransactionDAO loyaltyDAO = new LoyaltyTransactionDAO();
    private final ShopSettingDAO settingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // BF12 - MỞ TRANG: kiểm tra quyền trước khi đọc dữ liệu quản trị.
        if (!ScaffoldAccess.requireAdmin(request, response)) return;

        HttpSession session = request.getSession();
        if (session.getAttribute("loyaltyCsrfToken") == null) {
            session.setAttribute("loyaltyCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "loyaltyMessage");
        moveFlash(session, request, "loyaltyError");

        try {
            // Đọc quy tắc và thống kê; nhật ký bên dưới là 50 giao dịch mới nhất toàn hệ thống.
            ShopSettings settings = settingDAO.getSettings();
            long totalPoints = loyaltyDAO.getTotalPoints();
            long redeemableSteps = settings.getPointsStep() > 0 ? totalPoints / settings.getPointsStep() : 0;
            long totalPointValue = Math.round(redeemableSteps * settings.getPointsStepValue());

            request.setAttribute("settings", settings);
            request.setAttribute("customerCountWithPoints", loyaltyDAO.countCustomersWithPoints());
            request.setAttribute("totalPoints", totalPoints);
            request.setAttribute("totalPointValue", totalPointValue);
            request.setAttribute("transactionCount", loyaltyDAO.countTransactions());
            request.setAttribute("transactions", loyaltyDAO.findRecent(50));

            // BF12 - TRA CỨU: nhận phone từ form GET trong admin-loyalty.jsp rồi gọi DAO.
            // Chưa nhập phone thì chọn khách mặc định; có phone thì tìm đúng số điện thoại.
            String phone = trim(request.getParameter("phone"));
            User customer = phone.isEmpty() ? loyaltyDAO.findDefaultCustomer() : loyaltyDAO.findCustomerByPhone(phone);
            request.setAttribute("searchPhone", phone.isEmpty() && customer != null ? customer.getPhone() : phone);
            // DAO trả User về Controller; JSP đọc ${activeCustomer.totalPoints}, fullName...
            request.setAttribute("activeCustomer", customer);
            if (customer != null) {
                LoyaltyTransactionDAO.CustomerActivity activity = loyaltyDAO.getCustomerActivity(customer.getId());
                request.setAttribute("customerActivity", activity);
                long customerSteps = settings.getPointsStep() > 0
                        ? customer.getTotalPoints() / settings.getPointsStep() : 0;
                request.setAttribute("customerPointValue",
                        Math.round(customerSteps * settings.getPointsStepValue()));
            } else if (!phone.isEmpty()) {
                // Nhánh "Customer found? -> No": không tìm thấy thì chỉ hiển thị thông báo.
                request.setAttribute("lookupError", "Không tìm thấy khách hàng có số điện thoại " + phone + ".");
            }
            // KẾT QUẢ GET: chuyển dữ liệu trong request sang JSP để dựng giao diện.
            request.getRequestDispatcher("/admin-loyalty.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Failed to load admin loyalty", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được dữ liệu tích điểm. Hãy kiểm tra cơ sở dữ liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!ScaffoldAccess.requireAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

        // Kiểm tra mã bảo vệ form khớp session; đây không phải OTP và không thay thế kiểm tra quyền.
        if (!isValidCsrf(session, request.getParameter("csrfToken"))) {
            session.setAttribute("loyaltyError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/loyalty");
            return;
        }

        // BF12 - NHẬN THAO TÁC: trường ẩn action quyết định nhánh xử lý trong cùng doPost().
        String action = trim(request.getParameter("action"));
        String phone = trim(request.getParameter("phone"));
        try {
            if ("updateRules".equals(action)) {
                // Nút "Lưu quy tắc" -> kiểm tra dữ liệu -> ShopSettingDAO.saveSettings().
                updateRules(request);
                session.setAttribute("loyaltyMessage", "Đã lưu quy tắc tích và đổi điểm.");
            } else if ("adjust".equals(action)) {
                // Nút "Cộng điểm/Trừ điểm" -> kiểm tra đầu vào -> LoyaltyTransactionDAO.adjustPoints().
                adjustPoints(request);
                session.setAttribute("loyaltyMessage", "Đã cập nhật điểm khách hàng và ghi lịch sử giao dịch.");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException | SQLException e) {
            // Lỗi kiểm tra hoặc lỗi lưu được đưa về giao diện; không thông báo thành công.
            getServletContext().log("Admin loyalty update failed", e);
            session.setAttribute("loyaltyError", e.getMessage());
        }

        // POST -> redirect -> GET: đọc lại dữ liệu để hiển thị, tránh gửi lại POST khi F5 trang kết quả.
        // Giữ phone trên URL để sau khi điều chỉnh vẫn xem đúng khách vừa chọn.
        String redirect = request.getContextPath() + "/admin/loyalty";
        if (!phone.isEmpty()) redirect += "?phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }

    private void updateRules(HttpServletRequest request) {
        // BF12 - KIỂM TRA QUY TẮC: giới hạn nghiệp vụ nằm ở Controller, trước khi gọi DAO lưu.
        long earnUnit = parseLong(request.getParameter("pointsEarnUnit"), "Số tiền chi tiêu để nhận 1 điểm");
        int pointsStep = (int) parseLong(request.getParameter("pointsStep"), "Số điểm cho mỗi lần đổi");
        long stepValue = parseLong(request.getParameter("pointsStepValue"), "Số tiền giảm cho mỗi lần đổi");
        if (earnUnit < 1 || earnUnit > 100_000_000L) {
            throw new IllegalArgumentException("Số tiền chi tiêu để nhận 1 điểm phải từ 1 đến 100.000.000đ.");
        }
        if (pointsStep < 1 || pointsStep > 1_000_000) {
            throw new IllegalArgumentException("Số điểm cho mỗi lần đổi phải từ 1 đến 1.000.000 điểm.");
        }
        if (stepValue < 0 || stepValue > 100_000_000L) {
            throw new IllegalArgumentException("Số tiền giảm cho mỗi lần đổi phải từ 0 đến 100.000.000đ.");
        }

        // Lấy cấu hình hiện có rồi thay ba giá trị điểm; giữ các thiết lập khác trong đối tượng.
        ShopSettings settings = settingDAO.getSettings();
        settings.setPointsEarnUnit(earnUnit);
        settings.setPointsStep(pointsStep);
        settings.setPointsStepValue(stepValue);
        if (!settingDAO.saveSettings(settings)) {
            throw new IllegalArgumentException("Không thể lưu quy tắc vào shop_settings.");
        }
    }

    private void adjustPoints(HttpServletRequest request) throws SQLException {
        // BF12 - KIỂM TRA ĐIỀU CHỈNH: ID hợp lệ, số điểm 1..1.000.000, loại add/subtract.
        int customerId = (int) parseLong(request.getParameter("customerId"), "Khách hàng");
        int points = (int) parseLong(request.getParameter("points"), "Số điểm");
        if (customerId <= 0) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        if (points <= 0 || points > 1_000_000) {
            throw new IllegalArgumentException("Số điểm điều chỉnh phải từ 1 đến 1.000.000.");
        }
        String adjustType = trim(request.getParameter("adjustType"));
        if (!"add".equals(adjustType) && !"subtract".equals(adjustType)) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }
        // Cộng truyền số dương; trừ truyền số âm. DAO kiểm tra tiếp số dư thực tế trong DB.
        loyaltyDAO.adjustPoints(customerId, "subtract".equals(adjustType) ? -points : points);
    }

    private long parseLong(String raw, String label) {
        try {
            return Long.parseLong(trim(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private boolean isValidCsrf(HttpSession session, String submitted) {
        Object expected = session.getAttribute("loyaltyCsrfToken");
        return expected != null && expected.equals(submitted);
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            request.setAttribute(key, value);
            session.removeAttribute(key);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
