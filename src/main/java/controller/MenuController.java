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
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "MenuController", urlPatterns = {"/menu"})
public class MenuController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Xử lý lấy chi tiết sản phẩm qua JSON API cho Modal popup tại Menu
        if ("detail".equals(request.getParameter("action"))) {
            handleProductDetailApi(request, response);
            return;
        }

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

    private void handleProductDetailApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing product ID\"}");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            Product product = productDAO.getProductById(id);

            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Product not found\"}");
                return;
            }

            // Xuất dữ liệu JSON an toàn
            StringBuilder json = new StringBuilder("{");
            json.append("\"id\":").append(product.getId()).append(",");
            json.append("\"name\":").append(escapeJson(product.getName())).append(",");
            json.append("\"categoryName\":").append(escapeJson(product.getCategoryName())).append(",");
            json.append("\"basePrice\":").append(product.getBasePrice()).append(",");
            json.append("\"formattedPrice\":").append(escapeJson(product.getFormattedPrice())).append(",");
            json.append("\"description\":").append(escapeJson(product.getDescription() != null ? product.getDescription() : "")).append(",");
            json.append("\"imageUrl\":").append(escapeJson(product.getDisplayImageUrl() != null ? product.getDisplayImageUrl() : "")).append(",");
            json.append("\"isAvailable\":").append(product.isAvailable()).append(",");
            json.append("\"variantsConfig\":").append(product.getVariantsConfig() != null && !product.getVariantsConfig().isEmpty() ? product.getVariantsConfig() : "{}");
            json.append("}");

            out.print(json.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
