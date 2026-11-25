package controller;

import dao.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.*;
import service.InventoryRules;
import java.io.IOException;
import java.math.*;
import java.util.*;

@WebServlet(name="AdminPurchaseController",urlPatterns={"/admin/purchases"})
public class AdminPurchaseController extends HttpServlet {

    // Giới hạn dữ liệu khớp với DECIMAL(12,3), DECIMAL(12,2) và VARCHAR(500) trong DB
    static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");
    static final int MAX_QUANTITY_SCALE = 3;
    static final BigDecimal MAX_MONEY = new BigDecimal("9999999999.99");
    static final int MAX_MONEY_SCALE = 2;
    static final int MIN_REASON_LENGTH = 3;
    static final int MAX_TEXT_LENGTH = 500;

    private final PurchaseOrderDAO dao = new PurchaseOrderDAO();
    private final PurchaseReceiptDAO receipts = new PurchaseReceiptDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    @Override
    protected void doGet(HttpServletRequest r, HttpServletResponse p) throws ServletException, IOException {
        if (InventoryAccess.require(r, p, true) == null) return;
        InventoryAccess.token(r.getSession());
        InventoryAccess.flash(r);
        try {
            List<PurchaseOrder> orders = dao.findAll("ALL");
            receipts.populateReceived(orders);

            List<PurchaseReceipt> allReceipts = receipts.findAll();
            Map<Integer, List<PurchaseReceipt>> receiptsByPo = new HashMap<>();
            for (PurchaseReceipt rc : allReceipts) {
                receiptsByPo.computeIfAbsent(rc.getPoId(), k -> new ArrayList<>()).add(rc);
            }

            int pendingCount = 0;
            int completedCount = 0;
            BigDecimal totalSpend = BigDecimal.ZERO;
            for (PurchaseOrder po : orders) {
                String st = po.getStatus();
                if ("DRAFT".equals(st) || "ORDERED".equals(st) || "PARTIALLY_RECEIVED".equals(st)) {
                    pendingCount++;
                } else if ("RECEIVED".equals(st) || "CLOSED".equals(st)) {
                    completedCount++;
                }
                if (!"CANCELLED".equals(st) && po.getTotalCost() != null) {
                    totalSpend = totalSpend.add(po.getTotalCost());
                }
            }

            Map<Integer, String> receiptKeys = new HashMap<>();
            for (PurchaseOrder po : orders) {
                if (List.of("ORDERED", "PARTIALLY_RECEIVED").contains(po.getStatus())) {
                    receiptKeys.put(po.getId(), UUID.randomUUID().toString());
                }
            }
            r.setAttribute("receiptKeys", receiptKeys);
            r.setAttribute("purchaseOrders", orders);
            r.setAttribute("receiptsByPo", receiptsByPo);
            r.setAttribute("totalOrdersCount", orders.size());
            r.setAttribute("pendingOrdersCount", pendingCount);
            r.setAttribute("completedOrdersCount", completedCount);
            r.setAttribute("totalSpendAmount", totalSpend);
            r.setAttribute("activeSuppliers", new SupplierDAO().getActiveSuppliers());
            r.setAttribute("allIngredients", new IngredientDAO().getActiveIngredientsWithDetails());
            List<RestockRequest> availableRequests = new RestockRequestDAO().getRestockRequests("APPROVED").stream()
                    .filter(request -> request.getPurchaseOrderId() == null)
                    .toList();
            r.setAttribute("approvedRequests", availableRequests);
            r.setAttribute("history", new InventoryWorkflowDAO().history("PURCHASE", null));
            r.getRequestDispatcher("/admin-purchases.jsp").forward(r, p);
        } catch (Exception e) {
            throw new ServletException("Không tải được danh sách phiếu nhập hàng.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest r, HttpServletResponse p) throws IOException {
        r.setCharacterEncoding("UTF-8");
        User u = InventoryAccess.require(r, p, true);
        if (u == null || !InventoryAccess.csrf(r, p)) return;

        HttpSession session = r.getSession();
        session.removeAttribute("workflowMessage");
        session.removeAttribute("workflowError");

        try {
            String action = r.getParameter("action");
            if (action == null) action = "";

            if ("createDraft".equals(action) || "create".equals(action)) {
                List<PurchaseOrderItem> items = parseItems(r);
                int supplier = parsePositiveId(r.getParameter("supplierId"), "Nhà cung cấp");

                String note = r.getParameter("note");
                String noteError = validateOptionalText(note, "Ghi chú phiếu nhập");
                if (noteError != null) {
                    throw new IllegalArgumentException(noteError);
                }

                PurchaseOrder po = new PurchaseOrder();
                po.setSupplierId(supplier);
                po.setCreatorId(u.getId());
                po.setNote(note != null ? note.trim() : "");

                String rr = r.getParameter("restockRequestId");
                if (rr != null && !rr.isBlank()) {
                    po.setRestockRequestId(parsePositiveId(rr, "Mã đề xuất nhập hàng"));
                }

                boolean draft = "createDraft".equals(action) || "draft".equalsIgnoreCase(r.getParameter("saveMode"));
                int poId = dao.create(po, items, !draft);
                String result = draft ? "Đã lưu phiếu nháp PO-" : "Đã đặt hàng cho PO-";
                session.setAttribute("workflowMessage", result + poId + ".");

            } else if ("edit".equals(action)) {
                int poId = parsePositiveId(r.getParameter("id"), "Mã phiếu nhập");
                int supplier = parsePositiveId(r.getParameter("supplierId"), "Nhà cung cấp");

                BigDecimal qty = parseDecimalParam(r.getParameter("quantity"), "Số lượng đặt", false);
                String qtyError = validateQuantity(qty, false, "Số lượng đặt");
                if (qtyError != null) {
                    throw new IllegalArgumentException(qtyError);
                }
                if (qty.scale() > MAX_QUANTITY_SCALE) qty = qty.stripTrailingZeros();

                BigDecimal total = parseDecimalParam(r.getParameter("totalPrice"), "Tổng tiền", false);
                String moneyError = validateMoney(total, "Tổng tiền");
                if (moneyError != null) {
                    throw new IllegalArgumentException(moneyError);
                }
                if (total.scale() > MAX_MONEY_SCALE) total = total.stripTrailingZeros();

                String note = r.getParameter("note");
                String noteError = validateOptionalText(note, "Ghi chú phiếu nhập");
                if (noteError != null) {
                    throw new IllegalArgumentException(noteError);
                }

                String editReason = r.getParameter("editReason");
                if (editReason == null || editReason.isBlank()) {
                    editReason = "Cập nhật thông tin phiếu nhập";
                } else {
                    String reasonError = validateRequiredReason(editReason, "Lý do chỉnh sửa");
                    if (reasonError != null) {
                        throw new IllegalArgumentException(reasonError);
                    }
                    editReason = editReason.trim();
                }

                dao.updatePendingOrder(poId, u.getId(), supplier, qty, total,
                        note != null ? note.trim() : "", editReason);
                session.setAttribute("workflowMessage", "Đã cập nhật thông tin phiếu nhập PO-" + poId + " thành công!");

            } else if ("receive".equals(action)) {
                int poId = parsePositiveId(r.getParameter("poId"), "Mã phiếu nhập");
                String[] itemIds = r.getParameterValues("itemId");
                if (itemIds == null || itemIds.length == 0 || itemIds.length > 100) {
                    throw new IllegalArgumentException("Thiếu danh sách nguyên liệu kiểm nhận.");
                }

                List<PurchaseReceipt.Item> items = new ArrayList<>();
                BigDecimal totalDelivered = BigDecimal.ZERO;

                for (String val : itemIds) {
                    int itemId = parsePositiveId(val, "Mã dòng nguyên liệu");
                    BigDecimal accepted = parseDecimalParam(r.getParameter("accepted_" + itemId), "Số lượng thực nhận", true);
                    String accError = validateQuantity(accepted, true, "Số lượng thực nhận");
                    if (accError != null) throw new IllegalArgumentException(accError);
                    if (accepted.scale() > MAX_QUANTITY_SCALE) accepted = accepted.stripTrailingZeros();

                    // Nếu form chỉ gửi 1 ô số lượng thực nhận thì tự lấy SL giao bằng SL thực nhận
                    String rawDelivered = r.getParameter("delivered_" + itemId);
                    BigDecimal delivered;
                    if (rawDelivered == null || rawDelivered.isBlank()) {
                        delivered = accepted;
                    } else {
                        delivered = parseDecimalParam(rawDelivered, "Số lượng NCC giao tới", true);
                        String delError = validateQuantity(delivered, true, "Số lượng NCC giao tới");
                        if (delError != null) throw new IllegalArgumentException(delError);
                        if (delivered.scale() > MAX_QUANTITY_SCALE) delivered = delivered.stripTrailingZeros();
                        if (accepted.compareTo(delivered) > 0) {
                            throw new IllegalArgumentException("Số lượng thực nhận đạt không được vượt quá số lượng NCC giao tới.");
                        }
                    }

                    totalDelivered = totalDelivered.add(delivered);
                    items.add(new PurchaseReceipt.Item(itemId, delivered, accepted));
                }

                if (totalDelivered.signum() <= 0) {
                    throw new IllegalArgumentException("Cần có ít nhất một nguyên liệu có số lượng thực nhận lớn hơn 0.");
                }

                String note = r.getParameter("note");
                if (note == null || note.isBlank()) {
                    note = "Kiểm nhận và nhập kho nguyên liệu";
                } else {
                    String noteError = validateRequiredReason(note, "Ghi chú kiểm nhận");
                    if (noteError != null) throw new IllegalArgumentException(noteError);
                    note = note.trim();
                }

                receipts.confirm(poId, u.getId(), r.getParameter("submissionKey"), note, items,
                        "yes".equalsIgnoreCase(r.getParameter("closeRemainder")));
                session.setAttribute("workflowMessage", "Đã kiểm nhận và cộng tồn kho cho phiếu PO-" + poId + " thành công!");

            } else if ("adjust".equals(action)) {
                int poId = parsePositiveId(r.getParameter("poId"), "Mã phiếu nhập");
                BigDecimal delta = parseDecimalParam(r.getParameter("delta"), "Số lượng thay đổi", false);
                if (delta.signum() == 0) {
                    throw new IllegalArgumentException("Số lượng điều chỉnh phải khác 0.");
                }
                String deltaError = validateQuantity(delta.abs(), false, "Số lượng điều chỉnh");
                if (deltaError != null) {
                    throw new IllegalArgumentException(deltaError);
                }
                if (delta.abs().scale() > MAX_QUANTITY_SCALE) {
                    delta = delta.stripTrailingZeros();
                }

                String reason = r.getParameter("reason");
                String reasonError = validateRequiredReason(reason, "Lý do điều chỉnh");
                if (reasonError != null) {
                    throw new IllegalArgumentException(reasonError);
                }
                reason = reason.trim();

                String receiptItemParam = r.getParameter("receiptItemId");
                if (receiptItemParam != null && !receiptItemParam.isBlank() && !"0".equals(receiptItemParam.trim())) {
                    int receiptItemId = parsePositiveId(receiptItemParam, "Mã dòng kiểm nhận");
                    receipts.adjust(receiptItemId, u.getId(), UUID.randomUUID().toString(), delta, reason);
                } else {
                    int ingredientId = parsePositiveId(r.getParameter("ingredientId"), "Nguyên liệu");
                    boolean isIncrease = delta.signum() > 0;
                    inventoryDAO.recordStockAdjustment(
                            ingredientId,
                            "ADJUSTMENT",
                            delta.abs(),
                            isIncrease,
                            "PURCHASE_ORDER",
                            poId,
                            (isIncrease ? "Bổ sung sau nhập PO-" + poId + ": " : "Giảm/Trả hàng PO-" + poId + ": ") + reason
                    );
                }
                session.setAttribute("workflowMessage", "Đã điều chỉnh số lượng tồn kho cho phiếu PO-" + poId + " thành công!");

            } else if ("order".equals(action) || "cancel".equals(action) || "close".equals(action)) {
                int poId = parsePositiveId(r.getParameter("id"), "Mã phiếu nhập");
                String reason = r.getParameter("reason");
                if (reason == null || reason.isBlank()) {
                    reason = "order".equals(action) ? "Đã đặt hàng với nhà cung cấp"
                            : "cancel".equals(action) ? "Hủy phiếu nhập hàng" : "Chốt đóng phần còn thiếu";
                } else {
                    String reasonError = validateOptionalText(reason, "Lý do thao tác");
                    if (reasonError != null) throw new IllegalArgumentException(reasonError);
                    reason = reason.trim();
                }
                dao.changeStatus(poId, u.getId(), action, reason);
                session.setAttribute("workflowMessage", "Đã cập nhật trạng thái phiếu PO-" + poId + " thành công!");
            } else {
                session.setAttribute("workflowError", "Thao tác không được hỗ trợ.");
            }
        } catch (NumberFormatException e) {
            getServletContext().log("Purchase action failed", e);
            session.setAttribute("workflowError", purchaseErrorMessage(e));
        } catch (IllegalArgumentException e) {
            session.setAttribute("workflowError", e.getMessage());
        } catch (Exception e) {
            getServletContext().log("Purchase action failed", e);
            session.setAttribute("workflowError", purchaseErrorMessage(e));
        }
        p.sendRedirect(r.getContextPath() + "/admin/purchases");
    }

    static List<PurchaseOrderItem> parseItems(HttpServletRequest request) {
        String[] ids = request.getParameterValues("ingredientId");
        String[] quantities = request.getParameterValues("quantity");
        String[] totals = request.getParameterValues("totalPrice");
        if (ids == null || quantities == null || totals == null || ids.length == 0 || ids.length > 100
                || ids.length != quantities.length || ids.length != totals.length) {
            throw new IllegalArgumentException("Danh sách nguyên liệu, số lượng và tiền lô không khớp.");
        }

        List<PurchaseOrderItem> items = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int i = 0; i < ids.length; i++) {
            int id = parsePositiveId(ids[i], "Nguyên liệu");
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Nguyên liệu không hợp lệ hoặc bị trùng.");
            }

            BigDecimal quantity = parseDecimalParam(quantities[i], "Số lượng đặt", false);
            String qtyError = validateQuantity(quantity, false, "Số lượng đặt");
            if (qtyError != null) {
                throw new IllegalArgumentException(qtyError);
            }
            if (quantity.scale() > MAX_QUANTITY_SCALE) {
                quantity = quantity.stripTrailingZeros();
            }

            BigDecimal total = parseDecimalParam(totals[i], "Tổng tiền lô", false);
            String moneyError = validateMoney(total, "Tổng tiền lô");
            if (moneyError != null) {
                throw new IllegalArgumentException(moneyError);
            }
            if (total.scale() > MAX_MONEY_SCALE) {
                total = total.stripTrailingZeros();
            }

            grandTotal = grandTotal.add(total);
            items.add(new PurchaseOrderItem(id, quantity, InventoryRules.purchaseUnitPrice(quantity, total)));
        }

        String totalError = validateMoney(grandTotal, "Tổng tiền toàn phiếu");
        if (totalError != null) {
            throw new IllegalArgumentException(totalError);
        }
        return items;
    }

