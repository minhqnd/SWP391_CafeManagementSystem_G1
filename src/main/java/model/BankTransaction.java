package model;

import java.io.Serializable;
import java.text.DecimalFormat;

public class BankTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private long sepayId;
    private String gateway;
    private String accountNumber;
    private String transferType;
    private double transferAmount;
    private String content;
    private String referenceCode;
    private String transactionDate;
    private String matchedOrderCode;
    private String reconcileStatus; // MATCHED, OVERPAID, UNDERPAID, DUPLICATE, ORDER_NOT_FOUND, ORDER_CANCELLED, NO_CODE
    private String reconcileNote;
    private double orderFinalAmount;
    private double diffAmount;
    private String orderStatus;
    private String recipientName;

    public BankTransaction() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getSepayId() { return sepayId; }
    public void setSepayId(long sepayId) { this.sepayId = sepayId; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }

    public double getTransferAmount() { return transferAmount; }
    public void setTransferAmount(double transferAmount) { this.transferAmount = transferAmount; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getMatchedOrderCode() { return matchedOrderCode; }
    public void setMatchedOrderCode(String matchedOrderCode) { this.matchedOrderCode = matchedOrderCode; }

    public String getReconcileStatus() { return reconcileStatus; }
    public void setReconcileStatus(String reconcileStatus) { this.reconcileStatus = reconcileStatus; }

    public String getReconcileNote() { return reconcileNote; }
    public void setReconcileNote(String reconcileNote) { this.reconcileNote = reconcileNote; }

    public double getOrderFinalAmount() { return orderFinalAmount; }
    public void setOrderFinalAmount(double orderFinalAmount) { this.orderFinalAmount = orderFinalAmount; }

    public double getDiffAmount() { return diffAmount; }
    public void setDiffAmount(double diffAmount) { this.diffAmount = diffAmount; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getFormattedAmount() {
        return "+" + new DecimalFormat("###,###,###").format(transferAmount).replace(",", ".") + " đ";
    }

    public String getFormattedOrderAmount() {
        return new DecimalFormat("###,###,###").format(orderFinalAmount).replace(",", ".") + " đ";
    }

    public String getFormattedDiff() {
        return new DecimalFormat("###,###,###").format(Math.abs(diffAmount)).replace(",", ".") + " đ";
    }
}
