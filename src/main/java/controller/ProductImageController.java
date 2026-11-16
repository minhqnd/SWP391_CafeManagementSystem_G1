package controller;

import dao.ProductDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ProductImage;

import java.io.IOException;

// BF11 - ĐỌC ẢNH: thẻ img gọi GET /product-image?id=... -> DAO đọc BLOB -> response ghi byte ảnh, không forward JSP.
@WebServlet(name = "ProductImageController", urlPatterns = {"/product-image"})
public class ProductImageController extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int productId;
        try {
            productId = Integer.parseInt(request.getParameter("id"));
            if (productId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        ProductImage image;
        try {
            // IM03: . Ở đây em nhận yêu cầu tải ảnh món và kiểm tra mã món.
            // .. Có ảnh thì trả nội dung ảnh cho trình duyệt; không có thì báo không tìm thấy.
            // ... productDAO.getProductImage(mã món) [IM04] → ghi byte ảnh vào response.
            image = productDAO.getProductImage(productId);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load product image " + productId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (image == null || image.getData() == null || image.getData().length == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(image.getContentType());
        response.setContentLength(image.getData().length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(image.getData());
    }
}
