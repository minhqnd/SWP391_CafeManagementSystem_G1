<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<style>
    .is-invalid { border: 1px solid #dc3545 !important; }
    .invalid-feedback { color: #dc3545; }
    .account-password-toggle { display: inline-flex; align-items: center; justify-content: center; }
    .account-password-group { display: flex; flex-wrap: nowrap; }
    .account-password-group > input { flex: 1 1 0%; width: 1%; min-width: 0; }
    .account-password-group > .account-password-toggle { flex: 0 0 auto; }

    input[data-password-field]::-ms-reveal { display: none; }
</style>

<%
    if (request.getAttribute("accountFeedback") == null) {
        request.setAttribute("accountFeedback", session.getAttribute("accountFeedback"));
    }
    session.removeAttribute("accountFeedback");
%>

<%-- [BMTV02]
     . Ở account_feedback.jsp, em nhận accountFeedback mà controller chuẩn bị qua AccountFormFeedback.flash
       (giữ lỗi và các ô qua lần chuyển trang) hoặc show (hiện lỗi trên trang đang mở). Em đưa tên form,
       lỗi và giá trị vào data-account-feedback để account-validation.js tìm đúng các ô, điền lại và hiển
       thị lỗi khi trang mở ra.
--%>
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
