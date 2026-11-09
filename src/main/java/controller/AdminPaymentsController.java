package controller;

import dao.PaymentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Payment;
import model.User;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AdminPaymentsController", urlPatterns = {"/admin/payments"})
public class AdminPaymentsController extends HttpServlet {
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        String search = request.getParameter("search");
        String status = request.getParameter("status");
        String method = request.getParameter("method");

        List<Payment> payments = paymentDAO.getAllPayments(search, status, method);

        // Thống kê nhanh
        int totalCount = payments.size();
        double successRevenue = 0;
        int bankTransferCount = 0;
        int cashCount = 0;

        for (Payment p : payments) {
            if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
                successRevenue += p.getAmount();
            }
            if ("BANK_TRANSFER".equalsIgnoreCase(p.getPaymentMethod()) || "VIETQR".equalsIgnoreCase(p.getPaymentMethod())) {
                bankTransferCount++;
            } else if ("CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                cashCount++;
            }
        }

        request.setAttribute("payments", payments);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("successRevenue", successRevenue);
        request.setAttribute("bankTransferCount", bankTransferCount);
        request.setAttribute("cashCount", cashCount);
        request.setAttribute("currentSearch", search != null ? search : "");
        request.setAttribute("currentStatus", status != null ? status : "ALL");
        request.setAttribute("currentMethod", method != null ? method : "ALL");

        request.getRequestDispatcher("/admin-payments.jsp").forward(request, response);
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(403, "Chỉ quản trị viên mới có quyền xem lịch sử giao dịch.");
            return false;
        }
        return true;
    }
}