    // Kiểm tra số lượng khớp DECIMAL(12,3): lớn hơn 0 (hoặc >= 0 nếu allowZero), tối đa 999,999,999.999 và 3 số lẻ.
    static String validateQuantity(BigDecimal value, boolean allowZero, String fieldLabel) {
        if (value == null) {
            return fieldLabel + " không hợp lệ.";
        }
        if (allowZero ? value.compareTo(BigDecimal.ZERO) < 0 : value.compareTo(BigDecimal.ZERO) <= 0) {
            return fieldLabel + (allowZero ? " không được nhỏ hơn 0." : " phải lớn hơn 0.");
        }
        if (value.compareTo(MAX_QUANTITY) > 0) {
            return fieldLabel + " quá lớn (tối đa 999,999,999.999).";
        }
        if (value.stripTrailingZeros().scale() > MAX_QUANTITY_SCALE) {
            return fieldLabel + " chỉ được có tối đa 3 chữ số thập phân.";
        }
        return null;
    }

    // Kiểm tra số tiền khớp DECIMAL(12,2): >= 0, tối đa 9,999,999,999.99 đ và 2 số lẻ.
    static String validateMoney(BigDecimal value, String fieldLabel) {
        if (value == null) {
            return fieldLabel + " không hợp lệ.";
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return fieldLabel + " không được nhỏ hơn 0.";
        }
        if (value.compareTo(MAX_MONEY) > 0) {
            return fieldLabel + " quá lớn (tối đa 9,999,999,999.99 đ).";
        }
        if (value.stripTrailingZeros().scale() > MAX_MONEY_SCALE) {
            return fieldLabel + " chỉ được có tối đa 2 chữ số thập phân.";
        }
        return null;
    }

