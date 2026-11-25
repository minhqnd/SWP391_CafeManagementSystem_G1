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
    // [TDM02]
    // . Hàm doGet trong AdminLoyaltyController chuẩn bị màn hình tích điểm. Em đọc quy tắc, số liệu tổng và
    //   lịch sử từ các class làm việc với dữ liệu. Nếu có số điện thoại thì em tìm khách theo số đó; lúc mở
    //   lần đầu em chọn khách mặc định, rồi đưa thông tin về trang.
    //
    // Tra tiếp: admin-loyalty.jsp / TDM03.
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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
        try {

            ShopSettings settings = settingDAO.getSettingsStrict();
            long totalPoints = loyaltyDAO.getTotalPoints();

            // [SQL12-13] Thẻ Giá trị quy đổi: Java lấy tổng điểm chia nguyên cho mốc đổi rồi nhân tiền giảm.

            long redeemableSteps = settings.getPointsStep() > 0 ? totalPoints / settings.getPointsStep() : 0;
            long totalPointValue = Math.round(redeemableSteps * settings.getPointsStepValue());

            request.setAttribute("settings", settings);
            request.setAttribute("customerCountWithPoints", loyaltyDAO.countCustomersWithPoints());
            request.setAttribute("totalPoints", totalPoints);
            request.setAttribute("totalPointValue", totalPointValue);
            request.setAttribute("transactionCount", loyaltyDAO.countTransactions());

            request.setAttribute("transactions", loyaltyDAO.findRecent(50));

            // [TDTK03]
            // . Trong doGet của AdminLoyaltyController, em nhận và kiểm tra lại số điện thoại; sai thì báo rõ
            //   để người dùng sửa. Số đúng thì em gọi findCustomerByPhone trong LoyaltyTransactionDAO để đọc
            //   khách có số điện thoại này.
            //
            // Tra tiếp: LoyaltyTransactionDAO.java / TDTK04.
            String rawPhone = request.getParameter("phone");

            boolean phoneTooLong = rawPhone != null && rawPhone.length() > 64;
            String phone = phoneTooLong
                    ? rawPhone.substring(0, 64) : AccountValidation.phone(rawPhone);
            String phoneError = phoneTooLong ? "Số điện thoại tối đa 64 ký tự." : AccountValidation.phoneError(phone);
            boolean invalidPhone = rawPhone != null && phoneError != null;

            User customer = invalidPhone ? null : rawPhone == null
                    ? loyaltyDAO.findDefaultCustomer() : loyaltyDAO.findCustomerByPhone(phone);
            request.setAttribute("searchPhone", phone.isEmpty() && customer != null ? customer.getPhone() : phone);

            request.setAttribute("activeCustomer", customer);
            if (customer != null) {
                LoyaltyTransactionDAO.CustomerActivity activity = loyaltyDAO.getCustomerActivity(customer.getId());

                request.setAttribute("customerActivity", activity);

                // [SQL12-14] Tiền có thể đổi của một khách: Java dùng số điểm riêng của khách với bộ quy tắc
                //   hiện tại.

                long customerSteps = settings.getPointsStep() > 0
                        ? customer.getTotalPoints() / settings.getPointsStep() : 0;
                request.setAttribute("customerPointValue",
                        Math.round(customerSteps * settings.getPointsStepValue()));
            } else if (invalidPhone) {

                request.setAttribute("lookupError", phoneError);
            } else if (!phone.isEmpty()) {

                request.setAttribute("lookupError", "Không tìm thấy khách hàng có số điện thoại " + phone + ".");
            }

            // [TDQT08]
            // . Ở cuối doGet của AdminLoyaltyController, em đưa quy tắc, khách hàng, lịch sử và thông báo
            //   sang admin-loyalty.jsp. Trang dùng những thông tin này để hiện kết quả của lần thao tác vừa
            //   rồi.
            //
            // Tra tiếp: admin-loyalty.jsp / TDQT09.
            moveFlash(session, request, "loyaltyMessage");
            moveFlash(session, request, "loyaltyError");
            moveFlash(session, request, "loyaltyRulesDraft");
            moveFlash(session, request, "loyaltyFieldErrors");
            request.getRequestDispatcher("/admin-loyalty.jsp").forward(request, response);
        } catch (SQLException | RuntimeException e) {
            getServletContext().log("Failed to load admin loyalty", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được dữ liệu tích điểm lúc này. Vui lòng thử lại sau.");
        }
    }

    @Override

    // [TDQT03]
    // . Khi nhận form từ nút Lưu quy tắc, doPost trong AdminLoyaltyController chuyển việc kiểm tra và lưu
    //   cho hàm updateRules ngay trong class này. Em dùng hàm này để lấy đủ ba giá trị và kiểm tra cả bộ
    //   trước khi ghi vào dữ liệu.
    //
    // Tra tiếp: TDQT04.
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!ScaffoldAccess.requireAdmin(request, response)) return;
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        session.removeAttribute("loyaltyMessage");
        session.removeAttribute("loyaltyError");
        session.removeAttribute("loyaltyFieldErrors");
        session.removeAttribute("loyaltyRulesDraft");

        if (!isValidCsrf(session, request.getParameter("csrfToken"))) {
            session.setAttribute("loyaltyError", "Không xác nhận được yêu cầu. Vui lòng thử lại trên trang vừa tải.");
            response.sendRedirect(request.getContextPath() + "/admin/loyalty");
            return;
        }
        if (!claimOperation(session, trim(request.getParameter("operationToken")))) {
            session.setAttribute("loyaltyError", "Yêu cầu không còn hiệu lực. Vui lòng kiểm tra kết quả trên trang trước khi gửi lại.");
            response.sendRedirect(request.getContextPath() + "/admin/loyalty");
            return;
        }

        String action = trim(request.getParameter("action"));

        String phone = AccountValidation.phone(request.getParameter("phone"));
        if (AccountValidation.phoneError(phone) != null) phone = "";
        session.removeAttribute("loyaltyMessage");
        session.removeAttribute("loyaltyError");
        session.removeAttribute("loyaltyFieldErrors");
        session.removeAttribute("loyaltyRulesDraft");
        try {
            if ("updateRules".equals(action)) {

                updateRules(request);

                // [TDQT06]
                // . Quay lại doPost của AdminLoyaltyController, em đặt thông báo đã lưu rồi mở lại màn hình
                //   tích điểm. Khi trang mở lại, doGet gọi getSettingsStrict trong ShopSettingDAO để đọc bộ
                //   quy tắc hiện tại và đưa lên ba ô cho Admin đối chiếu.
                //
                // Tra tiếp: ShopSettingDAO.java / TDQT07.

                session.setAttribute("loyaltyMessage", "Đã lưu quy tắc tích và đổi điểm.");
            } else if ("adjust".equals(action)) {

                adjustPoints(request);
                // [TDDC05]
                // . Khi số dư và lịch sử đã được lưu, em quay lại doPost trong AdminLoyaltyController để
                //   đặt thông báo thành công rồi mở lại trang. doGet đọc dữ liệu hiện tại và đưa về
                //   admin-loyalty.jsp, để Admin đối chiếu khách, số dư và lần điều chỉnh vừa thực hiện.
                //
                // Tra tiếp: admin-loyalty.jsp / TDTK05.
                session.setAttribute("loyaltyMessage", "Đã cập nhật điểm khách hàng và ghi lịch sử giao dịch.");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {

            if ("updateRules".equals(action)) preserveRulesDraft(request, session);
            session.setAttribute("loyaltyError", e.getMessage());
        } catch (SQLException e) {

            getServletContext().log("Admin loyalty update failed", e);
            if ("updateRules".equals(action)) preserveRulesDraft(request, session);
            session.setAttribute("loyaltyError", "updateRules".equals(action)
                    ? "Không lưu được quy tắc tích và đổi điểm lúc này. Vui lòng thử lại sau."
                    : "Không cập nhật được điểm lúc này. Số dư và lịch sử chưa thay đổi. Vui lòng thử lại sau.");
        } catch (RuntimeException e) {
            getServletContext().log("Admin loyalty operation failed", e);
            if ("updateRules".equals(action)) preserveRulesDraft(request, session);
            session.setAttribute("loyaltyError", "Không hoàn tất được thao tác tích điểm lúc này. Vui lòng tải lại trang rồi thử lại.");
        }

        String redirect = request.getContextPath() + "/admin/loyalty";
        if (!phone.isEmpty()) redirect += "?phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8);

        response.sendRedirect(redirect);
    }

    // [TDQT04]
    // . Trong updateRules, em lấy ba giá trị vừa nhập và kiểm tra từng ô. Nếu có lỗi, em gom lời báo theo
    //   đúng ô để Admin sửa; nếu cả bộ đều đúng, em gọi saveLoyaltyRules trong ShopSettingDAO để lưu ba
    //   quy tắc cùng nhau.
    //
    // Tra tiếp: ShopSettingDAO.java / TDQT05.
    private void updateRules(HttpServletRequest request) throws SQLException {

        Map<String, String> errors = new LinkedHashMap<>();

        long earnUnit = ruleValue(request, "pointsEarnUnit", "Số tiền chi tiêu để nhận 1 điểm", 1,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        long pointsStep = ruleValue(request, "pointsStep", "Số điểm cho mỗi lần đổi", 1,
                LoyaltyValidation.MAX_POINTS, errors);

        long stepValue = ruleValue(request, "pointsStepValue", "Số tiền giảm cho mỗi lần đổi", 0,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        if (!errors.isEmpty()) {
            request.getSession().setAttribute("loyaltyFieldErrors", errors);
            throw new IllegalArgumentException("Vui lòng kiểm tra các trường quy tắc được đánh dấu.");
        }

        if (!settingDAO.saveLoyaltyRules(earnUnit, Math.toIntExact(pointsStep), stepValue)) {
            throw new SQLException("Could not save loyalty rules");
        }
    }

    // [TDDC03]
    // . Trong adjustPoints của AdminLoyaltyController, em kiểm tra khách đang chọn, số điểm và lựa chọn cộng
    //   hay trừ. Hợp lệ thì em gọi adjustPoints trong LoyaltyTransactionDAO, truyền điểm dương để cộng hoặc
    //   điểm âm để trừ vào số dư của khách.
    //
    // Tra tiếp: LoyaltyTransactionDAO.java / TDDC04.
    private void adjustPoints(HttpServletRequest request) throws SQLException {

        int customerId;
        try {
            customerId = Math.toIntExact(LoyaltyValidation.wholeNumber(request.getParameter("customerId"),
                    "Mã khách hàng", 1, Integer.MAX_VALUE));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Không xác định được khách hàng cần điều chỉnh. Vui lòng tìm lại khách hàng.");
        }

        int points = Math.toIntExact(LoyaltyValidation.wholeNumber(request.getParameter("points"),
                "Số điểm điều chỉnh", 1, LoyaltyValidation.MAX_POINTS));

        String adjustType = trim(request.getParameter("adjustType"));

        if (!"add".equals(adjustType) && !"subtract".equals(adjustType)) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }

        if (!loyaltyDAO.adjustPoints(customerId, "subtract".equals(adjustType) ? -points : points)) {
            throw new IllegalArgumentException("Không thể điều chỉnh điểm khách hàng.");
        }
    }

    // Kiểm tra một ô quy tắc và giữ lời báo theo tên ô, để cuối cùng xét đủ cả bộ.
    private long ruleValue(HttpServletRequest request, String field, String label,
                           long min, long max, Map<String, String> errors) {
        try {

            return LoyaltyValidation.wholeNumber(request.getParameter(field), label, min, max);
        } catch (IllegalArgumentException e) {

            errors.put(field, e.getMessage());
            return 0;
        }
    }

    // Giữ ba ô quy tắc đang nhập để người dùng sửa tiếp khi chưa lưu được.
    private void preserveRulesDraft(HttpServletRequest request, HttpSession session) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : new String[]{"pointsEarnUnit", "pointsStep", "pointsStepValue"}) {
            String value = trim(request.getParameter(field));
            values.put(field, value.length() > 128 ? value.substring(0, 128) : value);
        }
        session.setAttribute("loyaltyRulesDraft", values);
    }

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

        Object value = session.getAttribute(key);
        if (value != null) {
            request.setAttribute(key, value);
            session.removeAttribute(key);
        }
    }

    private String trim(String value) {
        return utils.FormInput.trim(value);
    }
}
