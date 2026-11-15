<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- . Đọc lỗi từ request/session và xóa bản lưu trong session sau khi đọc; c:out bảo vệ nội dung hiển thị.
    ... account-validation.js đọc payload để điền lại form và báo lỗi từng trường. --%>
<style>
    .is-invalid { border: 1px solid #dc3545 !important; }
    .invalid-feedback { color: #dc3545; }
    .account-password-toggle { display: inline-flex; align-items: center; justify-content: center; }
    .account-password-group { display: flex; flex-wrap: nowrap; }
    .account-password-group > input { flex: 1 1 0%; width: 1%; min-width: 0; }
    .account-password-group > .account-password-toggle { flex: 0 0 auto; }
    /* Edge also adds a reveal button; keep the application's single toggle. */
    input[data-password-field]::-ms-reveal { display: none; }
</style>

<%
    if (request.getAttribute("accountFeedback") == null) {
        request.setAttribute("accountFeedback", session.getAttribute("accountFeedback"));
    }
    session.removeAttribute("accountFeedback");
%>
<c:if test="${not empty accountFeedback}">
    <div hidden data-account-feedback data-form="<c:out value='${accountFeedback.form}'/>">
        <c:forEach var="entry" items="${accountFeedback.errors}">
            <span data-field="<c:out value='${entry.key}'/>" data-error="<c:out value='${entry.value}'/>"></span>
        </c:forEach>
        <c:forEach var="entry" items="${accountFeedback.values}">
            <span data-value-field="<c:out value='${entry.key}'/>" data-value="<c:out value='${entry.value}'/>"></span>
        </c:forEach>
    </div>
</c:if>
<script src="${pageContext.request.contextPath}/assets/js/account-validation.js" defer></script>
