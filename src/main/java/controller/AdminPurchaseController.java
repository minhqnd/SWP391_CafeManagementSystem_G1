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

        try {
            String action = r.getParameter("action");
            if ("createDraft".equals(action) || "create".equals(action)) {
                List<PurchaseOrderItem> items = parseItems(r);

                int supplier = Integer.parseInt(r.getParameter("supplierId"));
                PurchaseOrder po = new PurchaseOrder();
                po.setSupplierId(supplier);
                po.setCreatorId(u.getId());
                String note = r.getParameter("note");
                po.setNote(InventoryRules.text(note == null ? "" : note, false));
                String rr = r.getParameter("restockRequestId");
                if (rr != null && !rr.isBlank()) {
                    po.setRestockRequestId(Integer.parseInt(rr));
                }

                boolean draft = "createDraft".equals(action) || "draft".equalsIgnoreCase(r.getParameter("saveMode"));
                int poId = dao.create(po, items, !draft);
                String result = draft ? "Đã lưu phiếu nháp PO-" : "Đã đặt hàng cho PO-";
                r.getSession().setAttribute("workflowMessage", result + poId + ".");

            } else if ("edit".equals(action)) {
                int poId = Integer.parseInt(r.getParameter("id"));
                BigDecimal qty = InventoryRules.quantity(new BigDecimal(r.getParameter("quantity").trim()), true);
                BigDecimal total = InventoryRules.money(new BigDecimal(r.getParameter("totalPrice").trim()));
                int supplier = Integer.parseInt(r.getParameter("supplierId"));
                String note = r.getParameter("note");
                String editReason = r.getParameter("editReason");
                if (editReason == null || editReason.trim().length() < 3) {
                    editReason = "Cập nhật thông tin phiếu nhập";
                }
                dao.updatePendingOrder(poId, u.getId(), supplier, qty, total, note, editReason);
                r.getSession().setAttribute("workflowMessage", "Đã cập nhật thông tin phiếu nhập PO-" + poId + " thành công!");

            } else if ("receive".equals(action)) {
                int poId = Integer.parseInt(r.getParameter("poId"));
                String[] itemIds = r.getParameterValues("itemId");
                if (itemIds == null || itemIds.length == 0) {
                    throw new IllegalArgumentException("Thiếu danh sách nguyên liệu kiểm nhận.");
                }
                List<PurchaseReceipt.Item> items = new ArrayList<>();
                for (String val : itemIds) {
                    int itemId = Integer.parseInt(val.trim());
                    String delStr = r.getParameter("delivered_" + itemId);
                    String accStr = r.getParameter("accepted_" + itemId);
                    BigDecimal accepted = new BigDecimal(accStr != null && !accStr.isBlank() ? accStr.trim() : "0");
                    BigDecimal delivered = new BigDecimal(delStr != null && !delStr.isBlank() ? delStr.trim() : "0");
                    items.add(new PurchaseReceipt.Item(itemId, delivered, accepted));
                }

                String note = r.getParameter("note");
                if (note == null || note.trim().length() < 3) {
                    note = "Kiểm nhận và nhập kho nguyên liệu";
                }
                receipts.confirm(poId, u.getId(), r.getParameter("submissionKey"), note.trim(), items,
                        "yes".equalsIgnoreCase(r.getParameter("closeRemainder")));
                r.getSession().setAttribute("workflowMessage", "Đã kiểm nhận và cộng tồn kho cho phiếu PO-" + poId + " thành công!");

            } else if ("adjust".equals(action)) {
                int poId = Integer.parseInt(r.getParameter("poId"));
                BigDecimal delta = new BigDecimal(r.getParameter("delta").trim());
                if (delta.signum() == 0) {
                    throw new IllegalArgumentException("Số lượng điều chỉnh phải khác 0.");
                }
                String reason = r.getParameter("reason");
                if (reason == null || reason.trim().length() < 3) {
                    reason = "Điều chỉnh số lượng sau khi nhận hàng PO-" + poId;
                }
                String receiptItemParam = r.getParameter("receiptItemId");
                if (receiptItemParam != null && !receiptItemParam.isBlank() && !"0".equals(receiptItemParam.trim())) {
                    receipts.adjust(Integer.parseInt(receiptItemParam.trim()), u.getId(),
                            UUID.randomUUID().toString(), delta, reason.trim());
                } else {
                    int ingredientId = Integer.parseInt(r.getParameter("ingredientId"));
                    boolean isIncrease = delta.signum() > 0;
                    inventoryDAO.recordStockAdjustment(
                            ingredientId,
                            "ADJUSTMENT",
                            delta.abs(),
                            isIncrease,
                            "PURCHASE_ORDER",
                            poId,
                            (isIncrease ? "Bổ sung sau nhập PO-" + poId + ": " : "Giảm/Trả hàng PO-" + poId + ": ") + reason.trim()
                    );
                }
                r.getSession().setAttribute("workflowMessage", "Đã điều chỉnh số lượng tồn kho cho phiếu PO-" + poId + " thành công!");

            } else {
                int poId = Integer.parseInt(r.getParameter("id"));
                String reason = r.getParameter("reason");
                if (reason == null || reason.trim().length() < 3) {
                    reason = "order".equals(action) ? "Đã đặt hàng với nhà cung cấp"
                            : "cancel".equals(action) ? "Hủy phiếu nhập hàng" : "Chốt đóng phần còn thiếu";
                }
                dao.changeStatus(poId, u.getId(), action, reason);
                r.getSession().setAttribute("workflowMessage", "Đã cập nhật trạng thái phiếu PO-" + poId + " thành công!");
            }
        } catch (Exception e) {
            getServletContext().log("Purchase action failed", e);
            r.getSession().setAttribute("workflowError",
                    e.getMessage() != null ? e.getMessage() : "Không thể xử lý thao tác. Vui lòng kiểm tra lại dữ liệu.");
        }
        p.sendRedirect(r.getContextPath() + "/admin/purchases");
    }

    static List<PurchaseOrderItem> parseItems(HttpServletRequest request) {
        String[] ids=request.getParameterValues("ingredientId");
        String[] quantities=request.getParameterValues("quantity");
        String[] totals=request.getParameterValues("totalPrice");
        if(ids==null || quantities==null || totals==null || ids.length==0 || ids.length>100
                || ids.length!=quantities.length || ids.length!=totals.length)
            throw new IllegalArgumentException("Danh sách nguyên liệu, số lượng và tiền lô không khớp.");
        List<PurchaseOrderItem> items=new ArrayList<>();
        Set<Integer> seen=new HashSet<>();
        BigDecimal grandTotal=BigDecimal.ZERO;
        try {
            for(int i=0;i<ids.length;i++) {
                int id=Integer.parseInt(ids[i]);
                if(id<=0 || !seen.add(id))throw new IllegalArgumentException("Nguyên liệu không hợp lệ hoặc bị trùng.");
                BigDecimal quantity=InventoryRules.quantity(new BigDecimal(quantities[i]),true);
                BigDecimal total=InventoryRules.money(new BigDecimal(totals[i]));
                grandTotal=grandTotal.add(total);
                items.add(new PurchaseOrderItem(id,quantity,InventoryRules.purchaseUnitPrice(quantity,total)));
            }
        } catch(NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Số lượng hoặc tiền lô không hợp lệ.",e);
        }
        InventoryRules.money(grandTotal);
        return items;
    }
}
