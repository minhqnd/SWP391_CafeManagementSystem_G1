package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: search customer by phone, load earn/redeem/refund history and configurable point rules. */
@WebServlet(name="AdminLoyaltyController", urlPatterns={"/admin/loyalty"})
public class AdminLoyaltyController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireAdmin(r,p))return; r.getRequestDispatcher("/admin-loyalty.jsp").forward(r,p);
 }
}
