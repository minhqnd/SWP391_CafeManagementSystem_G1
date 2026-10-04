import dao.VoucherDAO;
import model.Voucher;

public class TestVoucherDAO {
    public static void main(String[] args) {
        VoucherDAO dao = new VoucherDAO();
        Voucher v1 = dao.getUsableVoucherByCode("RWD9BNM829", 4);
        if (v1 != null) {
            System.out.println("Found RWD9BNM829 for customer 4!");
        } else {
            System.out.println("RWD9BNM829 NOT FOUND for customer 4!");
        }
        
        Voucher v2 = dao.getUsableVoucherByCode("RWD9BNM829", null);
        if (v2 != null) {
            System.out.println("Found RWD9BNM829 for customer null!");
        } else {
            System.out.println("RWD9BNM829 NOT FOUND for customer null!");
        }
    }
}
