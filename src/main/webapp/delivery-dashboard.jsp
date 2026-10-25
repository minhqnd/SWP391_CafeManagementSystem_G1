<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Aura Café | Giao hàng</title>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&family=Material+Symbols+Outlined" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<style>
body{background:#f7f7f6;font-family:"Plus Jakarta Sans",sans-serif;color:#292524}.layout{display:flex;min-height:100vh}.side{width:245px;background:#fff;border-right:1px solid #e7e5e4;padding:20px;display:flex;flex-direction:column}.brand{font-size:20px;font-weight:800;color:#a51225}.main{flex:1;padding:28px;max-width:1250px}.cardx{background:#fff;border:1px solid #e7e5e4;border-radius:16px;padding:18px;box-shadow:0 1px 2px #00000008}.badge-aura{background:#a51225;color:#fff}.muted{color:#78716c}.btn-aura{background:#a51225;color:#fff;font-weight:700}.btn-aura:hover{background:#831020;color:#fff}.logout{margin-top:auto;color:#a51225;text-decoration:none;font-weight:700}.code{font-family:monospace;font-weight:800;color:#a51225}.money{font-weight:800;color:#a51225}@media(max-width:800px){.layout{display:block}.side{width:100%;min-height:auto}.main{padding:16px}}
</style></head>
<body><div class="layout">
<aside class="side"><div class="brand">Aura Café</div><div class="small text-uppercase text-muted mb-4">Delivery Portal</div>
<div class="cardx mb-3"><b><c:out value="${sessionScope.user.fullName}"/></b><div class="muted"><c:out value="${sessionScope.user.phone}"/></div><div class="mt-2 text-success fw-bold">SHIPPER • ACTIVE</div></div>
<a class="logout" href="${ctx}/login?action=logout"><span class="material-symbols-outlined align-middle">logout</span> Đăng xuất</a></aside>
<main class="main">
<div class="d-flex justify-content-between align-items-center mb-4"><div><div class="text-uppercase small fw-bold text-danger">Giao hàng online</div><h1 class="h3 fw-bold mb-0">Đơn giao hàng</h1></div><a class="btn btn-light" href="${ctx}/delivery">Làm mới</a></div>
<c:if test="${not empty deliveryMessage}"><div class="alert alert-success"><c:out value="${deliveryMessage}"/></div></c:if>
<c:if test="${not empty deliveryError}"><div class="alert alert-danger"><c:out value="${deliveryError}"/></div></c:if>

<div class="row g-3">
<c:forEach var="o" items="${deliveryOrders}">
  <div class="col-xl-6"><section class="cardx h-100">
    <div class="d-flex justify-content-between align-items-center"><span class="code">#<c:out value="${o.orderCode}"/></span><span class="badge rounded-pill text-bg-light"><c:out value="${o.statusLabel}"/></span></div>
    <hr><h2 class="h6 fw-bold"><c:out value="${o.recipientName}"/> • <c:out value="${o.recipientPhone}"/></h2>
    <p class="mb-1"><b>Địa chỉ:</b> <c:out value="${o.deliveryAddress}"/></p>
    <c:if test="${not empty o.orderNote}"><p class="p-2 bg-warning-subtle rounded"><b>Ghi chú:</b> <c:out value="${o.orderNote}"/></p></c:if>
    <div class="d-flex justify-content-between my-3"><span>Phương thức: <b>${o.paymentMethod == 'CASH' ? 'COD' : 'Đã thanh toán QR'}</b></span><span class="money"><fmt:formatNumber value="${o.finalAmount}" pattern="#,##0"/>đ</span></div>
    <div class="d-flex gap-2 flex-wrap">
      <c:if test="${o.status == 'READY' && empty o.shipperId}">
        <form method="post"><input type="hidden" name="action" value="claim"><input type="hidden" name="orderId" value="${o.id}"><button class="btn btn-aura">Nhận đơn</button></form>
      </c:if>
      <c:if test="${o.status == 'READY' && o.shipperId == sessionScope.user.id}">
        <form method="post"><input type="hidden" name="action" value="start"><input type="hidden" name="orderId" value="${o.id}"><button class="btn btn-aura">Bắt đầu giao</button></form>
      </c:if>
      <c:if test="${o.status == 'DELIVERING'}">
        <form method="post"><input type="hidden" name="action" value="complete"><input type="hidden" name="orderId" value="${o.id}">
          <c:if test="${o.paymentMethod == 'CASH'}"><input type="hidden" name="collectedAmount" value="<fmt:formatNumber value='${o.finalAmount}' pattern='0'/>"></c:if>
          <button class="btn btn-success fw-bold">Giao thành công<c:if test="${o.paymentMethod == 'CASH'}"> & thu COD</c:if></button>
        </form>
      </c:if>
      <button class="btn btn-outline-danger" type="button" data-bs-toggle="collapse" data-bs-target="#incident${o.id}">Báo sự cố</button>
    </div>
    <div class="collapse mt-3" id="incident${o.id}"><form method="post" class="border rounded p-3">
      <input type="hidden" name="action" value="incident"><input type="hidden" name="orderId" value="${o.id}">
      <select name="incidentType" class="form-select mb-2" required><option value="CUSTOMER_UNAVAILABLE">Không liên hệ được khách</option><option value="WRONG_ADDRESS">Sai địa chỉ</option><option value="DAMAGED">Hàng hỏng</option><option value="PAYMENT_PROBLEM">Vấn đề thanh toán</option><option value="OTHER">Khác</option></select>
      <textarea name="description" class="form-control mb-2" maxlength="1000" required placeholder="Mô tả chi tiết"></textarea><button class="btn btn-danger">Gửi sự cố</button>
    </form></div>
  </section></div>
</c:forEach>
<c:if test="${empty deliveryOrders}"><div class="col-12"><div class="cardx text-center muted py-5">Hiện không có đơn giao hàng phù hợp.</div></div></c:if>
</div>

<section class="cardx mt-4"><h2 class="h5 fw-bold">Đối soát COD</h2><div class="table-responsive"><table class="table align-middle"><thead><tr><th>Đơn</th><th>Số tiền</th><th>Trạng thái</th><th>Thu lúc</th></tr></thead><tbody>
<c:forEach var="cod" items="${codRecords}"><tr><td>#${cod.orderId()}</td><td><fmt:formatNumber value="${cod.amount()}" pattern="#,##0"/>đ</td><td>${cod.status()}</td><td>${cod.collectedAt()}</td></tr></c:forEach>
<c:if test="${empty codRecords}"><tr><td colspan="4" class="text-center muted">Chưa có giao dịch COD.</td></tr></c:if>
</tbody></table></div></section>
</main></div><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script></body></html>
