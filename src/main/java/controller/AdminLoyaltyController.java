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
    // . Khi mở Quản lý tích điểm, doGet trong AdminLoyaltyController.java chuẩn bị dữ liệu cho trang. Em
    //   gọi ShopSettingDAO.getSettingsStrict (đọc quy tắc, báo lỗi nếu không đọc được) và các hàm trong
    //   LoyaltyTransactionDAO.java để lấy khách, thống kê, lịch sử, rồi đưa về admin-loyalty.jsp.
    // ... doGet lần lượt đọc quy tắc, tìm khách và đọc lịch sử. Muốn theo phần đọc lịch sử, xem
    //     LoyaltyTransactionDAO.java, tìm [TDM03]; danh sách qua controller rồi hiện trên trang.
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

            // [SUA12-05] Luyện sửa: Thay cách tính thẻ Giá trị quy đổi của toàn bộ điểm.
            // . Hiện lấy tổng điểm chia nguyên cho mốc đổi, nhân tiền giảm rồi làm tròn; mốc và tiền được đọc
            //   từ settings.
            // . Đây là thẻ tổng quan. Đổi tiền giảm thật của đơn hàng còn liên quan nơi sử dụng điểm trong
            //   giỏ/thanh toán.
            long redeemableSteps = settings.getPointsStep() > 0 ? totalPoints / settings.getPointsStep() : 0;
            long totalPointValue = Math.round(redeemableSteps * settings.getPointsStepValue());

            request.setAttribute("settings", settings);
            request.setAttribute("customerCountWithPoints", loyaltyDAO.countCustomersWithPoints());
            request.setAttribute("totalPoints", totalPoints);
            request.setAttribute("totalPointValue", totalPointValue);
            request.setAttribute("transactionCount", loyaltyDAO.countTransactions());

            // [SUA12-01] Luyện sửa: Bảng lịch sử chung chỉ hiện tối đa 10 giao dịch mới nhất thay vì 50.
            // . Đổi đối số findRecent từ 50 thành 10; findDetailed đưa số này xuống LIMIT và đã xếp thời gian
            //   mới trước.
            // . Chỉ giảm số dòng được lấy để hiển thị, không xóa lịch sử và không đổi số của thẻ tổng giao
            //   dịch.
            request.setAttribute("transactions", loyaltyDAO.findRecent(50));

            // [TDTK02]
            // . doGet trong AdminLoyaltyController.java nhận phone từ form tra cứu. Em làm gọn số điện thoại
            //   bằng AccountValidation.phone rồi kiểm tra đủ 10 chữ số, bắt đầu bằng 0. Đúng mới gọi
            //   LoyaltyTransactionDAO.findCustomerByPhone để tìm khách.
            // .. Chưa gửi số để tìm thì lấy khách mặc định. Đã gửi số nhưng nhập sai hoặc không tìm thấy thì
            //   hiện lỗi tương ứng.
            // ... Đọc tiếp tại LoyaltyTransactionDAO.java, tìm [TDTK03].
            String rawPhone = request.getParameter("phone");
            // [TDSDT02]
            // . doGet nhận phone từ ô Tra cứu. Chuỗi dài quá 64 ký tự bị báo sai; các số khác được
            //   AccountValidation.phone làm gọn, rồi phoneError kiểm tra đủ 10 chữ số bắt đầu bằng 0.
            // . Đúng mới gọi findCustomerByPhone; sai thì hiện lỗi. Chỉ khi chưa gửi phone mới gọi
            //   findDefaultCustomer. doGet đưa khách tìm được và lookupError về admin-loyalty.jsp để hiển thị.
            // ... doGet chọn activeCustomer và giữ lookupError nếu có. Đọc điểm đưa khách về trang tại
            //     AdminLoyaltyController.java, tìm mã TDTK04; số sai hoặc không tìm thấy thì trang hiện lỗi thay
            //     cho thông tin khách.
            boolean phoneTooLong = rawPhone != null && rawPhone.length() > 64;
            String phone = phoneTooLong
                    ? rawPhone.substring(0, 64) : AccountValidation.phone(rawPhone);
            String phoneError = phoneTooLong ? "Số điện thoại tối đa 64 ký tự." : AccountValidation.phoneError(phone);
            boolean invalidPhone = rawPhone != null && phoneError != null;

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

                // [SUA12-06] Luyện sửa: Thay cách tính tiền có thể đổi của khách đang xem.
                // . Dùng điểm riêng của khách, chia nguyên cho mốc rồi nhân tiền giảm; phải cùng quy tắc với
                //   phần tổng quan và hồ sơ.
                // . Nếu đề chỉ đổi mốc/tiền giảm đang dùng thì cập nhật qua Lưu quy tắc; sửa riêng phép hiển
                //   thị chưa đổi tiền giảm của đơn hàng.
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
            // . Chuẩn bị xong dữ liệu trong doGet, em chuyển quy tắc, thông tin khách, lịch sử và thông báo
            //   sang admin-loyalty.jsp. Trang dùng những dữ liệu này để hiện lại kết quả hiện tại.
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
    // . doPost trong AdminLoyaltyController.java nhận action=updateRules từ nút Lưu quy tắc. Em gọi
    //   updateRules(request) cùng file để đọc ba ô tiền tích điểm, mốc đổi và tiền giảm, rồi kiểm tra
    //   trước khi lưu.
    // ... doPost gọi updateRules cùng file; mỗi ô được ruleValue đưa tới LoyaltyValidation.wholeNumber.
    //     Đọc hàm kiểm tra số tại LoyaltyValidation.java, tìm mã TDQT03; xong thì quay về xét errors của
    //     cả ba ô.
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
                // . Lưu thành công thì saveLoyaltyRules trả true, updateRules kết thúc và doPost trong
                //   AdminLoyaltyController.java tiếp tục đặt thông báo. Em mở lại /admin/loyalty để doGet đọc bộ quy
                //   tắc vừa lưu trước khi đưa lên màn hình.
                // ... doPost đặt thông báo rồi chuyển lại /admin/loyalty. Yêu cầu mới chạy doGet và gọi
                //     getSettingsStrict để đọc bộ vừa lưu; đọc tại ShopSettingDAO.java, tìm mã TDQT07.

                session.setAttribute("loyaltyMessage", "Đã lưu quy tắc tích và đổi điểm.");
            } else if ("adjust".equals(action)) {
                // . Với action=adjust, em gọi hàm adjustPoints (kiểm tra yêu cầu cộng/trừ điểm) ngay trong
                //   AdminLoyaltyController.java và truyền request để nhận mã khách, số điểm cùng lựa chọn cộng hoặc trừ.
                adjustPoints(request);
                // [TDDC06]
                // . DAO cập nhật xong số dư và lịch sử, rồi trả kết quả về AdminLoyaltyController.adjustPoints. Hàm
                //   này kết thúc, doPost đặt thông báo và mở lại trang để doGet đọc số dư, lịch sử mới sang
                //   admin-loyalty.jsp.
                // ... doPost đặt thông báo rồi mở lại trang; doGet đọc số dư và lịch sử mới. Đọc phần nhận thông báo
                //     tại admin-loyalty.jsp, tìm mã TDQT09 rồi theo chỉ dẫn tới số dư và lịch sử để kết thúc.
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
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em
        //   đưa giá trị pointsEarnUnit vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra tiền chi
        //   tiêu nhận một điểm. Em truyền cả tên ô và giới hạn từ 1 đến 100.000.000; số hợp lệ được trả về
        //   để lưu cùng hai quy tắc còn lại.
        // ... ruleValue gọi kiểm tra số tại LoyaltyValidation.java, tìm mã TDQT03, nhận kết quả về earnUnit.
        //     Kiểm tra xong cả ba ô thì updateRules xét errors trước khi ghi.
        long earnUnit = ruleValue(request, "pointsEarnUnit", "Số tiền chi tiêu để nhận 1 điểm", 1,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        // [TDMOC02]
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em
        //   đưa pointsStep vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra số điểm mỗi lần đổi. Em
        //   truyền tên ô và giới hạn từ 1 đến 1.000.000 để nếu sai thì báo đúng ô, còn đúng thì nhận số để
        //   tiếp tục lưu.
        // .. Mốc đổi không được bằng 0 vì đây là số dùng để chia khi tính số lần đổi điểm.
        // ... ruleValue gọi kiểm tra số tại LoyaltyValidation.java, tìm mã TDQT03, nhận kết quả về
        //     pointsStep. Kiểm tra xong cả ba ô thì updateRules xét errors trước khi ghi.
        long pointsStep = ruleValue(request, "pointsStep", "Số điểm cho mỗi lần đổi", 1,
                LoyaltyValidation.MAX_POINTS, errors);

        // [TDGT02]
        // . Trong updateRules (kiểm tra và lưu quy tắc tích/đổi điểm) của AdminLoyaltyController.java, em
        //   đưa pointsStepValue vào ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra tiền giảm mỗi lần
        //   đổi. Giới hạn là từ 0 đến 100.000.000, nên giá trị 0 vẫn hợp lệ. Kết quả được dùng cùng hai ô
        //   còn lại để lưu quy tắc.
        // ... ruleValue gọi kiểm tra số tại LoyaltyValidation.java, tìm mã TDQT03, nhận kết quả về
        //     stepValue. Kiểm tra xong cả ba ô thì updateRules xét errors trước khi ghi.
        long stepValue = ruleValue(request, "pointsStepValue", "Số tiền giảm cho mỗi lần đổi", 0,
                LoyaltyValidation.MAX_RULE_MONEY, errors);

        // [TDQT04]
        // . Quay lại updateRules, em xem errors sau khi ruleValue kiểm tra ba ô. Còn lỗi thì giữ lời báo
        //   theo ô và dừng lưu. Không còn lỗi mới gọi ShopSettingDAO.saveLoyaltyRules, truyền ba số để ghi
        //   bộ quy tắc.
        // ... Còn errors thì doPost giữ ba ô cùng lời báo rồi mở lại trang; xem kết quả tại
        //     admin-loyalty.jsp, tìm mã TDQT09. Không còn lỗi thì gọi saveLoyaltyRules; đọc bước ghi tại
        //     ShopSettingDAO.java, tìm mã TDQT05.
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
    // . doPost nhận action=adjust rồi gọi adjustPoints trong AdminLoyaltyController.java. Em đọc mã khách, số
    //   điểm và lựa chọn cộng/trừ; hợp lệ mới gọi adjustPoints trong LoyaltyTransactionDAO.java.
    // . Nếu cộng thì truyền số điểm dương, nếu trừ thì truyền số âm. Ví dụ nhập trừ 30 điểm thì DAO nhận -30.
    // .. Mã khách phải dương, điểm nhập là số nguyên từ 1 đến 1.000.000, loại thao tác phải là add hoặc
    //   subtract.
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

        // [SUA12-02] Luyện sửa: Khi cộng 10 điểm thì tặng thêm 5, khách thực nhận 15 điểm.
        // . Đổi điểm ở nhánh add trước khi gọi DAO; nhánh subtract vẫn truyền số âm như quy định.
        // . Truyền cùng một số điểm thực tế để DAO vừa tính số dư vừa ghi ADJUST; kiểm tra giới hạn sau khi
        //   cộng thưởng.
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
            // . Em gọi LoyaltyValidation.wholeNumber, truyền nội dung ô, tên ô và hai giới hạn.
            //   Hàm trả số nếu đúng; báo lỗi thì nhánh catch bên dưới giữ lời báo cho đúng ô đang nhập.
            // ... Đọc tiếp tại LoyaltyValidation.java, tìm [TDQT03].
            return LoyaltyValidation.wholeNumber(request.getParameter(field), label, min, max);
        } catch (IllegalArgumentException e) {
            // . Giữ lời báo vào errors rồi trả 0 tạm; updateRules thấy errors nên dừng trước khi lưu quy tắc.
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
        return utils.FormInput.trim(value);
    }
}
