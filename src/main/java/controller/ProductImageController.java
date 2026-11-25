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
    // [SPAN02]
    // . Trong doGet của ProductImageController, em nhận mã món rồi gọi getProductImage của ProductDAO để đọc
    //   ảnh đang lưu. Controller dùng nội dung và loại ảnh nhận được để trả ảnh cho trình duyệt.
    //
    // Tra tiếp: ProductDAO.java / SPAN03.
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
