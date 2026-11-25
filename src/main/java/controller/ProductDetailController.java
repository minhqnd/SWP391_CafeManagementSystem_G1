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
    // [MNTHE02]
    // . Em dùng ProductDetailController nhận mã món rồi đọc sản phẩm đang được bán để chuẩn bị trang chi
    //   tiết.
    //
    // Tra tiếp: ProductDAO.java / MNCT04.
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id");

        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        try {
            int productId = Integer.parseInt(idParam.trim());

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
