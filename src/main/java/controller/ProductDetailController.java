package controller;

import dao.ProductDAO;
import model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * ProductDetailController - Hiển thị trang chi tiết sản phẩm
 * URL: /product-detail?id=1
 */
@WebServlet(name = "ProductDetailController", urlPatterns = {"/product-detail"})
public class ProductDetailController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id");
        // MN11: . Ở đây em nhận mã món khách muốn xem ở trang chi tiết riêng.
        // .. Em đọc món đó và các món liên quan rồi gửi sang trang hiển thị.
        // ... ProductDAO.getAvailableProductById() [MN10] → request → product-detail.jsp [MN12].

        // Validate tham số id
        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        try {
            int productId = Integer.parseInt(idParam.trim());
            // BF11: nhận ID từ URL -> DAO chỉ trả món còn bán trong danh mục ACTIVE -> gán request -> product-detail.jsp.
            Product product = productDAO.getAvailableProductById(productId);

            if (product == null) {
                // Sản phẩm không tồn tại -> chuyển về menu
                request.setAttribute("errorMsg", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
                response.sendRedirect(request.getContextPath() + "/menu");
                return;
            }

            // Lấy danh sách sản phẩm liên quan (cùng danh mục, loại trừ sản phẩm hiện tại)
            List<Product> relatedProducts = productDAO.getRelatedProducts(product.getCategoryId(), productId, 4);

            // Gán thuộc tính gửi sang View
            request.setAttribute("product", product);
            request.setAttribute("relatedProducts", relatedProducts);

            // Forward tới trang chi tiết
            request.getRequestDispatcher("/product-detail.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/menu");
        }
    }
}
