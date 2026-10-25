package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: validate date filters and bind ReportDashboard aggregates. */
@WebServlet(name="AdminReportsController", urlPatterns={"/admin/reports"})
public class AdminReportsController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireAdmin(r,p))return; r.getRequestDispatcher("/admin-reports.jsp").forward(r,p);
 }
}
