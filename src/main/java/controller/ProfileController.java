package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

@WebServlet(name = "ProfileController", urlPatterns = {"/profile"})
public class ProfileController extends HttpServlet {

    private void calculateLoyaltyData(HttpServletRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        int points = user.getTotalPoints();
        String nextTier = "Vàng";
        String currentDisplayTier = "SILVER";
        int nextPoints = 300;

        if (points < 300) {
            currentDisplayTier = "MEMBER";
            nextTier = "Vàng";
            nextPoints = 300;
        } else if (points < 800) {
            currentDisplayTier = "GOLD";
            nextTier = "Kim Cương";
            nextPoints = 800;
        } else if (points < 1500) {
            currentDisplayTier = "DIAMOND";
            nextTier = "VIP";
            nextPoints = 1500;
        } else {
            currentDisplayTier = "VIP";
            nextTier = "Tối đa";
            nextPoints = points;
        }

        int pointsNeeded = nextPoints - points;
        int progressPercent = (int) Math.round((double) points / nextPoints * 100);
        if (progressPercent > 100) progressPercent = 100;

        request.setAttribute("pointsNeeded", pointsNeeded);
        request.setAttribute("nextTierName", nextTier);
        request.setAttribute("currentDisplayTier", currentDisplayTier);
        request.setAttribute("nextPoints", nextPoints);
        request.setAttribute("progressPercent", progressPercent);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // Require login
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        calculateLoyaltyData(request, session);
        
        dao.UserDAO userDAO = new dao.UserDAO();
        java.util.List<model.LoyaltyTransaction> history = userDAO.getLoyaltyHistoryByUserId(((User)session.getAttribute("user")).getId());
        request.setAttribute("loyaltyHistory", history);

        dao.RewardDAO rewardDAO = new dao.RewardDAO();
        java.util.List<model.RewardItem> rewards = rewardDAO.getAllRewards();
        request.setAttribute("rewardsList", rewards);

        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        User user = (User) session.getAttribute("user");
        
        String action = request.getParameter("action");
        if ("update".equals(action)) {
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            if (phone == null) {
                phone = user.getPhone(); // Giữ nguyên sđt cũ do đã ẩn field này
            }
            String email = request.getParameter("email");
            if (email == null) {
                email = user.getEmail(); // Giữ nguyên email
            }
            String birthday = request.getParameter("birthday");
            String address = request.getParameter("address");
            
            // Backend validation
            if (fullName == null || fullName.trim().isEmpty()) {
                request.setAttribute("errorMsg", "Họ và tên không được để trống!");
            } else if (phone != null && !phone.trim().isEmpty() && !phone.matches("^[0-9]{9,15}$")) {
                request.setAttribute("errorMsg", "Số điện thoại không hợp lệ!");
            } else if (email != null && !email.trim().isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                request.setAttribute("errorMsg", "Địa chỉ email không hợp lệ!");
            } else {
                dao.UserDAO userDAO = new dao.UserDAO();
                String updateResult = userDAO.updateUserProfile(user.getId(), fullName, phone, email, birthday, address);
                
                if ("success".equals(updateResult)) {
                    user.setFullName(fullName);
                    user.setPhone(phone);
                    user.setEmail(email);
                    user.setBirthday(birthday);
                    user.setAddress(address);
                    session.setAttribute("user", user);
                    request.setAttribute("successMsg", "Cập nhật thông tin thành công!");
                } else {
                    request.setAttribute("errorMsg", updateResult);
                }
            }
        } else if ("changePassword".equals(action)) {
            String oldPassword = request.getParameter("oldPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (newPassword == null || newPassword.length() < 6) {
                request.setAttribute("errorMsg", "Mật khẩu mới phải dài ít nhất 6 ký tự!");
            } else if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("errorMsg", "Mật khẩu xác nhận không khớp!");
            } else {
                dao.UserDAO userDAO = new dao.UserDAO();
                // Verify old password by trying to log in
                if (userDAO.login(user.getEmail(), oldPassword) != null) {
                    boolean success = userDAO.updatePassword(user.getEmail(), newPassword);
                    if (success) {
                        request.setAttribute("successMsg", "Đổi mật khẩu thành công!");
                    } else {
                        request.setAttribute("errorMsg", "Đổi mật khẩu thất bại. Vui lòng thử lại!");
                    }
                } else {
                    request.setAttribute("errorMsg", "Mật khẩu hiện tại không đúng!");
                }
            }
        } else if ("redeemReward".equals(action)) {
            try {
                int rewardId = Integer.parseInt(request.getParameter("rewardId"));
                dao.RewardDAO rewardDAO = new dao.RewardDAO();
                String voucherCode = rewardDAO.redeemReward(user.getId(), rewardId);
                if (voucherCode != null) {
                    user.setTotalPoints(new dao.UserDAO().getTotalPoints(user.getId()));
                    session.setAttribute("user", user);
                    request.setAttribute("successMsg", "Đổi thưởng thành công! Voucher " + voucherCode
                            + " đã được thêm vào tài khoản, chọn mã này ở Giỏ hàng để được giảm giá.");
                } else {
                    request.setAttribute("errorMsg", "Đổi thưởng thất bại. Có thể bạn không đủ điểm hoặc phần quà đã hết!");
                }
            } catch (Exception e) {
                request.setAttribute("errorMsg", "Lỗi xử lý đổi thưởng.");
            }
        }
        
        calculateLoyaltyData(request, session);
        
        java.util.List<model.LoyaltyTransaction> history = new dao.UserDAO().getLoyaltyHistoryByUserId(((User)session.getAttribute("user")).getId());
        request.setAttribute("loyaltyHistory", history);
        
        dao.RewardDAO rewardDAO = new dao.RewardDAO();
        java.util.List<model.RewardItem> rewards = rewardDAO.getAllRewards();
        request.setAttribute("rewardsList", rewards);
        
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }
}
