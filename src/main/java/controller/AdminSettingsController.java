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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@WebServlet(name = "AdminSettingsController", urlPatterns = {"/admin/settings"})
public class AdminSettingsController extends HttpServlet {
    private static final Pattern SHIFT_TIME_PATTERN = Pattern.compile(
            "^(?:[01]\\d|2[0-3]):[0-5]\\d-(?:[01]\\d|2[0-3]):[0-5]\\d$");
    private final ShopSettingDAO shopSettingDAO;

    public AdminSettingsController() { this(new ShopSettingDAO()); }
    AdminSettingsController(ShopSettingDAO shopSettingDAO) { this.shopSettingDAO = shopSettingDAO; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("settingsCsrfToken") == null) {
            session.setAttribute("settingsCsrfToken", java.util.UUID.randomUUID().toString());
        }
        CartController.moveFlashMessage(request.getSession(), request, "settingsMessage");
        showSettings(request, response, shopSettingDAO.findAll(), null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        Object expectedToken = session.getAttribute("settingsCsrfToken");
        if (expectedToken == null || !expectedToken.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Yêu cầu không hợp lệ.");
            return;
        }

        List<ShopSettingEntry> entries = shopSettingDAO.findAll();
        Map<String, String> form = new LinkedHashMap<>();
        Map<String, String> changed = new LinkedHashMap<>();
        String error = entries.isEmpty() ? "Không tải được cấu hình cửa hàng để lưu." : null;
        for (ShopSettingEntry entry : entries) {
            String raw = request.getParameter("setting_" + entry.getKey());
            String value = raw == null ? "" : raw.trim();
            form.put(entry.getKey(), value);
            String fieldError = validate(entry.getKey(), value);
            if (error == null && fieldError != null) error = fieldError;
            if (fieldError == null) {
                String normalized = isNumericSetting(entry.getKey()) ? String.valueOf(parseNumber(value)) : value;
                if (!normalized.equals(entry.getValue())) changed.put(entry.getKey(), normalized);
            }
        }
        if (error == null && !shopSettingDAO.updateAll(changed)) {
            error = "Không thể lưu cấu hình cửa hàng. Vui lòng thử lại.";
        }
        if (error != null) {
            request.setAttribute("settingsError", error);
            showSettings(request, response, entries, form);
            return;
        }
        session.setAttribute("settingsMessage",
                changed.isEmpty() ? "Không có thay đổi nào." : "Đã lưu cấu hình cửa hàng.");
        response.sendRedirect(request.getContextPath() + "/admin/settings");
    }

    private void showSettings(HttpServletRequest request, HttpServletResponse response,
                              List<ShopSettingEntry> entries, Map<String, String> form)
            throws ServletException, IOException {
        Map<String, String> values = new LinkedHashMap<>();
        for (ShopSettingEntry entry : entries) values.put(entry.getKey(), entry.getValue());
        if (form != null) values.putAll(form);
        request.setAttribute("settingGroups", groupSettings(entries));
        request.setAttribute("form", values);
        request.setAttribute("settingsPageLoaded", true);
        request.getRequestDispatcher("/admin-settings.jsp").forward(request, response);
    }

    static Map<String, List<ShopSettingEntry>> groupSettings(List<ShopSettingEntry> entries) {
        Map<String, List<ShopSettingEntry>> groups = new LinkedHashMap<>();
        for (String name : new String[]{"Dùng điểm Aura trừ vào đơn", "Giao hàng & Freeship",
                "Thông tin ngân hàng", "Thanh toán", "Ca làm việc", "Cài đặt khác"}) {
            groups.put(name, new ArrayList<>());
        }
        for (ShopSettingEntry entry : entries) {
            String key = entry.getKey();
            String group = key.startsWith("points_") ? "Dùng điểm Aura trừ vào đơn"
                    : key.startsWith("delivery_") || key.equals("freeship_threshold") ? "Giao hàng & Freeship"
                    : key.startsWith("payment_bank_") || key.startsWith("payment_account_")
                            || key.startsWith("bank_") ? "Thông tin ngân hàng"
                    : key.startsWith("payment_") ? "Thanh toán"
                    : key.startsWith("shift_") ? "Ca làm việc" : "Cài đặt khác";
            groups.get(group).add(entry);
        }
        groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return groups;
    }

    static boolean isNumericSetting(String key) {
        return key.equals(ShopSettings.KEY_FREESHIP_THRESHOLD) || key.equals(ShopSettings.KEY_DELIVERY_FEE)
                || key.equals(ShopSettings.KEY_DELIVERY_MINUTES) || key.equals(ShopSettings.KEY_POINTS_STEP)
                || key.equals(ShopSettings.KEY_POINTS_STEP_VALUE) || key.equals(ShopSettings.KEY_POINTS_EARN_UNIT);
    }

    static String validate(String key, String value) {
        if (value == null || value.isBlank()) return "Giá trị cài đặt không được để trống.";
        if (value.length() > 255) return "Giá trị cài đặt tối đa 255 ký tự.";
        if (key.startsWith("shift_") && !SHIFT_TIME_PATTERN.matcher(value).matches())
            return "Giờ ca phải đúng định dạng HH:mm-HH:mm.";
        if (!isNumericSetting(key)) return null;
        long n = parseNumber(value);
        switch (key) {
            case ShopSettings.KEY_FREESHIP_THRESHOLD:
                return n >= 0 && n <= 100_000_000 ? null : "Mức Freeship phải từ 0 đến 100.000.000đ.";
            case ShopSettings.KEY_DELIVERY_FEE:
                return n >= 0 && n <= 1_000_000 ? null : "Phí giao hàng phải từ 0 đến 1.000.000đ.";
            case ShopSettings.KEY_DELIVERY_MINUTES:
                return n >= 5 && n <= 300 ? null : "Thời gian giao dự kiến phải từ 5 đến 300 phút.";
            case ShopSettings.KEY_POINTS_STEP:
                return n >= 1 && n <= 100_000 ? null : "Mốc điểm phải từ 1 đến 100.000 điểm.";
            case ShopSettings.KEY_POINTS_STEP_VALUE:
                return n >= 1_000 && n <= 10_000_000 ? null : "Số tiền giảm cho mỗi mốc điểm phải từ 1.000đ đến 10.000.000đ.";
            case ShopSettings.KEY_POINTS_EARN_UNIT:
                return n >= 1_000 && n <= 100_000_000 ? null : "Mức chi tiêu để nhận 1 điểm phải từ 1.000đ đến 100.000.000đ.";
            default: return null;
        }
    }

    private static long parseNumber(String value) {
        try { return Long.parseLong(value.replaceAll("[.,\\s]", "")); }
        catch (RuntimeException e) { return -1; }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được thay đổi cấu hình cửa hàng.");
            return false;
        }
        return true;
    }
}
