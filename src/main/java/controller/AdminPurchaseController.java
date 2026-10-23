package controller;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.IOException;
/** TODO: load purchase orders, create PO, change status and receive inventory transactionally. */
@WebServlet(name="AdminPurchaseController", urlPatterns={"/admin/purchases"})
public class AdminPurchaseController extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
  if(!ScaffoldAccess.requireAdmin(r,p))return; r.getRequestDispatcher("/admin-purchases.jsp").forward(r,p);
 }
}
