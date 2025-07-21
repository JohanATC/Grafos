// Transaction.java
package com.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {
    private final String transactionId;
    private final com.banking.model.Account sourceAccount;
    private final com.banking.model.Account destinationAccount;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final String description;
    private final String transactionType;
    private final TransactionStatus status;

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }

    public Transaction(String transactionId, com.banking.model.Account sourceAccount, com.banking.model.Account destinationAccount,
                       BigDecimal amount, String description, String transactionType) {
        this.transactionId = transactionId;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.description = description;
        this.transactionType = transactionType;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.COMPLETED;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public com.banking.model.Account getSourceAccount() { return sourceAccount; }
    public com.banking.model.Account getDestinationAccount() { return destinationAccount; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getTransactionType() { return transactionType; }
    public TransactionStatus getStatus() { return status; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', from='%s', to='%s', amount=%s, date=%s}",
                transactionId, sourceAccount.getAccountId(),
                destinationAccount.getAccountId(), amount, timestamp);
    }
}