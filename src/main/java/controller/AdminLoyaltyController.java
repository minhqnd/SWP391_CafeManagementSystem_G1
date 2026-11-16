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
import utils.AccountValidation;
import utils.LoyaltyValidation;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "AdminLoyaltyController", urlPatterns = {"/admin/loyalty"})
public class AdminLoyaltyController extends HttpServlet {
    private final LoyaltyTransactionDAO loyaltyDAO;
    private final ShopSettingDAO settingDAO;

    public AdminLoyaltyController() { this(new LoyaltyTransactionDAO(), new ShopSettingDAO()); }

    AdminLoyaltyController(LoyaltyTransactionDAO loyaltyDAO, ShopSettingDAO settingDAO) {
        this.loyaltyDAO = loyaltyDAO;
        this.settingDAO = settingDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // RU02: . Nhận GET mở trang; chỉ Admin ACTIVE trong session được đi tiếp.
        // .. Nạp quy tắc, thống kê, nhật ký và khách đang tra; không cộng/trừ điểm.
        // ... ShopSettingDAO.getSettings() [RU03] đọc quy tắc; doGet() gửi form tại [RU04].
        if (!ScaffoldAccess.requireAdmin(request, response)) return;

        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute("loyaltyCsrfToken") == null) {
                session.setAttribute("loyaltyCsrfToken", UUID.randomUUID().toString());
            }
            if (session.getAttribute("loyaltyOperationToken") == null) {
                session.setAttribute("loyaltyOperationToken", UUID.randomUUID().toString());
            }
        }
        moveFlash(session, request, "loyaltyMessage");
        moveFlash(session, request, "loyaltyError");
        moveFlash(session, request, "loyaltyRulesDraft");
        moveFlash(session, request, "loyaltyFieldErrors");

        try {
            ShopSettings settings = settingDAO.getSettings();
            long totalPoints = loyaltyDAO.getTotalPoints();
            // ST03: . quy đổi tổng điểm thành tiền giảm ước tính.
            // .. Kết quả là tiền giảm ước tính, chỉ tính những mốc đổi trọn vẹn.
            // ... totalPointValue → ô thống kê [ST07].
            long redeemableSteps = settings.getPointsStep() > 0 ? totalPoints / settings.getPointsStep() : 0;
            long totalPointValue = Math.round(redeemableSteps * settings.getPointsStepValue());

            request.setAttribute("settings", settings);
            request.setAttribute("customerCountWithPoints", loyaltyDAO.countCustomersWithPoints());
            request.setAttribute("totalPoints", totalPoints);
            request.setAttribute("totalPointValue", totalPointValue);
            request.setAttribute("transactionCount", loyaltyDAO.countTransactions());
            // HI01: . Nạp 50 giao dịch điểm gần nhất khi mở trang hoặc sau redirect.
            // .. Chỉ đọc lịch sử, không tự tạo giao dịch tích/đổi mới.
            // ... findRecent(50) [HI02].
            request.setAttribute("transactions", loyaltyDAO.findRecent(50));

            // CU04: [SLBF12] Find customer
            // . Đọc phone, chuẩn hóa dấu phân cách và kiểm tra đủ 10 số bắt đầu bằng 0.
            // .. Có tham số nhưng trống/sai thì báo lỗi; không có tham số khi mở trang mới lấy khách mặc định.
            // ... Hợp lệ gọi findCustomerByPhone() [CU05]; không có tham số gọi [CU06]; sai định dạng tới [CU13].
            String rawPhone = request.getParameter("phone");
            String phone = rawPhone != null && rawPhone.length() > 64
                    ? rawPhone.substring(0, 64) : AccountValidation.phone(rawPhone);
            boolean invalidPhone = rawPhone != null && (rawPhone.length() > 64
                    || AccountValidation.phoneError(phone) != null);
            // LP17: . Đọc lại khách sau khi redirect để lấy số dư hiện tại trong database.
            // .. Dùng cùng nhánh tìm khách khi tra cứu và sau điều chỉnh, không cộng/trừ thêm điểm.
            // ... findCustomerByPhone() [CU05] trả khách; đọc thêm hoạt động [CU07] rồi hiển thị [CU10].
            User customer = invalidPhone ? null : rawPhone == null
                    ? loyaltyDAO.findDefaultCustomer() : loyaltyDAO.findCustomerByPhone(phone);
            request.setAttribute("searchPhone", phone.isEmpty() && customer != null ? customer.getPhone() : phone);
            request.setAttribute("activeCustomer", customer);
            if (customer != null) {
                LoyaltyTransactionDAO.CustomerActivity activity = loyaltyDAO.getCustomerActivity(customer.getId());
                request.setAttribute("customerActivity", activity);
                // CU08: . Quy đổi số dư điểm của khách thành tiền theo các mốc trọn vẹn.
                // .. Đây là số hiển thị, không đổi điểm thật và không ghi giao dịch.
                // ... Gắn customerPointValue vào request, forward tại [CU09].
                long customerSteps = settings.getPointsStep() > 0
                        ? customer.getTotalPoints() / settings.getPointsStep() : 0;
                request.setAttribute("customerPointValue",
                        Math.round(customerSteps * settings.getPointsStepValue()));
            } else if (invalidPhone) {
                // CU13: . SĐT rỗng, quá dài hoặc không đúng định dạng thì báo lỗi và không tìm khách.
                // ... lookupError hiển thị dưới form tra cứu [CU01] sau forward [CU09].
                request.setAttribute("lookupError", "Số điện thoại sai định dạng.");
            } else if (!phone.isEmpty()) {
                // CU12: . SĐT đúng định dạng nhưng không có tài khoản CUSTOMER thì báo không tìm thấy.
                // .. Không tự đổi sang khách khác và không sửa dữ liệu.
                // ... lookupError được chuyển qua [CU09] để hiển thị tại [CU01].
                request.setAttribute("lookupError", "Không tìm thấy khách hàng có số điện thoại " + phone + ".");
            }
            // CU09: . Forward dữ liệu đã nạp sang admin-loyalty.jsp, không redirect khi chỉ tra cứu.
            // ... Thẻ khách [CU10] → số dư [LP18]; không có khách hiển thị lookupError.
            request.getRequestDispatcher("/admin-loyalty.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Failed to load admin loyalty", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được dữ liệu tích điểm. Hãy kiểm tra cơ sở dữ liệu.");
        }
    }

    @Override
    // RU09: . POST /admin/loyalty dùng chung cho nút Lưu quy tắc và Cộng/Trừ điểm.
    // ... Kiểm tra quyền và CSRF [RU10], token một lần [RU11], rồi chọn nhánh tại [RU12].
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!ScaffoldAccess.requireAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

        // RU10: . Chỉ xử lý POST sau khi xác nhận Admin; kiểm tra csrfToken của form.
        // .. Token sai thì redirect kèm lỗi, chưa lưu quy tắc hay điểm.
        // ... Token đúng gọi claimOperation() [RU11] để chặn gửi lặp.
        if (!isValidCsrf(session, request.getParameter("csrfToken"))) {
            session.setAttribute("loyaltyError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/admin/loyalty");
            return;
        }
        if (!claimOperation(session, trim(request.getParameter("operationToken")))) {
            session.setAttribute("loyaltyError", "Yêu cầu đã được xử lý hoặc không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/admin/loyalty");
            return;
        }

        String action = trim(request.getParameter("action"));
        // RU12: . Đọc action từ form sau khi kiểm tra quyền, CSRF và token chống lặp [RU10].
        // .. updateRules sửa quy tắc; adjust cộng/trừ điểm; giá trị khác bị từ chối.
        // ... action=updateRules → [RU13]; action=adjust → [LP04].
        String phone = AccountValidation.phone(request.getParameter("phone"));
        if (AccountValidation.phoneError(phone) != null) phone = "";
        session.removeAttribute("loyaltyMessage");
        session.removeAttribute("loyaltyError");
        session.removeAttribute("loyaltyFieldErrors");
        session.removeAttribute("loyaltyRulesDraft");
        try {
            if ("updateRules".equals(action)) {
                // RU13: . action=updateRules chạy vào nhánh lưu quy tắc.
                // .. Chọn nhánh chưa làm thay đổi shop_settings.
                // ... updateRules(request) [RU14] kiểm tra ba trường trước khi lưu.
                updateRules(request);
                // RU23: . Lưu ba quy tắc thành công mới đặt loyaltyMessage.
                // ... Redirect chung [LP16] → doGet() [RU02] → thông báo [MS03].
                session.setAttribute("loyaltyMessage", "Đã lưu quy tắc tích và đổi điểm.");
            } else if ("adjust".equals(action)) {
                // LP04: . action=adjust chọn nhánh điều chỉnh điểm.
                // .. Bước chọn nhánh chưa sửa số dư; chỉ báo thành công sau khi DAO đã commit.
                // ... adjustPoints(request) [LP05] kiểm tra dữ liệu.
                adjustPoints(request);
                session.setAttribute("loyaltyMessage", "Đã cập nhật điểm khách hàng và ghi lịch sử giao dịch.");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            if ("updateRules".equals(action)) preserveRulesDraft(request, session);
            session.setAttribute("loyaltyError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Admin loyalty update failed", e);
            session.setAttribute("loyaltyError", "Không thể lưu thay đổi điểm. Vui lòng thử lại.");
        }

        String redirect = request.getContextPath() + "/admin/loyalty";
        if (!phone.isEmpty()) redirect += "?phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8);
        // LP16: . Sau xử lý POST, redirect về trang tích điểm và giữ SĐT hợp lệ trên URL.
        // .. Bấm F5 chỉ tải GET, không lặp cộng/trừ hay lưu quy tắc.
        // ... doGet() đọc lại quy tắc [RU02], số dư [LP17] và nhật ký [HI01].
        response.sendRedirect(redirect);
    }

    private void updateRules(HttpServletRequest request) {
        // RU14: [SLBF12] Check rule values
        // . Tạo danh sách lỗi cho ba trường; kiểm tra bằng số long trước khi chuyển kiểu.
        // .. Có lỗi thì chưa gọi DAO; hợp lệ mới lưu đúng ba thiết lập điểm.
        // ... Đọc mức tích [RU15] → mốc đổi [RU16] → tiền giảm [RU17]; quyết định lưu tại [RU21].
        Map<String, String> errors = new LinkedHashMap<>();
        // RU15: . Đọc pointsEarnUnit từ ô [RU05], kiểm tra từ 1 đến 100.000.000.
        // ... ruleValue() [RU18] dùng bộ kiểm tra chung; đọc tiếp mốc đổi tại [RU16].
        long earnUnit = ruleValue(request, "pointsEarnUnit", "Số tiền chi tiêu để nhận 1 điểm", 1,
                LoyaltyValidation.MAX_RULE_MONEY, errors);
        // RU16: . Đọc pointsStep từ ô [RU06], kiểm tra từ 1 đến 1.000.000.
        // .. Chỉ chuyển sang int sau khi đã xác nhận nằm trong giới hạn.
        // ... ruleValue() [RU18]; đọc tiếp tiền giảm tại [RU17].
        long pointsStep = ruleValue(request, "pointsStep", "Số điểm cho mỗi lần đổi", 1,
                LoyaltyValidation.MAX_POINTS, errors);
        // RU17: . Đọc pointsStepValue từ ô [RU07], kiểm tra từ 0 đến 100.000.000.
        // ... ruleValue() [RU18]; tập hợp các lỗi rồi xử lý tại [RU21].
        long stepValue = ruleValue(request, "pointsStepValue", "Số tiền giảm cho mỗi lần đổi", 0,
                LoyaltyValidation.MAX_RULE_MONEY, errors);
        // RU21: . Kiểm tra danh sách lỗi sau khi đã xét cả ba trường.
        // .. Có lỗi: lưu loyaltyFieldErrors rồi dừng; không có lỗi: được chuyển mốc điểm sang int.
        // ... Hợp lệ gọi ShopSettingDAO.saveLoyaltyRules() [RU22]; lỗi được giữ bản nháp tại [RU24].
        if (!errors.isEmpty()) {
            request.getSession().setAttribute("loyaltyFieldErrors", errors);
            throw new IllegalArgumentException("Vui lòng kiểm tra các trường quy tắc được đánh dấu.");
        }
        if (!settingDAO.saveLoyaltyRules(earnUnit, Math.toIntExact(pointsStep), stepValue)) {
            throw new IllegalArgumentException("Không thể lưu quy tắc vào shop_settings.");
        }
    }

    private void adjustPoints(HttpServletRequest request) throws SQLException {
        // LP05: [SLBF12] Check adjustment information
        // . Kiểm tra id khách, điểm nguyên và loại cộng/trừ trên server.
        // .. Cộng giữ dấu dương; trừ chuyển thành âm sau khi đã kiểm tra biên.
        // ... Kiểm tra customerId [LP06], points [LP07], adjustType [LP08] rồi gọi DAO [LP09].
        // LP06: . Đọc customerId ẩn từ [LP03], yêu cầu từ 1 đến Integer.MAX_VALUE.
        // .. Kiểm tra long trước khi chuyển int để số âm lớn không bị cuộn thành id dương.
        // ... LoyaltyValidation.wholeNumber() [RU19], rồi xét số điểm [LP07].
        int customerId = Math.toIntExact(LoyaltyValidation.wholeNumber(request.getParameter("customerId"),
                "Khách hàng", 1, Integer.MAX_VALUE));
        // LP07: . Đọc points từ [LP03], yêu cầu số nguyên từ 1 đến 1.000.000.
        // .. Không tin kết quả kiểm tra JavaScript; server tự chặn chữ, số âm/lẻ và tràn số.
        // ... LoyaltyValidation.wholeNumber() [RU19], rồi kiểm tra loại điều chỉnh [LP08].
        int points = Math.toIntExact(LoyaltyValidation.wholeNumber(request.getParameter("points"),
                "Số điểm điều chỉnh", 1, LoyaltyValidation.MAX_POINTS));
        // LP08: . Chỉ nhận adjustType=add hoặc subtract; loại khác bị từ chối.
        // .. subtract mới đổi points sang âm, add giữ dương.
        // ... LoyaltyTransactionDAO.adjustPoints(customerId, điểm có dấu) [LP09].
        String adjustType = trim(request.getParameter("adjustType"));
        if (!"add".equals(adjustType) && !"subtract".equals(adjustType)) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }
        if (!loyaltyDAO.adjustPoints(customerId, "subtract".equals(adjustType) ? -points : points)) {
            throw new IllegalArgumentException("Không thể điều chỉnh điểm khách hàng.");
        }
    }

    // RU18: . Kiểm tra một ô theo tên trường và giới hạn được truyền vào.
    // .. Sai thì gắn lỗi vào đúng field; tiếp tục kiểm tra các ô còn lại để báo đủ lỗi.
    // ... LoyaltyValidation.wholeNumber() [RU19] trả giá trị hoặc lỗi cho [RU14].
    private long ruleValue(HttpServletRequest request, String field, String label,
                           long min, long max, Map<String, String> errors) {
        try {
            return LoyaltyValidation.wholeNumber(request.getParameter(field), label, min, max);
        } catch (IllegalArgumentException e) {
            errors.put(field, e.getMessage());
            return 0;
        }
    }

    // RU24: . Giữ ba giá trị đã nhập khi lưu quy tắc lỗi; tối đa 128 ký tự mỗi ô.
    // .. Chưa ghi dữ liệu sai vào shop_settings.
    // ... Redirect [LP16] → moveFlash() [MS01] → form [RU04] điền lại bản nháp.
    private void preserveRulesDraft(HttpServletRequest request, HttpSession session) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : new String[]{"pointsEarnUnit", "pointsStep", "pointsStepValue"}) {
            String value = trim(request.getParameter(field));
            values.put(field, value.length() > 128 ? value.substring(0, 128) : value);
        }
        session.setAttribute("loyaltyRulesDraft", values);
    }

    // RU11: . Kiểm tra và đổi operationToken bên trong cùng synchronized(session).
    // .. Cùng một token chỉ một POST được xử lý, kể cả khi gửi đồng thời; lỗi dữ liệu cần tải form với token mới.
    // ... Token đúng quay về doPost() chọn action [RU12]; sai trả thông báo [MS04].
    private boolean claimOperation(HttpSession session, String submitted) {
        synchronized (session) {
            Object expected = session.getAttribute("loyaltyOperationToken");
            if (expected == null || !expected.equals(submitted)) return false;
            session.setAttribute("loyaltyOperationToken", UUID.randomUUID().toString());
            return true;
        }
    }

    private boolean isValidCsrf(HttpSession session, String submitted) {
        Object expected = session.getAttribute("loyaltyCsrfToken");
        return expected != null && expected.equals(submitted);
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        // MS01: . Chuyển thông báo, lỗi từng ô và bản nháp từ session sang request rồi xóa khỏi session.
        // .. Dữ liệu chỉ được dùng cho lần GET sau redirect; không ghi database.
        // ... JSP hiển thị thông báo [MS02], thành công [MS03], lỗi [MS04], và điền lại quy tắc [RU04].
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
