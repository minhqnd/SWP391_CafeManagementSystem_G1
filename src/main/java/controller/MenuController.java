package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "MenuController", urlPatterns = {"/menu"})
public class MenuController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy danh mục sản phẩm kèm số lượng món
        List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
        int totalProducts = productDAO.getTotalAvailableCount();

        // 2. Nhận tham số lọc và tìm kiếm
        String categoryParam = request.getParameter("category");
        String keywordParam = request.getParameter("keyword");

        Integer categoryId = null;
        if (categoryParam != null && !categoryParam.trim().isEmpty()) {
            try {
                categoryId = Integer.parseInt(categoryParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String keyword = null;
        if (keywordParam != null && !keywordParam.trim().isEmpty()) {
            keyword = keywordParam.trim();
        }

        // 3. Truy vấn danh sách món ăn đã lọc
        List<Product> products = productDAO.filterProducts(categoryId, keyword);

        // 4. Gán thuộc tính gửi sang view menu.jsp
        request.setAttribute("categories", categories);
        request.setAttribute("products", products);
        request.setAttribute("totalCount", totalProducts);
        request.setAttribute("currentCategory", categoryId);
        request.setAttribute("currentKeyword", keyword != null ? keyword : "");

        // 5. Chuyển tiếp tới giao diện menu.jsp
        request.getRequestDispatcher("/menu.jsp").forward(request, response);
    }
}
