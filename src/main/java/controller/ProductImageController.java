package controller;

import dao.ProductDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ProductImage;

import java.io.IOException;

@WebServlet(name = "ProductImageController", urlPatterns = {"/product-image"})
public class ProductImageController extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    // [SPAN03]
    // . Khi thẻ img cần ảnh sản phẩm, doGet trong ProductImageController.java nhận mã món. Em gọi
    //   getProductImage trong ProductDAO.java bằng mã đó để đọc ảnh rồi gửi nội dung về trình duyệt, giúp
    //   ảnh hiển thị đúng sản phẩm.
    // ... Kết thúc tải ảnh khi nội dung ảnh được trả về thẻ img của đúng món; nếu ảnh không tải được thì
    //     cách hiển thị thay thế do trang đang xem quyết định.
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
