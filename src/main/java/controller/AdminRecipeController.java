package controller;

import dao.ProductIngredientDAO;
import model.ProductIngredient;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AdminRecipeController", urlPatterns = {"/admin/recipe/update"})
public class AdminRecipeController extends HttpServlet {

    private final ProductIngredientDAO productIngredientDAO = new ProductIngredientDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            boolean isOnline = request.getParameter("isAvailableOnline") != null;
            
            // Theo SRS, Recipe cần lưu "instructions". Vì DB không có cột này,
            // ta linh hoạt lưu nó vào variants_config (cột JSON) luôn để đúng nghiệp vụ.
            String instructions = request.getParameter("instructions");
            if (instructions == null) instructions = "";
            
            // Xử lý Escape chuỗi JSON đơn giản (tránh lỗi ngắt ngoặc kép)
            instructions = instructions.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            
            String variantsConfig = String.format("{\"isAvailableOnline\": %b, \"instructions\": \"%s\"}", isOnline, instructions);
            
            String[] ingredientIds = request.getParameterValues("ingredientId[]");
            String[] quantities = request.getParameterValues("quantity[]");
            
            List<ProductIngredient> ingredients = new ArrayList<>();
            if (ingredientIds != null && quantities != null) {
                for (int i = 0; i < ingredientIds.length; i++) {
                    if (!ingredientIds[i].isEmpty() && !quantities[i].isEmpty()) {
                        int ingId = Integer.parseInt(ingredientIds[i]);
                        double qty = Double.parseDouble(quantities[i]);
                        ingredients.add(new ProductIngredient(productId, ingId, qty, null, null));
                    }
                }
            }
            
            productIngredientDAO.saveRecipe(productId, variantsConfig, ingredients);
            
            request.getSession().setAttribute("successMsg", "Đã cập nhật công thức và Hướng dẫn pha chế (SOP) thành công!");
            response.sendRedirect(request.getContextPath() + "/admin-dashboard.jsp"); 
            
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin-dashboard.jsp");
        }
    }
}
