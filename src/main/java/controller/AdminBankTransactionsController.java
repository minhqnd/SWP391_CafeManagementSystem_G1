package controller;

import dao.BankTransactionDAO;
import dao.ShopSettingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.BankTransaction;
import model.ShopSettings;
import model.User;
import service.BankTransactionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "AdminBankTransactionsController", urlPatterns = {"/admin/bank-transactions"})
public class AdminBankTransactionsController extends HttpServlet {

    // Giới hạn dữ liệu khớp với bảng `bank_transaction_refunds` trong MySQL
    static final int MAX_TX_KEY_LENGTH = 120;
    static final int MAX_REFERENCE_CODE_LENGTH = 150;
    static final int MAX_ORDER_CODE_LENGTH = 100;
    static final int MAX_REFUND_NOTE_LENGTH = 255;
    static final BigDecimal MAX_REFUND_AMOUNT = new BigDecimal("9999999999.99");
    static final int MAX_REFUND_SCALE = 2;
    static final Set<String> ALLOWED_RECONCILE_STATUSES = Set.of(
            "", "MATCHED", "OVERPAID", "UNDERPAID", "DUPLICATE",
            "ORDER_CANCELLED", "ORDER_NOT_FOUND", "NO_CODE"
    );

    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final BankTransactionDAO bankTransactionDAO = new BankTransactionDAO();
    private final BankTransactionService bankTransactionService = new BankTransactionService(bankTransactionDAO);

    /** Bản rút gọn đơn hàng (giữ tương thích cho unit test đối soát). */
    static class OrderSummary extends BankTransactionDAO.OrderInfo {
        OrderSummary() {
            super();
        }

