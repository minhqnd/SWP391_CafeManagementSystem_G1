<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
<%-- . Đọc accountFeedback từ request/session, xóa bản session sau khi đọc và xuất payload ẩn.
     .. c:out bảo vệ nội dung; payload chứa form, lỗi và các giá trị an toàn.
     ... account-validation.js phần đọc payload trong account-validation.js đọc payload → feedback() trong account-validation.js tô đỏ đúng ô. --%>
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
