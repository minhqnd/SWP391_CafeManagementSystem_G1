package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: load attendance history for the authenticated employee only. */
@WebServlet(name="StaffAttendanceController", urlPatterns={"/staff/attendance"})
public class StaffAttendanceController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireStaff(r,p))return; r.getRequestDispatcher("/staff-attendance.jsp").forward(r,p);
 }
}
