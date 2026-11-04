package controller;

import dao.CategoryDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.CategoryImage;

import java.io.IOException;

// BF11 - ĐỌC ẢNH: GET /category-image?id=... trả BLOB ảnh từ category_images, không trả trang HTML.
@WebServlet(name = "CategoryImageController", urlPatterns = {"/category-image"})
public class CategoryImageController extends HttpServlet {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int categoryId;
        try {
            categoryId = Integer.parseInt(request.getParameter("id"));
            if (categoryId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        CategoryImage image;
        try {
            image = categoryDAO.getCategoryImage(categoryId);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load category image " + categoryId, e);
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
