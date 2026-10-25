package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: list/filter customer_reviews and save admin_response/status. */
@WebServlet(name="AdminFeedbackController", urlPatterns={"/admin/feedback"})
public class AdminFeedbackController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireAdmin(r,p))return; r.getRequestDispatcher("/admin-feedback.jsp").forward(r,p);
 }
}