        OrderSummary(int id, String orderCode, double finalAmount, String status) {
            super(id, orderCode, finalAmount, status);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        // Chỉ tạo CSRF token nếu session chưa có để auto-polling GET mỗi 5 giây không làm lệch token của modal
        HttpSession session = request.getSession();
        if (session.getAttribute("bankTxCsrfToken") == null) {
            session.setAttribute("bankTxCsrfToken", UUID.randomUUID().toString());
        }

        ShopSettings settings = shopSettingDAO.getSettings();
        List<BankTransaction> transactions = bankTransactionService.loadTransactions(settings);

        request.setAttribute("transactions", transactions);
        request.setAttribute("totalInAmount", bankTransactionService.calculateNetInAmount(transactions));
        request.setAttribute("totalRefundedAmount", bankTransactionService.calculateTotalRefunded(transactions));
        request.setAttribute("totalCount", transactions.size());
        request.setAttribute("bankAccountNo", settings.getPaymentAccountNo());
        request.setAttribute("bankName", settings.getPaymentBankName());
        request.setAttribute("gatewayUrl", settings.getPaymentGatewayUrl());

        request.getRequestDispatcher("/admin-bank-transactions.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        String redirectUrl = request.getContextPath() + "/admin/bank-transactions";

        String sessionToken = (String) session.getAttribute("bankTxCsrfToken");
        String formToken = request.getParameter("csrfToken");
        if (sessionToken == null || !sessionToken.equals(formToken)) {
            session.setAttribute("bankTxError", "Yêu cầu không hợp lệ hoặc phiên làm việc đã hết hạn.");
            response.sendRedirect(redirectUrl);
            return;
        }

        session.removeAttribute("bankTxFlash");
        session.removeAttribute("bankTxError");

        String txKey = trimParam(request.getParameter("txKey"));
        String referenceCode = trimParam(request.getParameter("referenceCode"));
        String orderCode = trimParam(request.getParameter("orderCode"));
        String refundNote = trimParam(request.getParameter("refundNote"));
        String reconcileStatus = trimParam(request.getParameter("reconcileStatus"));
        String sepayIdParam = trimParam(request.getParameter("sepayId"));
        String refundAmountParam = trimParam(request.getParameter("refundAmount"));
        String transferAmountParam = trimParam(request.getParameter("transferAmount"));

        // Kiểm tra mã SePay (nếu có truyền lên phải là số nguyên >= 0)
        long sepayId = 0;
        if (!sepayIdParam.isEmpty()) {
            try {
                sepayId = Long.parseLong(sepayIdParam);
                if (sepayId < 0) {
                    session.setAttribute("bankTxError", "Mã giao dịch SePay không hợp lệ.");
                    response.sendRedirect(redirectUrl);
                    return;
                }
            } catch (NumberFormatException e) {
                session.setAttribute("bankTxError", "Mã giao dịch SePay không hợp lệ.");
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        // Kiểm tra toàn bộ dữ liệu hoàn tiền trước khi lưu xuống DB
        String error = validateRefundInput(
                txKey, referenceCode, orderCode, reconcileStatus,
                refundAmountParam, transferAmountParam, refundNote
        );
        if (error != null) {
            session.setAttribute("bankTxError", error);
            response.sendRedirect(redirectUrl);
            return;
        }

        User admin = (User) session.getAttribute("user");
        BigDecimal refundAmount = parseAmount(refundAmountParam);
        try {
            bankTransactionDAO.saveRefund(
                    txKey, sepayId, referenceCode, orderCode,
                    refundAmount, refundNote, admin.getId(), reconcileStatus
            );
            session.setAttribute("bankTxFlash", "Đã xác nhận hoàn tiền thành công.");
        } catch (Exception e) {
            System.err.println("[AdminBankTransactionsController] Lỗi lưu hoàn tiền: " + e.getMessage());
            session.setAttribute("bankTxError", refundErrorMessage(e));
        }

        response.sendRedirect(redirectUrl);
    }

    // Parse chuỗi số tiền sang BigDecimal; trả về null nếu rỗng hoặc không hợp lệ
    static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Kiểm tra hợp lệ dữ liệu form xác nhận hoàn tiền
    static String validateRefundInput(
            String txKey,
            String referenceCode,
            String orderCode,
            String reconcileStatus,
            String refundAmountParam,
            String transferAmountParam,
            String refundNote
    ) {
        String key = txKey == null ? "" : txKey.trim();
        if (key.isEmpty()) {
            return "Không xác định được mã giao dịch cần hoàn tiền.";
        }
        if (key.length() > MAX_TX_KEY_LENGTH) {
            return "Mã khóa giao dịch không được vượt quá " + MAX_TX_KEY_LENGTH + " ký tự.";
        }

        String ref = referenceCode == null ? "" : referenceCode.trim();
        if (ref.length() > MAX_REFERENCE_CODE_LENGTH) {
            return "Mã tham chiếu không được vượt quá " + MAX_REFERENCE_CODE_LENGTH + " ký tự.";
        }

        String order = orderCode == null ? "" : orderCode.trim();
        if (order.length() > MAX_ORDER_CODE_LENGTH) {
            return "Mã đơn hàng không được vượt quá " + MAX_ORDER_CODE_LENGTH + " ký tự.";
        }

        String status = reconcileStatus == null ? "" : reconcileStatus.trim();
        if (!ALLOWED_RECONCILE_STATUSES.contains(status)) {
            return "Trạng thái đối soát của giao dịch không hợp lệ.";
        }

        BigDecimal refundAmount = parseAmount(refundAmountParam);
        if (refundAmount == null) {
            return "Số tiền hoàn không hợp lệ.";
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Số tiền hoàn phải lớn hơn 0.";
        }
        if (refundAmount.compareTo(MAX_REFUND_AMOUNT) > 0) {
            return "Số tiền hoàn vượt quá giới hạn cho phép.";
        }
        if (refundAmount.stripTrailingZeros().scale() > MAX_REFUND_SCALE) {
            return "Số tiền hoàn chỉ hỗ trợ tối đa " + MAX_REFUND_SCALE + " chữ số thập phân.";
        }

        BigDecimal transferAmount = parseAmount(transferAmountParam);
        if (transferAmount == null
                || transferAmount.compareTo(BigDecimal.ZERO) <= 0
                || transferAmount.compareTo(MAX_REFUND_AMOUNT) > 0) {
            return "Số tiền gốc của giao dịch không hợp lệ.";
        }
        if (refundAmount.compareTo(transferAmount) > 0) {
            return "Số tiền hoàn không được vượt quá số tiền của giao dịch.";
        }

        String note = refundNote == null ? "" : refundNote.trim();
        if (note.length() > MAX_REFUND_NOTE_LENGTH) {
            return "Ghi chú hoàn tiền không được vượt quá " + MAX_REFUND_NOTE_LENGTH + " ký tự.";
        }

        return null;
    }

    // Chuyển lỗi SQL/hệ thống thành thông báo tiếng Việt an toàn, không lộ chi tiết kỹ thuật
    static String refundErrorMessage(Exception e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof SQLException sqlEx) {
                String state = sqlEx.getSQLState();
                int code = sqlEx.getErrorCode();
                if ("45000".equals(state)) {
                    String msg = sqlEx.getMessage();
                    if (msg != null && !msg.isBlank()) {
                        return msg;
                    }
                }
                if ("23000".equals(state)) {
                    return "Dữ liệu hoàn tiền bị trùng hoặc vi phạm ràng buộc hệ thống.";
                }
                if ("22003".equals(state) || code == 3819) {
                    return "Số tiền hoàn không hợp lệ hoặc vượt quá giới hạn lưu trữ.";
                }
                if ("22001".equals(state)) {
                    return "Nội dung hoàn tiền vượt quá độ dài cho phép.";
                }
            }
            cur = cur.getCause();
        }
        return "Không thể lưu thông tin hoàn tiền. Vui lòng thử lại sau.";
    }

    // =========================================================================
    // Các hàm ủy quyền sang BankTransactionService để giữ tương thích cho unit test
    // =========================================================================

    double calculateNetInAmount(List<BankTransaction> transactions) {
        return bankTransactionService.calculateNetInAmount(transactions);
    }

    double calculateTotalRefunded(List<BankTransaction> transactions) {
        return bankTransactionService.calculateTotalRefunded(transactions);
    }

    void reconcileTransactionsWithOrders(List<BankTransaction> transactions, Map<String, OrderSummary> orders) {
        Map<String, BankTransactionDAO.OrderInfo> mappedOrders = new HashMap<>();
        if (orders != null) {
            mappedOrders.putAll(orders);
        }
        bankTransactionService.reconcileTransactionsWithOrders(transactions, mappedOrders);
    }

    private String extractOrderCode(String content) {
        return bankTransactionService.extractOrderCode(content);
    }

    private String adjustUtcToVietnamTime(String utcStr) {
        return bankTransactionService.adjustUtcToVietnamTime(utcStr);
    }

    private String trimParam(String val) {
        return val == null ? "" : val.trim();
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(403, "Chỉ quản trị viên mới có quyền xem giao dịch ngân hàng.");
            return false;
        }
        return true;
    }
}
