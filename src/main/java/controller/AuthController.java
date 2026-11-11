package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "AuthController", urlPatterns = {"/auth"})
// AU00: . Ở đây em để sẵn khung xử lý tài khoản cho đường dẫn này.
// .. Các hàm hiện còn trống, form thật chưa dùng đường dẫn này.
// Tham khảo: Muốn lần luồng đang chạy thì mở đăng nhập [LG01], đăng ký [RG01] hoặc quên mật khẩu [OT01].
public class AuthController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO: Xử lý hiển thị trang đăng nhập, đăng ký, đăng xuất
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO: Xử lý đăng nhập, đăng ký tài khoản
    }
}
