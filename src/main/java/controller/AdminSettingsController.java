package controller;

import dao.ShopSettingDAO;
import model.ShopSettings;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminSettingsController - Admin cài đặt bán hàng online (chỉ tài khoản ADMIN)
 * GET  /admin/settings : Xem mức freeship, phí giao hàng, thời gian giao dự kiến, mốc điểm Aura
 * POST /admin/settings : Lưu cấu hình - giỏ hàng của khách áp dụng ngay ở lần tải trang tiếp theo
 */
@WebServlet(name = "AdminSettingsController", urlPatterns = {"/admin/settings"})
public class AdminSettingsController extends HttpServlet {

    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        ShopSettings settings = shopSettingDAO.getSettings();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("freeshipThreshold", String.valueOf(Math.round(settings.getFreeshipThreshold())));
        form.put("deliveryFee", String.valueOf(Math.round(settings.getDeliveryFee())));
        form.put("deliveryMinutes", String.valueOf(settings.getDeliveryMinutes()));
        form.put("pointsStep", String.valueOf(settings.getPointsStep()));
        form.put("pointsStepValue", String.valueOf(Math.round(settings.getPointsStepValue())));

        CartController.moveFlashMessage(request.getSession(), request, "settingsMessage");
        showSettings(request, response, settings, form);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        // Giữ nguyên dữ liệu Admin vừa nhập nếu có lỗi
        Map<String, String> form = new LinkedHashMap<>();
        for (String name : new String[]{"freeshipThreshold", "deliveryFee", "deliveryMinutes", "pointsStep", "pointsStepValue"}) {
            String value = request.getParameter(name);
            form.put(name, value != null ? value.trim() : "");
        }

        long freeshipThreshold = parseNumber(form.get("freeshipThreshold"));
        long deliveryFee = parseNumber(form.get("deliveryFee"));
        long deliveryMinutes = parseNumber(form.get("deliveryMinutes"));
        long pointsStep = parseNumber(form.get("pointsStep"));
        long pointsStepValue = parseNumber(form.get("pointsStepValue"));

        String error = null;
        if (freeshipThreshold < 0 || freeshipThreshold > 100_000_000) {
            error = "Mức mua để được Freeship phải từ 0 đến 100.000.000đ (nhập 0 để miễn phí giao hàng mọi đơn).";
        } else if (deliveryFee < 0 || deliveryFee > 1_000_000) {
            error = "Phí giao hàng phải từ 0 đến 1.000.000đ.";
        } else if (deliveryMinutes < 5 || deliveryMinutes > 300) {
            error = "Thời gian giao dự kiến phải từ 5 đến 300 phút.";
        } else if (pointsStep < 1 || pointsStep > 100_000) {
            error = "Mốc điểm phải từ 1 đến 100.000 điểm.";
        } else if (pointsStepValue < 1_000 || pointsStepValue > 10_000_000) {
            error = "Số tiền giảm cho mỗi mốc điểm phải từ 1.000đ đến 10.000.000đ.";
        }

        if (error != null) {
            request.setAttribute("settingsError", error);
            showSettings(request, response, shopSettingDAO.getSettings(), form);
            return;
        }

        ShopSettings settings = new ShopSettings();
        settings.setFreeshipThreshold(freeshipThreshold);
        settings.setDeliveryFee(deliveryFee);
        settings.setDeliveryMinutes((int) deliveryMinutes);
        settings.setPointsStep((int) pointsStep);
        settings.setPointsStepValue(pointsStepValue);

        if (!shopSettingDAO.saveSettings(settings)) {
            request.setAttribute("settingsError", "Không lưu được cấu hình. Hãy kiểm tra Database đã chạy "
                    + "database/update_shop_settings.sql (bảng shop_settings) chưa.");
            showSettings(request, response, shopSettingDAO.getSettings(), form);
            return;
        }

        // PRG: tránh lưu lại khi F5
        request.getSession().setAttribute("settingsMessage", "Đã lưu cấu hình bán hàng online. "
                + "Giỏ hàng của khách áp dụng ngay ở lần tải trang tiếp theo.");
        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    private void showSettings(HttpServletRequest request, HttpServletResponse response,
                              ShopSettings settings, Map<String, String> form)
            throws ServletException, IOException {
        request.setAttribute("settings", settings);
        request.setAttribute("form", form);
        request.setAttribute("moneyPerPoint", model.Cart.MONEY_PER_POINT);
        request.getRequestDispatcher("/admin-settings.jsp").forward(request, response);
    }

    /** Chỉ tài khoản ADMIN được cài đặt; chưa đăng nhập thì mở popup đăng nhập ở trang chủ */
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được thay đổi cấu hình bán hàng.");
            return false;
        }
        return true;
    }

    /** Cho phép nhập "150.000" hoặc "150,000"; trả về -1 nếu không phải số nguyên */
    private long parseNumber(String value) {
        try {
            return Long.parseLong(value.replaceAll("[.,\\s]", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
