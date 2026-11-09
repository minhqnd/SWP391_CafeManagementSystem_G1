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
    private String reconcileStatus; // MATCHED, OVERPAID / DUPLICATE, UNMATCHED

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

    public String getFormattedAmount() {
        return "+" + new DecimalFormat("###,###,###").format(transferAmount).replace(",", ".") + " đ";
    }
}
