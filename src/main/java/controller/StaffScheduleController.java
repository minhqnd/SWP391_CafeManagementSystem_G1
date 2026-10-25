package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: load schedules for the authenticated employee only. */
@WebServlet(name="StaffScheduleController", urlPatterns={"/staff/schedule"})
public class StaffScheduleController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireStaff(r,p))return; r.getRequestDispatcher("/staff-schedule.jsp").forward(r,p);
 }
}
