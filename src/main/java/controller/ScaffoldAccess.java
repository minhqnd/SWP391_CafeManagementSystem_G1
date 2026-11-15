package controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import java.io.IOException;

final class ScaffoldAccess {
    private ScaffoldAccess() {}
    static boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // BF12: chỉ cho role ADMIN đi tiếp; không phải kiểm tra token hay kiểm tra số điểm.
        User user = (User) request.getSession().getAttribute("user");
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        return true;
    }
    static boolean requireStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())
                || (!"STAFF".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
