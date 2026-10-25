package controller;

import dao.DeliveryAreaDAO;
import dao.ShopSettingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.DeliveryArea;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AdminDeliveryAreaController - Phí giao hàng theo quận (chỉ ADMIN)
 * GET  /admin/delivery-areas       : danh sách quận + phí giao + đang nhận giao hay không
 * POST action=save                 : lưu phí giao / bật tắt nhận giao của tất cả quận
 * POST action=add                  : thêm quận mới
 * Phí giao của đơn = phí của quận khách chọn; miễn phí khi tiền hàng đạt mức freeship (/admin/settings).
 */
@WebServlet(name = "AdminDeliveryAreaController", urlPatterns = {"/admin/delivery-areas"})
public class AdminDeliveryAreaController extends HttpServlet {

    private static final long MAX_FEE = 1_000_000;

    private final DeliveryAreaDAO deliveryAreaDAO = new DeliveryAreaDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "areaSuccessMsg");
        CartController.moveFlashMessage(session, request, "areaErrorMsg");
        if (session.getAttribute("areaCsrfToken") == null) {
            session.setAttribute("areaCsrfToken", UUID.randomUUID().toString());
        }
        try {
            request.setAttribute("areas", deliveryAreaDAO.getAllAreas());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load delivery areas", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được phí giao theo quận. Hãy chạy lại database/aura_cafe.sql (bảng delivery_areas).");
            return;
        }
        request.setAttribute("settings", shopSettingDAO.getSettings());
        request.getRequestDispatcher("/admin-delivery-areas.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String expected = (String) session.getAttribute("areaCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            session.setAttribute("areaErrorMsg", "Phiên làm việc đã được làm mới. Vui lòng thao tác lại.");
            response.sendRedirect(request.getContextPath() + "/admin/delivery-areas");
            return;
        }

        try {
            if ("add".equals(request.getParameter("action"))) {
                String district = request.getParameter("district");
                district = district == null ? "" : district.trim().replaceAll("\\s+", " ");
                if (district.isEmpty() || district.length() > 50) {
                    throw new IllegalArgumentException("Tên quận/huyện bắt buộc, tối đa 50 ký tự.");
                }
                long fee = parseFee(request.getParameter("deliveryFee"), district);
                if (deliveryAreaDAO.districtExists(district)) {
                    throw new IllegalArgumentException("Đã có " + district + " trong danh sách.");
                }
                deliveryAreaDAO.insert(district, fee);
                session.setAttribute("areaSuccessMsg", "Đã thêm " + district + ".");
            } else {
                List<DeliveryArea> updates = new ArrayList<>();
                for (DeliveryArea area : deliveryAreaDAO.getAllAreas()) {
                    String raw = request.getParameter("fee_" + area.getId());
                    if (raw == null) continue;
                    area.setDeliveryFee(parseFee(raw, area.getDistrict()));
                    area.setActive(request.getParameter("active_" + area.getId()) != null);
                    updates.add(area);
                }
                deliveryAreaDAO.updateAll(updates);
                session.setAttribute("areaSuccessMsg", "Đã lưu phí giao của " + updates.size()
                        + " quận. Khách đặt hàng áp dụng ngay.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("areaErrorMsg", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Delivery area update failed", e);
            session.setAttribute("areaErrorMsg", "Không lưu được phí giao vào database. Vui lòng thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/delivery-areas");
    }

    /** Phí giao: số nguyên đồng 0 .. 1.000.000 */
    private long parseFee(String raw, String district) {
        try {
            long fee = Long.parseLong(raw == null ? "" : raw.trim());
            if (fee < 0 || fee > MAX_FEE) throw new NumberFormatException();
            return fee;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Phí giao của " + district + " phải từ 0 đến 1.000.000đ.");
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin có quyền truy cập trang này.");
            return false;
        }
        return true;
    }
}
