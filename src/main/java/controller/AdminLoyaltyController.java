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
    // . Khi mở Quản lý tích điểm, doGet trong AdminLoyaltyController.java chuẩn bị dữ liệu cho trang. Em gọi
    //   ShopSettingDAO.getSettingsStrict (đọc quy tắc, báo lỗi nếu không đọc được) và các hàm trong
    //   LoyaltyTransactionDAO.java để lấy khách, thống kê, lịch sử, rồi đưa về admin-loyalty.jsp.
    // ... Trong AdminLoyaltyController.java, tiếp tục thân doGet ngay dưới comment: đọc quy tắc, khách và lịch sử
    //     rồi forward admin-loyalty.jsp; trang hiện xong các phần này là kết thúc mở màn hình.
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
            // . Để điền ba ô quy tắc và tính tiền quy đổi, ở doGet trong AdminLoyaltyController.java em gọi
            //   getSettingsStrict (đọc các thiết lập đang lưu) trong ShopSettingDAO.java. Em nhận bộ quy tắc vào settings
            //   rồi đưa sang admin-loyalty.jsp.
            // ... Đọc tiếp tại ShopSettingDAO.java, tìm [TDQT07].

            ShopSettings settings = settingDAO.getSettingsStrict();
            long totalPoints = loyaltyDAO.getTotalPoints();

            // [TDTS01]
            // . Để tính các thẻ thống kê trong doGet của AdminLoyaltyController.java, em đọc số khách có điểm, tổng
            //   điểm và số giao dịch từ LoyaltyTransactionDAO. Với tiền quy đổi, em lấy tổng điểm chia lấy phần
            //   nguyên cho mốc đổi rồi nhân tiền giảm mỗi lần đổi, sau đó đưa các số về trang.
            // ... Đọc tiếp tại LoyaltyTransactionDAO.java, tìm [TDTS02].
            // [SQL12-13] Thẻ Giá trị quy đổi: Java lấy tổng điểm chia nguyên cho mốc đổi rồi nhân tiền giảm.
            // . Nguồn là getTotalPoints và getSettingsStrict; Math.round làm tròn tiền trước khi hiển thị.
            long redeemableSteps = settings.getPointsStep() > 0 ? totalPoints / settings.getPointsStep() : 0;
            long totalPointValue = Math.round(redeemableSteps * settings.getPointsStepValue());

            request.setAttribute("settings", settings);
            request.setAttribute("customerCountWithPoints", loyaltyDAO.countCustomersWithPoints());
            request.setAttribute("totalPoints", totalPoints);
            request.setAttribute("totalPointValue", totalPointValue);
            request.setAttribute("transactionCount", loyaltyDAO.countTransactions());

            request.setAttribute("transactions", loyaltyDAO.findRecent(50));

            // [TDTK02]
            // . Khi nhận phone trong doGet của AdminLoyaltyController.java, em chuẩn hóa số và kiểm tra định dạng.
            //   Số hợp lệ mới được đưa vào findCustomerByPhone trong LoyaltyTransactionDAO.java để tìm khách; số sai
            //   thì báo lỗi và không dùng khách mặc định thay thế.
            // .. Chỉ khi chưa gửi số để tra cứu thì em mới lấy khách mặc định; số nhập sai hoặc không tìm được sẽ
            //    được báo lỗi.
            // ... Đọc tiếp tại LoyaltyTransactionDAO.java, tìm [TDTK03].
            String rawPhone = request.getParameter("phone");
            // [TDSDT02]
            // . Ở doGet, em nhận phone từ ô Tra cứu. Đầu vào dài quá 64 ký tự bị coi là sai; số bình thường được
            //   AccountValidation.phone (chuẩn hóa số điện thoại) chuẩn hóa và phoneError (kiểm tra định dạng số
            //   điện thoại) kiểm tra 10 chữ số bắt đầu bằng 0. Nếu sai thì không gọi tìm khách; nếu đúng thì dùng
            //   findCustomerByPhone (tìm khách theo số điện thoại), còn không gửi phone mới dùng findDefaultCustomer
            //   (chọn khách ban đầu khi chưa tra cứu). Kết quả và lookupError được đưa về admin-loyalty.jsp.
            // ... Trong AdminLoyaltyController.java, đọc tiếp nhánh chọn activeCustomer trong doGet; kết quả tra
            //     cứu và lookupError được đưa tới admin-loyalty.jsp để kết thúc tra cứu.
            String phone = rawPhone != null && rawPhone.length() > 64
                    ? rawPhone.substring(0, 64) : AccountValidation.phone(rawPhone);
            boolean invalidPhone = rawPhone != null && (rawPhone.length() > 64
                    || AccountValidation.phoneError(phone) != null);

            User customer = invalidPhone ? null : rawPhone == null
                    ? loyaltyDAO.findDefaultCustomer() : loyaltyDAO.findCustomerByPhone(phone);
            request.setAttribute("searchPhone", phone.isEmpty() && customer != null ? customer.getPhone() : phone);
            // [TDTK04]
            // . Khi tìm được khách, em quay lại doGet trong AdminLoyaltyController.java và đặt tài khoản đó vào
            //   activeCustomer. Em đưa dữ liệu sang admin-loyalty.jsp để thông tin và các nút Cộng, Trừ cùng dùng mã
            //   của đúng khách đang xem.
            // ... Đọc tiếp tại admin-loyalty.jsp, tìm [TDTK05].

            request.setAttribute("activeCustomer", customer);
            if (customer != null) {
                LoyaltyTransactionDAO.CustomerActivity activity = loyaltyDAO.getCustomerActivity(customer.getId());
                // [TDCT02]
                // . Ở doGet trong AdminLoyaltyController.java, em gọi LoyaltyTransactionDAO.getCustomerActivity (đọc
                //   tổng tiền mua và ngày có đơn gần nhất) bằng mã khách đang xem. Em đặt kết quả vào customerActivity
                //   rồi đưa về admin-loyalty.jsp để hiện tổng tiền mua và ngày có đơn gần nhất.
                // ... Đọc tiếp tại admin-loyalty.jsp, tìm [TDCT03].

                request.setAttribute("customerActivity", activity);

                // [TDSD01]
                // . Để hiện tiền có thể đổi của khách, ở doGet trong AdminLoyaltyController.java em lấy số điểm chia lấy
                //   phần nguyên cho mốc đổi, rồi nhân tiền giảm mỗi lần đổi. Em đưa kết quả customerPointValue về
                //   admin-loyalty.jsp, đặt cạnh số dư điểm để đối chiếu.
                // ... Đọc tiếp tại admin-loyalty.jsp, tìm [TDSD02].
                // [SQL12-14] Tiền có thể đổi của một khách: Java dùng số điểm riêng của khách với bộ quy tắc
                //   hiện tại.
                // . Không lấy giá trị quy đổi của toàn hệ thống để hiển thị cho từng khách.
                long customerSteps = settings.getPointsStep() > 0
                        ? customer.getTotalPoints() / settings.getPointsStep() : 0;
                request.setAttribute("customerPointValue",
                        Math.round(customerSteps * settings.getPointsStepValue()));
            } else if (invalidPhone) {

                request.setAttribute("lookupError", AccountValidation.phoneError(phone));
            } else if (!phone.isEmpty()) {

                request.setAttribute("lookupError", "Không tìm thấy khách hàng có số điện thoại " + phone + ".");
            }

            // [TDQT08]
            // . Khi doGet trong AdminLoyaltyController.java đã chuẩn bị xong quy tắc, thông tin khách, lịch sử và
            //   thông báo, em chuyển dữ liệu sang admin-loyalty.jsp bằng forward để trang dựng lại màn hình với kết
            //   quả hiện tại.
            // ... Đọc tiếp tại admin-loyalty.jsp, tìm [TDQT09].
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

    // [TDQT02]
    // . Khi nhận form tại doPost trong AdminLoyaltyController.java, em đọc action=updateRules rồi gọi
    //   updateRules ngay trong controller để kiểm tra và lưu quy tắc. Hàm nhận dữ liệu form, đọc ba giá trị
    //   và kiểm tra từng ô trước khi ghi bộ mới.
    // ... Trong AdminLoyaltyController.java, theo lời gọi updateRules(request) tới định nghĩa private void
    //     updateRules bên dưới, rồi theo ruleValue để kiểm tra từng ô trước khi ghi.
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

        // . Em đọc action mà form gửi tới doPost trong AdminLoyaltyController.java để chọn xử lý đúng nút vừa
        //   bấm: updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) để lưu quy tắc hoặc adjustPoints (kiểm tra
        //   yêu cầu cộng/trừ điểm) để cộng, trừ điểm.
        String action = trim(request.getParameter("action"));

        String phone = AccountValidation.phone(request.getParameter("phone"));
        if (AccountValidation.phoneError(phone) != null) phone = "";
        session.removeAttribute("loyaltyMessage");
        session.removeAttribute("loyaltyError");
        session.removeAttribute("loyaltyFieldErrors");
        session.removeAttribute("loyaltyRulesDraft");
        try {
            if ("updateRules".equals(action)) {
                // . Với action=updateRules, em gọi hàm updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) ngay trong
                //   AdminLoyaltyController.java và truyền request để hàm nhận ba ô của form Lưu quy tắc, kiểm tra rồi
                //   lưu.
                updateRules(request);

                // [TDQT06]
                // . Khi saveLoyaltyRules lưu được cả bộ, chương trình quay qua updateRules về doPost trong
                //   AdminLoyaltyController.java. Em đặt thông báo thành công rồi mở lại /admin/loyalty để đọc và hiển
                //   thị bộ quy tắc vừa lưu.
                // ... Trong AdminLoyaltyController.java, đọc cuối doPost tại sendRedirect rồi quay tới doGet của
                //     cùng đường dẫn; doGet đọc bộ mới bằng getSettingsStrict và mở admin-loyalty.jsp để hiện kết
                //     quả.

                session.setAttribute("loyaltyMessage", "Đã lưu quy tắc tích và đổi điểm.");
            } else if ("adjust".equals(action)) {
                // . Với action=adjust, em gọi hàm adjustPoints (kiểm tra yêu cầu cộng/trừ điểm) ngay trong
                //   AdminLoyaltyController.java và truyền request để nhận mã khách, số điểm cùng lựa chọn cộng hoặc trừ.
                adjustPoints(request);
                // [TDDC06]
                // . Khi cập nhật điểm và ghi lịch sử thành công, chương trình quay về doPost trong
                //   AdminLoyaltyController.java. Em đặt thông báo rồi mở lại trang để doGet đọc số dư và lịch sử mới,
                //   đưa về admin-loyalty.jsp hiển thị kết quả vừa cộng hoặc trừ.
                // ... Xem kết quả tại admin-loyalty.jsp, vùng loyaltyMessage/loyaltyError, khối activeCustomer và
                //     bảng transactions; kết thúc điều chỉnh khi thông báo, số dư và lịch sử mới đã hiện.
                session.setAttribute("loyaltyMessage", "Đã cập nhật điểm khách hàng và ghi lịch sử giao dịch.");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            // . Nếu updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) báo dữ liệu chưa đúng, em bắt lỗi ở doPost
            //   rồi gọi preserveRulesDraft (giữ ba ô quy tắc khi lưu lỗi) để giữ ba giá trị đang nhập. Em đặt
            //   loyaltyError để khi trang mở lại, người dùng thấy lỗi và sửa tiếp trên form cũ.
            if ("updateRules".equals(action)) preserveRulesDraft(request, session);
            session.setAttribute("loyaltyError", e.getMessage());
        } catch (SQLException e) {
            // . Nếu gặp lỗi khi ghi dữ liệu, em bắt lỗi ở doPost trong AdminLoyaltyController.java và đặt
            //   loyaltyError. Trang sẽ báo chưa lưu được để người dùng biết thao tác chưa hoàn tất.
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

        // . Sau khi xử lý, em dùng sendRedirect (chuyển sang đường dẫn khác) trong doPost của
        //   AdminLoyaltyController.java để mở lại /admin/loyalty, giữ số điện thoại nếu hợp lệ. doGet đọc lại
        //   quy tắc, số dư và lịch sử rồi đưa dữ liệu mới về admin-loyalty.jsp.
        response.sendRedirect(redirect);
    }

    // . Ở updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) trong AdminLoyaltyController.java, em gọi
    //   ruleValue (đọc và kiểm tra một ô quy tắc) cho từng ô, truyền tên ô cùng giới hạn riêng để kiểm tra.
    //   Em chỉ gọi ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu) khi cả ba giá trị
    //   đều đúng.
    private void updateRules(HttpServletRequest request) throws SQLException {

        Map<String, String> errors = new LinkedHashMap<>();

        // [TDTICH02]
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em đưa
        //   giá trị pointsEarnUnit vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra tiền chi tiêu nhận
        //   một điểm. Em truyền cả tên ô và giới hạn từ 1 đến 100.000.000; số hợp lệ được trả về để lưu cùng hai
        //   quy tắc còn lại.
        // ... Trong AdminLoyaltyController.java, theo ruleValue tới LoyaltyValidation.wholeNumber; kết quả quay về
        //     earnUnit trong updateRules rồi được xét cùng hai ô ở đoạn kiểm tra errors.
        long earnUnit = ruleValue(request, "pointsEarnUnit", "Số tiền chi tiêu để nhận 1 điểm", 1,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        // [TDMOC02]
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em đưa
        //   pointsStep vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra số điểm mỗi lần đổi. Em truyền
        //   tên ô và giới hạn từ 1 đến 1.000.000 để nếu sai thì báo đúng ô, còn đúng thì nhận số để tiếp tục
        //   lưu.
        // .. Mốc đổi không được bằng 0 vì đây là số dùng để chia khi tính số lần đổi điểm.
        // ... Trong AdminLoyaltyController.java, theo ruleValue tới LoyaltyValidation.wholeNumber; kết quả quay về
        //     pointsStep trong updateRules rồi được xét cùng hai ô ở đoạn kiểm tra errors.
        long pointsStep = ruleValue(request, "pointsStep", "Số điểm cho mỗi lần đổi", 1,
                LoyaltyValidation.MAX_POINTS, errors);

        // [TDGT02]
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em đưa
        //   pointsStepValue vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra tiền giảm mỗi lần đổi.
        //   Giới hạn là từ 0 đến 100.000.000, nên giá trị 0 vẫn hợp lệ. Kết quả được dùng cùng hai ô còn lại để
        //   lưu quy tắc.
        // ... Trong AdminLoyaltyController.java, theo ruleValue tới LoyaltyValidation.wholeNumber; kết quả quay về
        //     stepValue trong updateRules rồi được xét cùng hai ô ở đoạn kiểm tra errors.
        long stepValue = ruleValue(request, "pointsStepValue", "Số tiền giảm cho mỗi lần đổi", 0,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        // [TDQT04]
        // . Khi ruleValue kiểm tra xong ba ô, em quay lại updateRules và xem errors. Nếu có lỗi thì giữ lời báo
        //   theo từng ô để người dùng sửa; nếu đúng hết thì gọi ShopSettingDAO.saveLoyaltyRules, truyền ba số đã
        //   kiểm tra để ghi cả bộ quy tắc.
        // ... Trong AdminLoyaltyController.java, đọc tiếp nhánh errors và lời gọi settingDAO.saveLoyaltyRules ngay
        //     bên dưới; còn lỗi thì doPost giữ form, đúng hết mới gọi hàm ghi trong ShopSettingDAO.java.
        if (!errors.isEmpty()) {
            request.getSession().setAttribute("loyaltyFieldErrors", errors);
            throw new IllegalArgumentException("Vui lòng kiểm tra các trường quy tắc được đánh dấu.");
        }
        // . Khi ba ô đều hợp lệ, em gọi saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu) trong
        //   ShopSettingDAO.java, truyền tiền tích điểm, mốc đổi và tiền giảm. Em nhận true hoặc false để biết có
        //   ghi được cả bộ quy tắc không trước khi báo kết quả.
        // ... Đọc tiếp tại ShopSettingDAO.java, tìm [TDQT05].
        if (!settingDAO.saveLoyaltyRules(earnUnit, Math.toIntExact(pointsStep), stepValue)) {
            throw new SQLException("Could not save loyalty rules");
        }
    }

    // [TDDC02]
    // . Khi doPost nhận action=adjust, em gọi adjustPoints trong AdminLoyaltyController.java để kiểm tra yêu
    //   cầu cộng/trừ điểm. Em nhận mã khách, số điểm và loại thao tác; hợp lệ mới gọi adjustPoints trong
    //   LoyaltyTransactionDAO.java, truyền điểm dương để cộng hoặc âm để trừ.
    // .. Mã khách phải dương và số điểm nhập phải là số nguyên từ 1 đến 1.000.000 trước khi chuyển sang bước
    //    cập nhật.
    // ... Đọc tiếp tại LoyaltyTransactionDAO.java, tìm [TDDC03].
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
        // . Trước khi cập nhật điểm, trong adjustPoints (kiểm tra yêu cầu cộng/trừ điểm) của
        //   AdminLoyaltyController.java em chỉ nhận adjustType là add hoặc subtract. Giá trị này quyết định cộng
        //   hay trừ; loại khác bị báo lỗi để tránh thực hiện sai ý người dùng.
        if (!"add".equals(adjustType) && !"subtract".equals(adjustType)) {
            throw new IllegalArgumentException("Loại điều chỉnh không hợp lệ.");
        }
        // . Sau khi kiểm tra, em gọi adjustPoints (cập nhật điểm và ghi lịch sử) trong
        //   LoyaltyTransactionDAO.java để cập nhật điểm và ghi lịch sử. Em truyền customerId cùng điểm dương nếu
        //   cộng, điểm âm nếu trừ, để hàm tính số dư cho đúng khách và đúng hướng.
        if (!loyaltyDAO.adjustPoints(customerId, "subtract".equals(adjustType) ? -points : points)) {
            throw new IllegalArgumentException("Không thể điều chỉnh điểm khách hàng.");
        }
    }

    // . Ở ruleValue (đọc và kiểm tra một ô quy tắc) trong AdminLoyaltyController.java, em nhận tên ô và giới
    //   hạn từ updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) rồi gọi LoyaltyValidation.wholeNumber
    //   (kiểm tra số nguyên trong giới hạn) để kiểm tra nội dung. Số đúng được trả về; lỗi được ghi vào
    //   errors theo tên ô để hiện đúng vị trí.
    private long ruleValue(HttpServletRequest request, String field, String label,
                           long min, long max, Map<String, String> errors) {
        try {
            // . Em gọi wholeNumber (kiểm tra số nguyên trong giới hạn) trong LoyaltyValidation.java, truyền nội dung
            //   ô, tên hiển thị và hai giới hạn để kiểm tra số nguyên. Hàm trả số về ruleValue (đọc và kiểm tra một
            //   ô quy tắc); nếu báo lỗi thì em bắt tại đây để gắn lời báo vào đúng ô đang nhập.
            // ... Đọc tiếp tại LoyaltyValidation.java, tìm [TDQT03].
            return LoyaltyValidation.wholeNumber(request.getParameter(field), label, min, max);
        } catch (IllegalArgumentException e) {
            errors.put(field, e.getMessage());
            return 0;
        }
    }

    // . Ở preserveRulesDraft (giữ ba ô quy tắc khi lưu lỗi) trong AdminLoyaltyController.java, em nhận dữ
    //   liệu form từ doPost sau lỗi rồi giữ ba ô vào loyaltyRulesDraft. Khi doGet mở lại trang, các ô được
    //   điền lại để người dùng sửa tiếp.
    private void preserveRulesDraft(HttpServletRequest request, HttpSession session) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : new String[]{"pointsEarnUnit", "pointsStep", "pointsStepValue"}) {
            String value = trim(request.getParameter(field));
            values.put(field, value.length() > 128 ? value.substring(0, 128) : value);
        }
        session.setAttribute("loyaltyRulesDraft", values);
    }

    // [TDVE01]
    // . Để một lần bấm không bị xử lý lặp, doPost trong AdminLoyaltyController.java gọi claimOperation (chặn
    //   xử lý lặp cùng một lần gửi). Em truyền mã đi kèm form để đối chiếu với lần gửi đang giữ; nếu mã đã
    //   dùng thì dừng, tránh cộng hoặc trừ điểm hai lần.
    // ... Quay lại AdminLoyaltyController.java, trong doPost sau claimOperation; được nhận thì xử lý action, mã đã
    //     dùng hoặc sai thì mở lại trang cùng thông báo và kết thúc yêu cầu đó.
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

    // . Ở moveFlash (đưa thông báo sang trang rồi bỏ bản đã dùng) trong AdminLoyaltyController.java, em nhận
    //   tên dữ liệu cần chuyển từ doGet. Em đưa thông báo hoặc bản nhập dở từ session sang request rồi xóa
    //   bản đã dùng, để admin-loyalty.jsp chỉ hiện kết quả của lần xử lý này.
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
