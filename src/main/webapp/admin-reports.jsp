<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%-- Trang chỉ hiển thị qua AdminReportsController (/admin/reports) --%>
<c:if test="${empty report}"><c:redirect url="/admin/reports" /></c:if>
<c:set var="r" value="${report}" />
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Aura Café | Báo cáo kinh doanh</title>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Material+Symbols+Outlined" rel="stylesheet"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet"><link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
<style>.report-page{max-width:1280px;color:#292524}.panel{background:#fff;border:1px solid #e7e5e4;border-radius:16px;box-shadow:0 1px 2px #1c191708}.head{padding:20px}.db{padding:3px 8px;border-radius:5px;background:#f5f5f4;color:#78716c;font:700 10px monospace}.head h1{font-size:24px;font-weight:800;margin:6px 0 3px}.head p{font-size:12px;color:#78716c}.filter{height:36px;border:1px solid #e7e5e4;border-radius:12px;background:#fafaf9;font-size:12px;padding:0 12px}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;padding-top:14px;border-top:1px solid #f5f5f4}.stat{padding:12px;background:#fafaf9;border:1px solid #e7e5e4;border-radius:12px}.stat.primary{background:#fff7f7;border-color:#fecdd3;color:#a51225}.stat.success{background:#ecfdf5;border-color:#a7f3d0;color:#047857}.stat span{display:block;font-size:10px;font-weight:800;text-transform:uppercase;opacity:.7}.stat strong{display:block;font:900 20px monospace}.stat small{font-size:10px}.box{padding:20px}.box h2{font-size:14px;font-weight:800}.chart{height:210px;border-bottom:1px solid #e7e5e4;display:flex;align-items:end;gap:6px;padding:0 4px 8px;overflow-x:auto}.bar-wrap{height:100%;flex:1;min-width:26px;display:flex;flex-direction:column;justify-content:end;align-items:center;gap:4px;color:#78716c;font:10px monospace}.bar{width:100%;max-width:56px;background:#c85b68;border-radius:8px 8px 0 0;min-height:2px}.bar.zero{background:#e7e5e4}.type-card,.status-row,.task{padding:10px 12px;background:#fafaf9;border-radius:10px}.type-card small{display:block;color:#a8a29e;font-size:10px;font-weight:800;text-transform:uppercase}.progress{height:6px;background:#f5f5f4}.progress-bar{background:#a51225}.rank{width:21px;height:21px;border-radius:50%;display:inline-grid;place-items:center;background:#a51225;color:#fff;font-size:10px;font-style:normal}.task{display:flex;justify-content:space-between;border:0;width:100%;margin-bottom:8px;text-align:left;color:inherit;text-decoration:none}.task.danger{background:#fff1f2;color:#9f1239}.task.warn{background:#fffbeb;color:#92400e}.range-tabs{display:flex;flex-wrap:wrap;gap:6px}.range-tabs a{padding:7px 12px;border:1px solid #e7e5e4;border-radius:999px;font-size:12px;font-weight:700;color:#57534e;text-decoration:none;background:#fff}.range-tabs a.active{background:#a51225;border-color:#a51225;color:#fff}.empty-note{font-size:12px;color:#a8a29e}@media(max-width:900px){.stats{grid-template-columns:repeat(2,1fr)}}</style></head>
<body class="admin-body"><jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="reports"/></jsp:include><main class="admin-main-wrapper report-page">

<section class="panel head mb-4">
    <div class="d-flex justify-content-between flex-wrap gap-3">
        <div>
            <span class="db">Bảng: orders, order_items, payments, inventory_transactions, ingredients</span>
            <h1>Báo cáo kinh doanh</h1>
            <p class="mb-0">Từ <strong>${fromDisplay}</strong> đến <strong>${toDisplay}</strong>. Doanh thu tính theo các thanh toán thành công trong khoảng ngày.</p>
        </div>
        <div class="d-grid gap-2" style="justify-items:end">
            <nav class="range-tabs" aria-label="Khoảng thời gian">
                <a class="${range eq 'today' ? 'active' : ''}" href="${ctx}/admin/reports?range=today">Hôm nay</a>
                <a class="${range eq '7d' ? 'active' : ''}" href="${ctx}/admin/reports?range=7d">7 ngày</a>
                <a class="${range eq '30d' ? 'active' : ''}" href="${ctx}/admin/reports?range=30d">30 ngày</a>
                <a class="${range eq 'month' ? 'active' : ''}" href="${ctx}/admin/reports?range=month">Tháng này</a>
            </nav>
            <form method="get" action="${ctx}/admin/reports" class="d-flex flex-wrap gap-2 align-items-center">
                <input type="hidden" name="range" value="custom">
                <input type="date" class="filter" name="from" value="${from}" required aria-label="Từ ngày">
                <span class="text-muted small">→</span>
                <input type="date" class="filter" name="to" value="${to}" required aria-label="Đến ngày">
                <button type="submit" class="filter fw-bold ${range eq 'custom' ? 'text-white' : ''}" style="${range eq 'custom' ? 'background:#a51225;border-color:#a51225' : ''}">Xem</button>
            </form>
        </div>
    </div>
    <c:if test="${not empty reportError}"><div class="alert alert-danger py-2 mt-3 mb-0"><c:out value="${reportError}" /></div></c:if>
    <div class="stats mt-3">
        <div class="stat primary"><span>Doanh thu</span><strong><fmt:formatNumber value="${r.revenue}" pattern="#,##0" />đ</strong><small>${r.paidOrderCount} đơn đã thanh toán</small></div>
        <div class="stat"><span>Số đơn tạo mới</span><strong>${r.totalOrders}</strong><small>${r.completedOrders} hoàn thành • ${r.cancelledOrders} huỷ</small></div>
        <div class="stat"><span>Giá trị TB / đơn</span><strong><fmt:formatNumber value="${r.averageOrderValue}" pattern="#,##0" />đ</strong><small>Doanh thu ÷ số đơn đã thanh toán</small></div>
        <div class="stat success"><span>Đang xử lý</span><strong>${r.processingOrders}</strong><small>Chờ pha, đang pha, sẵn sàng, đang giao</small></div>
    </div>
</section>

<div class="row g-4 mb-4">
    <div class="col-xl-8">
        <section class="panel box h-100">
            <h2>Doanh thu theo ${fn:length(r.revenueByDate) > 0 and fn:length(r.revenueByDate[0].key) == 7 ? 'tháng' : 'ngày'}</h2>
            <div class="chart">
                <c:forEach var="d" items="${r.revenueByDate}">
                    <div class="bar-wrap" title="${d.label}: ${d.count} đơn">
                        <c:if test="${d.amount > 0}"><span><fmt:formatNumber value="${d.amount / 1000}" pattern="#,##0" />k</span></c:if>
                        <div class="bar ${d.amount > 0 ? '' : 'zero'}" style="height:${d.amount > 0 ? d.percent : 1}%"></div>
                        <span>${d.label}</span>
                    </div>
                </c:forEach>
            </div>
            <div class="row g-3 mt-2">
                <c:forEach var="t" items="${r.revenueByType}">
                    <div class="col-sm-4"><div class="type-card"><small><c:out value="${t.label}" /></small><strong class="font-monospace"><fmt:formatNumber value="${t.amount}" pattern="#,##0" />đ</strong><span class="d-block text-muted">${t.count} đơn</span></div></div>
                </c:forEach>
            </div>
        </section>
    </div>
    <div class="col-xl-4">
        <section class="panel box h-100">
            <div class="d-flex justify-content-between border-bottom pb-2 mb-3"><h2 class="mb-0">Top món bán chạy</h2><a href="${ctx}/admin/products" class="text-danger fw-bold text-decoration-none">Sản phẩm →</a></div>
            <c:choose>
                <c:when test="${empty r.topProducts}"><p class="empty-note">Chưa có món nào được bán trong khoảng này.</p></c:when>
                <c:otherwise>
                    <c:forEach var="p" items="${r.topProducts}" varStatus="st">
                        <div class="mb-3">
                            <div class="d-flex justify-content-between gap-2"><span><i class="rank">${st.index + 1}</i> <strong><c:out value="${p.label}" /></strong></span><span class="font-monospace text-nowrap">${p.count} phần</span></div>
                            <div class="progress mt-1"><div class="progress-bar" style="width:${p.percent}%"></div></div>
                            <small class="text-muted"><fmt:formatNumber value="${p.amount}" pattern="#,##0" />đ</small>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>

<div class="row g-4 mb-4">
    <div class="col-lg-4">
        <section class="panel box h-100">
            <h2>Thanh toán thành công theo phương thức</h2>
            <c:choose>
                <c:when test="${empty r.paymentTotals}"><p class="empty-note mt-3">Chưa có thanh toán.</p></c:when>
                <c:otherwise>
                    <c:forEach var="p" items="${r.paymentTotals}">
                        <div class="d-flex justify-content-between mt-3"><span><c:out value="${p.label}" /> <small class="text-muted">(${p.count})</small></span><strong class="font-monospace"><fmt:formatNumber value="${p.amount}" pattern="#,##0" />đ</strong></div>
                        <div class="progress mt-1"><div class="progress-bar bg-warning" style="width:${p.percent}%"></div></div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
    <div class="col-lg-4">
        <section class="panel box h-100">
            <h2>Đơn theo trạng thái</h2>
            <c:set var="s" value="${r.ordersByStatus}" />
            <div class="status-row d-flex justify-content-between mt-3"><span>Chờ thanh toán</span><strong>${s['PENDING']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Chờ pha chế</span><strong>${s['CONFIRMED']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Đang pha chế</span><strong>${s['PREPARING']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Sẵn sàng</span><strong>${s['READY']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Đang giao</span><strong>${s['DELIVERING']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Hoàn thành</span><strong>${s['COMPLETED']}</strong></div>
            <div class="status-row d-flex justify-content-between mt-2"><span>Đã huỷ</span><strong>${s['CANCELLED']}</strong></div>
        </section>
    </div>
    <div class="col-lg-4">
        <section class="panel box h-100">
            <h2>Cần xử lý</h2>
            <a class="task danger mt-3" href="${ctx}/admin/inventory"><span>Nguyên liệu dưới mức tối thiểu</span><strong>${fn:length(r.lowStock)}</strong></a>
            <a class="task" href="${ctx}/admin/restock-requests"><span>Đề xuất nhập hàng chờ duyệt</span><strong>${r.pendingRestockRequests}</strong></a>
            <a class="task warn" href="${ctx}/admin/feedback"><span>Đánh giá / khiếu nại chờ xử lý</span><strong>${r.pendingFeedback}</strong></a>
        </section>
    </div>
</div>

<div class="row g-4">
    <div class="col-lg-6">
        <section class="panel box h-100">
            <h2>Nguyên liệu xuất cho đơn nhiều nhất</h2>
            <c:choose>
                <c:when test="${empty r.topIngredientsUsed}"><p class="empty-note mt-3">Chưa có nguyên liệu nào được xuất cho đơn trong khoảng này.</p></c:when>
                <c:otherwise>
                    <c:forEach var="i" items="${r.topIngredientsUsed}">
                        <div class="d-flex justify-content-between mt-3"><span><c:out value="${i.label}" /></span><strong class="font-monospace"><fmt:formatNumber value="${i.amount}" pattern="#,##0.###" /> <c:out value="${i.key}" /></strong></div>
                        <div class="progress mt-1"><div class="progress-bar" style="width:${i.percent}%"></div></div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
    <div class="col-lg-6">
        <section class="panel box h-100">
            <div class="d-flex justify-content-between"><h2>Tồn kho dưới mức tối thiểu (hiện tại)</h2><a href="${ctx}/admin/inventory" class="text-danger fw-bold text-decoration-none small">Tồn kho →</a></div>
            <c:choose>
                <c:when test="${empty r.lowStock}"><p class="empty-note mt-3">Tất cả nguyên liệu đều trên mức tối thiểu.</p></c:when>
                <c:otherwise>
                    <table class="table table-sm align-middle mt-2 mb-0">
                        <thead><tr><th>Nguyên liệu</th><th class="text-end">Tồn</th><th class="text-end">Tối thiểu</th></tr></thead>
                        <tbody>
                            <c:forEach var="l" items="${r.lowStock}">
                                <tr><td><c:out value="${l.name}" /></td>
                                    <td class="text-end font-monospace text-danger"><fmt:formatNumber value="${l.currentStock}" pattern="#,##0.###" /> <c:out value="${l.unit}" /></td>
                                    <td class="text-end font-monospace"><fmt:formatNumber value="${l.minStockLevel}" pattern="#,##0.###" /> <c:out value="${l.unit}" /></td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>
</main></body></html>