    // Kiểm tra văn bản không bắt buộc, tối đa 500 ký tự.
    static String validateOptionalText(String rawText, String fieldLabel) {
        if (rawText != null && rawText.trim().length() > MAX_TEXT_LENGTH) {
            return fieldLabel + " không được vượt quá 500 ký tự.";
        }
        return null;
    }

    // Kiểm tra lý do / ghi chú bắt buộc từ 3 đến 500 ký tự.
    static String validateRequiredReason(String rawText, String fieldLabel) {
        if (rawText == null) {
            return fieldLabel + " phải từ 3 đến 500 ký tự.";
        }
        int len = rawText.trim().length();
        if (len < MIN_REASON_LENGTH || len > MAX_TEXT_LENGTH) {
            return fieldLabel + " phải từ 3 đến 500 ký tự.";
        }
        return null;
    }

    // Đọc mã ID số nguyên dương (> 0).
    private static int parsePositiveId(String rawId, String fieldLabel) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " không hợp lệ.");
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                throw new IllegalArgumentException(fieldLabel + " không hợp lệ.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " không hợp lệ.");
        }
    }

    // Đọc chuỗi số thập phân; nếu blankAsZero = true thì chuỗi trống được tính là 0.
    private static BigDecimal parseDecimalParam(String raw, String fieldLabel, boolean blankAsZero) {
        if (raw == null || raw.isBlank()) {
            if (blankAsZero) {
                return BigDecimal.ZERO;
            }
            throw new IllegalArgumentException(fieldLabel + " không được để trống.");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " phải là một số hợp lệ.");
        }
    }

    // Chuyển lỗi DB / định dạng sang thông báo tiếng Việt rõ ràng, không lộ chi tiết kỹ thuật.
    static String purchaseErrorMessage(Exception error) {
        if (error instanceof java.sql.SQLException sql) {
            String state = sql.getSQLState();
            if ("45000".equals(state)) {
                return sql.getMessage();
            }
            if ("22003".equals(state)) {
                return "Giá trị số lượng, tổng tiền hoặc tồn kho sau cập nhật vượt quá giới hạn lưu trữ.";
            }
            if ("22001".equals(state)) {
                return "Nội dung ghi chú hoặc lý do vượt quá độ dài tối đa 500 ký tự.";
            }
            if ("23000".equals(state)) {
                return "Dữ liệu phiếu nhập không hợp lệ hoặc bị trùng với bản ghi hiện có.";
            }
            if (sql.getErrorCode() == 3819) {
                return "Dữ liệu không thỏa mãn ràng buộc hợp lệ (số lượng, tổng tiền hoặc tồn kho).";
            }
            return "Không thể xử lý phiếu nhập hàng. Vui lòng thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.";
        }
        if (error instanceof NumberFormatException) {
            return "Mã phiếu, số lượng hoặc tổng tiền nhập vào không hợp lệ.";
        }
        return "Không thể xử lý thao tác phiếu nhập hàng. Vui lòng kiểm tra lại dữ liệu.";
    }
}
