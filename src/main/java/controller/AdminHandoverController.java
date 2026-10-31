package controller;

import dao.ShiftHandoverDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

import java.io.IOException;
import java.sql.SQLException;

/** Admin chỉ xem lịch sử bàn giao; thu ngân tự hoàn tất khi chốt ca. */
@WebServlet(name = "AdminHandoverController", urlPatterns = {"/admin/handovers"})
public class AdminHandoverController extends HttpServlet {
    private final ShiftHandoverDAO shiftHandoverDAO = new ShiftHandoverDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        try {
            request.setAttribute("handovers", shiftHandoverDAO.getHandovers(null));
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load shift handovers", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được lịch sử bàn giao ca.");
            return;
        }
        request.getRequestDispatcher("/admin-handovers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request, response)) return;
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Biên bản được hoàn tất khi Thu ngân chốt ca và không cần Admin duyệt.");
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Admin có quyền xem lịch sử bàn giao ca.");
            return false;
        }
        return true;
    }
}
