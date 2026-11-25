<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Aura Café | Quản lý phiếu nhập hàng</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width: 1600px; display: flex; flex-direction: column; gap: 18px; }
        .overview-card, .data-card, .filter-card { background: #fff; border: 1px solid #e7e5e4; border-radius: 16px; box-shadow: 0 1px 2px rgba(28,25,23,.04); }
        .overview-card { padding: 22px; }
        .overview-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; flex-wrap: wrap; }
        .table-ref { display: inline-block; padding: 3px 8px; border-radius: 6px; background: #f5f5f4; color: #78716c; font: 700 11px/1.5 ui-monospace,monospace; }
        .page-title { margin: 6px 0 3px; color: #1c1917; font-size: 1.55rem; font-weight: 800; letter-spacing: -.025em; }
        .page-subtitle { margin: 0; color: #78716c; font-size: .84rem; }
        .stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding-top: 16px; margin-top: 18px; border-top: 1px solid #f5f5f4; }
        .stat-item { padding: 12px 16px; border: 1px solid #e7e5e4; border-radius: 12px; background: #fafaf9; }
        .stat-item span { display: block; color: #78716c; font-size: 10.5px; text-transform: uppercase; font-weight: 800; letter-spacing: .03em; }
        .stat-item strong { display: block; margin-top: 3px; color: #1c1917; font: 800 1.4rem ui-monospace,monospace; }
        .stat-item.warning strong { color: #d97706; }
        .stat-item.success strong { color: #059669; }
        .btn-aura { color: #fff; background: #900b1a; border-color: #900b1a; font-weight: 700; font-size: .85rem; border-radius: 10px; padding: .55rem 1.15rem; transition: all .2s; }
        .btn-aura:hover { color: #fff; background: #7a121d; border-color: #7a121d; }
        .filter-card { padding: 14px 18px; }
        .po-table > :not(caption) > * > * { padding: .85rem .8rem; vertical-align: middle; font-size: .85rem; }
        .po-table thead th { color: #78716c; font-size: 11px; font-weight: 800; letter-spacing: .04em; text-transform: uppercase; background: #fafaf9; white-space: nowrap; }
        .po-badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; white-space: nowrap; }
        .po-badge-draft { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; }
        .po-badge-ordered { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
        .po-badge-partial { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
        .po-badge-received { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
        .po-badge-closed { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
        .po-badge-cancelled { background: #fff1f2; color: #be123c; border: 1px solid #fecdd3; }
        .item-line-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 6px 10px; margin-bottom: 4px; font-size: 0.8rem; }
        .item-line-box:last-child { margin-bottom: 0; }
        @media(max-width:991.98px) { .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    </style>
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="purchases"/></jsp:include>

<main class="admin-main-wrapper">
    <c:if test="${not empty workflowMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="status">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${workflowMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty workflowError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${workflowError}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: purchase_orders · purchase_receipts · inventory_transactions</span>
                <h1 class="page-title">Phiếu nhập hàng &amp; Kiểm nhận kho</h1>
                <p class="page-subtitle">Lập đơn mua nguyên liệu, sửa đơn, kiểm nhận hàng giao (đủ / thiếu / thừa / hàng hỏng) và cộng tồn kho trực tiếp.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <a href="${ctx}/admin/restock-requests" class="btn btn-light border fw-semibold d-flex align-items-center gap-1" style="border-radius: 10px;">
                    <span class="material-symbols-outlined fs-5">notification_important</span>
                    <span>Đề xuất nhập hàng (${fn:length(approvedRequests)})</span>
                </a>
                <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#createPoModal">
                    <span class="material-symbols-outlined fs-5">add_circle</span>
                    <span>Lập phiếu nhập hàng</span>
                </button>
            </div>
        </div>
        <div class="stats-grid">
            <div class="stat-item">
                <span>Tổng số phiếu nhập</span>
                <strong><c:out value="${totalOrdersCount}" default="0" /></strong>
            </div>
            <div class="stat-item warning">
                <span>Chờ nhận / Giao thiếu</span>
                <strong><c:out value="${pendingOrdersCount}" default="0" /></strong>
            </div>
            <div class="stat-item success">
                <span>Đã hoàn tất nhập kho</span>
                <strong><c:out value="${completedOrdersCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Tổng giá trị đặt mua</span>
                <strong><fmt:formatNumber value="${totalSpendAmount}" pattern="#,##0" /> đ</strong>
            </div>
        </div>
    </div>

    <div class="filter-card">
        <div class="row g-2 align-items-center">
            <div class="col-md-7">
                <input type="text" id="poSearchInput" class="form-control" placeholder="Tìm theo mã phiếu (PO-...), nhà cung cấp, nguyên liệu..." oninput="filterPoTable()">
            </div>
            <div class="col-md-5 d-flex gap-2">
                <select id="poStatusFilter" class="form-select" onchange="filterPoTable()">
                    <option value="ALL">Tất cả trạng thái</option>
                    <option value="PENDING">Đang chờ nhận hàng (ORDERED / DRAFT)</option>
                    <option value="PARTIALLY_RECEIVED">Giao thiếu - Chờ giao bù (PARTIALLY_RECEIVED)</option>
                    <option value="COMPLETED">Đã hoàn tất nhập kho (RECEIVED / CLOSED)</option>
                    <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                </select>
            </div>
        </div>
    </div>

    <div class="data-card p-3">
        <div class="table-responsive">
            <table class="table po-table table-hover align-middle mb-0" id="poTable">
                <thead>
                    <tr>
                        <th style="width: 110px;">Mã phiếu</th>
                        <th style="width: 200px;">Nhà cung cấp</th>
                        <th style="min-width: 310px;">Nguyên liệu · Đặt mua vs Thực nhận</th>
                        <th class="text-end" style="width: 140px;">Tổng tiền đặt</th>
                        <th class="text-center" style="width: 165px;">Trạng thái</th>
                        <th class="text-end" style="min-width: 240px;">Thao tác xử lý</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="po" items="${purchaseOrders}">
                        <tr class="po-row" data-status="${po.status}">
                            <td>
                                <div class="fw-bold font-monospace text-danger">PO-${po.id}</div>
                                <c:if test="${not empty po.restockRequestId}">
                                    <span class="badge bg-light text-secondary border mt-1" style="font-size: 10px;">Từ đề xuất #RR-${po.restockRequestId}</span>
                                </c:if>
                                <div class="text-muted" style="font-size: 11px;">
                                    <fmt:formatDate value="${po.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                </div>
                            </td>
                            <td>
                                <div class="fw-bold text-dark"><c:out value="${po.supplierName}"/></div>
                                <div class="text-muted small">Người lập: <c:out value="${po.creatorName}"/></div>
                                <c:if test="${not empty po.note}">
                                    <div class="text-secondary fst-italic mt-1" style="font-size: 11.5px;"><c:out value="${po.note}"/></div>
                                </c:if>
                            </td>
                            <td>
                                <c:forEach var="it" items="${po.items}">
                                    <div class="item-line-box d-flex justify-content-between align-items-center flex-wrap gap-2">
                                        <div>
                                            <strong class="text-dark"><c:out value="${it.ingredientName}"/></strong>
                                            <span class="text-muted">(${it.unit})</span>
                                            <div class="text-muted" style="font-size: 11px;">
                                                Đơn giá: <fmt:formatNumber value="${it.unitPrice}" pattern="#,##0.##" /> đ/${it.unit}
                                            </div>
                                        </div>
                                        <div class="text-end font-monospace">
                                            <div>Đặt: <strong><fmt:formatNumber value="${it.quantity}" pattern="#,##0.###" /></strong> · Đã nhận: <strong class="text-success"><fmt:formatNumber value="${it.receivedQuantity}" pattern="#,##0.###" /></strong></div>
                                            <c:choose>
                                                <c:when test="${po.status eq 'CANCELLED'}">
                                                    <span class="badge bg-light text-secondary border">Đã hủy</span>
                                                </c:when>
                                                <c:when test="${it.receivedQuantity >= it.quantity and it.receivedQuantity > it.quantity}">
                                                    <span class="badge" style="background:#f5f3ff;color:#7c3aed;border:1px solid #ddd6fe;">
                                                        Nhận thừa +<fmt:formatNumber value="${it.receivedQuantity - it.quantity}" pattern="#,##0.###" /> ${it.unit}
                                                    </span>
                                                </c:when>
                                                <c:when test="${it.receivedQuantity >= it.quantity}">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle">Đã nhận đủ 100%</span>
                                                </c:when>
                                                <c:when test="${it.receivedQuantity > 0}">
                                                    <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                                                        Còn thiếu <fmt:formatNumber value="${it.remainingQuantity}" pattern="#,##0.###" /> ${it.unit}
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-light text-secondary border">Chưa nhận hàng</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </c:forEach>
                            </td>
                            <td class="text-end font-monospace fw-bold text-dark">
                                <fmt:formatNumber value="${po.totalCost}" pattern="#,##0" /> đ
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${po.status eq 'DRAFT'}">
                                        <span class="po-badge po-badge-draft">Bản nháp</span>
                                    </c:when>
                                    <c:when test="${po.status eq 'ORDERED'}">
                                        <span class="po-badge po-badge-ordered">Chờ giao hàng</span>
                                    </c:when>
                                    <c:when test="${po.status eq 'PARTIALLY_RECEIVED'}">
                                        <span class="po-badge po-badge-partial">Giao thiếu (Nhận 1 phần)</span>
                                    </c:when>
                                    <c:when test="${po.status eq 'RECEIVED'}">
                                        <span class="po-badge po-badge-received">Đã nhập kho đủ</span>
                                    </c:when>
                                    <c:when test="${po.status eq 'CLOSED'}">
                                        <span class="po-badge po-badge-closed">Đã chốt đóng phiếu</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="po-badge po-badge-cancelled">Đã hủy</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <div class="d-inline-flex flex-wrap justify-content-end gap-1">
                                    <c:if test="${po.status eq 'ORDERED' or po.status eq 'PARTIALLY_RECEIVED'}">
                                        <button type="button" class="btn btn-sm btn-success fw-semibold d-inline-flex align-items-center gap-1"
                                                data-bs-toggle="modal" data-bs-target="#receiveModal-${po.id}">
                                            <span class="material-symbols-outlined" style="font-size: 16px;">inventory</span>
                                            <span>${po.status eq 'PARTIALLY_RECEIVED' ? 'Nhận bù' : 'Nhận hàng'}</span>
                                        </button>
                                    </c:if>

                                    <c:if test="${(po.status eq 'DRAFT' or po.status eq 'ORDERED') and fn:length(po.items) == 1}">
                                        <button type="button" class="btn btn-sm btn-light border fw-semibold d-inline-flex align-items-center gap-1"
                                                data-bs-toggle="modal" data-bs-target="#editModal-${po.id}" title="Sửa đơn nhập">
                                            <span class="material-symbols-outlined" style="font-size: 16px;">edit</span>
                                            <span>Sửa</span>
                                        </button>
                                    </c:if>

                                    <c:if test="${po.status eq 'DRAFT'}">
                                        <form method="post" action="${ctx}/admin/purchases" class="d-inline">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                                            <input type="hidden" name="id" value="${po.id}">
                                            <button type="submit" class="btn btn-sm btn-outline-primary" name="action" value="order">Đặt hàng</button>
                                        </form>
                                    </c:if>

                                    <c:if test="${po.status eq 'PARTIALLY_RECEIVED'}">
                                        <form method="post" action="${ctx}/admin/purchases" class="d-inline" onsubmit="return confirm('Chốt đóng phần còn thiếu của phiếu PO-${po.id}?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                                            <input type="hidden" name="id" value="${po.id}">
                                            <input type="hidden" name="reason" value="Chốt đóng phần giao thiếu, không nhận thêm">
                                            <button type="submit" class="btn btn-sm btn-outline-secondary" name="action" value="close" title="Chốt phiếu, không chờ giao bù phần thiếu nữa">
                                                Chốt đóng
                                            </button>
                                        </form>
                                    </c:if>

                                    <c:if test="${po.status eq 'RECEIVED' or po.status eq 'PARTIALLY_RECEIVED' or po.status eq 'CLOSED'}">
                                        <button type="button" class="btn btn-sm btn-light border text-secondary d-inline-flex align-items-center gap-1"
                                                data-bs-toggle="modal" data-bs-target="#adjustModal-${po.id}" title="Điều chỉnh tăng/giảm hoặc trả hàng hỏng sau khi đã nhập">
                                            <span class="material-symbols-outlined" style="font-size: 16px;">tune</span>
                                            <span>Điều chỉnh</span>
                                        </button>
                                    </c:if>

                                    <c:if test="${po.status eq 'DRAFT' or po.status eq 'ORDERED'}">
                                        <form method="post" action="${ctx}/admin/purchases" class="d-inline" onsubmit="return confirm('Bạn có chắc muốn hủy phiếu nhập PO-${po.id}?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                                            <input type="hidden" name="id" value="${po.id}">
                                            <input type="hidden" name="reason" value="Quản trị viên hủy phiếu nhập">
                                            <button type="submit" class="btn btn-sm btn-outline-danger" name="action" value="cancel" title="Hủy phiếu">
                                                Hủy
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty purchaseOrders}">
                        <tr>
                            <td colspan="6" class="text-center py-5 text-muted">Chưa có phiếu nhập hàng nào. Hãy nhấn "Lập phiếu nhập hàng" để tạo mới.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<!-- MODAL 1: LẬP PHIẾU NHẬP HÀNG MỚI -->
<div class="modal fade" id="createPoModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow">
            <form method="post" action="${ctx}/admin/purchases" id="createPoForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">Lập phiếu nhập hàng từ Nhà cung cấp</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label class="form-label fw-bold small text-secondary">Nhà cung cấp <span class="text-danger">*</span></label>
                            <select class="form-select" name="supplierId" required>
                                <c:forEach var="s" items="${activeSuppliers}">
                                    <option value="${s.id}"><c:out value="${s.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-bold small text-secondary">Đề xuất đã duyệt (không bắt buộc)</label>
                            <select class="form-select" name="restockRequestId" id="purchaseRequest">
                                <option value="">-- Lập phiếu độc lập (Không liên kết) --</option>
                                <c:forEach var="r" items="${approvedRequests}">
                                    <option value="${r.id}">#RR-${r.id}: <c:out value="${r.itemsSummary}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

<div id="purchaseLines"><jsp:include page="/components/purchase-order-line.jsp"/></div>

                    <div>
                        <label class="form-label fw-bold small text-secondary">Ghi chú phiếu nhập</label>
                        <textarea class="form-control" name="note" rows="2" maxlength="500" placeholder="Ghi chú đợt nhập hàng, hạn sử dụng, chiết khấu..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light d-flex justify-content-between">
                    <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Đóng</button>
                    <div class="d-flex flex-wrap gap-2">
                        <button type="submit" class="btn btn-outline-secondary" name="action" value="createDraft">
                            Lưu nháp
                        </button>
                        <button type="submit" class="btn btn-aura fw-semibold" name="action" value="create">
                            Đặt hàng
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- CÁC MODAL SỬA PHIẾU, KIỂM NHẬN HÀNG (ĐỦ/THIẾU/THỪA), VÀ ĐIỀU CHỈNH CHO TỪNG PO -->
<c:forEach var="po" items="${purchaseOrders}">
    <!-- MODAL SỬA PHIẾU NHẬP -->
    <c:if test="${(po.status eq 'DRAFT' or po.status eq 'ORDERED') and fn:length(po.items) == 1}">
        <div class="modal fade" id="editModal-${po.id}" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow">
                    <form method="post" action="${ctx}/admin/purchases">
                        <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                        <input type="hidden" name="action" value="edit">
                        <input type="hidden" name="id" value="${po.id}">
                        <div class="modal-header bg-light">
                            <h5 class="modal-title fw-bold">Chỉnh sửa phiếu nhập PO-${po.id}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Nguyên liệu</label>
                                <input type="text" class="form-control bg-light" value="<c:out value='${po.items[0].ingredientName}'/> (${po.items[0].unit})" readonly>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Nhà cung cấp <span class="text-danger">*</span></label>
                                <select class="form-select" name="supplierId" required>
                                    <c:forEach var="s" items="${activeSuppliers}">
                                        <option value="${s.id}" ${po.supplierId == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="row g-2 mb-3">
                                <div class="col-6">
                                    <label class="form-label fw-bold small text-secondary">Số lượng đặt (${po.items[0].unit}) <span class="text-danger">*</span></label>
                                    <input class="form-control" name="quantity" value="${po.items[0].quantity}" type="number" step="0.001" min="0.001" max="999999999.999" required>
                                </div>
                                <div class="col-6">
                                    <label class="form-label fw-bold small text-secondary">Tổng tiền (đ) <span class="text-danger">*</span></label>
                                    <input class="form-control" name="totalPrice" value="${po.totalCost}" type="number" min="0" max="9999999999.99" step="0.01" required>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Ghi chú phiếu nhập</label>
                                <textarea class="form-control" name="note" rows="2" maxlength="500"><c:out value="${po.note}"/></textarea>
                            </div>
                            <div>
                                <label class="form-label fw-bold small text-secondary">Lý do chỉnh sửa</label>
                                <input class="form-control" name="editReason" value="Cập nhật số lượng / đơn giá theo báo giá NCC" minlength="3" maxlength="500" required>
                            </div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-aura">Lưu thay đổi</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </c:if>

    <!-- MODAL KIỂM NHẬN HÀNG GIAO (XỬ LÝ ĐỦ / GIAO THIẾU / GIAO THỪA / HÀNG HỎNG) -->
    <c:if test="${po.status eq 'ORDERED' or po.status eq 'PARTIALLY_RECEIVED'}">
        <div class="modal fade" id="receiveModal-${po.id}" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow">
                    <form method="post" action="${ctx}/admin/purchases">
                        <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                        <input type="hidden" name="action" value="receive">
                        <input type="hidden" name="submissionKey" value="${receiptKeys[po.id]}">
                        <input type="hidden" name="poId" value="${po.id}">
                        <div class="modal-header bg-light">
                            <div>
                                <h5 class="modal-title fw-bold mb-0">Kiểm nhận hàng &amp; Cộng tồn kho · PO-${po.id}</h5>
                                <small class="text-muted">Nhà cung cấp: <c:out value="${po.supplierName}"/></small>
                            </div>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <div class="alert alert-light border small mb-3">
                                <strong>Hướng dẫn nhanh:</strong> Mặc định hệ thống điền sẵn <em>Số lượng cần nhận</em>.
                                Nếu NCC <strong>giao thiếu</strong>, hãy giảm số lượng thực nhận. Nếu NCC <strong>giao thừa</strong>, hãy nhập số thực nhận lớn hơn số đặt. Nếu có <strong>hàng hỏng trả lại</strong>, nhập <em>SL thực nhận đạt</em> nhỏ hơn <em>SL giao tới</em>.
                            </div>

                            <c:forEach var="it" items="${po.items}">
                                <c:set var="defaultRecv" value="${it.remainingQuantity}" />
                                <div class="p-3 rounded-3 border bg-light mb-3 receive-item-card"
                                     data-expected="${defaultRecv}" data-unit="${fn:escapeXml(it.unit)}">
                                    <input type="hidden" name="itemId" value="${it.id}">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <div>
                                            <span class="fw-bold text-dark fs-6"><c:out value="${it.ingredientName}"/> (${it.unit})</span>
                                            <div class="text-muted small">
                                                Đặt mua: <strong><fmt:formatNumber value="${it.quantity}" pattern="#,##0.###" /></strong> ·
                                                Đã nhận trước đó: <strong><fmt:formatNumber value="${it.receivedQuantity}" pattern="#,##0.###" /></strong> ·
                                                Cần nhận đợt này: <strong class="text-primary"><fmt:formatNumber value="${defaultRecv}" pattern="#,##0.###" /> ${it.unit}</strong>
                                            </div>
                                        </div>
                                        <span class="badge bg-success-subtle text-success border border-success-subtle diff-status-badge">
                                            ✔ Nhận đủ hàng
                                        </span>
                                    </div>
                                    <div class="row g-3">
                                        <div class="col-md-6">
                                            <label class="form-label fw-bold small text-secondary">Số lượng NCC giao tới (${it.unit})</label>
                                            <input type="number" class="form-control delivered-input" name="delivered_${it.id}"
                                                   min="0" max="999999999.999" step="0.001" value="${defaultRecv}" required oninput="updateReceiveDiff(this)">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label fw-bold small text-success">Thực nhận đạt yêu cầu - Cộng vào kho (${it.unit}) <span class="text-danger">*</span></label>
                                            <input type="number" class="form-control border-success fw-bold accepted-input" name="accepted_${it.id}"
                                                   min="0" max="999999999.999" step="0.001" value="${defaultRecv}" required oninput="updateReceiveDiff(this)">
                                        </div>
                                    </div>
                                    <div class="small mt-2 text-muted diff-detail-text"></div>
                                </div>
                            </c:forEach>

                            <div class="form-check mb-3 p-3 rounded border bg-warning-subtle d-none short-delivery-box">
                                <input class="form-check-input ms-0 me-2" type="checkbox" name="closeRemainder" value="yes" id="closeRem-${po.id}">
                                <label class="form-check-label small fw-semibold text-dark" for="closeRem-${po.id}">
                                    Chốt đóng phiếu luôn sau lần nhận này (Không yêu cầu nhà cung cấp giao bù phần còn thiếu)
                                </label>
                            </div>

                            <div>
                                <label class="form-label fw-bold small text-secondary">Ghi chú kiểm nhận (Tình trạng hàng hóa, lý do thừa/thiếu nếu có)</label>
                                <input type="text" class="form-control" name="note" value="Hàng giao đạt tiêu chuẩn, nhập kho đầy đủ" minlength="3" maxlength="500" required>
                            </div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-success fw-bold d-flex align-items-center gap-1">
                                <span class="material-symbols-outlined fs-5">check_circle</span>
                                <span>Xác nhận &amp; Cộng tồn kho ngay</span>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </c:if>

    <!-- MODAL ĐIỀU CHỈNH SAU NHẬP -->
    <c:if test="${po.status eq 'RECEIVED' or po.status eq 'PARTIALLY_RECEIVED' or po.status eq 'CLOSED'}">
        <div class="modal fade" id="adjustModal-${po.id}" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow">
                    <form method="post" action="${ctx}/admin/purchases">
                        <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                        <input type="hidden" name="action" value="adjust">
                        <input type="hidden" name="poId" value="${po.id}">
                        <div class="modal-header bg-light">
                            <h5 class="modal-title fw-bold">Điều chỉnh sau nhập kho · PO-${po.id}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <p class="small text-muted mb-3">
                                Dùng khi phát hiện hàng lỗi trả lại nhà cung cấp hoặc kiểm đếm lệch sau khi phiếu đã nhập kho. Nhập số âm (VD: <code>-2</code>) để trừ kho trả hàng, số dương (VD: <code>+2</code>) để cộng thêm.
                            </p>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Nguyên liệu thuộc phiếu</label>
                                <select class="form-select" name="ingredientId" required>
                                    <c:forEach var="it" items="${po.items}">
                                        <option value="${it.ingredientId}">
                                            <c:out value="${it.ingredientName}"/> (Đã nhận: <fmt:formatNumber value="${it.receivedQuantity}" pattern="#,##0.###" /> ${it.unit})
                                        </option>
                                    </c:forEach>
                                </select>
                                <c:set var="poReceipts" value="${receiptsByPo[po.id]}" />
                                <c:if test="${not empty poReceipts and not empty poReceipts[0].items}">
                                    <input type="hidden" name="receiptItemId" value="${poReceipts[0].items[0].id}">
                                </c:if>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Số lượng thay đổi (+ / -) <span class="text-danger">*</span></label>
                                <input type="number" step="0.001" min="-999999999.999" max="999999999.999" name="delta" class="form-control" placeholder="VD: -2 (trả lại 2 đơn vị) hoặc 1.5 (bổ sung)" required>
                            </div>
                            <div>
                                <label class="form-label fw-bold small text-secondary">Lý do điều chỉnh / trả hàng <span class="text-danger">*</span></label>
                                <input type="text" name="reason" class="form-control" placeholder="VD: Phát hiện 2 bịch rách bao bì trả lại NCC" minlength="3" maxlength="500" required>
                            </div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-aura">Lưu điều chỉnh tồn kho</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </c:if>
</c:forEach>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function filterPoTable() {
        const q = (document.getElementById('poSearchInput')?.value || '').toLowerCase().trim();
        const statusFilter = document.getElementById('poStatusFilter')?.value || 'ALL';
        document.querySelectorAll('#poTable tbody .po-row').forEach(row => {
            const st = row.dataset.status || '';
            const text = row.innerText.toLowerCase();
            let matchStatus = true;
            if (statusFilter === 'PENDING') {
                matchStatus = (st === 'DRAFT' || st === 'ORDERED');
            } else if (statusFilter === 'PARTIALLY_RECEIVED') {
                matchStatus = (st === 'PARTIALLY_RECEIVED');
            } else if (statusFilter === 'COMPLETED') {
                matchStatus = (st === 'RECEIVED' || st === 'CLOSED');
            } else if (statusFilter === 'CANCELLED') {
                matchStatus = (st === 'CANCELLED');
            }
            const matchQuery = !q || text.includes(q);
            row.style.display = (matchStatus && matchQuery) ? '' : 'none';
        });
    }

    function updateReceiveDiff(inputElem) {
        const card = inputElem.closest('.receive-item-card');
        if (!card) return;
        const expected = parseFloat(card.dataset.expected || '0');
        const unit = card.dataset.unit || '';
        const deliveredInput = card.querySelector('.delivered-input');
        const acceptedInput = card.querySelector('.accepted-input');
        const badge = card.querySelector('.diff-status-badge');
        const detail = card.querySelector('.diff-detail-text');
        const modal = card.closest('.modal-body');
        const shortBox = modal?.querySelector('.short-delivery-box');

        const delivered = parseFloat(deliveredInput?.value || '0');
        const accepted = parseFloat(acceptedInput?.value || '0');
        const diff = accepted - expected;
        const rejected = Math.max(0, delivered - accepted);

        let extraNote = rejected > 0 ? (' · Trả lại/Hàng hỏng từ chối: ' + rejected.toFixed(3).replace(/\.?0+$/, '') + ' ' + unit) : '';

        if (Math.abs(diff) < 0.0005) {
            badge.className = 'badge bg-success-subtle text-success border border-success-subtle diff-status-badge';
            badge.innerText = '✔ Nhận đủ hàng (100%)';
            detail.innerText = 'Khớp đúng số lượng cần nhận' + extraNote;
        } else if (diff < 0) {
            const missing = Math.abs(diff).toFixed(3).replace(/\.?0+$/, '');
            badge.className = 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle diff-status-badge';
            badge.innerText = '⚠ Giao thiếu -' + missing + ' ' + unit;
            detail.innerText = 'Thiếu ' + missing + ' ' + unit + ' so với số lượng đặt. Mặc định phiếu sẽ chuyển sang trạng thái "Nhận một phần (Chờ giao bù)"' + extraNote;
        } else {
            const excess = diff.toFixed(3).replace(/\.?0+$/, '');
            badge.className = 'badge diff-status-badge';
            badge.style.cssText = 'background:#f5f3ff;color:#7c3aed;border:1px solid #ddd6fe;';
            badge.innerText = '⬆ Giao thừa +' + excess + ' ' + unit;
            detail.innerText = 'Nhận vượt đơn đặt +' + excess + ' ' + unit + ' (Hệ thống sẽ cộng toàn bộ số thực nhận vào kho)' + extraNote;
        }
        if (shortBox) {
            const hasShortDelivery = Array.from(modal.querySelectorAll('.receive-item-card')).some(line =>
                Number(line.querySelector('.accepted-input').value) < Number(line.dataset.expected)
            );
            shortBox.classList.toggle('d-none', !hasShortDelivery);
            if (!hasShortDelivery) shortBox.querySelector('input').checked = false;
        }
    }

    // Tự động mở modal tạo phiếu nếu đi từ trang Nguyên liệu hoặc Đề xuất nhập hàng sang
    window.addEventListener('DOMContentLoaded', () => {
        const params = new URLSearchParams(window.location.search);
        const ingId = params.get('ingredientId');
        const reqId = params.get('fromRequestId');
        if (ingId || reqId) {
            if (ingId) {
                const ingSelect = document.querySelector('#purchaseLines select[name=ingredientId]');
                if (ingSelect) ingSelect.value = ingId;
            }
            if (reqId) {
                const reqSelect = document.getElementById('purchaseRequest');
                if (!reqSelect || !Array.from(reqSelect.options).some(option => option.value === reqId)) {
                    return;
                }
                reqSelect.value = reqId;
                reqSelect.dispatchEvent(new Event('change'));
            }
            new bootstrap.Modal(document.getElementById('createPoModal')).show();
        }
    });
</script>
<template id="purchaseLineTemplate"><jsp:include page="/components/purchase-order-line.jsp"/></template>
<c:forEach var="r" items="${approvedRequests}">
    <template id="purchaseRequest-${r.id}">
        <c:forEach var="it" items="${r.items}">
            <fieldset class="purchase-line border rounded p-3 mb-3">
                <legend class="float-none w-auto fs-6"><c:out value="${it.ingredientName}"/> (<c:out value="${it.unit}"/>)</legend>
                <input type="hidden" name="ingredientId" value="${it.ingredientId}">
                <label class="form-label d-block">Số lượng đặt
                    <input class="form-control" name="quantity" type="number" min="0.001" max="999999999.999" step="0.001" value="${it.quantity}" required>
                </label>
                <label class="form-label d-block">Tổng tiền lô của nguyên liệu này (đ)
                    <input class="form-control" name="totalPrice" type="number" min="0" max="9999999999.99" step="0.01" required>
                </label>
            </fieldset>
        </c:forEach>
    </template>
</c:forEach>
<script id="purchase-form-script">
(() => {
    const request = document.getElementById('purchaseRequest');
    const lines = document.getElementById('purchaseLines');
    const blank = document.getElementById('purchaseLineTemplate');
    function selectRequest() {
        const template = request.value
            ? document.getElementById('purchaseRequest-' + request.value) : blank;
        if (!template) return;
        lines.replaceChildren(template.content.cloneNode(true));
    }
    request.addEventListener('change', selectRequest);
    // Reconcile a request selection restored by the browser after a refresh/back navigation.
    if (request.value) selectRequest();
})();
</script>
</body>
</html>
