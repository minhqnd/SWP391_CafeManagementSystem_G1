package controller;

import dao.ShopSettingDAO;
import model.ShopSettingEntry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AdminSettingsController - Admin cài đặt bán hàng online (chỉ tài khoản ADMIN)
 * GET  /admin/settings : Xem mức freeship, phí giao hàng, thời gian giao dự kiến, mốc điểm Aura
 * POST /admin/settings : Lưu cấu hình - giỏ hàng của khách áp dụng ngay ở lần tải trang tiếp theo
 */
@WebServlet(name = "AdminSettingsController", urlPatterns = {"/admin/settings"})
public class AdminSettingsController extends HttpServlet {

    private static final Pattern SETTING_KEY_PATTERN = Pattern.compile("^[a-z0-9_]{1,50}$");
    private static final Pattern SHIFT_TIME_PATTERN = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d-(?:[01]\\d|2[0-3]):[0-5]\\d$");
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        // Các thao tác mở rộng dùng action riêng; form cấu hình bán hàng cũ bên dưới giữ nguyên.
        String action = request.getParameter("action");
        if ("addSetting".equals(action)) {
            addSetting(request, response);
            return;
        }
        if ("saveAllSettings".equals(action)) {
            saveAllSettings(request, response);
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
            request.setAttribute("settingsError", "Không lưu được cấu hình. Hãy kiểm tra database aura_cafe_erp "
                    + "và xác nhận bảng shop_settings tồn tại.");
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
        request.setAttribute("settingGroups", groupSettings(shopSettingDAO.findAll()));
        request.setAttribute("settingsPageLoaded", true);
        request.getRequestDispatcher("/admin-settings.jsp").forward(request, response);
    }

    /** Bổ sung: lưu màn hình quản lý toàn bộ khóa, tách biệt với form bán hàng online cũ. */
    private void saveAllSettings(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<ShopSettingEntry> entries = shopSettingDAO.findAll();
        Map<String, String> changed = new LinkedHashMap<>();
        for (ShopSettingEntry entry : entries) {
            String value = trim(request.getParameter("setting_" + entry.getKey()));
            if (value.isEmpty()) {
                request.setAttribute("settingsError", "Giá trị cài đặt không được để trống.");
                showSettings(request, response, shopSettingDAO.getSettings(), buildLegacyForm(shopSettingDAO.getSettings()));
                return;
            }
            if (entry.getKey().startsWith("shift_") && !SHIFT_TIME_PATTERN.matcher(value).matches()) {
                request.setAttribute("settingsError", "Giờ ca phải đúng định dạng HH:mm-HH:mm.");
                showSettings(request, response, shopSettingDAO.getSettings(), buildLegacyForm(shopSettingDAO.getSettings()));
                return;
            }
            if (!value.equals(entry.getValue())) changed.put(entry.getKey(), value);
        }
        if (!shopSettingDAO.updateAll(changed)) {
            request.setAttribute("settingsError", "Không thể lưu danh sách cài đặt.");
            showSettings(request, response, shopSettingDAO.getSettings(), buildLegacyForm(shopSettingDAO.getSettings()));
            return;
        }
        request.getSession().setAttribute("settingsMessage",
                changed.isEmpty() ? "Không có thay đổi nào." : "Đã lưu " + changed.size() + " cài đặt.");
        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    /** Bổ sung: thêm khóa mới mà không thay đổi cấu trúc hay dữ liệu khởi tạo trong file SQL. */
    private void addSetting(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String key = trim(request.getParameter("newKey"));
        String value = trim(request.getParameter("newValue"));
        request.setAttribute("newKey", key);
        request.setAttribute("newValue", value);
        request.setAttribute("openAddModal", true);
        String error = null;
        if (!SETTING_KEY_PATTERN.matcher(key).matches()) error = "Khóa chỉ gồm chữ thường, số, gạch dưới và tối đa 50 ký tự.";
        else if (value.isEmpty()) error = "Giá trị cài đặt không được để trống.";
        else if (value.length() > 255) error = "Giá trị cài đặt tối đa 255 ký tự.";
        else if (shopSettingDAO.findAll().stream().anyMatch(item -> item.getKey().equals(key))) error = "Khóa cài đặt đã tồn tại.";

        if (error != null || !shopSettingDAO.insert(key, value)) {
            request.setAttribute("settingsError", error != null ? error : "Không thể thêm khóa cài đặt.");
            ShopSettings settings = shopSettingDAO.getSettings();
            showSettings(request, response, settings, buildLegacyForm(settings));
            return;
        }
        request.getSession().setAttribute("settingsMessage", "Đã thêm cài đặt " + key + ".");
        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    private Map<String, String> buildLegacyForm(ShopSettings settings) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("freeshipThreshold", String.valueOf(Math.round(settings.getFreeshipThreshold())));
        form.put("deliveryFee", String.valueOf(Math.round(settings.getDeliveryFee())));
        form.put("deliveryMinutes", String.valueOf(settings.getDeliveryMinutes()));
        form.put("pointsStep", String.valueOf(settings.getPointsStep()));
        form.put("pointsStepValue", String.valueOf(Math.round(settings.getPointsStepValue())));
        return form;
    }

    private Map<String, List<ShopSettingEntry>> groupSettings(List<ShopSettingEntry> entries) {
        Map<String, List<ShopSettingEntry>> groups = new LinkedHashMap<>();
        groups.put("Giao hàng", new ArrayList<>());
        groups.put("Tích điểm", new ArrayList<>());
        groups.put("Ca làm việc", new ArrayList<>());
        groups.put("Khác", new ArrayList<>());
        for (ShopSettingEntry entry : entries) {
            String key = entry.getKey();
            String group = key.startsWith("delivery_") || key.equals("freeship_threshold") ? "Giao hàng"
                    : key.startsWith("points_") ? "Tích điểm"
                    : key.startsWith("shift_") ? "Ca làm việc" : "Khác";
            groups.get(group).add(entry);
        }
        groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return groups;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
