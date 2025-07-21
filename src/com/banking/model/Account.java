// Account.java
package com.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Account {
    private final String accountId;
    private final String accountNumber;
    private final String ownerName;
    private final String bankName;
    private BigDecimal balance;
    private final LocalDateTime creationDate;

    public Account(String accountId, String accountNumber, String ownerName, String bankName, BigDecimal balance) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.bankName = bankName;
        this.balance = balance;
        this.creationDate = LocalDateTime.now();
    }

    // Getters
    public String getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getBankName() { return bankName; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreationDate() { return creationDate; }

    // Setters
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return Objects.equals(accountId, account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return String.format("Account{id='%s', number='%s', owner='%s', bank='%s', balance=%s}",
                accountId, accountNumber, ownerName, bankName, balance);
    }
}

