package controller;

import dao.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.RestockRequest.RestockItem;
import model.User;
import java.io.IOException;
import java.math.*;
import java.util.*;

@WebServlet(name = "AdminRestockController", urlPatterns = {"/admin/restock-requests", "/barista/restock-requests"})
public class AdminRestockController extends HttpServlet {

    // Giới hạn dữ liệu khớp với bảng `restock_requests` và `restock_request_items`
    static final int MAX_ITEMS = 100;
    static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");
    static final int MAX_QUANTITY_SCALE = 3;
    static final int MIN_REASON_LENGTH = 3;
    static final int MAX_TEXT_LENGTH = 500;

    private final RestockRequestDAO dao = new RestockRequestDAO();
    private final IngredientDAO ingredients = new IngredientDAO();

    @Override
    protected void doGet(HttpServletRequest r, HttpServletResponse p) throws ServletException, IOException {
        boolean adminRoute = r.getServletPath().startsWith("/admin/");
        User u = InventoryAccess.require(r, p, adminRoute);
        if (u == null) return;
        boolean admin = adminRoute && "ADMIN".equals(u.getRole());
        InventoryAccess.token(r.getSession());
        InventoryAccess.flash(r);
        try {
            r.setAttribute("workflowAdmin", admin);
            r.setAttribute("requests", dao.findForActor("ADMIN".equals(u.getRole()), u.getId()));
            r.setAttribute("restockStats", dao.getRestockStats());
            r.setAttribute("allIngredients", ingredients.getActiveIngredientsWithDetails());
            r.setAttribute("lowStockList", dao.getLowStockIngredients());
            r.setAttribute("history", new InventoryWorkflowDAO().history("RESTOCK", admin ? null : u.getId()));
            r.getRequestDispatcher("/admin-restock.jsp").forward(r, p);
        } catch (Exception e) {
            throw new ServletException("Không tải được đề xuất nhập hàng.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest r, HttpServletResponse p) throws IOException {
        r.setCharacterEncoding("UTF-8");
        boolean adminRoute = r.getServletPath().startsWith("/admin/");
        User u = InventoryAccess.require(r, p, adminRoute);
        if (u == null || !InventoryAccess.csrf(r, p)) return;

        HttpSession session = r.getSession();
        session.removeAttribute("workflowMessage");
        session.removeAttribute("workflowError");

        boolean admin = "ADMIN".equals(u.getRole());
        String action = r.getParameter("action");
        if (action == null) action = "";

        try {
            if ("create".equals(action) || "saveDraft".equals(action) || "edit".equals(action)) {
                // 1. Kiểm tra danh sách nguyên liệu & số lượng đề xuất
                List<RestockItem> items = parseRestockItems(
                        r.getParameterValues("ingredientId"),
                        r.getParameterValues("quantity")
                );

                // 2. Kiểm tra lý do đề xuất (3 - 500 ký tự) và ghi chú thêm (tối đa 500 ký tự)
                String reason = r.getParameter("reason");
                String reasonError = validateReason(reason);
                if (reasonError != null) {
                    throw new IllegalArgumentException(reasonError);
                }
                String note = r.getParameter("note");
                String noteError = validateOptionalText(note, "Ghi chú thêm");
                if (noteError != null) {
                    throw new IllegalArgumentException(noteError);
                }

                if ("edit".equals(action)) {
                    int id = parseRequestId(r.getParameter("id"));
                    String editReason = r.getParameter("editReason");
                    if (editReason == null || editReason.trim().isEmpty()) {
                        editReason = "Cập nhật số lượng đề xuất";
                    } else if (editReason.trim().length() > MAX_TEXT_LENGTH) {
                        throw new IllegalArgumentException("Lý do chỉnh sửa không được vượt quá 500 ký tự.");
                    } else if (editReason.trim().length() < MIN_REASON_LENGTH) {
                        throw new IllegalArgumentException("Lý do chỉnh sửa phải có ít nhất 3 ký tự.");
                    }
                    dao.edit(id, u.getId(), admin, reason.trim(), note != null ? note.trim() : "", items, editReason.trim());
                } else {
                    String initialStatus = "saveDraft".equals(action) ? "DRAFT" : "PENDING_APPROVAL";
                    dao.createRestockRequest(u.getId(), initialStatus, reason.trim(), note != null ? note.trim() : "", items);
                }
                session.setAttribute("workflowMessage", "Đã cập nhật đề xuất nhập hàng thành công!");
            } else if ("approveAndOrder".equals(action) && admin) {
                int reqId = parseRequestId(r.getParameter("id"));
                String reviewNote = r.getParameter("reviewNote");
                String reviewError = validateOptionalText(reviewNote, "Ghi chú duyệt");
                if (reviewError != null) {
                    throw new IllegalArgumentException(reviewError);
                }
                dao.transitionFromUi(reqId, u.getId(), true, "approve", reviewNote);
                session.setAttribute("workflowMessage", "Đã duyệt đề xuất #RR-" + reqId + ". Hãy kiểm tra và lưu phiếu nhập hàng.");
                p.sendRedirect(r.getContextPath() + "/admin/purchases?fromRequestId=" + reqId);
                return;
            } else if ("approve".equals(action) || "reject".equals(action)
                    || "requestChanges".equals(action) || "submit".equals(action)) {
                int reqId = parseRequestId(r.getParameter("id"));
                String reviewNote = r.getParameter("reviewNote");
                String reviewError = validateOptionalText(reviewNote, "Phản hồi duyệt");
                if (reviewError != null) {
                    throw new IllegalArgumentException(reviewError);
                }
                dao.transitionFromUi(reqId, u.getId(), admin, action, reviewNote);
                session.setAttribute("workflowMessage", "Đã cập nhật đề xuất nhập hàng thành công!");
            } else {
                session.setAttribute("workflowError", "Thao tác không được hỗ trợ.");
            }
        } catch (NumberFormatException e) {
            // Bắt trước IllegalArgumentException để không lộ thông báo lỗi Java
            getServletContext().log("Restock action failed", e);
            session.setAttribute("workflowError", restockErrorMessage(e));
        } catch (IllegalArgumentException e) {
            session.setAttribute("workflowError", e.getMessage());
        } catch (Exception e) {
            getServletContext().log("Restock action failed", e);
            session.setAttribute("workflowError", restockErrorMessage(e));
        }
        p.sendRedirect(r.getContextPath() + (adminRoute ? "/admin/restock-requests" : "/barista/restock-requests"));
    }

    /**
     * Kiểm tra số lượng đề xuất khớp với DECIMAL(12,3):
     * phải lớn hơn 0, tối đa 999,999,999.999 và tối đa 3 chữ số thập phân.
     */
    static String validateQuantity(BigDecimal value) {
        if (value == null) {
            return "Số lượng đề xuất không hợp lệ.";
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return "Số lượng đề xuất phải lớn hơn 0.";
        }
        if (value.compareTo(MAX_QUANTITY) > 0) {
            return "Số lượng đề xuất quá lớn (tối đa 999,999,999.999).";
        }
        if (value.stripTrailingZeros().scale() > MAX_QUANTITY_SCALE) {
            return "Số lượng đề xuất chỉ được có tối đa 3 chữ số thập phân.";
        }
        return null;
    }

    // Kiểm tra lý do đề xuất bắt buộc từ 3 đến 500 ký tự.
    static String validateReason(String rawReason) {
        if (rawReason == null) {
            return "Lý do đề xuất phải từ 3 đến 500 ký tự.";
        }
        int len = rawReason.trim().length();
        if (len < MIN_REASON_LENGTH || len > MAX_TEXT_LENGTH) {
            return "Lý do đề xuất phải từ 3 đến 500 ký tự.";
        }
        return null;
    }

    // Kiểm tra chuỗi không bắt buộc (ghi chú/phản hồi), tối đa 500 ký tự.
    static String validateOptionalText(String rawText, String fieldLabel) {
        if (rawText != null && rawText.trim().length() > MAX_TEXT_LENGTH) {
            return fieldLabel + " không được vượt quá 500 ký tự.";
        }
        return null;
    }

    // Đọc và kiểm tra danh sách nguyên liệu + số lượng đề xuất (1 đến 100 dòng, không trùng).
    static List<RestockItem> parseRestockItems(String[] ingredientIds, String[] quantities) {
        if (ingredientIds == null || quantities == null
                || ingredientIds.length == 0
                || ingredientIds.length != quantities.length
                || ingredientIds.length > MAX_ITEMS) {
            throw new IllegalArgumentException("Danh sách nguyên liệu không hợp lệ.");
        }

        List<RestockItem> items = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (int i = 0; i < ingredientIds.length; i++) {
            int ingId;
            try {
                ingId = Integer.parseInt(ingredientIds[i] != null ? ingredientIds[i].trim() : "");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Danh sách nguyên liệu có dòng không hợp lệ hoặc bị trùng.");
            }
            if (ingId <= 0 || !seenIds.add(ingId)) {
                throw new IllegalArgumentException("Danh sách nguyên liệu có dòng không hợp lệ hoặc bị trùng.");
            }

            String rawQty = quantities[i];
            if (rawQty == null || rawQty.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập số lượng đề xuất.");
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(rawQty.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Số lượng đề xuất phải là một số hợp lệ.");
            }

            String qtyError = validateQuantity(qty);
            if (qtyError != null) {
                throw new IllegalArgumentException(qtyError);
            }
            if (qty.scale() > MAX_QUANTITY_SCALE) {
                qty = qty.stripTrailingZeros();
            }
            items.add(new RestockItem(ingId, "", "", qty));
        }
        return items;
    }

    // Đọc mã đề xuất nhập hàng, bắt buộc > 0.
    private static int parseRequestId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("Mã đề xuất nhập hàng không hợp lệ.");
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Mã đề xuất nhập hàng không hợp lệ.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã đề xuất nhập hàng không hợp lệ.");
        }
    }

    // Chuyển lỗi DB / định dạng sang thông báo tiếng Việt rõ ràng.
    static String restockErrorMessage(Exception error) {
        if (error instanceof java.sql.SQLException sql) {
            String state = sql.getSQLState();
            if ("45000".equals(state)) {
                return sql.getMessage();
            }
            if ("22003".equals(state)) {
                return "Số lượng đề xuất vượt quá giới hạn lưu trữ (tối đa 999,999,999.999).";
            }
            if ("22001".equals(state)) {
                return "Lý do hoặc ghi chú vượt quá độ dài tối đa 500 ký tự.";
            }
            if ("23000".equals(state)) {
                return "Nguyên liệu không tồn tại hoặc bị trùng trong đề xuất.";
            }
            return "Không thể lưu đề xuất nhập hàng. Vui lòng thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.";
        }
        if (error instanceof NumberFormatException) {
            return "Mã đề xuất, mã nguyên liệu hoặc số lượng nhập vào không hợp lệ.";
        }
        return "Không thể lưu đề xuất nhập hàng. Vui lòng kiểm tra lại dữ liệu.";
    }

    static BigDecimal calculateUnitPrice(BigDecimal qty, BigDecimal total) {
        if (qty == null || total == null || qty.signum() <= 0 || total.signum() < 0) {
            throw new IllegalArgumentException("Số lượng và tổng tiền không hợp lệ.");
        }
        return total.divide(qty, 2, RoundingMode.HALF_UP);
    }
}
