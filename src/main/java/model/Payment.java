package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * Entity Payment - Bám sát mục 3.2 Entity Descriptions trong tài liệu SRS.
 * Ghi nhận lịch sử giao dịch và phương thức thanh toán cho đơn hàng.
 */
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private String paymentMethod; // CASH, BANK_TRANSFER, VNPAY, MOMO
    private String transactionCode;
    private double amount;
    private String status;        // PENDING, SUCCESS, FAILED
    private Timestamp paidAt;
    private String paymentNote;

    public Payment() {
    }

    public Payment(int id, int orderId, String paymentMethod, String transactionCode,
                   double amount, String status, Timestamp paidAt, String paymentNote) {
        this.id = id;
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.transactionCode = transactionCode;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.paymentNote = paymentNote;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Timestamp paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentNote() {
        return paymentNote;
    }

    public void setPaymentNote(String paymentNote) {
        this.paymentNote = paymentNote;
    }

    // Helper formatters
    public String getFormattedAmount() {
        return new DecimalFormat("###,###,###").format(amount).replace(",", ".") + " đ";
    }

    public String getFormattedPaidAt() {
        if (paidAt == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(paidAt);
    }

    public String getPaymentMethodDisplay() {
        if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod) || "VIETQR".equalsIgnoreCase(paymentMethod)) {
            return "Chuyển khoản VietQR (Vietcombank)";
        } else if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            return "Ví điện tử MoMo";
        } else if ("CARD".equalsIgnoreCase(paymentMethod) || "VNPAY".equalsIgnoreCase(paymentMethod)) {
            return "Thẻ quốc tế (Visa/Mastercard/VNPAY)";
        } else if ("CASH".equalsIgnoreCase(paymentMethod) || "COD".equalsIgnoreCase(paymentMethod)) {
            return "Tiền mặt khi nhận hàng (COD)";
        }
        return paymentMethod != null ? paymentMethod : "Chuyển khoản";
    }
}
