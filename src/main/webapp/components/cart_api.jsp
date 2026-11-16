<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="model.Cart, model.CartItem, java.util.List" %>
<%
    Cart cart = (Cart) session.getAttribute(Cart.SESSION_KEY);
    if (cart == null) {
        cart = new Cart();
        session.setAttribute(Cart.SESSION_KEY, cart);
    }

    String action = request.getParameter("action");
    String lineIdStr = request.getParameter("lineId");
    int lineId = -1;
    if (lineIdStr != null && !lineIdStr.isEmpty()) {
        try {
            lineId = Integer.parseInt(lineIdStr);
        } catch (Exception ignored) {}
    }

    if ("increase".equals(action)) {
        cart.changeQuantity(lineId, 1);
    } else if ("decrease".equals(action)) {
        CartItem item = cart.findItem(lineId);
        if (item != null && item.getQuantity() <= 1) {
            cart.removeItem(lineId);
        } else {
            cart.changeQuantity(lineId, -1);
        }
    } else if ("remove".equals(action)) {
        cart.removeItem(lineId);
    } else if ("clear".equals(action)) {
        cart.clear();
    }

    double totalAmount = 0;
    for (CartItem it : cart.getItems()) {
        totalAmount += it.getSubtotal();
    }
%>
{
  "success": true,
  "totalQuantity": <%= cart.getTotalQuantity() %>,
  "totalAmount": <%= (long) totalAmount %>,
  "items": [
  <%
      List<CartItem> items = cart.getItems();
      for (int i = 0; i < items.size(); i++) {
          CartItem it = items.get(i);
          if (i > 0) out.print(",");
          String pName = it.getProductName() != null ? it.getProductName().replace("\\", "\\\\").replace("\"", "\\\"") : "";
          String img = it.getImageUrl() != null ? it.getImageUrl().replace("\\", "\\\\").replace("\"", "\\\"") : "";
          String sz = it.getSize() != null ? it.getSize().replace("\\", "\\\\").replace("\"", "\\\"") : "";
          String szLbl = it.getSizeLabel() != null ? it.getSizeLabel().replace("\\", "\\\\").replace("\"", "\\\"") : "";
  %>
    {
      "lineId": <%= it.getLineId() %>,
      "productId": <%= it.getProductId() %>,
      "name": "<%= pName %>",
      "imageUrl": "<%= img %>",
      "size": "<%= sz %>",
      "sizeLabel": "<%= szLbl %>",
      "unitPrice": <%= (long) it.getUnitPrice() %>,
      "quantity": <%= it.getQuantity() %>,
      "subtotal": <%= (long) it.getSubtotal() %>,
      "toppings": [
      <%
          List<String> tops = it.getToppings();
          for (int j = 0; j < tops.size(); j++) {
              if (j > 0) out.print(",");
              String tName = tops.get(j).replace("\\", "\\\\").replace("\"", "\\\"");
      %>
        "<%= tName %>"
      <% } %>
      ]
    }
  <% } %>
  ]
}
